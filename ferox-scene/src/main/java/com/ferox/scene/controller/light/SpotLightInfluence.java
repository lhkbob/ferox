package com.ferox.scene.controller.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.scene.SpotLight;
import com.lhkbob.entreri.TypeId;

public class SpotLightInfluence implements LightInfluence<SpotLight> {

    @Override
    public boolean influences(SpotLight light, @Const Matrix4 lightTransform, @Const AxisAlignedBox bounds) {
        // FIXME: make sure the bounds are within the cone of light.
        // FIXME: should I do distance or cone check first? I feel like it should
        // be the cone check since that will exclude more
        if (light.getFalloffDistance() > 0.0) {
            // check distance
        }
        return true;
    }

    @Override
    public TypeId<SpotLight> getType() {
        return SpotLight.ID;
    }
}
