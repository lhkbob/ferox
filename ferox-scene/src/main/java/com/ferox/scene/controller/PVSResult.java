package com.ferox.scene.controller;

import com.ferox.math.bounds.Frustum;
import com.ferox.util.Bag;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.Result;

public class PVSResult implements Result {
    private final Component<?> source;
    private final Frustum frustum;
    
    private final Bag<Entity> pvs;
    
    public PVSResult(Component<?> source, Frustum frustum, Bag<Entity> pvs) {
        this.source = source;
        this.frustum = frustum;
        this.pvs = pvs;
    }
    
    public Component<?> getSource() {
        return source;
    }
    
    public Frustum getFrustum() {
        return frustum;
    }
    
    public Bag<Entity> getPotentiallyVisibleSet() {
        return pvs;
    }
    
    @Override
    public boolean isSingleton() {
        return false;
    }
}
