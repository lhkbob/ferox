package com.ferox.physics.collision.shape;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

public class Cone extends AxisSweptShape {
    private float halfHeight;
    private float baseRadius;
    
    public Cone(float baseRadius, float height) {
        this(baseRadius, height, Axis.Z);
    }
    
    public Cone(float baseRadius, float height, Axis dominantAxis) {
        super(dominantAxis);
        setBaseRadius(baseRadius);
        setHeight(height);
    }
    
    public float getHeight() {
        return 2f * halfHeight;
    }
    
    public float getBaseRadius() {
        return baseRadius;
    }
    
    public void setHeight(float height) {
        if (height <= 0f)
            throw new IllegalArgumentException("Height must be greater than 0, not: " + height);
        this.halfHeight = height / 2f;
        updateBounds();
    }
    
    public void setBaseRadius(float radius) {
        if (radius <= 0f)
            throw new IllegalArgumentException("Radius must be greater than 0, not: " + radius);
        baseRadius = radius;
        updateBounds();
    }
    
    @Override
    public MutableVector3f computeSupport(ReadOnlyVector3f v, MutableVector3f result) {
        if (result == null)
            result = new Vector3f();
        
        float sin = baseRadius / (float) Math.sqrt(baseRadius * baseRadius + 4 * halfHeight * halfHeight);
        switch(dominantAxis) {
        case X:
            if (v.getX() <= v.length() * sin) {
                float sigma = sigma(v);
                if (sigma <= 0f)
                    result.set(-halfHeight, 0f, 0f);
                else
                    result.set(-halfHeight, baseRadius / sigma * v.getY(), baseRadius / sigma * v.getZ());
            } else
                result.set(halfHeight, 0f, 0f);
            break;
        case Y:
            if (v.getY() <= v.length() * sin) {
                float sigma = sigma(v);
                if (sigma <= 0f)
                    result.set(0f, -halfHeight, 0f);
                else
                    result.set(baseRadius / sigma * v.getX(), -halfHeight, baseRadius / sigma * v.getZ());
            } else
                result.set(0f, halfHeight, 0f);
            break;
        case Z:
            if (v.getZ() <= v.length() * sin) {
                float sigma = sigma(v);
                if (sigma <= 0f)
                    result.set(-0f, 0f, -halfHeight);
                else
                    result.set(baseRadius / sigma * v.getX(), baseRadius / sigma * v.getY(), -halfHeight);
            } else
                result.set(0f, 0f, halfHeight);
            break;
        }

        return result;
    }
    
    private void updateBounds() {
        Vector3f min = aabb.getMin();
        Vector3f max = aabb.getMax();
        
        switch(dominantAxis) {
        case X:
            min.set(-halfHeight / 2f, -baseRadius, -baseRadius);
            max.set(halfHeight / 2f, baseRadius, baseRadius);
            break;
        case Y:
            min.set(-baseRadius, -halfHeight / 2f, -baseRadius);
            max.set(baseRadius, halfHeight / 2f, baseRadius);
            break;
        case Z:
            min.set(-baseRadius, -baseRadius, -halfHeight / 2f);
            max.set(baseRadius, baseRadius, halfHeight / 2f);
            break;
        }
    }
}
