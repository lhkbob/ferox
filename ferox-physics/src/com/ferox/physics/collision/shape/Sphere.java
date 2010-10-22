package com.ferox.physics.collision.shape;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.AxisAlignedBox;

public class Sphere implements ConvexShape {
    private float radius;
    private final AxisAlignedBox aabb;
    private float inertiaTensorPartial;
    
    public Sphere(float radius) {
        aabb = new AxisAlignedBox();
        
        setRadius(radius);
    }
    
    public void setRadius(float radius) {
        if (radius <= 0f)
            throw new IllegalArgumentException("Radius must be greater than 0, not: " + radius);
        
        this.radius = radius;
        aabb.getMin().set(-radius - .05f, -radius -.05f, -radius - .05f);
        aabb.getMax().set(radius + .05f, radius + .05f, radius + .05f);
        
        inertiaTensorPartial = 2f * radius * radius / 5f;
    }
    
    public float getRadius() {
        return radius;
    }
    
    @Override
    public boolean computeSupport(ReadOnlyVector3f v, MutableVector3f result) {
        v.normalize(result).scale(radius);
        return true;
    }

    @Override
    public AxisAlignedBox getBounds() {
        return aabb;
    }

    @Override
    public MutableVector3f getInertiaTensor(float mass, MutableVector3f result) {
        if (result == null)
            result = new Vector3f();
        float m = inertiaTensorPartial * mass;
        return result.set(m, m, m);
    }
}
