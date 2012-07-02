package com.ferox.scene.controller.ffp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Future;

import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Framework;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Surface;
import com.ferox.renderer.Task;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Filter;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.Texture.WrapMode;
import com.ferox.scene.Camera;
import com.ferox.scene.controller.PVSResult;
import com.ferox.scene.controller.light.LightGroupResult;
import com.ferox.util.Bag;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.Result;
import com.lhkbob.entreri.SimpleController;

public class FixedFunctionRenderController extends SimpleController {
    private final Framework framework;
    private final TextureSurface shadowMap;
    
    private final int shadowmapTextureUnit;
    private final int diffuseTextureUnit;
    private final int emissiveTextureUnit;
    
    private List<PVSResult> pvs;
    private LightGroupResult lightGroups;
    
    private final Queue<Future<Void>> previousFrame;

    public FixedFunctionRenderController(Framework framework) {
        this(framework, 512);
    }

    public FixedFunctionRenderController(Framework framework, int shadowMapSize) {
        if (framework == null)
            throw new NullPointerException("Framework cannot be null");
        
        RenderCapabilities caps = framework.getCapabilities();
        if (!caps.hasFixedFunctionRenderer())
            throw new IllegalArgumentException("Framework must support a FixedFunctionRenderer");
        
        this.framework = framework;
        
        previousFrame = new ArrayDeque<Future<Void>>();
        
        int numTex = caps.getMaxFixedPipelineTextures();
        boolean shadowsRequested = shadowMapSize > 0; // size is positive
        boolean shadowSupport = (caps.getFboSupport() || caps.getPbufferSupport()) && 
                                numTex > 1 && caps.getDepthTextureSupport();
                        
        if (shadowsRequested && shadowSupport) {
            // convert size to a power of two
            int sz = 1;
            while(sz < shadowMapSize)
                sz = sz << 1;
            // create the shadow map
            TextureSurfaceOptions options = new TextureSurfaceOptions().setTarget(Target.T_2D)
                                                                       .setWidth(sz)
                                                                       .setHeight(sz)
                                                                       .setUseDepthTexture(true)
                                                                       .setColorBufferFormats();
            shadowMap = framework.createSurface(options);
            
            // set up the depth comparison
            Texture sm = shadowMap.getDepthBuffer();
            sm.setFilter(Filter.LINEAR);
            sm.setWrapMode(WrapMode.CLAMP);
            sm.setDepthCompareEnabled(true);
            sm.setDepthComparison(Comparison.LEQUAL);
            
            // use the 3rd unit if available, or the 2nd if not
            shadowmapTextureUnit = (numTex > 2 ? 2 : 1);
            // reserve one unit for the shadow map
            numTex--;
        } else {
            shadowMap = null;
            shadowmapTextureUnit = -1;
        }
        
        // FIXME should this be the responsibility of the TextureGroupFactory
        // I'm not sure because other factories might also need texture units
        if (numTex >= 2) {
            diffuseTextureUnit = 0;
            emissiveTextureUnit = 1;
        } else {
            // multiple passes for textures
            diffuseTextureUnit = 0;
            emissiveTextureUnit = 0;
        }
    }
    
    public static long blocktime = 0L;
    @Override
    @SuppressWarnings("unchecked")
    public void process(double dt) {
        // go through all results and render all camera frustums
        List<Future<Void>> thisFrame = new ArrayList<Future<Void>>();
        Camera camera = getEntitySystem().createDataInstance(Camera.ID);
        for (PVSResult visible: pvs) {
            if (visible.getSource().getTypeId() == Camera.ID) {
                camera.set((Component<Camera>) visible.getSource());
                thisFrame.add(render(camera.getSurface(), visible.getFrustum(), 
                                         visible.getPotentiallyVisibleSet()));
            }
        }
        
        // Block until previous frame is completed to prevent the main thread
        // from getting too ahead of the rendering thread.
        //  - We do the blocking at the end so that this thread finishes all 
        //    processing before waiting on the rendering thread.
        long now = System.nanoTime();
        while(!previousFrame.isEmpty()) {
            Future<Void> f = previousFrame.poll();
            try {
                f.get();
            } catch (Exception e) {
                throw new RuntimeException("Previous frame failed", e);
            }
        }
        blocktime += (System.nanoTime() - now);
        previousFrame.addAll(thisFrame);
    }
    
    public static long rendertime = 0L;
    private Future<Void> render(final Surface surface, final Frustum view, Bag<Entity> pvs) {
        GeometryGroupFactory geomGroup = new GeometryGroupFactory(getEntitySystem(), view.getViewMatrix());
//        TextureGroupFactory textureGroup = new TextureGroupFactory(getEntitySystem(), diffuseTextureUnit, emissiveTextureUnit, 
//                                                                   geomGroup);
        LightGroupFactory lightGroup = new LightGroupFactory(getEntitySystem(), lightGroups, 8, geomGroup);
        
        final StateNode rootNode = new StateNode(lightGroup.newGroup());
        for (Entity e: pvs) {
            rootNode.add(e);
        }
        
        Future<Void> future = framework.queue(new Task<Void>() {
            @Override
            public Void run(HardwareAccessLayer access) {
                long now = System.nanoTime();
                Context ctx = access.setActiveSurface(surface);
                if (ctx != null) {
                    FixedFunctionRenderer ffp = ctx.getFixedFunctionRenderer();
                    // FIXME clear color should be configurable somehow
                    ffp.clear(true, true, true, new Vector4(0.0, 0.0, 0.0, 1.0), 1f, 0);
                    ffp.setProjectionMatrix(view.getProjectionMatrix());
                    ffp.setModelViewMatrix(view.getViewMatrix());
                    
                    rootNode.render(ffp, new AppliedEffects(view.getViewMatrix()));
                    
                    ctx.flush();
                }
                rendertime += (System.nanoTime() - now);
                return null;
            }
        }, "main");
        return future;
    }
    
    @Override
    public void preProcess(double dt) {
        pvs = new ArrayList<PVSResult>();
        lightGroups = null;
    }
    
    @Override
    public void report(Result r) {
        if (r instanceof PVSResult) {
            pvs.add((PVSResult) r);
        } else if (r instanceof LightGroupResult) {
            lightGroups = (LightGroupResult) r;
        }
    }
}
