package com.ferox.scene.task.ffp;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;

public class LightingState implements State {
    public static final LightingState LIT = new LightingState(true);
    public static final LightingState UNLIT = new LightingState(false);

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
