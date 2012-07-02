package com.ferox.scene.controller.ffp;

import com.ferox.renderer.FixedFunctionRenderer;
import com.lhkbob.entreri.Entity;

public interface State {
    public void add(Entity e);
    
    public AppliedEffects applyState(FixedFunctionRenderer r, AppliedEffects effects, int index);
    
    public void unapplyState(FixedFunctionRenderer r, AppliedEffects effects, int index);
}
