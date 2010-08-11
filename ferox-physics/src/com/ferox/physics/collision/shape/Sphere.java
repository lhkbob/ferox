package com.ferox.physics.collision.shape;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.physics.collision.ConvexShape;

public class Sphere implements ConvexShape {
    private final float radius;
    private final AxisAlignedBox aabb;
    
    public Sphere(float radius) {
        this.radius = radius;
        aabb = new AxisAlignedBox(new Vector3f(-radius, -radius, -radius), 
                                  new Vector3f(radius, radius, radius));
    }
    
    
    @Override
    public Vector3f computeSupport(ReadOnlyVector3f v, Vector3f result) {
        return v.normalize(result).scale(radius);
    }

    @Override
    public AxisAlignedBox getBounds() {
        return aabb;
    }
}
