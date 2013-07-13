package com.ferox.scene.task.ffp;

import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.BlendFactor;

public class TransparentState implements State {
    public static final TransparentState OPAQUE = new TransparentState(BlendFactor.ONE, BlendFactor.ZERO);
    public static final TransparentState NORMAL = new TransparentState(BlendFactor.SRC_ALPHA,
                                                                       BlendFactor.ONE_MINUS_SRC_ALPHA);
    public static final TransparentState ADDITIVE = new TransparentState(BlendFactor.SRC_ALPHA,
                                                                         BlendFactor.ONE);

    private final BlendFactor src;
    private final BlendFactor dst;

    public TransparentState(BlendFactor src, BlendFactor dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
        AppliedEffects newEffects = effects.applyBlending(src, dst);

        newEffects.pushBlending(access.getCurrentContext().getFixedFunctionRenderer());
        access.getCurrentContext().getFixedFunctionRenderer()
              .setTwoSidedLightingEnabled(newEffects.isBlendingEnabled());
        currentNode.visitChildren(newEffects, access);
    }
}
