package com.ferox.scene.controller.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.scene.Light;
import com.lhkbob.entreri.TypeId;

public interface LightInfluence<T extends Light<T>> {
    public boolean influences(T light, @Const Matrix4 lightTransform, @Const AxisAlignedBox entityBounds);
    
    public TypeId<T> getType();
}
