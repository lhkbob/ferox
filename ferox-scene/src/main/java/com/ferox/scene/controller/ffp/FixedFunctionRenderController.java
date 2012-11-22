/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.scene.controller.ffp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Future;

import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Framework;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Surface;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.AtmosphericFog;
import com.ferox.scene.BlinnPhongMaterial;
import com.ferox.scene.Camera;
import com.ferox.scene.DecalColorMap;
import com.ferox.scene.DiffuseColor;
import com.ferox.scene.DiffuseColorMap;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.EmittedColor;
import com.ferox.scene.EmittedColorMap;
import com.ferox.scene.Light;
import com.ferox.scene.PointLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.SpecularColor;
import com.ferox.scene.SpecularColorMap;
import com.ferox.scene.SpotLight;
import com.ferox.scene.Transform;
import com.ferox.scene.Transparent;
import com.ferox.scene.controller.PVSResult;
import com.ferox.scene.controller.light.LightGroupResult;
import com.ferox.util.Bag;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

public class FixedFunctionRenderController implements Task, ParallelAware {
    private static final Set<Class<? extends ComponentData<?>>> COMPONENTS;
    static {
        Set<Class<? extends ComponentData<?>>> types = new HashSet<Class<? extends ComponentData<?>>>();
        types.add(AmbientLight.class);
        types.add(DirectionLight.class);
        types.add(SpotLight.class);
        types.add(PointLight.class);
        types.add(Renderable.class);
        types.add(Transform.class);
        types.add(BlinnPhongMaterial.class);
        types.add(DiffuseColor.class);
        types.add(EmittedColor.class);
        types.add(SpecularColor.class);
        types.add(DiffuseColorMap.class);
        types.add(EmittedColorMap.class);
        types.add(SpecularColorMap.class);
        types.add(DecalColorMap.class);
        types.add(Transparent.class);
        types.add(Camera.class);
        types.add(AtmosphericFog.class);
        COMPONENTS = Collections.unmodifiableSet(types);
    }
    private final Framework framework;
    private final ShadowMapCache shadowMap;

    private final int shadowmapTextureUnit;
    private final int diffuseTextureUnit;
    private final int emissiveTextureUnit;
    private final int decalTextureUnit;

    private List<PVSResult> pvs;
    private LightGroupResult lightGroups;

    private final Queue<Future<Void>> previousFrame;

    public FixedFunctionRenderController(Framework framework) {
        this(framework, 1024);
    }

    public FixedFunctionRenderController(Framework framework, int shadowMapSize) {
        if (framework == null) {
            throw new NullPointerException("Framework cannot be null");
        }

        RenderCapabilities caps = framework.getCapabilities();
        if (!caps.hasFixedFunctionRenderer()) {
            throw new IllegalArgumentException("Framework must support a FixedFunctionRenderer");
        }

        this.framework = framework;

        previousFrame = new ArrayDeque<Future<Void>>();

        int numTex = caps.getMaxFixedPipelineTextures();
        boolean shadowsRequested = shadowMapSize > 0; // size is positive
        boolean shadowSupport = ((caps.getFboSupport() || caps.getPbufferSupport()) && numTex > 1 && caps.getDepthTextureSupport());

        if (shadowsRequested && shadowSupport) {
            // convert size to a power of two
            int sz = 1;
            while (sz < shadowMapSize) {
                sz = sz << 1;
            }
            shadowMap = new ShadowMapCache(framework, sz, sz);

            // use the 4th unit if available, or the last unit if we're under
            shadowmapTextureUnit = Math.max(numTex, 3) - 1;
            // reserve one unit for the shadow map
            numTex--;
        } else {
            shadowMap = null;
            shadowmapTextureUnit = -1;
        }

        if (numTex >= 3) {
            diffuseTextureUnit = 0;
            emissiveTextureUnit = 2;
            decalTextureUnit = 1;
        } else if (numTex == 2) {
            diffuseTextureUnit = 0;
            // merge emissive and decal units
            emissiveTextureUnit = 1;
            decalTextureUnit = 1;
        } else if (numTex == 1) {
            // merge all units
            diffuseTextureUnit = 0;
            emissiveTextureUnit = 0;
            decalTextureUnit = 0;
        } else {
            // disable texturing
            diffuseTextureUnit = -1;
            emissiveTextureUnit = -1;
            decalTextureUnit = -1;
        }
    }

    public static long blocktime = 0L;

    @Override
    @SuppressWarnings("unchecked")
    public Task process(EntitySystem system, Job job) {
        // FIXME normally we'd block for the previous frame after we built
        // the tree but before submitting it to the renderer, but this causes
        // problems with the shadow map cache (see below). So for now, I've
        // moved the blocking to the beginning

        // Block until previous frame is completed to prevent the main thread
        // from getting too ahead of the rendering thread.
        // - We do the blocking at the end so that this thread finishes all
        // processing before waiting on the rendering thread.
        long now = System.nanoTime();
        while (!previousFrame.isEmpty()) {
            Future<Void> f = previousFrame.poll();
            try {
                f.get();
            } catch (Exception e) {
                throw new RuntimeException("Previous frame failed", e);
            }
        }
        blocktime += (System.nanoTime() - now);

        Camera camera = system.createDataInstance(Camera.class);

        // first cache all shadow map frustums so any view can easily prepare a texture
        // FIXME must properly thread-safety this because we start the reset
        // before the previous frame has completed use of it
        // - I'm not sure but this might be the sole problem with the deadlock, or just the crash
        //   When it doesn't crash something else might still cause it to freeze up entirely
        //   including the rest of the OS which is bad
        shadowMap.reset();
        for (PVSResult visible : pvs) {
            // cacheShadowScene properly ignores PVSResults that aren't for lights
            shadowMap.cacheShadowScene(visible);
        }

        // go through all results and render all camera frustums
        List<Future<Void>> thisFrame = new ArrayList<Future<Void>>();
        for (PVSResult visible : pvs) {
            if (visible.getSource().getType().equals(Camera.class)) {
                camera.set((Component<Camera>) visible.getSource());
                thisFrame.add(render(camera.getSurface(), visible.getFrustum(),
                                     visible.getPotentiallyVisibleSet(), system));
            }
        }

        previousFrame.addAll(thisFrame);

        // FIXME return a flush task
        return null;
    }

