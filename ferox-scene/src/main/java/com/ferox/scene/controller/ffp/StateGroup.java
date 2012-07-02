package com.ferox.scene.controller.ffp;

import java.util.List;

import com.ferox.renderer.FixedFunctionRenderer;
import com.lhkbob.entreri.Entity;

public interface StateGroup {
    public StateNode getNode(Entity e);
    
    public List<StateNode> getNodes();
    
    public AppliedEffects applyGroupState(FixedFunctionRenderer r, AppliedEffects effects);
    
    public void unapplyGroupState(FixedFunctionRenderer r, AppliedEffects effects);
}
