package com.ferox.scene.controller.ffp2;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.scene.controller.ffp.AppliedEffects;

public class LightingState implements State {
    private final boolean lit;

    public LightingState(boolean lit) {
        this.lit = lit;
    }

    public boolean isLightingEnabled() {
        return lit;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();
        r.setLightingEnabled(lit);

        currentNode.visitChildren(effects, access);
    }
}
