package com.ferox.physics.collision.shape;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

public class Box extends ConvexShape {
    private final Vector3 localTensorPartial;
    private final Vector3 halfExtents;

    public Box(double xExtent, double yExtent, double zExtent) {
        localTensorPartial = new Vector3();
        halfExtents = new Vector3();

        setExtents(xExtent, yExtent, zExtent);
    }

    public @Const
    Vector3 getHalfExtents() {
        return halfExtents;
    }

    public Vector3 getExtents() {
        return new Vector3().scale(halfExtents, 2.0);
    }

    public void setExtents(double width, double height, double depth) {
        if (width <= 0) {
            throw new IllegalArgumentException("Invalid width, must be greater than 0, not: " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Invalid height, must be greater than 0, not: " + height);
        }
        if (depth <= 0) {
            throw new IllegalArgumentException("Invalid depth, must be greater than 0, not: " + depth);
        }

        halfExtents.set(width / 2.0, height / 2.0, depth / 2.0);
        localTensorPartial.set((height * height + depth * depth) / 12.0,
                               (width * width + depth * depth) / 12.0,
                               (width * width + height * height) / 12.0);
        updateBounds();
    }

    public double getWidth() {
        return halfExtents.x * 2.0;
    }

    public double getHeight() {
        return halfExtents.y * 2.0;
    }

    public double getDepth() {
        return halfExtents.z * 2.0;
    }

    @Override
    public Vector3 computeSupport(@Const Vector3 v, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }

        double x = (v.x < 0.0 ? -halfExtents.x : halfExtents.x);
        double y = (v.y < 0.0 ? -halfExtents.y : halfExtents.y);
        double z = (v.z < 0.0 ? -halfExtents.z : halfExtents.z);

        return result.set(x, y, z);
    }

    @Override
    public Vector3 getInertiaTensor(double mass, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }
        return result.scale(localTensorPartial, mass);
    }
}
