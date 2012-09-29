package com.ferox.physics.collision.shape;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

/**
 * Sphere is a ConvexShape that represents a mathematical sphere.
 * 
 * @author Michael Ludwig
 */
public class Sphere extends ConvexShape {
    private double radius;
    private double inertiaTensorPartial;

    /**
     * Create a new Sphere with the initial radius, ignoring the margin.
     * 
     * @see #setRadius(double)
     * @param radius The initial radius
     * @throws IllegalArgumentException if radius is less than or equal to 0
     */
    public Sphere(double radius) {
        setRadius(radius);
    }

    /**
     * Set the radius of the sphere. This does not include the margin that all
     * shapes are padded with.
     * 
     * @param radius The new radius, must be greater than 0
     * @throws IllegalArgumentException if radius is less than or equal to 0
     */
    public void setRadius(double radius) {
        if (radius <= 0.0) {
            throw new IllegalArgumentException("Radius must be greater than 0, not: " + radius);
        }

        this.radius = radius;
        inertiaTensorPartial = 2.0 * radius * radius / 5.0;
        updateBounds();
    }

    /**
     * Return the current radius of the sphere, excluding the margin.
     * 
     * @return The radius of the sphere
     */
    public double getRadius() {
        return radius;
    }

    @Override
    public Vector3 computeSupport(@Const Vector3 v, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }
        return result.normalize(v).scale(radius);
    }

    @Override
    public Vector3 getInertiaTensor(double mass, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }
        double m = inertiaTensorPartial * mass;
        return result.set(m, m, m);
    }
}
