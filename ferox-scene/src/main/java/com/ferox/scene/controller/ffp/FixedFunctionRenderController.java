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
    private final int decalTextureUnit;

    private List<PVSResult> pvs;
    private LightGroupResult lightGroups;

    private final Queue<Future<Void>> previousFrame;

    public FixedFunctionRenderController(Framework framework) {
        this(framework, 512);
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
    public void process(double dt) {
        // go through all results and render all camera frustums
        List<Future<Void>> thisFrame = new ArrayList<Future<Void>>();
        Camera camera = getEntitySystem().createDataInstance(Camera.ID);
        for (PVSResult visible : pvs) {
            if (visible.getSource().getTypeId() == Camera.ID) {
                camera.set((Component<Camera>) visible.getSource());
                thisFrame.add(render(camera.getSurface(), visible.getFrustum(),
                                     visible.getPotentiallyVisibleSet()));
            }
        }

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
        previousFrame.addAll(thisFrame);
    }

    public static long rendertime = 0L;

    private Future<Void> render(final Surface surface, Frustum view, Bag<Entity> pvs) {
        // FIXME can we somehow preserve this tree across frames instead of continuing
        // to build it over and over again?
        GeometryGroupFactory geomGroup = new GeometryGroupFactory(getEntitySystem(),
                                                                  view.getViewMatrix());
        TextureGroupFactory textureGroup = new TextureGroupFactory(getEntitySystem(),
                                                                   diffuseTextureUnit,
                                                                   emissiveTextureUnit,
                                                                   decalTextureUnit,
                                                                   geomGroup);
        MaterialGroupFactory materialGroup = new MaterialGroupFactory(getEntitySystem(),
                                                                      (diffuseTextureUnit >= 0 ? textureGroup : geomGroup));

        LightGroupFactory lightGroup = new LightGroupFactory(getEntitySystem(),
                                                             lightGroups,
                                                             framework.getCapabilities()
                                                                      .getMaxActiveLights(),
                                                             materialGroup);
        SolidColorGroupFactory solidColorGroup = new SolidColorGroupFactory(getEntitySystem(),
                                                                            textureGroup);

        LightingGroupFactory lightingGroup = new LightingGroupFactory(solidColorGroup,
                                                                      lightGroup);
        CameraGroupFactory cameraGroup = new CameraGroupFactory(view, lightingGroup);

        final StateNode rootNode = new StateNode(cameraGroup.newGroup());
        for (Entity e : pvs) {
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
                    ffp.clear(true, true, true, new Vector4(0.5, 0.5, 0.5, 1.0), 1f, 0);

                    rootNode.render(ffp, new AppliedEffects());
                }
                rendertime += (System.nanoTime() - now);
                return null;
            }
        });
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
