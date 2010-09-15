package com.ferox.physics.collision.shape;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.physics.collision.ConvexShape;

public abstract class AxisSweptShape implements ConvexShape {
    public static enum Axis {
        X, Y, Z
    }
    
    protected final Axis dominantAxis;
    protected final AxisAlignedBox aabb;
    
    public AxisSweptShape(Axis dominantAxis) {
        if (dominantAxis == null)
            throw new NullPointerException("Axis cannot be null");
        this.dominantAxis = dominantAxis;
        aabb = new AxisAlignedBox();
    }
    
    public Axis getDominantAxis() {
        return dominantAxis;
    }

    @Override
    public AxisAlignedBox getBounds() {
        return aabb;
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
