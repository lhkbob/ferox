package com.ferox.physics.collision.shape;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

/**
 * Cylinder is a convex shape implementation that represents a cylinder. It has
 * two parameters, the height of the cylinder and the radius of the cylinder
 * caps. The orientation of the cylinder is chosen by its dominant axis.
 * 
 * @author Michael Ludwig
 */
public class Cylinder extends AxisSweptShape {
    private double capRadius;
    private double halfHeight;

    /**
     * 
     * @param capRadius
     * @param height
     */
    public Cylinder(double capRadius, double height) {
        this(capRadius, height, Axis.Z);
    }

    public Cylinder(double capRadius, double height, Axis dominantAxis) {
        super(dominantAxis);
        setCapRadius(capRadius);
        setHeight(height);
    }

    public double getCapRadius() {
        return capRadius;
    }

    public double getHeight() {
        return 2.0 * halfHeight;
    }

    public void setCapRadius(double radius) {
        if (radius <= 0.0) {
            throw new IllegalArgumentException("Radius must be greater than 0, not: " + radius);
        }
        capRadius = radius;
        update();
    }

    public void setHeight(double height) {
        if (height <= 0.0) {
            throw new IllegalArgumentException("Height must be greater than 0, not: " + height);
        }
        halfHeight = height / 2.0;
        update();
    }

    @Override
    public Vector3 computeSupport(@Const Vector3 v, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }

        double sigma = sigma(v);
        if (sigma > 0.0) {
            double scale = capRadius / sigma;
            switch (dominantAxis) {
            case X:
                result.set(sign(v) * halfHeight, scale * v.y, scale * v.z);
                break;
            case Y:
                result.set(scale * v.x, sign(v) * halfHeight, scale * v.z);
                break;
            case Z:
                result.set(scale * v.x, scale * v.y, sign(v) * halfHeight);
                break;
            }
        } else {
            switch (dominantAxis) {
            case X:
                result.set(sign(v) * halfHeight, 0, 0);
                break;
            case Y:
                result.set(0, sign(v) * halfHeight, 0);
                break;
            case Z:
                result.set(0, 0, sign(v) * halfHeight);
                break;
            }
        }

        return result;
    }

    private void update() {
        double m1 = (3.0 * capRadius * capRadius + 4.0 * halfHeight * halfHeight) / 12.0;
        double m2 = capRadius * capRadius / 2.0;

        switch (dominantAxis) {
        case X:
            inertiaTensorPartial.set(m2, m1, m1);
            break;
        case Y:
            inertiaTensorPartial.set(m1, m2, m1);
            break;
        case Z:
            inertiaTensorPartial.set(m1, m1, m2);
            break;
        }
        updateBounds();
    }
}
