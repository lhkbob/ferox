package com.ferox.scene.controller;

import com.ferox.math.bounds.Frustum;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Result;

public class FrustumResult implements Result {
    private final Component<?> frustumSource;
    private final Frustum frustum;
    
    public FrustumResult(Component<?> source, Frustum view) {
        if (source == null || view == null)
            throw new NullPointerException("Component and Frustum cannot be null");
        
        frustumSource = source;
        frustum = view;
    }
    
    public Component<?> getSource() {
        return frustumSource;
    }
    
    public Frustum getFrustum() {
        return frustum;
    }
    
    @Override
    public boolean isSingleton() {
        return false;
    }
}
