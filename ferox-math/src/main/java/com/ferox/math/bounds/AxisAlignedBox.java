package com.ferox.math.bounds;

import java.nio.FloatBuffer;

import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

/**
 * AxisAlignedBox is a mutable implementation of {@link ReadOnlyAxisAlignedBox}.
 * It exposes setters that will modify the vectors returned by {@link #getMin()}
 * and {@link #getMax()}, and overrides the return type of those methods to
 * return mutable vectors. Additionally, it provides new methods for
 * transforming, intersecting, and unifying boxes that mutate the AxisAlignedBox
 * in place. Finally, the AxisAlignedBox can safely be used as the result
 * parameter to the similar functions defined in ReadOnlyAxisAlignedBox, even if
 * it's also the caller or other input box.
 * 
 * @author Michael Ludwig
 */
public class AxisAlignedBox extends ReadOnlyAxisAlignedBox {
    private final Vector3f min;
    private final Vector3f max;

    /**
     * Create a new AxisAlignedBox that has its minimum and maximum at the
     * origin.
     */
    public AxisAlignedBox() {
        super();
        min = new Vector3f();
        max = new Vector3f();
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
     * @param aabb The ReadOnlyAxisAlignedBox to copy
     * @throws NullPointerException if aabb is null
     */
    public AxisAlignedBox(ReadOnlyAxisAlignedBox aabb) {
        this();
        set(aabb);
    }

    /**
     * Create a new AxisAlignedBox that is fitted to the coordinate data stored
     * in <tt>vertices</tt>. It is assumed that <tt>vertices</tt> holds
     * <tt>numVertices</tt> 3D vertices, starting at <tt>offset</tt>. The x, y,
     * and z coordinates are consecutive elements, with <tt>stride</tt> elements
     * between consecutive vertices. The created AxisAlignedBox will fit the set
     * of vertices as best as possible.
     * 
     * @param vertices A set of 3D vertices representing a shape, either a point
     *            cloud or something more complex
     * @param offset The first array element to take the first vertex from
     * @param stride The number of array elements between consecutive vertices
     * @param numVertices The number of vertices to use within the array
     * @throws NullPointerException if vertices is null
     * @throws ArrayIndexOutOfBoundsException if the offset, stride, and
     *             numVertices would cause an out-of-bounds access into vertices
     */
    public AxisAlignedBox(float[] vertices, int offset, int stride, int numVertices) {
        this();
        if (vertices == null)
            throw new NullPointerException("Vertices cannot be null");
        
        max.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        min.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

        int realStride = 3 + stride;
        for (int i = offset; i < numVertices; i += realStride)
            enclosePoint(vertices[i], vertices[i + 1], vertices[i + 2]); 
    }

    /**
     * Equivalent constructor to {@link #ReadOnlyAxisAlignedBox(float[])} except
     * that the vertices are stored within a FloatBuffer. The vertices are
     * accessed from 0 up to the capacity, the position and limit are ignored.
     * 
     * @param vertices A set of 3D vertices representing a shape, either a point
     *            cloud or something more complex
     * @param offset The first buffer element (measured from 0, not the
     *            position) to take the first vertex from
     * @param stride The number of buffer elements between consecutive vertices
     * @param numVertices The number of vertices within the array
     * @throws NullPointerException if vertices is null
     * @throws IndexOutOfBoundsException if the offset, stride, and numVertices
     *             would cause an out-of-bounds access into vertices
     */
    public AxisAlignedBox(FloatBuffer vertices, int offset, int stride, int numVertices) {
        this();
        if (vertices == null)
            throw new NullPointerException("Vertices cannot be null");
        
        max.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        min.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        
        int realStride = 3 + stride;
        for (int i = offset; i < numVertices; i += realStride)
            enclosePoint(vertices.get(i), vertices.get(i + 1), vertices.get(i + 2)); 
    }
    
    private void enclosePoint(float x, float y, float z) {
        max.set(Math.max(max.getX(), x), Math.max(max.getY(), y), Math.max(max.getZ(), z));
        min.set(Math.min(min.getX(), x), Math.min(min.getY(), y), Math.min(min.getZ(), z));
    }
    
    /**
     * Copy the state of <tt>aabb</tt> into this ReadOnlyAxisAlignedBox so that this
     * ReadOnlyAxisAlignedBox is equivalent to <tt>aabb</tt>.
     * 
     * @param aabb The ReadOnlyAxisAlignedBox to clone
     * @throws NullPointerException if aabb is null
     */
    public void set(ReadOnlyAxisAlignedBox aabb) {
        min.set(aabb.getMin());
        max.set(aabb.getMax());
    }

    /**
     * Copy <tt>min</tt> into the minimum corner of this AABB. This is
     * equivalent to <code>getMin().set(min)</code>
     * 
     * @param min The new minimum
     * @throws NullPointerException if min is null
     */
    public void setMin(ReadOnlyVector3f min) {
        this.min.set(min);
    }
    
    /**
     * Copy <tt>max</tt> into the maximum corner of this AABB. This is
     * equivalent to <code>getMax().set(max)</code>
     * 
     * @param max The new maximum
     * @throws NullPointerException if max is null
     */
    public void setMax(ReadOnlyVector3f max) {
        this.max.set(max);
    }

    /**
     * As {@link #intersect(ReadOnlyAxisAlignedBox, AxisAlignedBox)} but the
     * this AxisAlignedBox is also used as the result box, so it is modified in
     * place.
     * 
     * @param other The box to compute the intersection with
     * @return This AxisAlignedBox
     * @throws NullPointerException if other is null
     */
    public AxisAlignedBox intersect(ReadOnlyAxisAlignedBox other) {
        return intersect(other, this);
    }
    
    /**
     * As {@link #union(ReadOnlyAxisAlignedBox, AxisAlignedBox)} but the
     * this AxisAlignedBox is also used as the result box, so it is modified in
     * place.
     * 
     * @param other The box to compute the union with
     * @return This AxisAlignedBox
     * @throws NullPointerException if other is null
     */
    public AxisAlignedBox union(ReadOnlyAxisAlignedBox other) {
        return union(other, this);
    }

    /**
     * As {@link #transform(ReadOnlyMatrix4f)} but the this AxisAlignedBox is
     * also used as the result box, so it is modified in place.
     * 
     * @param trans The matrix transform applied to this box
     * @return This AxisAlignedBox
     * @throws NullPointerException if trans is null
     */
    public AxisAlignedBox transform(ReadOnlyMatrix4f trans) {
        return transform(trans, this);
    }

    /**
     * Overridden to return a mutable vector. Modifying this vector will
     * effectively modify the AxisAlignedBox.
     * 
     * @return A mutable vector representing this box's minimum corner
     */
    @Override
    public Vector3f getMin() {
        return min;
    }

    /**
     * Overridden to return a mutable vector. Modifying this vector will
     * effectively modify the AxisAlignedBox.
     * 
     * @return A mutable vector representing this box's maximum corner
     */
    @Override
    public Vector3f getMax() {
        return max;
    }
}
