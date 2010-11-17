package com.ferox.physics.collision.shape;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

public class Cylinder extends AxisSweptShape {
    private float capRadius;
    private float halfHeight;
    
    public Cylinder(float capRadius, float height) {
        this(capRadius, height, Axis.Z);
    }
    
    public Cylinder(float capRadius, float height, Axis dominantAxis) {
        super(dominantAxis);
        setCapRadius(capRadius);
        setHeight(height);
    }
    
    public float getCapRadius() {
        return capRadius;
    }
    
    public float getHeight() {
        return 2f * halfHeight;
    }
    
    public void setCapRadius(float radius) {
        if (radius <= 0f)
            throw new IllegalArgumentException("Radius must be greater than 0, not: " + radius);
        capRadius = radius;
        update();
    }
    
    public void setHeight(float height) {
        if (height <= 0f)
            throw new IllegalArgumentException("Height must be greater than 0, not: " + height);
        halfHeight = height / 2f;
        update();
    }

    @Override
    public MutableVector3f computeSupport(ReadOnlyVector3f v, MutableVector3f result) {
        if (result == null)
            result = new Vector3f();
        
        float sigma = sigma(v);
        if (sigma > 0f) {
            float scale = capRadius / sigma;
            switch(dominantAxis) {
            case X: result.set(sign(v) * halfHeight, scale * v.getY(), scale * v.getZ()); break;
            case Y: result.set(scale * v.getX(), sign(v) * halfHeight, scale * v.getZ()); break;
            case Z: result.set(scale * v.getX(), scale * v.getY(), sign(v) * halfHeight); break;
            }
        } else {
            switch(dominantAxis) {
            case X: result.set(sign(v) * halfHeight, 0f, 0f); break;
            case Y: result.set(0f, sign(v) * halfHeight, 0f); break;
            case Z: result.set(0f, 0f, sign(v) * halfHeight); break;
            }
        }

        return result;
    }
    
    private void update() {
        float m1 = (3f * capRadius * capRadius + 4f * halfHeight * halfHeight) / 12f;
        float m2 = capRadius * capRadius / 2f;
        
        switch(dominantAxis) {
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
