package com.ferox.scene.controller.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Matrix4;
import com.ferox.scene.Light;

public class GlobalLightInfluence implements LightInfluence {
    public static final GlobalLightInfluence INSTANCE = new GlobalLightInfluence();
    
    @Override
    public boolean influences(AxisAlignedBox entityBounds) {
        // always return true
        return true;
    }
    
    public static <T extends Light<T>> LightInfluence.Factory<T> factory() {
        return new LightInfluence.Factory<T>() {
            @Override
            public LightInfluence create(T light, Matrix4 lightTransform) {
                return INSTANCE;
            }
        };
    }
}
