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

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.scene.Light;

public class AppliedEffects {
    // Non-null when actively rendering a shadow map, the general set of
    // shadow-casting lights is not the responsibility of AppliedEffects
    private final Light shadowLight;

    // BlendFunction will always be ADD
    private final BlendFactor destBlend;
    private final BlendFactor sourceBlend;

    private final Matrix4 viewMatrix;

    public AppliedEffects() {
        shadowLight = null;
        destBlend = BlendFactor.ZERO;
        sourceBlend = BlendFactor.ONE;
        viewMatrix = new Matrix4();
    }

    private AppliedEffects(@Const Matrix4 view, BlendFactor sourceBlend, BlendFactor destBlend,
                           Light shadowLight) {
        this.sourceBlend = sourceBlend;
        this.destBlend = destBlend;
        this.shadowLight = shadowLight;
        viewMatrix = view;
    }

    public void pushBlending(FixedFunctionRenderer r) {
        if (isBlendingEnabled()) {
            r.setBlendingEnabled(true);
            r.setBlendMode(BlendFunction.ADD, sourceBlend, destBlend);
            r.setDepthTest(Comparison.LEQUAL);
            r.setDepthWriteMask(false);
        } else {
            r.setBlendingEnabled(false);
            r.setDepthTest(Comparison.LESS);
            r.setDepthWriteMask(true);
        }
    }

    public AppliedEffects applyBlending(BlendFactor source, BlendFactor dest) {
        return new AppliedEffects(viewMatrix, source, dest, shadowLight);
    }

    public AppliedEffects applyShadowMapping(Light light) {
        return new AppliedEffects(viewMatrix, sourceBlend, destBlend, light);
    }

    public AppliedEffects applyViewMatrix(@Const Matrix4 view) {
        return new AppliedEffects(view.clone(), sourceBlend, destBlend, shadowLight);
    }

    @Const
    public Matrix4 getViewMatrix() {
        return viewMatrix;
    }

    public boolean isShadowBeingRendered() {
        return shadowLight != null;
    }

    public boolean isBlendingEnabled() {
        return destBlend != BlendFactor.ZERO;
    }

    public BlendFactor getSourceBlendFactor() {
        return sourceBlend;
    }

    public BlendFactor getDestinationBlendFactor() {
        return destBlend;
    }

    public Light getShadowMappingLight() {
        return shadowLight;
    }
}
