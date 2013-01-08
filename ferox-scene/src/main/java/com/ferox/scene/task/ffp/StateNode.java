package com.ferox.scene.task.ffp;

import java.util.Arrays;

import com.ferox.renderer.HardwareAccessLayer;

public class StateNode {
    private final State state;
    private StateNode[] children;

    private int maxIndex;

    public StateNode(State state) {
        this.state = state;
        children = null;
        maxIndex = 0;
    }

    public StateNode(State state, int expectedChildCount) {
        this.state = state;
        children = new StateNode[expectedChildCount];
    }

    public State getState() {
        return state;
    }

    public void setChild(int index, StateNode child) {
        if (children == null) {
            children = new StateNode[2 * (index + 1)];
        } else if (children.length <= index) {
            children = Arrays.copyOf(children, 2 * index);
        }

        children[index] = child;
        maxIndex = Math.max(index + 1, maxIndex);
    }

    public void addChild(StateNode child) {
        setChild(maxIndex, child);
    }

    public StateNode getChild(int index) {
        if (children == null || children.length <= index) {
            return null;
        }
        return children[index];
    }

    public void visit(AppliedEffects effects, HardwareAccessLayer access) {
        state.visitNode(this, effects, access);
    }

    public void visitChildren(AppliedEffects effects, HardwareAccessLayer access) {
        if (children != null) {
            for (int i = 0; i < maxIndex; i++) {
                if (children[i] != null) {
                    children[i].visit(effects, access);
                }
            }
        }
    }
}
