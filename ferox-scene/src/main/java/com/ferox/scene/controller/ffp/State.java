package com.ferox.scene.controller.ffp;

import com.ferox.renderer.HardwareAccessLayer;

public interface State {
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access);
}
