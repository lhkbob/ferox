package com.ferox.math.bounds;

import java.nio.FloatBuffer;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.ReadOnlyVector4f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.Frustum.FrustumIntersection;
import com.ferox.util.geom.Geometry;

/**
 * <p>
 * AxisAlignedBox is a bounding box represented by a minimum and maximum vertex.
 * The box formed by these two points is aligned with the basis vectors that the
 * box is defined in. The AxisAlignedBox can be transformed from one space to
 * another to allow bounds to be described in a local space and then be
 * converted into a world space.
 * </p>
 * <p>
 * The AxisAlignedBox is intended to approximate some spatial shape and
 * represents the extents of that shape. A collision or intersection with the
 * AxisAlignedBox hints that the actual shape may be collided or intersected,
 * but not necessarily. A failed collision or intersection with the bounds
 * guarantees that the wrapped shape is not collided or intersected.
 * </p>
 * <p>
 * The AxisAlignedBox is used by {@link SpatialHierarchy} implementations to
 * efficiently organize shapes within a 3D space so that spatial or view queries
 * can run quickly. An AxisAlignedBox assumes the invariant that its maximum
 * vertex is greater than an or equal to its minimum vertex. If this is not
 * true, the box is in an inconsistent state. In general, it should be assumed
 * that when an AxisAlignedBox is used for computational purposes in a method,
 * it must be consistent. Only
 * {@link #intersect(AxisAlignedBox, AxisAlignedBox)} creates an inconsistent
 * box intentionally when an intersection fails to exist.
 * </p>
 * <p>
 * Like the other simple math objects in the com.ferox.math package,
 * AxisAlignedBox implements equals() and hashCode() appropriately.
 * </p>
 * 
 * @author Michael Ludwig
 */
// FIXME: make a ReadOnlyAxisAlignedBox that has protected vectors so that AxisAlignedBox can properly
// expose setters for min/max
// FIXME: revisit this ReadOnly paradigm and see if the type abstraction is actually worth
public class AxisAlignedBox {
    private final Vector3f min;
    private final Vector3f max;
    private int lastFailedPlane;

    /**
     * Create a new AxisAlignedBox that has its minimum and maximum at the
     * origin.
     */
    public AxisAlignedBox() {
        min = new Vector3f();
        max = new Vector3f();
        lastFailedPlane = -1;
    }

    /**
     * Create a new AxisAlignedBox that uses the given minimum and maximum
     * vectors as its two control points. Both <tt>min</tt> and <tt>max</tt>
     * will be copied into the vectors used by the box. It is permissible but
     * not recommended to create an AxisAlignedBox in an inconsistent state.
     * 
     * @param min The vector coordinate to use as the minimum control point
     * @param max The vector coordinate to use as the maximum control point
     * @throws NullPointerException if min or max are null
     */
    public AxisAlignedBox(ReadOnlyVector3f min, ReadOnlyVector3f max) {
        this();
        this.min.set(min);
        this.max.set(max);
    }

    /**
     * Create a new AxisAlignedBox that is a clone of <tt>aabb</tt>.
     * 
     * @param aabb The AxisAlignedBox to copy
     * @throws NullPointerException if aabb is null
     */
    public AxisAlignedBox(AxisAlignedBox aabb) {
        this();
        set(aabb);
    }

