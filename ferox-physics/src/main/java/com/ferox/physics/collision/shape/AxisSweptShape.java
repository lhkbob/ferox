package com.ferox.physics.collision.shape;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

/**
 * AxisSweptShape represents a class of class of convex shapes that features a
 * dominant axis and a curve that is swept around that axis. Choosing different
 * dominant axis for the shape is equivalent to applying a rotation. Some
 * examples include cylinders, cones and capsules.
 * 
 * @author Michael Ludwig
 */
public abstract class AxisSweptShape extends ConvexShape {
    public static enum Axis {
        X, Y, Z
    }
    
    protected final Vector3 inertiaTensorPartial;
    protected final Axis dominantAxis;

    /**
     * Create an AxisSweptShape that uses the given dominant axis.
     * 
     * @param dominantAxis The dominant axis for the created shape
     * @throws NullPointerException if dominantAxis is null
     */
    public AxisSweptShape(Axis dominantAxis) {
        if (dominantAxis == null)
            throw new NullPointerException("Axis cannot be null");
        this.dominantAxis = dominantAxis;
        inertiaTensorPartial = new Vector3();
    }
    
    /**
     * @return The dominant axis of the shape
     */
    public Axis getDominantAxis() {
        return dominantAxis;
    }

    @Override
    public Vector3 getInertiaTensor(double mass, Vector3 result) {
        if (result == null)
            result = new Vector3();
        return result.scale(inertiaTensorPartial, mass);
    }

    /**
     * Return the sign of component of <tt>v</tt> matching the shape's dominant
     * axis. Thus, if the dominant axis was Z, it returns 1 of
     * <code>v.getZ()</code> is positive, and -1 if not.
     * 
     * @param v The input vector whose sign is queried
     * @return The sign of the dominant component of v
     * @throws NullPointerException if v is null
     */
    protected int sign(@Const Vector3 v) {
        switch(dominantAxis) {
        case X: return (v.x >= 0.0 ? 1 : -1);
        case Y: return (v.y >= 0.0 ? 1 : -1);
        case Z: return (v.z >= 0.0 ? 1 : -1);
        default: return 0;
        }
    }

    /**
     * Evaluate the "sigma" function of <tt>v</tt>. This is the same as the
     * projected distance of v to the dominant axis.
     * 
     * @param v The input vector evaluated by the sigma function
     * @return The projected distance of v to the dominant axis
     * @throws NullPointerException if v is null
     */
    protected double sigma(Vector3 v) {
        double c1, c2;
        switch(dominantAxis) {
        case X: c1 = v.y; c2 = v.z; break;
        case Y: c1 = v.x; c2 = v.z; break;
        case Z: c1 = v.x; c2 = v.y; break;
        default: return -1;
        }
        
        return Math.sqrt(c1 * c1 + c2 * c2);
    }
}
