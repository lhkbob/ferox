package com.ferox.scene.controller.ffp;

import com.ferox.renderer.HardwareAccessLayer;

public class NullState implements State {
    public static final NullState INSTANCE = new NullState();

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
        currentNode.visitChildren(effects, access);
    }
}
