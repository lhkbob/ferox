package com.ferox.scene.controller.ffp;

import java.util.List;

import com.ferox.math.ColorRGB;
import com.ferox.renderer.FixedFunctionRenderer;
import com.lhkbob.entreri.Entity;

public class SolidColorGroupFactory implements StateGroupFactory {

    @Override
    public StateGroup newGroup() {
        // TODO Auto-generated method stub
        return null;
    }
 
    private class SolidColorGroup implements StateGroup {

        @Override
        public StateNode getNode(Entity e) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<StateNode> getNodes() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public AppliedEffects applyGroupState(FixedFunctionRenderer r, AppliedEffects effects) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void unapplyGroupState(FixedFunctionRenderer r, AppliedEffects effects) {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    private class SolidColorState implements State {
        private final ColorRGB rgb;
        
        public SolidColorState(ColorRGB rgb) {
            
        }
        
        @Override
        public void add(Entity e) {
            // do nothing
        }

        @Override
        public AppliedEffects applyState(FixedFunctionRenderer r, AppliedEffects effects, int index) {
            r.setM
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void unapplyState(FixedFunctionRenderer r, AppliedEffects effects, int index) {
               
        }
    }
}
