package com.ferox.scene.controller.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.bounds.Frustum;
import com.ferox.math.bounds.Frustum.FrustumIntersection;
import com.ferox.scene.SpotLight;

public class SpotLightInfluence implements LightInfluence {
    private final Frustum lightFrustum;

    public SpotLightInfluence(@Const Matrix4 lightTransform, double falloff,
                              double cutoffAngle) {
        // construct a Frustum that approximates the cone of the light
        double zfar = (falloff < 0 ? Double.MAX_VALUE : falloff);
        lightFrustum = new Frustum(60.0, 1.0, 0.1, 1.0);
        lightFrustum.setPerspective(cutoffAngle, 1.0, 0.00001, zfar);
        lightFrustum.setOrientation(lightTransform);
    }

    @Override
    public boolean influences(@Const AxisAlignedBox bounds) {
        return lightFrustum.intersects(bounds, null) != FrustumIntersection.OUTSIDE;
    }

    public static LightInfluence.Factory<SpotLight> factory() {
        return new LightInfluence.Factory<SpotLight>() {
            @Override
            public LightInfluence create(SpotLight light, Matrix4 lightTransform) {
                return new SpotLightInfluence(lightTransform,
                                              light.getFalloffDistance(),
                                              light.getCutoffAngle());
            }
        };
    }
}
