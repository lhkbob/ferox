package com.ferox.scene.controller.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.bounds.Frustum;
import com.ferox.math.bounds.Frustum.FrustumIntersection;
import com.ferox.scene.SpotLight;
import com.lhkbob.entreri.TypeId;

public class SpotLightInfluence implements LightInfluence<SpotLight> {
    private final Frustum cone = new Frustum(60.0, 1.0, 0.1, 1.0);
    
    @Override
    public boolean influences(SpotLight light, @Const Matrix4 lightTransform, @Const AxisAlignedBox bounds) {
        // construct a Frustum that approximates the cone of the light
        double zfar = (light.getFalloffDistance() < 0 ? Double.MAX_VALUE : light.getFalloffDistance());
        cone.setPerspective(light.getCutoffAngle(), 1.0, 0.00001, zfar);
        cone.setOrientation(lightTransform);
        
        return cone.intersects(bounds, null) != FrustumIntersection.OUTSIDE;
    }

    @Override
    public TypeId<SpotLight> getType() {
        return SpotLight.ID;
    }
}
