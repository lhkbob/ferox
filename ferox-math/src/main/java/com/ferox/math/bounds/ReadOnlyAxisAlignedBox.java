package com.ferox.math.bounds;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.ReadOnlyVector4f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.Frustum.FrustumIntersection;

/**
 * <p>
 * ReadOnlyAxisAlignedBox is a bounding box represented by a getMin()imum and getMax()imum
 * vertex. The box formed by these two points is aligned with the basis vectors
 * that the box is defined in. The ReadOnlyAxisAlignedBox can be transformed
 * from one space to another to allow bounds to be described in a local space
 * and then be converted into a world space.
 * </p>
 * <p>
 * The ReadOnlyAxisAlignedBox is intended to approximate some spatial shape and
 * represents the extents of that shape. A collision or intersection with the
 * ReadOnlyAxisAlignedBox hints that the actual shape may be collided or
 * intersected, but not necessarily. A failed collision or intersection with the
 * bounds guarantees that the wrapped shape is not collided or intersected.
 * </p>
 * <p>
 * The ReadOnlyAxisAlignedBox is used by {@link SpatialIndex}
 * implementations to efficiently organize shapes within a 3D space so that
 * spatial or view queries can run quickly. An ReadOnlyAxisAlignedBox assumes
 * the invariant that its maximum vertex is greater than an or equal to its
 * minimum vertex. If this is not true, the box is in an inconsistent state. In
 * general, it should be assumed that when an ReadOnlyAxisAlignedBox is used for
 * computational purposes in a method, it must be consistent. Only
 * {@link #intersect(ReadOnlyAxisAlignedBox, ReadOnlyAxisAlignedBox)} creates an
 * inconsistent box intentionally when an intersection fails to exist.
 * </p>
 * <p>
 * Like the other simple math objects in the com.ferox.math package,
 * ReadOnlyAxisAlignedBox implements equals() and hashCode() appropriately.
 * ReadOnlyAxisAlignedBox is a read-only abstraction over the concept of an
 * AABB, and {@link AxisAlignedBox} provides a concrete implementation.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class ReadOnlyAxisAlignedBox {
    private int lastFailedPlane;

    /**
     * Create a new ReadOnlyAxisAlignedBox that has its getMin()imum and getMax()imum at the
     * origin.
     */
    public ReadOnlyAxisAlignedBox() {
        lastFailedPlane = -1;
    }

    /**
     * Return the vector coordinate of this ReadOnlyAxisAlignedBox's minimum corner. 
     * The returned vector is read-only, but may be modified if the box implementation
     * exposes mutators.
     * 
     * @return The minimum corner of the ReadOnlyAxisAlignedBox, in its current
     *         transform space
     */
    public abstract ReadOnlyVector3f getMin();
    
    /**
     * Return the vector coordinate of this ReadOnlyAxisAlignedBox's maximum corner. 
     * The returned vector is read-only, but may be modified if the box implementation
     * exposes mutators.
     * 
     * @return The maximum corner of the ReadOnlyAxisAlignedBox, in its current
     *         transform space
     */
    public abstract ReadOnlyVector3f getMax();

    /**
     * Create and return a new Vector3 containing the center location of this
     * ReadOnlyAxisAlignedBox. The center of the box is the average of the box's minimum
     * and maximum corners.
     * 
     * @return A new Vector3 storing the center of this box
     */
    public MutableVector3f getCenter() {
        return getCenter(null);
    }

    /**
     * Compute the center location of this ReadOnlyAxisAlignedBox and store it within
     * <tt>result</tt>. A new Vector3 is created and returned if
     * <tt>result</tt> is null, otherwise the input vector is returned after
     * being modified.
     * 
     * @param result The Vector3 to store the center location
     * @return result or a new Vector3 if result is null
     */
    public MutableVector3f getCenter(MutableVector3f result) {
        return getMin().add(getMax(), result).scale(.5f);
    }

    /**
     * <p>
     * Compute and return the intersection of this ReadOnlyAxisAlignedBox and the
     * Frustum, <tt>f</tt>. It is assumed that the Frustum and ReadOnlyAxisAlignedBox
     * exist in the same coordinate frame. {@link FrustumIntersection#INSIDE} is
     * returned when the ReadOnlyAxisAlignedBox is fully contained by the Frustum.
     * {@link FrustumIntersection#INTERSECT} is returned when this box is
     * partially contained by the Frustum, and
     * {@link FrustumIntersection#OUTSIDE} is returned when the box has no
     * intersection with the Frustum.
     * </p>
     * <p>
     * If <tt>OUTSIDE</tt> is returned, it is guaranteed that the objects
     * enclosed by this box cannot be seen by the Frustum. If <tt>INSIDE</tt> is
     * returned, any object {@link #contains(ReadOnlyAxisAlignedBox) contained} by this
     * box will also be completely inside the Frustum. When <tt>INTERSECT</tt>
     * is returned, there is a chance that the true representation of the
     * objects enclosed by the box will be outside of the Frustum, but it is
     * unlikely. This can occur when a corner of the box intersects with the
     * planes of <tt>f</tt>, but the shape does not exist in that corner.
     * </p>
     * <p>
     * The argument <tt>planeState</tt> can be used to hint to this function
     * which planes of the Frustum require checking and which do not. When a
     * hierarchy of bounds is used, the planeState can be used to remove
     * unnecessary plane comparisons. If <tt>planeState</tt> is null it is
     * assumed that all planes need to be checked. If <tt>planeState</tt> is not
     * null, this method will mark any plane that the box is completely inside
     * of as not requiring a comparison. It is the responsibility of the caller
     * to save and restore the plane state as needed based on the structure of
     * the bound hierarchy.
     * </p>
     * 
     * @param f The Frustum to intersect this box with
     * @param planeState An optional PlaneState hint specifying which planes to
     *            check
     * @return A FrustumIntersection indicating how this box and the Frustum are
     *         related
     * @throws NullPointerException if f is null
     */
    public FrustumIntersection intersects(Frustum f, PlaneState planeState) {
        if (f == null)
            throw new NullPointerException("Frustum cannot be null");
        
        // early escape for potentially deeply nested nodes in a tree
        if (planeState != null && !planeState.getTestsRequired())
            return FrustumIntersection.INSIDE;
        
        FrustumIntersection result = FrustumIntersection.INSIDE;
        float distMax;
        float distMin;
        int plane = 0;

        Vector3f c = new Vector3f(); 

        ReadOnlyVector4f p;
        for (int i = Frustum.NUM_PLANES; i >= 0; i--) {
            // skip the last failed plane since that was is checked first,
            // or skip the default first check if we haven't failed yet
            if (i == lastFailedPlane || (i == Frustum.NUM_PLANES && lastFailedPlane < 0))
                continue;

            // check the last failed plane first, since we're likely to fail there again
            plane = (i == Frustum.NUM_PLANES ? lastFailedPlane : i);
            if (planeState == null || planeState.isTestRequired(plane)) {
                p = f.getFrustumPlane(plane);
                extent(p, false, c);
                distMax = Plane.getSignedDistance(p, c, true);
                
                if (distMax < 0) {
                    // the point closest to the plane is behind the plane, so
                    // we know the bounds must be outside of the frustum
                    lastFailedPlane = plane;
                    return FrustumIntersection.OUTSIDE;
                } else {
                    // the point closest to the plane is in front of the plane,
                    // but we need to check the farthest away point

                    extent(p, true, c);
                    distMin = Plane.getSignedDistance(p, c, true);
                    
                    if (distMin < 0) {
                        // the farthest point is behind the plane, so at best
                        // this box will be intersecting the frustum
                        result = FrustumIntersection.INTERSECT;
                    } else {
                        // the box is completely contained by the plane, so
                        // the return result can be INSIDE or INTERSECT (if set by another plane)
                        if (planeState != null)
                            planeState.setTestRequired(plane, false);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * Return true if this ReadOnlyAxisAlignedBox and <tt>other</tt> intersect. It is
     * assumed that both boxes exist within the same coordinate space. An
     * intersection occurs if any portion of the two boxes overlap.
     * 
     * @param other The ReadOnlyAxisAlignedBox to test for intersection
     * @return True if this box and other intersect each other
     * @throws NullPointerException if other is null
     */
    public boolean intersects(ReadOnlyAxisAlignedBox other) {
        return (getMax().getX() >= other.getMin().getX() && getMin().getX() <= other.getMax().getX()) &&
               (getMax().getY() >= other.getMin().getY() && getMin().getY() <= other.getMax().getY()) &&
               (getMax().getZ() >= other.getMin().getZ() && getMin().getZ() <= other.getMax().getZ());
    }

    /**
     * Return true if <tt>other</tt> is completely contained within the extents
     * of this ReadOnlyAxisAlignedBox. It is assumed that both bounds exist within the
     * same coordinate space.
     * 
     * @param other The ReadOnlyAxisAlignedBox to test for containment
     * @return True when other is contained in this box
     * @throws NullPointerException if other is null
     */
    public boolean contains(ReadOnlyAxisAlignedBox other) {
        return (getMin().getX() <= other.getMin().getX() && getMax().getX() >= other.getMax().getX()) &&
               (getMin().getY() <= other.getMin().getY() && getMax().getY() >= other.getMax().getY()) &&
               (getMin().getZ() <= other.getMin().getZ() && getMax().getZ() >= other.getMax().getZ());
    }

    /**
     * Compute the intersection of this ReadOnlyAxisAlignedBox and
     * <tt>other</tt> and store it into <tt>result</tt>. If <tt>result</tt> is
     * null, a new AxisAlignedBox is created and returned, otherwise
     * <tt>result</tt> is returned. If this box and <tt>other</tt> do not
     * {@link #intersects(ReadOnlyAxisAlignedBox) intersect}, the computed
     * intersection will be an inconsistent box.
     * 
     * @param other The ReadOnlyAxisAlignedBox to intersect with
     * @param result The AxisAlignedBox containing the result of the
     *            intersection
     * @return result, or a new AxisAlignedBox if result was null, holding the
     *         intersection of this and other
     * @throws NullPointerException if other is null
     */
    public AxisAlignedBox intersect(ReadOnlyAxisAlignedBox other, AxisAlignedBox result) {
        if (result == null)
            result = new AxisAlignedBox();
        
        // in the event that getMin() > getMax(), there is no true intersection
        result.getMin().set(Math.max(getMin().getX(), other.getMin().getX()), 
                            Math.max(getMin().getY(), other.getMin().getY()), 
                            Math.max(getMin().getZ(), other.getMin().getZ()));
        result.getMax().set(Math.min(getMax().getX(), other.getMax().getX()), 
                            Math.min(getMax().getY(), other.getMax().getY()), 
                            Math.min(getMax().getZ(), other.getMax().getZ()));
        return result;
    }

    /**
     * Compute the union of this ReadOnlyAxisAlignedBox and <tt>other</tt>, storing the
     * union into <tt>result</tt>. If <tt>result</tt> is null, a new
     * AxisAlignedBox is created and returned, which holds the union. Otherwise,
     * <tt>result</tt> is returned after being modified.
     * 
     * @param other The ReadOnlyAxisAlignedBox to union with
     * @param result The AxisAlignedBox that will contain the result of the
     *            union
     * @return result, or a new AxisAlignedBox if result was null, holding the
     *         union of this and other
     * @throws NullPointerException if other is null
     */
    public AxisAlignedBox union(ReadOnlyAxisAlignedBox other, AxisAlignedBox result) {
        if (result == null)
            result = new AxisAlignedBox();
        
        result.getMin().set(Math.min(getMin().getX(), other.getMin().getX()), 
                            Math.min(getMin().getY(), other.getMin().getY()), 
                            Math.min(getMin().getZ(), other.getMin().getZ()));
        result.getMax().set(Math.max(getMax().getX(), other.getMax().getX()), 
                            Math.max(getMax().getY(), other.getMax().getY()), 
                            Math.max(getMax().getZ(), other.getMax().getZ()));
        return result;
    }

    /**
     * <p>
     * Transform this ReadOnlyAxisAlignedBox by <tt>m</tt> and store the new
     * result in <tt>result</tt>. This can be used to transform an
     * ReadOnlyAxisAlignedBox from one coordinate space to another while
     * preserving the property that whatever was contained by the box in its
     * pre-transform space, will be contained by the transformed box after it
     * has been transformed as well.
     * <p>
     * For best results, <tt>m</tt> should be an affine transformation.
     * </p>
     * 
     * @param m The Matrix4 to act as a transform on this
     *            ReadOnlyAxisAlignedBox
     * @param result The AxisAlignedBox that will hold the transformed result
     * @return result, or a new AxisAlignedBox if result is null, containing the
     *         transformed box
     * @throws NullPointerException if m is null
     */
    public AxisAlignedBox transform(ReadOnlyMatrix4f m, AxisAlignedBox result) {
        // make sure we have an instance to work with
        if (result == null)
            result = new AxisAlignedBox();
        
        // cache these vectors so we're not making x18 abstract calls instead of
        // just these 2
        ReadOnlyVector3f min = getMin();
        ReadOnlyVector3f max = getMax();
        
        // if we operate on this box, we need temporary vectors to accumulate
        // the new bounds in (since it's not atomic), but if they're different,
        // we can accumulate the transformed box directly within result
        Vector3f newMin, newMax;
        if (result == this) {
            newMin = new Vector3f(); 
            newMax = new Vector3f(); 
        } else {
            newMin = result.getMin();
            newMax = result.getMax();
        }
        
        float av, bv, cv;
        float minc, maxc;
        int i, j;
        for (i = 0; i < 3; i++) {
            // we manipulate the ith coordinate in minc and maxc
            // and then re-assign it at the end of the loop to reduce function calls
            minc = m.get(i, 3);
            maxc = minc;
            
            for (j = 0; j < 3; j++) {
                cv = m.get(i, j);
                av = cv * min.get(j);
                bv = cv * max.get(j);
                
                if (av < bv) {
                    minc += av;
                    maxc += bv;
                } else {
                    minc += bv;
                    maxc += av;
                }
            }
            
            newMin.set(i, minc);
            newMax.set(i, maxc);
        }
        
        // assign temporary vectors to the result if we're operating on this box,
        // otherwise newMin/max == result.getMin/Max() so they've been updated 
        // in place
        if (result == this) {
            result.getMin().set(newMin);
            result.getMax().set(newMax);
        }
        return result;
    }
    
    @Override
    public int hashCode() {
        return (17 * getMin().hashCode()) ^ (31 * getMax().hashCode());
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ReadOnlyAxisAlignedBox))
            return false;
        ReadOnlyAxisAlignedBox that = (ReadOnlyAxisAlignedBox) o;
        return getMin().equals(that.getMin()) && getMax().equals(that.getMax());
    }
    
    @Override
    public String toString() {
        return "(getMin()=" + getMin() + ", getMax()=" + getMax() + ")";
    }
    
    private void extent(ReadOnlyVector4f plane, boolean reverseDir, Vector3f result) {
        ReadOnlyVector3f sourceMin = (reverseDir ? getMax() : getMin());
        ReadOnlyVector3f sourceMax = (reverseDir ? getMin() : getMax());
        
        if (plane.getX() > 0) {
            if (plane.getY() > 0) {
                if (plane.getZ() > 0)
                    result.set(sourceMax.getX(), sourceMax.getY(), sourceMax.getZ());
                else
                    result.set(sourceMax.getX(), sourceMax.getY(), sourceMin.getZ());
            } else {
                if (plane.getZ() > 0)
                    result.set(sourceMax.getX(), sourceMin.getY(), sourceMax.getZ());
                else
                    result.set(sourceMax.getX(), sourceMin.getY(), sourceMin.getZ());
            }
        } else {
            if (plane.getY() > 0) {
                if (plane.getZ() > 0)
                    result.set(sourceMin.getX(), sourceMax.getY(), sourceMax.getZ());
                else
                    result.set(sourceMin.getX(), sourceMax.getY(), sourceMin.getZ());
            } else {
                if (plane.getZ() > 0)
                    result.set(sourceMin.getX(), sourceMin.getY(), sourceMax.getZ());
                else
                    result.set(sourceMin.getX(), sourceMin.getY(), sourceMin.getZ());
            }
        }
    }
}
