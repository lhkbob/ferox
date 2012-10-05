package com.ferox.scene.controller.ffp;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.scene.Light;
import com.lhkbob.entreri.Component;

public class AppliedEffects {
    // Non-null when actively rendering a shadow map, the general set of
    // shadow-casting lights is not the responsibility of AppliedEffects
    private final Component<? extends Light<?>> shadowLight;

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

    private AppliedEffects(@Const Matrix4 view, BlendFactor sourceBlend,
                           BlendFactor destBlend,
                           Component<? extends Light<?>> shadowLight) {
        this.sourceBlend = sourceBlend;
        this.destBlend = destBlend;
        this.shadowLight = shadowLight;
        viewMatrix = view;
    }

    public void pushBlending(FixedFunctionRenderer r) {
        if (isBlendingEnabled()) {
            r.setBlendingEnabled(true);
            r.setBlendMode(BlendFunction.ADD, sourceBlend, destBlend);
            // FIXME might need a different comparison for shadow-mapping?
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

    public AppliedEffects applyShadowMapping(Component<? extends Light<?>> light) {
        return new AppliedEffects(viewMatrix, sourceBlend, destBlend, light);
    }

    public AppliedEffects applyViewMatrix(@Const Matrix4 view) {
        return new AppliedEffects(view, sourceBlend, destBlend, shadowLight);
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

    public Component<? extends Light<?>> getShadowMappingLight() {
        return shadowLight;
    }
}
