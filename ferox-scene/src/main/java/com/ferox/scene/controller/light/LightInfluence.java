package com.ferox.scene.controller.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.scene.Light;

public interface LightInfluence {
    public static interface Factory<T extends Light<T>> {
        public LightInfluence create(T light, @Const Matrix4 lightTransform);
    }

    public boolean influences(@Const AxisAlignedBox entityBounds);
}
