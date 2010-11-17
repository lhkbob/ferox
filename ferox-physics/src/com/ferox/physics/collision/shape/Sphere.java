package com.ferox.physics.collision.shape;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

public class Sphere extends ConvexShape {
    private float radius;
    private float inertiaTensorPartial;
    
    public Sphere(float radius) {
        setRadius(radius);
    }
    
    public void setRadius(float radius) {
        if (radius <= 0f)
            throw new IllegalArgumentException("Radius must be greater than 0, not: " + radius);
        
        this.radius = radius;
        inertiaTensorPartial = 2f * radius * radius / 5f;
        updateBounds();
    }
    
    public float getRadius() {
        return radius;
    }
    
    @Override
    public MutableVector3f computeSupport(ReadOnlyVector3f v, MutableVector3f result) {
        return v.normalize(result).scale(radius);
    }

    @Override
    public MutableVector3f getInertiaTensor(float mass, MutableVector3f result) {
        if (result == null)
            result = new Vector3f();
        float m = inertiaTensorPartial * mass;
        return result.set(m, m, m);
    }
}
