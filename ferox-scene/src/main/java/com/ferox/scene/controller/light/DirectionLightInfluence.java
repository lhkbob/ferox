package com.ferox.scene.controller.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.scene.DirectionLight;
import com.lhkbob.entreri.TypeId;

public class DirectionLightInfluence implements LightInfluence<DirectionLight> {
    @Override
    public boolean influences(DirectionLight light, @Const Matrix4 lightTransform, @Const AxisAlignedBox bounds) {
        // direction lights shine on everything
        return true;
    }

    @Override
    public TypeId<DirectionLight> getType() {
        return DirectionLight.ID;
    }
}
