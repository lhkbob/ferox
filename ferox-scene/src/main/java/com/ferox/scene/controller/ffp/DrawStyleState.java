package com.ferox.scene.controller.ffp;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.DrawStyle;

public class DrawStyleState implements StaticState {
    private DrawStyle front;
    private DrawStyle back;

    public void set(DrawStyle front, DrawStyle back) {
        this.front = front;
        this.back = back;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        r.setDrawStyle(front, back);

        currentNode.visitChildren(effects, access);
    }

    @Override
    public int hashCode() {
        return front.hashCode() ^ back.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DrawStyleState)) {
            return false;
        }
        DrawStyleState s = (DrawStyleState) o;
        return s.front.equals(front) && s.back.equals(back);
    }
}
