package com.ferox.scene.controller.ffp;

import java.util.Arrays;
import java.util.List;

import com.ferox.math.Vector4;
import com.ferox.renderer.FixedFunctionRenderer;
import com.lhkbob.entreri.Entity;

public class LightGroupFactory implements StateGroupFactory {
    private final StateGroupFactory child;
    
    public LightGroupFactory(StateGroupFactory child) {
        this.child = child;
    }
    
    @Override
    public StateGroup newGroup() {
        return new LightGroup();
    }
    // The index for LightGroupFactory state is based on the assigned light group,
    // so the LightGroupFactory instance will know about all possible groups and can do
    // a simple lookup to which state to use.
    
    private class LightGroup implements StateGroup {
        private final List<StateNode> singleNode;
        
        public LightGroup() {
            singleNode = Arrays.asList(new StateNode((child == null ? null : child.newGroup()), new LightState()));
        }
        
        @Override
        public StateNode getNode(Entity e) {
            return singleNode.get(0);
        }

        @Override
        public List<StateNode> getNodes() {
            return singleNode;
        }
    }
    
    private class LightState implements State {
        @Override
        public void add(Entity e) {
            // do nothing
        }

        @Override
        public boolean applyState(FixedFunctionRenderer r) {
            r.setLightingEnabled(true);
            r.setLightEnabled(0, true);
            r.setLightPosition(0, new Vector4(1.0, 1.0, 1.0, 1.0));
            r.setLightColor(0, new Vector4(0.1, 0.1, 0.1, 1.0), new Vector4(.3, .3, .3, 1.0), new Vector4(0, 0, 0, 1));
            r.setMaterial(new Vector4(0.5, 0.5, 0.5, 1.0), new Vector4(.4, .2, .1, 1.0), new Vector4(.4, .4, .4, 1.0), new Vector4(0, 0, 0, 0));
            return true;
        }

        @Override
        public void unapplyState(FixedFunctionRenderer r) {
            r.setLightingEnabled(false);
        }
    }
}
