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
import java.util.List;
import java.util.Set;

import com.ferox.math.Matrix4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.FixedFunctionRenderer.TexCoord;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.resource.Texture;
import com.ferox.scene.Light;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Entity;

public class ShadowMapGroupFactory implements StateGroupFactory {
    // FIXME must fix auto-formatting ugliness in these situations
    private static final Matrix4 bias = new Matrix4().set(.5, 0, 0, .5, 0, .5, 0, .5, 0,
                                                          0, .5, .5, 0, 0, 0, 1);

    private final StateGroupFactory childFactory;
    private final ShadowMapCache smCache;

    private final int shadowMapUnit;

    public ShadowMapGroupFactory(ShadowMapCache shadowMapCache, int shadowMapUnit,
                                 StateGroupFactory childFactory) {
        this.childFactory = childFactory;
        this.shadowMapUnit = shadowMapUnit;
        smCache = shadowMapCache;
    }

    @Override
    public StateGroup newGroup() {
        return new ShadowMapGroup();
    }

    private class ShadowMapGroup implements StateGroup {
        private final StateNode node;

        public ShadowMapGroup() {
            Set<Component<? extends Light<?>>> smLights = smCache.getShadowCastingLights();
            State[] states = new State[smLights.size() + 1];

            int i = 1;
            states[0] = new BaseLightState();
            for (Component<? extends Light<?>> l : smLights) {
                states[i++] = new ShadowState(l);
            }

            node = new StateNode((childFactory == null ? null : childFactory.newGroup()),
                                 states);
        }

        @Override
        public StateNode getNode(Entity e) {
            return node;
        }

        @Override
        public List<StateNode> getNodes() {
            return Collections.singletonList(node);
        }

        @Override
        public AppliedEffects applyGroupState(HardwareAccessLayer access,
                                              AppliedEffects effects) {
            return effects;
        }

        @Override
        public void unapplyGroupState(HardwareAccessLayer access, AppliedEffects effects) {
            FixedFunctionRenderer r = access.getCurrentContext()
                                            .getFixedFunctionRenderer();
            r.setTexture(shadowMapUnit, null);
            effects.pushBlending(r); // restore blending from ShadowState passes
        }
    }

    private class ShadowState implements State {
        private final Component<? extends Light<?>> light;

        public ShadowState(Component<? extends Light<?>> light) {
            this.light = light;
        }

        @Override
        public void add(Entity e) {
            // do nothing
        }

        @Override
        public AppliedEffects applyState(HardwareAccessLayer access,
                                         AppliedEffects effects, int index) {
            Frustum smFrustum = smCache.getShadowMapFrustum(light);
            Texture shadowMap = smCache.getShadowMap(light, access);

            // must get renderer after the shadow map because that will change
            // and restore the active surface (invalidating any previous renderer)
            FixedFunctionRenderer r = access.getCurrentContext()
                                            .getFixedFunctionRenderer();

            // configure shadow map texturing
            r.setTexture(shadowMapUnit, shadowMap);
            r.setTextureCoordGeneration(shadowMapUnit, TexCoordSource.EYE);

            Matrix4 texM = new Matrix4();
            texM.mul(bias, smFrustum.getProjectionMatrix())
                .mul(smFrustum.getViewMatrix());

            r.setTextureEyePlane(shadowMapUnit, TexCoord.S, texM.getRow(0));
            r.setTextureEyePlane(shadowMapUnit, TexCoord.T, texM.getRow(1));
            r.setTextureEyePlane(shadowMapUnit, TexCoord.R, texM.getRow(2));
            r.setTextureEyePlane(shadowMapUnit, TexCoord.Q, texM.getRow(3));

            // configure depth testing and blending into previous pass
            r.setDepthOffsetsEnabled(true);
            r.setDepthOffsets(0, -5); // offset depth in opposite direction from SM depth
            effects = effects.applyBlending(BlendFactor.SRC_ALPHA, BlendFactor.ONE);
            effects.pushBlending(r); // this also sets the depth-mask/test appropriately

            return effects.applyShadowMapping(light);
        }

        @Override
        public void unapplyState(HardwareAccessLayer access, AppliedEffects effects,
                                 int index) {
            // do nothing
        }
    }

    private class BaseLightState implements State {
        @Override
        public void add(Entity e) {
            // do nothing
        }

        @Override
        public AppliedEffects applyState(HardwareAccessLayer access,
                                         AppliedEffects effects, int index) {
            // here we assume nothing else has tampered with the shadow light
            return effects;
        }

        @Override
        public void unapplyState(HardwareAccessLayer access, AppliedEffects effects,
                                 int index) {
            // do nothing
        }
    }
}
