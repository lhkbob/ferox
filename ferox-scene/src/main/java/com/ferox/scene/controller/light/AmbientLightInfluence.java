package com.ferox.scene.controller.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.scene.AmbientLight;
import com.lhkbob.entreri.TypeId;

public class AmbientLightInfluence implements LightInfluence<AmbientLight> {
    @Override
    public boolean influences(AmbientLight light, @Const Matrix4 lightTransform, @Const AxisAlignedBox bounds) {
        // ambient light influences everything
        return true;
    }

    @Override
    public TypeId<AmbientLight> getType() {
        return AmbientLight.ID;
    }
}
