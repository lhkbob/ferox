package com.ferox.scene.controller;

import com.ferox.math.bounds.SpatialIndex;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.Result;

public class SpatialIndexResult implements Result {
    private final SpatialIndex<Entity> index;
    
    public SpatialIndexResult(SpatialIndex<Entity> index) {
        if (index == null)
            throw new NullPointerException("Index cannot be null");
        this.index = index;
    }
    
    public SpatialIndex<Entity> getIndex() {
        return index;
    }
    
    @Override
    public boolean isSingleton() {
        return true;
    }
}
