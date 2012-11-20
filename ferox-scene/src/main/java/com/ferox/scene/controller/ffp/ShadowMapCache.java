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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.ContextState;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Framework;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Surface;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Filter;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.Texture.WrapMode;
import com.ferox.scene.Light;
import com.ferox.scene.controller.PVSResult;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Entity;

public class ShadowMapCache {
    private final TextureSurface shadowMap;

    private Map<Component<? extends Light<?>>, ShadowMapScene> shadowScenes;
    private Set<Component<? extends Light<?>>> shadowLights;

    public ShadowMapCache(Framework framework, int width, int height) {
        shadowMap = framework.createSurface(new TextureSurfaceOptions().setWidth(width)
                                                                       .setHeight(height)
                                                                       .setDepth(1)
                                                                       .setTarget(Target.T_2D)
                                                                       .setUseDepthTexture(true)
                                                                       .setColorBufferFormats());
        shadowScenes = new HashMap<Component<? extends Light<?>>, ShadowMapScene>();

        Texture sm = shadowMap.getDepthBuffer();
        sm.setFilter(Filter.LINEAR);
        sm.setWrapMode(WrapMode.CLAMP_TO_BORDER);
        sm.setBorderColor(new Vector4(1, 1, 1, 1));
        sm.setDepthCompareEnabled(true);
        sm.setDepthComparison(Comparison.LEQUAL);
    }

    public void reset() {
        shadowScenes = new HashMap<Component<? extends Light<?>>, ShadowMapScene>();
        shadowLights = Collections.unmodifiableSet(shadowScenes.keySet());
    }

    public Set<Component<? extends Light<?>>> getShadowCastingLights() {
        return shadowLights;
    }

    public Frustum getShadowMapFrustum(Component<? extends Light<?>> light) {
        ShadowMapScene scene = shadowScenes.get(light);
        if (scene == null) {
            throw new IllegalArgumentException("Light was not cached previously");
        }
        return scene.frustum;
    }

    public Texture getShadowMap() {
        return shadowMap.getDepthBuffer();
    }

    public Texture getShadowMap(Component<? extends Light<?>> shadowLight,
                                HardwareAccessLayer access) {
        ShadowMapScene scene = shadowScenes.get(shadowLight);
        if (scene == null) {
            throw new IllegalArgumentException("Light was not cached previously");
        }

        Surface origSurface = access.getCurrentContext().getSurface();
        int origLayer = access.getCurrentContext().getSurfaceLayer(); // in case of texture-surface
        ContextState<FixedFunctionRenderer> origState = access.getCurrentContext()
                                                              .getFixedFunctionRenderer()
                                                              .getCurrentState();

        FixedFunctionRenderer r = access.setActiveSurface(shadowMap)
                                        .getFixedFunctionRenderer();

        r.clear(true, true, true);
        r.setColorWriteMask(false, false, false, false);

        // set style to be just depth, while drawing only back faces
        r.setDrawStyle(DrawStyle.NONE, DrawStyle.SOLID);

        // move everything backwards slightly to account for floating errors
        r.setDepthOffsets(0, 5);
        r.setDepthOffsetsEnabled(true);

        scene.scene.render(access, new AppliedEffects());

        // restore original surface and state
        if (origSurface instanceof TextureSurface) {
            access.setActiveSurface((TextureSurface) origSurface, origLayer);
        } else {
            access.setActiveSurface(origSurface);
        }

        // FIXME race condition on shutdown exists if the orig surface is
        // destroyed while rendering to the shadow map, if the map is on a pbuffer
        access.getCurrentContext().getFixedFunctionRenderer().setCurrentState(origState);

        return shadowMap.getDepthBuffer();
    }

    @SuppressWarnings("unchecked")
    public void cacheShadowScene(PVSResult pvs) {
        if (!Light.class.isAssignableFrom(pvs.getSource().getTypeId().getType())) {
            // only take PVS's produced by lights
            return;
        }

        GeometryGroupFactory geom = new GeometryGroupFactory(pvs.getSource()
                                                                .getEntitySystem());
        CameraGroupFactory cam = new CameraGroupFactory(pvs.getFrustum(), geom);
        StateNode scene = new StateNode(cam.newGroup());

        for (Entity e : pvs.getPotentiallyVisibleSet()) {
            scene.add(e);
        }

        Component<? extends Light<?>> source = (Component<? extends Light<?>>) pvs.getSource();
        shadowScenes.put(source, new ShadowMapScene(pvs.getFrustum(), scene));
    }

    private static class ShadowMapScene {
        private final Frustum frustum;
        private final StateNode scene;

        public ShadowMapScene(Frustum frustum, StateNode scene) {
            this.frustum = frustum;
            this.scene = scene;
        }
    }
}
