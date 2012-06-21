package com.ferox.scene.controller.ffp;

import java.util.List;

import com.lhkbob.entreri.Entity;

public interface StateGroup {
    public StateNode getNode(Entity e);
    
    public List<StateNode> getNodes();
}
