package com.ferox.scene.controller.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.scene.PointLight;
import com.lhkbob.entreri.TypeId;

public class PointLightInfluence implements LightInfluence<PointLight> {
    private final Vector3 lightPos = new Vector3();
    private final Vector3 objectPos = new Vector3();
    
    @Override
    public boolean influences(PointLight light, @Const Matrix4 lightTransform, @Const AxisAlignedBox bounds) {
        if (light.getFalloffDistance() <= 0.0) {
            // no energy falloff so the light will influence everything
            return true;
        } else {
            // make sure the bounds intersects with the bounding sphere centered
            // on the light with a radius equal to the falloff distance
            lightPos.set(lightTransform.m03, lightTransform.m13, lightTransform.m23)
                    .add(light.getPosition());
            
            // center of aabb
            objectPos.add(bounds.min, bounds.max).scale(0.5);
            // updated to direction vector from light to center
            objectPos.sub(lightPos);
            // compute near extent in place
            objectPos.nearExtent(bounds, objectPos);
            
            // distance between lightPos and objectPos is distance of light
            // to closest point on the entity bounds
            return objectPos.distanceSquared(lightPos) <= light.getFalloffDistance() * light.getFalloffDistance();
        }
    }

    @Override
    public TypeId<PointLight> getType() {
        return PointLight.ID;
    }
}
