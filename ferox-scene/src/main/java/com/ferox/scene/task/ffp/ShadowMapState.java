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
package com.ferox.scene.task.ffp;

import com.ferox.math.Matrix4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.DepthMap2D;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.scene.Light;

public class ShadowMapState implements State {
    private static final Matrix4 bias = new Matrix4()
            .set(.5, 0, 0, .5, 0, .5, 0, .5, 0, 0, .5, .5, 0, 0, 0, 1);

    private final int shadowMapUnit;
    private final ShadowMapCache shadowMap;

    public ShadowMapState(ShadowMapCache cache, int textureUnit) {
        shadowMap = cache;
        shadowMapUnit = textureUnit;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        // render base pass with current configuration
        currentNode.visitChildren(effects, access);

        // configure global state for shadow mapping passes
        r.setDepthOffsetsEnabled(true);
        r.setDepthOffsets(0, -5); // offset depth in opposite direction from SM depth
        AppliedEffects shadowEffects = effects.applyBlending(BlendFactor.SRC_ALPHA, BlendFactor.ONE);
        shadowEffects.pushBlending(r); // this also sets the depth-mask/test appropriately

        // now apply shadow passes
        for (Light light : shadowMap.getShadowCastingLights()) {
            renderShadowPass(light, currentNode, shadowEffects, access);
        }

        // restore state (since there won't be multiple shadow-map states
        // together we can do it this way instead of using a reset node like
        // textures and colors, etc)
        r = access.getCurrentContext().getFixedFunctionRenderer(); // must re-get renderer, though
        effects.pushBlending(r);
        r.setDepthOffsetsEnabled(false);
        r.setTexture(shadowMapUnit, null);
    }

    private void renderShadowPass(Light shadowCaster, StateNode node, AppliedEffects effects,
                                  HardwareAccessLayer access) {
        Frustum smFrustum = shadowMap.getShadowMapFrustum(shadowCaster);
        DepthMap2D shadowTexture = shadowMap.getShadowMap(shadowCaster, access);

        // must get renderer after the shadow map because that will change
        // and restore the active surface (invalidating any previous renderer)
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        // configure shadow map texturing
        r.setTexture(shadowMapUnit, shadowTexture);
        r.setTextureCoordinateSource(shadowMapUnit, TexCoordSource.EYE);

        Matrix4 texM = new Matrix4();
        texM.mul(bias, smFrustum.getProjectionMatrix()).mul(smFrustum.getViewMatrix());
        r.setTextureEyePlanes(shadowMapUnit, texM);

        // depth bias and blending have already been configured, since
        // they won't change from pass to pass
        node.visitChildren(effects.applyShadowMapping(shadowCaster), access);
    }
}
