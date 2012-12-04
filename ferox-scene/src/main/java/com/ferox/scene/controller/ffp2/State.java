package com.ferox.scene.controller.ffp2;

import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.scene.controller.ffp.AppliedEffects;

public interface State {
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access);
}
