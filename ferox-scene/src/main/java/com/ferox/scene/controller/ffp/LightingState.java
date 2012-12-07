package com.ferox.scene.controller.ffp;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;

public class LightingState implements State {
    private final boolean lit;

    public LightingState(boolean lit) {
        this.lit = lit;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();
        r.setLightingEnabled(lit);

        currentNode.visitChildren(effects, access);
    }
}
