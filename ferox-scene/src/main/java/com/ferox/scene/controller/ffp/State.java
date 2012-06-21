package com.ferox.scene.controller.ffp;

import com.ferox.renderer.FixedFunctionRenderer;
import com.lhkbob.entreri.Entity;

public interface State {
    public void add(Entity e);
    
    public boolean applyState(FixedFunctionRenderer r);
    
    public void unapplyState(FixedFunctionRenderer r);
}