    public static long rendertime = 0L;

    private Future<Void> render(final Surface surface, Frustum view, Bag<Entity> pvs,
                                EntitySystem system) {
        // FIXME can we somehow preserve this tree across frames instead of continuing
        // to build it over and over again?
        GeometryGroupFactory geomGroup = new GeometryGroupFactory(system);
        TextureGroupFactory textureGroup = new TextureGroupFactory(system,
                                                                   diffuseTextureUnit,
                                                                   emissiveTextureUnit,
                                                                   decalTextureUnit,
                                                                   geomGroup);
        MaterialGroupFactory materialGroup = new MaterialGroupFactory(system,
                                                                      (diffuseTextureUnit >= 0 ? textureGroup : geomGroup));

        LightGroupFactory lightGroup = new LightGroupFactory(system,
                                                             lightGroups,
                                                             (shadowMap != null ? shadowMap.getShadowCastingLights() : Collections.<Component<? extends Light<?>>> emptySet()),
                                                             framework.getCapabilities()
                                                                      .getMaxActiveLights(),
                                                             materialGroup);

        ShadowMapGroupFactory shadowGroup = (shadowMap != null ? new ShadowMapGroupFactory(shadowMap,
                                                                                           shadowmapTextureUnit,
                                                                                           lightGroup) : null);

        SolidColorGroupFactory solidColorGroup = new SolidColorGroupFactory(system,
                                                                            textureGroup);

        LightingGroupFactory lightingGroup = new LightingGroupFactory(solidColorGroup,
                                                                      (shadowMap != null ? shadowGroup : lightGroup));
        CameraGroupFactory cameraGroup = new CameraGroupFactory(view, lightingGroup);

        final StateNode rootNode = new StateNode(cameraGroup.newGroup());
        for (Entity e : pvs) {
            rootNode.add(e);
        }

        return framework.queue(new com.ferox.renderer.Task<Void>() {
            @Override
            public Void run(HardwareAccessLayer access) {
                long now = System.nanoTime();
                Context ctx = access.setActiveSurface(surface);
                if (ctx != null) {
                    FixedFunctionRenderer ffp = ctx.getFixedFunctionRenderer();
                    // FIXME clear color should be configurable somehow
                    ffp.clear(true, true, true, new Vector4(0, 0, 0, 1.0), 1, 0);
                    ffp.setDrawStyle(DrawStyle.SOLID, DrawStyle.SOLID);
                    rootNode.render(access, new AppliedEffects());

                    //                    Frustum twoD = new Frustum(true,
                    //                                               0,
                    //                                               surface.getWidth(),
                    //                                               0,
                    //                                               surface.getHeight(),
                    //                                               -1,
                    //                                               1);
                    //                    ffp.setProjectionMatrix(twoD.getProjectionMatrix());
                    //                    ffp.setModelViewMatrix(twoD.getViewMatrix());
                    //                    ffp.setDepthTest(Comparison.ALWAYS);
                    //
                    //                    shadowMap.getShadowMap().setDepthCompareEnabled(false);
                    //                    access.update(shadowMap.getShadowMap());
                    //                    ffp.setTexture(0, shadowMap.getShadowMap());
                    //                    ffp.setTextureCombineRGB(0, CombineFunction.REPLACE,
                    //                                             CombineSource.CURR_TEX,
                    //                                             CombineOperand.COLOR,
                    //                                             CombineSource.CURR_TEX,
                    //                                             CombineOperand.COLOR,
                    //                                             CombineSource.CURR_TEX, CombineOperand.COLOR);
                    //
                    //                    Geometry g = Rectangle.create(0, 250, 0, 250);
                    //                    ffp.setVertices(g.getVertices());
                    //                    ffp.setTextureCoordinates(0, g.getTextureCoordinates());
                    //                    if (g.getIndices() == null) {
                    //                        ffp.render(g.getPolygonType(), g.getIndexOffset(),
                    //                                   g.getIndexCount());
                    //                    } else {
                    //                        ffp.render(g.getPolygonType(), g.getIndices(),
                    //                                   g.getIndexOffset(), g.getIndexCount());
                    //                    }
                    //
                    //                    ffp.setTexture(0, null);
                    //                    shadowMap.getShadowMap().setDepthCompareEnabled(true);
                    //                    access.update(shadowMap.getShadowMap());
                }
                rendertime += (System.nanoTime() - now);
                return null;
            }
        });
    }

    @Override
    public void reset(EntitySystem system) {
        pvs = new ArrayList<PVSResult>();
        lightGroups = null;
    }

    public void report(PVSResult pvs) {
        this.pvs.add(pvs);
    }

    public void report(LightGroupResult r) {
        lightGroups = r;
    }

    @Override
    public Set<Class<? extends ComponentData<?>>> getAccessedComponents() {
        return COMPONENTS;
    }

    @Override
    public boolean isEntitySetModified() {
        return false;
    }
}