    /**
     * Create a new AxisAlignedBox that is fitted to the coordinate data stored
     * in <tt>vertices</tt>. It is assumed that every three elements within
     * <tt>vertices</tt> represents a 3D point, ordered: x, y, z. The created
     * AxisAlignedBox will fit the set of vertices as best as possible. This
     * constructor is intended for use when creating an AxisAlignedBox enclosing
     * a {@link Geometry}.
     * 
     * @param vertices A set of 3D vertices representing a shape, either a point
     *            cloud or something more complex
     * @throws NullPointerException if vertices is null
     * @throws IllegalArgumentException if vertices.length isn't a multiple of
     *             3, or if it has fewer than 3 elements
     */
    public AxisAlignedBox(float[] vertices) {
        this();
        if (vertices == null)
            throw new NullPointerException("Vertices cannot be null");
        if (vertices.length % 3 != 0 || vertices.length < 3)
            throw new IllegalArgumentException("Vertices length must be a multiple of 3, and at least 3: " + vertices.length);
        
        int vertexCount = vertices.length / 3;
        max.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        min.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

        for (int i = 0; i < vertexCount; i++)
            enclosePoint(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2]); 
    }

    /**
     * Equivalent constructor to {@link #AxisAlignedBox(float[])} except that
     * the vertices are stored within a FloatBuffer. The vertices are accessed
     * from position to 0 up to the capacity, the position and limit are
     * ignored.
     * 
     * @param vertices A set of 3D vertices representing a shape, either a point
     *            cloud or something more complex
     * @throws NullPointerException if vertices is null
     * @throws IllegalArgumentException if vertices.capacity() isn't a multiple
     *             of 3, or if it has fewer than 3 elements
     */
    public AxisAlignedBox(FloatBuffer vertices) {
        this();
        if (vertices == null)
            throw new NullPointerException("Vertices cannot be null");
        if (vertices.capacity() % 3 != 0 || vertices.capacity() < 3)
            throw new IllegalArgumentException("Vertices length must be a multiple of 3, and at least 3: " + vertices.capacity());
        
        int vertexCount = vertices.capacity() / 3;
        max.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        min.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        
        for (int i = 0; i < vertexCount; i++)
            enclosePoint(vertices.get(i * 3), vertices.get(i * 3 + 1), vertices.get(i * 3 + 2)); 
    }
    
    private void enclosePoint(float x, float y, float z) {
        max.set(Math.max(max.getX(), x), Math.max(max.getY(), y), Math.max(max.getZ(), z));
        min.set(Math.min(min.getX(), x), Math.min(min.getY(), y), Math.min(min.getZ(), z));
    }

    /**
     * Copy the state of <tt>aabb</tt> into this AxisAlignedBox so that this
     * AxisAlignedBox is equivalent to <tt>aabb</tt>.
     * 
     * @param aabb The AxisAlignedBox to clone
     * @throws NullPointerException if aabb is null
     */
    public void set(AxisAlignedBox aabb) {
        min.set(aabb.min);
        max.set(aabb.max);
    }

    /**
     * Return the vector coordinate of this AxisAlignedBox's minimum corner. Any
     * modifications to the returned vector will modify this AxisAlignedBox.
     * 
     * @return The minimum corner of the AxisAlignedBox, in its current
     *         transform space
     */
    public Vector3f getMin() {
        return min;
    }
    
    /**
     * Return the vector coordinate of this AxisAlignedBox's maximum corner. Any
     * modifications to the returned vector will modify this AxisAlignedBox.
     * 
     * @return The maximum corner of the AxisAlignedBox, in its current
     *         transform space
     */
    public Vector3f getMax() {
        return max;
    }

    /**
     * Create and return a new Vector3f containing the center location of this
     * AxisAlignedBox. The center of the box is the average of the box's minimum
     * and maximum corners.
     * 
     * @return A new Vector3f storing the center of this box
     */
    public MutableVector3f getCenter() {
        return getCenter(null);
    }

    /**
     * Compute the center location of this AxisAlignedBox and store it within
     * <tt>result</tt>. A new Vector3f is created and returned if
     * <tt>result</tt> is null, otherwise the input vector is returned after
     * being modified.
     * 
     * @param result The Vector3f to store the center location
     * @return result or a new Vector3f if result is null
     */
    public MutableVector3f getCenter(MutableVector3f result) {
        return min.add(max, result).scale(.5f);
    }

    /**
     * <p>
     * Compute and return the intersection of this AxisAlignedBox and the
     * Frustum, <tt>f</tt>. It is assumed that the Frustum and AxisAlignedBox
     * exist in the same coordinate frame. {@link FrustumIntersection#INSIDE} is
     * returned when the AxisAlignedBox is fully contained by the Frustum.
     * {@link FrustumIntersection#INTERSECT} is returned when this box is
     * partially contained by the Frustum, and
     * {@link FrustumIntersection#OUTSIDE} is returned when the box has no
     * intersection with the Frustum.
     * </p>
     * <p>
     * If <tt>OUTSIDE</tt> is returned, it is guaranteed that the objects
     * enclosed by this box cannot be seen by the Frustum. If <tt>INSIDE</tt> is
     * returned, any object {@link #contains(AxisAlignedBox) contained} by this
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

        Vector3f c = TEMP1.get();

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
     * Return true if this AxisAlignedBox and <tt>other</tt> intersect. It is
     * assumed that both boxes exist within the same coordinate space. An
     * intersection occurs if any portion of the two boxes overlap.
     * 
     * @param other The AxisAlignedBox to test for intersection
     * @return True if this box and other intersect each other
     * @throws NullPointerException if other is null
     */
    public boolean intersects(AxisAlignedBox other) {
        return (max.getX() >= other.min.getX() && min.getX() <= other.max.getX()) &&
               (max.getY() >= other.min.getY() && min.getY() <= other.max.getY()) &&
               (max.getZ() >= other.min.getZ() && min.getZ() <= other.max.getZ());
    }

    /**
     * Return true if <tt>other</tt> is completely contained within the extents
     * of this AxisAlignedBox. It is assumed that both bounds exist within the
     * same coordinate space.
     * 
     * @param other The AxisAlignedBox to test for containment
     * @return True when other is contained in this box
     * @throws NullPointerException if other is null
     */
    public boolean contains(AxisAlignedBox other) {
        return (min.getX() <= other.min.getX() && max.getX() >= other.max.getX()) &&
               (min.getY() <= other.min.getY() && max.getY() >= other.max.getY()) &&
               (min.getZ() <= other.min.getZ() && max.getZ() >= other.max.getZ());
    }

    /**
     * Compute the intersection of this AxisAlignedBox and <tt>other</tt> and
     * store it into <tt>result</tt>. If <tt>result</tt> is null, a new
     * AxisAlignedBox is created and returned, otherwise <tt>result</tt> is
     * returned. If this box and <tt>other</tt> do not
     * {@link #intersects(AxisAlignedBox) intersect}, the computed intersection
     * will be an inconsistent box.
     * 
     * @param other The AxisAlignedBox to intersect with
     * @param result The AxisAlignedBox containing the result of the
     *            intersection
     * @return result, or a new AxisAlignedBox if result was null, holding the
     *         intersection of this and other
     * @throws NullPointerException if other is null
     */
    public AxisAlignedBox intersect(AxisAlignedBox other, AxisAlignedBox result) {
        if (result == null)
            result = new AxisAlignedBox();
        
        // in the event that min > max, there is no true intersection
        result.min.set(Math.max(min.getX(), other.min.getX()), 
                       Math.max(min.getY(), other.min.getY()), 
                       Math.max(min.getZ(), other.min.getZ()));
        result.max.set(Math.min(max.getX(), other.max.getX()), 
                       Math.min(max.getY(), other.max.getY()), 
                       Math.min(max.getZ(), other.max.getZ()));
        return result;
    }

    /**
     * Compute the union of this AxisAlignedBox and <tt>other</tt>, storing the
     * union into <tt>result</tt>. If <tt>result</tt> is null, a new
     * AxisAlignedBox is created and returned, which holds the union. Otherwise,
     * <tt>result</tt> is returned after being modified.
     * 
     * @param other The AxisAlignedBox to union with
     * @param result The AxisAlignedBox that will contain the result of the
     *            union
     * @return result, or a new AxisAlignedBox if result was null, holding the
     *         union of this and other
     * @throws NullPointerException if other is null
     */
    public AxisAlignedBox union(AxisAlignedBox other, AxisAlignedBox result) {
        if (result == null)
            result = new AxisAlignedBox();
        
        result.min.set(Math.min(min.getX(), other.min.getX()), 
                       Math.min(min.getY(), other.min.getY()), 
                       Math.min(min.getZ(), other.min.getZ()));
        result.max.set(Math.max(max.getX(), other.max.getX()), 
                       Math.max(max.getY(), other.max.getY()), 
                       Math.max(max.getZ(), other.max.getZ()));
        return result;
    }

    /**
     * <p>
     * Transform this AxisAlignedBox by <tt>m</tt> and store the new
     * AxisAlignedBox in <tt>result</tt>. This can be used to transform an
     * AxisAlignedBox from one coordinate space to another while preserving the
     * property that whatever was contained by the box in its pre-transform
     * space, will be contained by the transformed box after it has been
     * transformed as well. It is permissible for this AxisAlignedBox to be
     * <tt>result</tt> and have the box be correctly transformed in place. If
     * this box is not <tt>result</tt>, this box will be unmodified.
     * </p>
     * <p>
     * For best results, <tt>m</tt> should be an affine transformation.
     * </p>
     * 
     * @param m The Matrix4f to act as a transform on this AxisAlignedBox
     * @param result The AxisAlignedBox that will hold the transformed result
     * @return result, or a new AxisAlignedBox if result is null, containing the
     *         transformed box
     * @throws NullPointerException if m is null
     */
    public AxisAlignedBox transform(ReadOnlyMatrix4f m, AxisAlignedBox result) {
        // we use temporary vectors because this method isn't atomic,
        // and result might be this box
        MutableVector3f newMin = TEMP1.get().set(m.get(0, 3), m.get(1, 3), m.get(2, 3));
        MutableVector3f newMax = TEMP2.get().set(m.get(0, 3), m.get(1, 3), m.get(2, 3));
        
        float av, bv, cv;
        int i, j;
        for (i = 0; i < 3; i++) {
            for (j = 0; j < 3; j++) {
                cv = m.get(i, j);
                av = cv * min.get(j);
                bv = cv * max.get(j);
                
                if (av < bv) {
                    newMin.set(i, newMin.get(i) + av);
                    newMax.set(i, newMax.get(i) + bv);
                } else {
                    newMin.set(i, newMin.get(i) + bv);
                    newMax.set(i, newMax.get(i) + av);
                }
            }
        }
        
        // assign temporary vectors to the result
        if (result != null) {
            result.min.set(newMin);
            result.max.set(newMax);
        } else
            result = new AxisAlignedBox(newMin, newMax);
        return result;
    }
    
    @Override
    public int hashCode() {
        return min.hashCode() ^ max.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AxisAlignedBox))
            return false;
        AxisAlignedBox that = (AxisAlignedBox) o;
        return min.equals(that.min) && max.equals(that.max);
    }
    
    @Override
    public String toString() {
        return "(min=" + min + ", max=" + max + ")";
    }
    
    private void extent(ReadOnlyVector4f plane, boolean reverseDir, Vector3f result) {
        Vector3f sourceMin = (reverseDir ? max : min);
        Vector3f sourceMax = (reverseDir ? min : max);
        
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

    private static final ThreadLocal<Vector3f> TEMP1 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
    private static final ThreadLocal<Vector3f> TEMP2 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
}
