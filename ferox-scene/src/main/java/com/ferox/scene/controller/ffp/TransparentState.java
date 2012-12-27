package com.ferox.scene.controller.ffp;

import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.BlendFactor;

public class TransparentState implements State {
    private final BlendFactor src;
    private final BlendFactor dst;

    public TransparentState(BlendFactor src, BlendFactor dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
        AppliedEffects newEffects = effects.applyBlending(src, dst);

        newEffects.pushBlending(access.getCurrentContext().getFixedFunctionRenderer());
        currentNode.visitChildren(newEffects, access);
    }

    public static TransparentState opaque() {
        return new TransparentState(BlendFactor.ONE, BlendFactor.ZERO);
    }

    public static TransparentState normalBlend() {
        return new TransparentState(BlendFactor.SRC_ALPHA,
                                    BlendFactor.ONE_MINUS_SRC_ALPHA);
    }

    public static TransparentState additiveBlend() {
        return new TransparentState(BlendFactor.SRC_ALPHA, BlendFactor.ONE);
    }
}
