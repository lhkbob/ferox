package com.ferox.physics.collision.shape;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

public abstract class AxisSweptShape extends ConvexShape {
    public static enum Axis {
        X, Y, Z
    }
    
    protected final Vector3f inertiaTensorPartial;
    protected final Axis dominantAxis;
    
    public AxisSweptShape(Axis dominantAxis) {
        if (dominantAxis == null)
            throw new NullPointerException("Axis cannot be null");
        this.dominantAxis = dominantAxis;
        inertiaTensorPartial = new Vector3f();
    }
    
    public Axis getDominantAxis() {
        return dominantAxis;
    }

    @Override
    public MutableVector3f getInertiaTensor(float mass, MutableVector3f result) {
        return inertiaTensorPartial.scale(mass, result);
    }
    
    protected int sign(ReadOnlyVector3f v) {
        switch(dominantAxis) {
        case X: return (v.getX() >= 0f ? 1 : -1);
        case Y: return (v.getY() >= 0f ? 1 : -1);
        case Z: return (v.getZ() >= 0f ? 1 : -1);
        default: return 0;
        }
    }
    
    protected float sigma(ReadOnlyVector3f v) {
        float c1, c2;
        switch(dominantAxis) {
        case X: c1 = v.getY(); c2 = v.getZ(); break;
        case Y: c1 = v.getX(); c2 = v.getZ(); break;
        case Z: c1 = v.getX(); c2 = v.getY(); break;
        default: return -1;
        }
        
        return (float) Math.sqrt(c1 * c1 + c2 * c2);
    }
}
