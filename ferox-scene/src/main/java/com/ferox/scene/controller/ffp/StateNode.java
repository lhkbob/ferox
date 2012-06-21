package com.ferox.scene.controller.ffp;

import java.util.List;

import com.ferox.renderer.FixedFunctionRenderer;
import com.lhkbob.entreri.Entity;

public class StateNode {
    // for low-level reasons a single high-level state
    // might be represented as multiple actual state groups
    private final State[] state;
    
    private final StateGroup children;
    
    public StateNode(StateGroup child) {
        this(child, (State[]) null);
    }
    
    public StateNode(StateGroup child, State... state) {
        if (state == null || state.length == 0)
            state = new State[] { new NullState() };
        
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
            if (child != null) 
                child.add(e);
        }
    }
    
    public void render(FixedFunctionRenderer r) {
        List<StateNode> childNodes = (children != null ? children.getNodes() : null);
        int childCount = (children != null ? childNodes.size() : 0);
        
        for (int i = 0; i < state.length; i++) {
            if (state[i].applyState(r)) {
                for (int j = 0; j < childCount; j++) {
                    childNodes.get(j).render(r);
                }
                state[i].unapplyState(r);
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
        public boolean applyState(FixedFunctionRenderer r) {
            // do nothing, but continue
            return true;
        }

        @Override
        public void unapplyState(FixedFunctionRenderer r) {
            // do nothing
        }
    }
}
