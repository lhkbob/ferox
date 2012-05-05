package com.ferox.scene.controller.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.scene.PointLight;
import com.lhkbob.entreri.TypeId;

public class PointLightInfluence implements LightInfluence<PointLight> {
    @Override
    public boolean influences(PointLight light, @Const Matrix4 lightTransform, @Const AxisAlignedBox bounds) {
        if (light.getFalloffDistance() <= 0.0) {
            // no energy falloff so the light will influence everything
            return true;
        } else {
            // make sure the bounds intersects with the bounding sphere centered
            // on the light with a radius equal to the falloff distance
            // FIXME
            return false; 
        }
    }

    @Override
    public TypeId<PointLight> getType() {
        return PointLight.ID;
    }
}
