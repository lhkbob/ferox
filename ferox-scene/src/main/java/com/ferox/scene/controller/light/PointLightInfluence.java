package com.ferox.scene.controller.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.scene.PointLight;

public class PointLightInfluence implements LightInfluence {
    private final Vector3 lightPos;
    private final Vector3 objectPos;
    private final double falloff;

    public PointLightInfluence(@Const Matrix4 lightTransform, double falloff) {
        lightPos = new Vector3(lightTransform.m03, lightTransform.m13, lightTransform.m23);
        objectPos = new Vector3();
        this.falloff = falloff * falloff;
    }

    @Override
    public boolean influences(@Const AxisAlignedBox bounds) {
        // center of aabb
        objectPos.add(bounds.min, bounds.max).scale(0.5);
        // updated to direction vector from light to center
        objectPos.sub(lightPos);
        // compute near extent in place
        objectPos.nearExtent(bounds, objectPos);

        // make sure the bounds intersects with the bounding sphere centered
        // on the light with a radius equal to the falloff distance

        // distance between lightPos and objectPos is distance of light
        // to closest point on the entity bounds
        return objectPos.distanceSquared(lightPos) <= falloff;
    }

    public static LightInfluence.Factory<PointLight> factory() {
        return new LightInfluence.Factory<PointLight>() {
            @Override
            public LightInfluence create(PointLight light, Matrix4 lightTransform) {
                if (light.getFalloffDistance() <= 0.0) {
                    return GlobalLightInfluence.INSTANCE;
                } else {
                    return new PointLightInfluence(lightTransform, light.getFalloffDistance());
                }
            }
        };
    }
}
