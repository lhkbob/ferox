package com.ferox.scene.controller.ffp;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.scene.Light;
import com.lhkbob.entreri.Component;

public class AppliedEffects {
    private final boolean shadowedLightingPhase;
    // FIXME this might need to be a set, since in the main phase, we need
    // to specify all shadowed lights so that all of them can be disabled.
    private final Component<? extends Light<?>> shadowLight;

    // BlendFunction will always be ADD
    // FIXME do we need to specify separate factors for RGB and alpha?
    private final BlendFactor destBlend;
    private final BlendFactor sourceBlend;

    private final Matrix4 viewMatrix;

    public AppliedEffects(@Const Matrix4 view) {
        shadowedLightingPhase = false;
        shadowLight = null;
        destBlend = BlendFactor.ZERO;
        sourceBlend = BlendFactor.ONE;
        viewMatrix = view;
    }

    private AppliedEffects(@Const Matrix4 view, boolean shadowedLighting,
                           BlendFactor sourceBlend, BlendFactor destBlend,
                           Component<? extends Light<?>> shadowLight) {
        this.sourceBlend = sourceBlend;
        this.destBlend = destBlend;
        this.shadowLight = shadowLight;
        viewMatrix = view;
        shadowedLightingPhase = shadowedLighting;
    }

    public AppliedEffects setBlending(BlendFactor source, BlendFactor dest) {
        return new AppliedEffects(viewMatrix,
                                  shadowedLightingPhase,
                                  source,
                                  dest,
                                  shadowLight);
    }

    public @Const
    Matrix4 getViewMatrix() {
        return viewMatrix;
    }

    public boolean isShadowBeingRendered() {
        return shadowedLightingPhase;
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

    public Component<? extends Light<?>> getShadowCaster() {
        return shadowLight;
    }
}
