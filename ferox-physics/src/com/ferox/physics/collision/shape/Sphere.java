package com.ferox.physics.collision.shape;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.physics.collision.ConvexShape;

public class Sphere implements ConvexShape {
    private float radius;
    private final AxisAlignedBox aabb;
    
    public Sphere(float radius) {
        aabb = new AxisAlignedBox();
        setRadius(radius);
    }
    
    public void setRadius(float radius) {
        if (radius <= 0f)
            throw new IllegalArgumentException("Radius must be greater than 0, not: " + radius);
        
        this.radius = radius;
        aabb.getMin().set(-radius, -radius, -radius);
        aabb.getMax().set(radius, radius, radius);
    }
    
    public float getRadius() {
        return radius;
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
