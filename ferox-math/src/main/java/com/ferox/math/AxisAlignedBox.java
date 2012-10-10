/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.math;

import java.nio.FloatBuffer;

import com.ferox.math.bounds.SpatialIndex;

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
 * The AxisAlignedBox is used by {@link SpatialIndex} implementations to
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
 * AxisAlignedBox implements equals() and hashCode() appropriately. Similarly,
 * the calling AABB is mutated.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class AxisAlignedBox implements Cloneable {
    public final Vector3 min = new Vector3();
    public final Vector3 max = new Vector3();

    /**
     * Create a new AxisAlignedBox that has its minimum and maximum at the
     * origin.
     */
    public AxisAlignedBox() {}

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
    public AxisAlignedBox(@Const Vector3 min, @Const Vector3 max) {
        this.min.set(min);
        this.max.set(max);
    }

    /**
     * Create a new AxisAlignedBox that is a clone of <tt>aabb</tt>.
     * 
     * @param aabb The AxisAlignedBox to copy
     * @throws NullPointerException if aabb is null
     */
    public AxisAlignedBox(@Const AxisAlignedBox aabb) {
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
        if (vertices == null) {
            throw new NullPointerException("Vertices cannot be null");
        }

        max.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        min.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

        int realStride = 3 + stride;
        for (int i = offset; i < numVertices * realStride; i += realStride) {
            enclosePoint(vertices[i], vertices[i + 1], vertices[i + 2]);
        }
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
        if (vertices == null) {
            throw new NullPointerException("Vertices cannot be null");
        }

        max.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        min.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

        int realStride = 3 + stride;
        for (int i = offset; i < numVertices * realStride; i += realStride) {
            enclosePoint(vertices.get(i), vertices.get(i + 1), vertices.get(i + 2));
        }
    }

    private void enclosePoint(float x, float y, float z) {
        max.set(Math.max(max.x, x), Math.max(max.y, y), Math.max(max.z, z));
        min.set(Math.min(min.x, x), Math.min(min.y, y), Math.min(min.z, z));
    }

    @Override
    public AxisAlignedBox clone() {
        return new AxisAlignedBox(this);
    }

    /**
     * Copy the state of <tt>aabb</tt> into this AxisAlignedBox so that this
     * AxisAlignedBox is equivalent to <tt>aabb</tt>.
     * 
     * @param aabb The AxisAlignedBox to clone
     * @throws NullPointerException if aabb is null
     */
    public void set(@Const AxisAlignedBox aabb) {
        min.set(aabb.min);
        max.set(aabb.max);
    }

    /**
     * As {@link #intersect(AxisAlignedBox, AxisAlignedBox)} where the first
     * argument is this AxisAlignedBox.
     * 
     * @param other The box to compute the intersection with
     * @return This AxisAlignedBox
     * @throws NullPointerException if other is null
     */
    public AxisAlignedBox intersect(@Const AxisAlignedBox other) {
        return intersect(this, other);
    }

    /**
     * As {@link #union(AxisAlignedBox, AxisAlignedBox)} where the first
     * argument is this AxisAlignedBox.
     * 
     * @param other The box to compute the union with
     * @return This AxisAlignedBox
     * @throws NullPointerException if other is null
     */
    public AxisAlignedBox union(@Const AxisAlignedBox other) {
        return union(other, this);
    }

    /**
     * As {@link #transform(AxisAlignedBox, Matrix4)} where the first argument
     * is this AxisAlignedBox.
     * 
     * @param trans The matrix transform applied to this box
     * @return This AxisAlignedBox
     * @throws NullPointerException if trans is null
     */
    public AxisAlignedBox transform(@Const Matrix4 trans) {
        return transform(this, trans);
    }

    /**
     * Create and return a new Vector3 containing the center location of this
     * AxisAlignedBox. The center of the box is the average of the box's minimum
     * and maximum corners.
     * 
     * @return A new Vector3 storing the center of this box
     */
    public Vector3 getCenter() {
        return new Vector3().add(min, max).scale(0.5);
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
    public boolean intersects(@Const AxisAlignedBox other) {
        return (max.x >= other.min.x && min.x <= other.max.x) && (max.y >= other.min.y && min.y <= other.max.y) && (max.z >= other.min.z && min.z <= other.max.z);
    }

    /**
     * Return true if <tt>other</tt> is completely contained within the extents
     * of this ReadOnlyAxisAlignedBox. It is assumed that both bounds exist
     * within the same coordinate space.
     * 
     * @param other The AxisAlignedBox to test for containment
     * @return True when other is contained in this box
     * @throws NullPointerException if other is null
     */
    public boolean contains(@Const AxisAlignedBox other) {
        return (min.x <= other.min.x && max.x >= other.max.x) && (min.y <= other.min.y && max.y >= other.max.y) && (min.z <= other.min.z && max.z >= other.max.z);
    }

    /**
     * Compute the intersection of <tt>a</tt> and <tt>b</tt> and store it in
     * this AxisAlignedBox. If <tt>a</tt> and <tt>b</tt> do not
     * {@link #intersects(ReadOnlyAxisAlignedBox) intersect}, the computed
     * intersection will be an inconsistent box.
     * 
     * @param a The first AxisAlignedBox in the intersection
     * @param b The second AxisAlignedBox in the intersection
     * @return This AxisAlignedBox
     * @throws NullPointerException if a or b are null
     */
    public AxisAlignedBox intersect(@Const AxisAlignedBox a, @Const AxisAlignedBox b) {
        // in the event that getMin() > getMax(), there is no true intersection
        min.set(Math.max(a.min.x, b.min.x), Math.max(a.min.y, b.min.y),
                Math.max(a.min.z, b.min.z));
        max.set(Math.min(a.max.x, b.max.x), Math.min(a.max.y, b.max.y),
                Math.min(a.max.z, b.max.z));
        return this;
    }

    /**
     * Compute the union of <tt>a</tt> and <tt>b</tt> and store the computed
     * bounds in this AxisAlignedBox.
     * 
     * @param a The AxisAlignedBox that is part of the union
     * @param b The AxisAlignedBox that is part of the union
     * @return This AxisAlignedBox
     * @throws NullPointerException if a or b are null
     */
    public AxisAlignedBox union(@Const AxisAlignedBox a, @Const AxisAlignedBox b) {
        min.set(Math.min(a.min.x, b.min.x), Math.min(a.min.y, b.min.y),
                Math.min(a.min.z, b.min.z));
        max.set(Math.max(a.max.x, b.max.x), Math.max(a.max.y, b.max.y),
                Math.max(a.max.z, b.max.z));
        return this;
    }

    /**
     * <p>
     * Transform <tt>aabb</tt> by <tt>m</tt> and store the transformed bounds in
     * this AxisAlignedBox. This can be used to transform an AxisAlignedBox from
     * one coordinate space to another while preserving the property that
     * whatever was contained by the box in its original space, will be
     * contained by the transformed box after it has been transformed as well.
     * <p>
     * For best results, <tt>m</tt> should be an affine transformation.
     * </p>
     * 
     * @param aabb The AxisAlignedBox that is transformed
     * @param m The Matrix4 to act as a transform
     * @return This AxisAlignedBox
     * @throws NullPointerException if aabb or m are null
     */
    public AxisAlignedBox transform(@Const AxisAlignedBox aabb, @Const Matrix4 m) {
        // clone the state in case aabb == this
        double minX = aabb.min.x;
        double minY = aabb.min.y;
        double minZ = aabb.min.z;
        double maxX = aabb.max.x;
        double maxY = aabb.max.y;
        double maxZ = aabb.max.z;

        double av, bv;
        double minc, maxc;

        // this is an unrolled loop that goes over the upper 3x3 matrix
        // - this avoids the if's required if we used get(i, j)

        // row 0
        {
            minc = m.m03;
            maxc = m.m03;

            // col 0
            av = m.m00 * minX;
            bv = m.m00 * maxX;
            if (av < bv) {
                minc += av;
                maxc += bv;
            } else {
                minc += bv;
                maxc += av;
            }
            // col 1
            av = m.m01 * minY;
            bv = m.m01 * maxY;
            if (av < bv) {
                minc += av;
                maxc += bv;
            } else {
                minc += bv;
                maxc += av;
            }
            // col 2
            av = m.m02 * minZ;
            bv = m.m02 * maxZ;
            if (av < bv) {
                minc += av;
                maxc += bv;
            } else {
                minc += bv;
                maxc += av;
            }

            min.x = minc;
            max.x = maxc;
        }
        // row 1
        {
            minc = m.m13;
            maxc = m.m13;

            // col 0
            av = m.m10 * minX;
            bv = m.m10 * maxX;
            if (av < bv) {
                minc += av;
                maxc += bv;
            } else {
                minc += bv;
                maxc += av;
            }
            // col 1
            av = m.m11 * minY;
            bv = m.m11 * maxY;
            if (av < bv) {
                minc += av;
                maxc += bv;
            } else {
                minc += bv;
                maxc += av;
            }
            // col 2
            av = m.m12 * minZ;
            bv = m.m12 * maxZ;
            if (av < bv) {
                minc += av;
                maxc += bv;
            } else {
                minc += bv;
                maxc += av;
            }

            min.y = minc;
            max.y = maxc;
        }
        // row 2
        {
            minc = m.m23;
            maxc = m.m23;

            // col 0
            av = m.m20 * minX;
            bv = m.m20 * maxX;
            if (av < bv) {
                minc += av;
                maxc += bv;
            } else {
                minc += bv;
                maxc += av;
            }
            // col 1
            av = m.m21 * minY;
            bv = m.m21 * maxY;
            if (av < bv) {
                minc += av;
                maxc += bv;
            } else {
                minc += bv;
                maxc += av;
            }
            // col 2
            av = m.m22 * minZ;
            bv = m.m22 * maxZ;
            if (av < bv) {
                minc += av;
                maxc += bv;
            } else {
                minc += bv;
                maxc += av;
            }

            min.z = minc;
            max.z = maxc;
        }

        return this;
    }

    @Override
    public int hashCode() {
        return (17 * min.hashCode()) ^ (31 * max.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AxisAlignedBox)) {
            return false;
        }
        AxisAlignedBox that = (AxisAlignedBox) o;
        return min.equals(that.min) && max.equals(that.max);
    }

    @Override
    public String toString() {
        return "(min=" + min + ", max=" + max + ")";
    }
}
