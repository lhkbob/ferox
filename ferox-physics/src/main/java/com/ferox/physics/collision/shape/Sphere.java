package com.ferox.physics.collision.shape;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

/**
 * Sphere is a ConvexShape that represents a mathematical sphere.
 * 
 * @author Michael Ludwig
 */
public class Sphere extends ConvexShape {
    private float radius;
    private float inertiaTensorPartial;

    /**
     * Create a new Sphere with the initial radius, ignoring the margin.
     * 
     * @see #setRadius(float)
     * @param radius The initial radius
     * @throws IllegalArgumentException if radius is less than or equal to 0
     */
    public Sphere(float radius) {
        setRadius(radius);
    }

    /**
     * Set the radius of the sphere. This does not include the margin that all
     * shapes are padded with.
     * 
     * @param radius The new radius, must be greater than 0
     * @throws IllegalArgumentException if radius is less than or equal to 0
     */
    public void setRadius(float radius) {
        if (radius <= 0f)
            throw new IllegalArgumentException("Radius must be greater than 0, not: " + radius);
        
        this.radius = radius;
        inertiaTensorPartial = 2f * radius * radius / 5f;
        updateBounds();
    }

    /**
     * Return the current radius of the sphere, excluding the margin.
     * 
     * @return The radius of the sphere
     */
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
