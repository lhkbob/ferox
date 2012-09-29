package com.ferox.scene.controller.ffp;

import java.util.List;

import com.ferox.renderer.FixedFunctionRenderer;
import com.lhkbob.entreri.Entity;

public class StateNode {
    // for low-level reasons a single high-level state
    // might be represented as multiple actual states
    private final State[] state;

    private final StateGroup children;

    public StateNode(StateGroup child) {
        this(child, (State[]) null);
    }

    public StateNode(StateGroup child, State... state) {
        if (state == null || state.length == 0) {
            state = new State[] { new NullState() };
        }

        this.state = state;
        children = child;
    }

    public void add(Entity e) {
        for (int i = 0; i < state.length; i++) {
            state[i].add(e);
        }
        if (children != null) {
            // recurse to children if we're not a leaf
            StateNode child = children.getNode(e);
            if (child != null) {
                child.add(e);
            }
        }
    }

    public void render(FixedFunctionRenderer r, AppliedEffects effects) {
        List<StateNode> childNodes = (children != null ? children.getNodes() : null);
        int childCount = (children != null ? childNodes.size() : 0);

        // FIXME we're applying a child group state before the specific states
        // of this node, which means they can mutate the applied effects too soon,
        // maybe the StateGroup mutations don't get to mutate the effects?
        AppliedEffects forStates = (children != null ? children.applyGroupState(r, effects) : effects);
        if (forStates != null) {
            for (int i = 0; i < state.length; i++) {
                AppliedEffects childEffects = state[i].applyState(r, forStates, i);
                if (childEffects != null) {
                    for (int j = 0; j < childCount; j++) {
                        childNodes.get(j).render(r, childEffects);
                    }
                    state[i].unapplyState(r, forStates, i);
                }
            }

            if (children != null) {
                children.unapplyGroupState(r, effects);
            }
        }
    }

    /*
     * A state that doesn't do anything but effectively pass-through to children
     */
    private static class NullState implements State {
        @Override
        public void add(Entity e) {
            // do nothing
        }

        @Override
        public AppliedEffects applyState(FixedFunctionRenderer r, AppliedEffects effects, int index) {
            // do nothing, but continue
            return effects;
        }

        @Override
        public void unapplyState(FixedFunctionRenderer r, AppliedEffects effects, int index) {
            // do nothing
        }
    }
}
