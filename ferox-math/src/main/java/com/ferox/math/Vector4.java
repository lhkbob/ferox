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

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * <p/>
 * Vector4 provides an implementation of a 4-element mathematical vector with many common
 * operations available from linear algebra. It's 4 components are exposed as public
 * fields for performance reasons. This means vectors are always mutable.
 * <p/>
 * <p/>
 * The {@link Const} annotation can be used the same way that the 'const' token can modify
 * a type in C++. On input, it declares the method will not modify that instance; on
 * output, it declares that the returned instance should not be modified (internal code
 * might still modify it in some controlled manner).
 * <p/>
 * In all mathematical functions whose result is a vector, the Vector4 calling the method
 * will contain the result. The input vectors will be left unmodified. It is safe for the
 * calling vector to be any vector parameter into the function.
 *
 * @author Michael Ludwig
 */
public final class Vector4 implements Cloneable {
    public double x;
    public double y;
    public double z;
    public double w;

    /**
     * Create a new vector with all components equal to 0.
     */
    public Vector4() {
        this(0, 0, 0, 0);
    }

    /**
     * Create a new vector that copies the x, y, z, and w values from <tt>v</tt> .
     *
     * @param v The vector to copy
     */
    public Vector4(@Const Vector4 v) {
        this(v.x, v.y, v.z, v.w);
    }

    /**
     * Create a new vector with the given x, y, z, and w values.
     *
     * @param x The x value
     * @param y The y value
     * @param z The z value
     * @param w The w value
     */
    public Vector4(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    @Override
    public Vector4 clone() {
        return new Vector4(this);
    }

    /**
     * Floor each component of the vector <tt>v</tt> and store them in this vector.
     *
     * @param v The vector to be floored
     *
     * @return This vector
     *
     * @throws NullPointerException if v is null
     */
    public Vector4 floor(Vector4 v) {
        return set(Math.floor(v.x), Math.floor(v.y), Math.floor(v.z), Math.floor(v.w));
    }

    /**
     * Floor this vector component-wise.
     *
     * @return This vector
     */
    public Vector4 floor() {
        return floor(this);
    }

    /**
     * Compute the ceiling of each component of the vector <tt>v</tt> and store them in
     * this vector.
     *
     * @param v The vector to be ceil'ed
     *
     * @return This vector
     *
     * @throws NullPointerException if v is null
     */
    public Vector4 ceil(Vector4 v) {
        return set(Math.ceil(v.x), Math.ceil(v.y), Math.ceil(v.z), Math.ceil(v.w));
    }

    /**
     * Compute the ceiling this vector component-wise.
     *
     * @return This vector
     */
    public Vector4 ceil() {
        return ceil(this);
    }

    /**
     * Solve the linear system of equations, <code>[m] x [this] = [a]</code> and store the
     * resultant values of (x, y, z, w) into this vector:
     * <p/>
     * <pre>
     * m.m00*x + m.m01*y + m.m02*z + m.m03*w = a.x
     * m.m10*x + m.m11*y + m.m12*z + m.m13*w = a.y
     * m.m20*x + m.m21*y + m.m22*z + m.m23*w = a.z
     * m.m30*x + m.m31*y + m.m32*z + m.m33*w = a.w
     * </pre>
     *
     * @param m The matrix describing the coefficients of the 4 equations
     * @param a The vector constraining the values that solve the linear system of
     *          equations
     *
     * @return This vector
     *
     * @throws ArithmeticException  if no solution or an infinite solutions exist
     * @throws NullPointerException if m or a are null
     */
    public Vector4 solve(@Const Matrix4 m, @Const Vector4 a) {
        // the system is b = [A]x and we're solving for x
        // which becomes [A]^-1 b = x
        Matrix4 inv = new Matrix4().inverse(m);
        return mul(inv, a);
    }

    /**
     * <p/>
     * Compute <code>[b] x [m]</code> and store the resulting 1x4 matrix in this vector.
     * For sake of multiplication, [b] is considered to be a 1x4 matrix, where x is the
     * first column, y is the second, z is the third, and w is the fourth.
     * <p/>
     * <p/>
     * Note that this uses the reverse order of {@link #mul(Matrix4, Vector4)}.
     *
     * @param b Vector to be interpreted as a 1x4 matrix in the multiplication
     * @param m The matrix in the right side of the multiplication
     *
     * @return This vector
     *
     * @throws NullPointerException if m or b are null
     */
    public Vector4 mul(@Const Vector4 b, @Const Matrix4 m) {
        return set(m.m00 * b.x + m.m10 * b.y + m.m20 * b.z + m.m30 * b.w,
                   m.m01 * b.x + m.m11 * b.y + m.m21 * b.z + m.m31 * b.w,
                   m.m02 * b.x + m.m12 * b.y + m.m22 * b.z + m.m32 * b.w,
                   m.m03 * b.x + m.m13 * b.y + m.m23 * b.z + m.m33 * b.w);
    }

    /**
     * Compute <code>[m] x [b]</code> and store the resulting 4x1 matrix in this vector.
     * For sake of multiplication, [b] is considered to be a 4x1 matrix where x is the
     * first row, y is the second, z is the third, and w is the fourth.
     *
     * @param m The matrix on the left side of the multiplication
     * @param b The vector to interpret as a 4x1 matrix
     *
     * @return This vector
     *
     * @throws NullPointerException if a or b are null
     */
    public Vector4 mul(@Const Matrix4 m, @Const Vector4 b) {
        return set(m.m00 * b.x + m.m01 * b.y + m.m02 * b.z + m.m03 * b.w,
                   m.m10 * b.x + m.m11 * b.y + m.m12 * b.z + m.m13 * b.w,
                   m.m20 * b.x + m.m21 * b.y + m.m22 * b.z + m.m23 * b.w,
                   m.m30 * b.x + m.m31 * b.y + m.m32 * b.z + m.m33 * b.w);
    }

    /**
     * Compute the length of this vector.
     *
     * @return Length of this vector
     */
    public double length() {
        return Math.sqrt(lengthSquared());
    }

    /**
     * Compute the length of this vector, squared. Often, when only length comparisons are
     * necessary, the sauared length is useful because its computation doesn't involve a
     * slow square root operation.
     *
     * @return The squared length of this vector
     */
    public double lengthSquared() {
        return x * x + y * y + z * z + w * w;
    }

    /**
     * Compute the distance between this vector and <tt>v</tt>, treating both vectors as
     * 4D points.
     *
     * @param v The other vector
     *
     * @return The distance between this and v
     *
     * @throws NullPointerException if v is null
     */
    public double distance(@Const Vector4 v) {
        return Math.sqrt(distanceSquared(v));
    }

    /**
     * Compute the square of the distance between this and <tt>v</tt>, treating both
     * vectors as 4D points.
     *
     * @param v The other vector
     *
     * @return The distance between this and v
     *
     * @throws NullPointerException if v is null
     */
    public double distanceSquared(@Const Vector4 v) {
        double dx = x - v.x;
        double dy = y - v.y;
        double dz = z - v.z;
        double dw = w - v.w;
        return dx * dx + dy * dy + dz * dz + dw * dw;
    }

    /**
     * Compute and return the shortest angle between this vector and v. The returned angle
     * must be in radians.
     *
     * @param v The other vector involved
     *
     * @return The smallest angle, in radians, between this and v
     *
     * @throws NullPointerException if v is null
     */
    public double angle(@Const Vector4 v) {
        return Math.acos(dot(v) / (length() * v.length()));
    }

    /**
     * Compute and return the dot product between this vector and the given vector, v.
     *
     * @param v Vector to compute dot product with
     *
     * @return The dot product between v and this
     *
     * @throws NullPointerException if v is null
     */
    public double dot(@Const Vector4 v) {
        return x * v.x + y * v.y + z * v.z + w * v.w;
    }

    /**
     * Compute a vector orthogonal to <tt>b</tt> and store the result in this vector. The
     * orthogonal vector will be in the plane formed by <tt>a</tt> and <tt>b</tt>, and it
     * will be in the same half-plane as <tt>a</tt> formed by a line through <tt>b</tt>.
     * This uses the Gram-Schmidt process.
     *
     * @param a The vector that helps form the plane and chooses the result's orientation
     * @param b The vector the result is orthogonal to
     *
     * @return This vector
     *
     * @throws NullPointerException if a or b are null
     */
    public Vector4 ortho(@Const Vector4 a, @Const Vector4 b) {
        double tx = a.x;
        double ty = a.y;
        double tz = a.z;
        double tw = a.w;

        // if this == a, project modifies a, which is why we preserve tx, ty, tz, tw
        project(a, b);
        return set(tx - x, ty - y, tz - z, tw - w);
    }

    /**
     * Project <tt>a</tt> onto <tt>b</tt> and store the result in this vector.
     *
     * @param a The vector being projected
     * @param b The vector that is projected onto
     *
     * @return This vector
     *
     * @throws NullPointerException if a or b are null
     */
    public Vector4 project(@Const Vector4 a, @Const Vector4 b) {
        return scale(b, a.dot(b) / b.lengthSquared());
    }

    /**
     * Add <tt>a</tt> and <tt>b</tt> together and store the result in this vector.
     *
     * @param a The left side of the addition
     * @param b The right side of the addition
     *
     * @return This vector
     *
     * @throws NullPointerException if a or b are null
     */
    public Vector4 add(@Const Vector4 a, @Const Vector4 b) {
        return set(a.x + b.x, a.y + b.y, a.z + b.z, a.w + b.w);
    }

    /**
     * Compute <code>a + (scalar * b)</code> and store the result in this vector.
     *
     * @param a      The left side of the addition
     * @param scalar The scaling factor applied to
     * @param b      The vector to be scaled and then added to a
     *
     * @return This vector
     *
     * @throws NullPointerException if a or b are null
     */
    public Vector4 addScaled(@Const Vector4 a, double scalar, @Const Vector4 b) {
        return set(a.x + scalar * b.x, a.y + scalar * b.y, a.z + scalar * b.z,
                   a.w + scalar * b.w);
    }

    /**
     * Compute <code>a - b</code> and store the result in this vector.
     *
     * @param a The left side of the subtraction
     * @param b The right side of the subtraction
     *
     * @return This vector
     *
     * @throws NullPointerException if a or b are null
     */
    public Vector4 sub(@Const Vector4 a, @Const Vector4 b) {
        return set(a.x - b.x, a.y - b.y, a.z - b.z, a.w - b.w);
    }

    /**
     * Scale <tt>v</tt> by <tt>scalar</tt> and store the result in this vector.
     *
     * @param v      The vector whose scale is computed
     * @param scalar The scale factor
     *
     * @return This vector
     *
     * @throws NullPointerException if v is null
     */
    public Vector4 scale(@Const Vector4 v, double scalar) {
        return set(v.x * scalar, v.y * scalar, v.z * scalar, v.w * scalar);
    }

    /**
     * Normalize <tt>v</tt> to be of length 1 and store it in this vector. The vector
     * can't be normalized if it's length is 0. If it's length is very close to 0, the
     * results may suffer from loss of precision.
     *
     * @param v The vector to normalize
     *
     * @return This vector
     *
     * @throws ArithmeticException if v can't be normalized
     */
    public Vector4 normalize(@Const Vector4 v) {
        double d = v.length();
        if (d == 0f) {
            throw new ArithmeticException("Cannot normalize 0 vector");
        }
        return scale(v, 1 / d);
    }

    /**
     * As {@link #ortho(Vector4, Vector4)} with the first argument being this vector.
     *
     * @param proj
     *
     * @return This vector
     *
     * @throws NullPointerException if proj is null
     */
    public Vector4 ortho(@Const Vector4 proj) {
        return ortho(this, proj);
    }

    /**
     * As {@link #project(Vector4, Vector4)} with the first argument being this vector.
     *
     * @param proj
     *
     * @return This vector
     *
     * @throws NullPointerException if proj is null
     */
    public Vector4 project(@Const Vector4 proj) {
        return project(this, proj);
    }

    /**
     * As {@link #add(Vector4, Vector4)} with the first argument being this vector.
     *
     * @param v
     *
     * @return This vector
     *
     * @throws NullPointerException if v is null
     */
    public Vector4 add(@Const Vector4 v) {
        return add(this, v);
    }

    /**
     * As {@link #addScaled(Vector4, double, Vector4)} where the first argument is the
     * calling Vector4.
     *
     * @param scale Scale factor applied to v
     * @param v     The vector scaled then added to this vector
     *
     * @return This vector
     *
     * @throws NullPointerException if v is null
     */
    public Vector4 addScaled(double scale, @Const Vector4 v) {
        return addScaled(this, scale, v);
    }

    /**
     * As {@link #sub(Vector4, Vector4)} with the first argument being this vector.
     *
     * @param v
     *
     * @return This vector
     *
     * @throws NullPointerException if v is null
     */
    public Vector4 sub(@Const Vector4 v) {
        return sub(this, v);
    }

    /**
     * As {@link #scale(Vector4, double)} with the first argument being this vector.
     *
     * @param scalar
     *
     * @return This vector
     */
    public Vector4 scale(double scalar) {
        return scale(this, scalar);
    }

    /**
     * Normalize this vector in place, equivalent to {@link #normalize(Vector4)} with the
     * first argument being this vector.
     *
     * @return This vector
     *
     * @throws ArithmeticException if this vector cannot be normalized
     */
    public Vector4 normalize() {
        return normalize(this);
    }

    /**
     * Set the vector coordinate at index to the given value. index must be one of 0 (x),
     * 1 (y), 2 (z), or 3 (w).
     *
     * @param index Coordinate to modify
     * @param val   New value for coordinate
     *
     * @return This vector
     *
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public Vector4 set(int index, double val) {
        switch (index) {
        case 0:
            x = val;
            break;
        case 1:
            y = val;
            break;
        case 2:
            z = val;
            break;
        case 3:
            w = val;
            break;
        default:
            throw new IndexOutOfBoundsException("Index must be in [0, 3]");
        }
        return this;
    }

    /**
     * Set the x, y, z, and w values of this Vector4 to the values held in v.
     *
     * @param v Vector to be copied into this
     *
     * @return This vector
     *
     * @throws NullPointerException if v is null
     */
    public Vector4 set(@Const Vector4 v) {
        return set(v.x, v.y, v.z, v.w);
    }

    /**
     * Set the x, y, z, and w values of this Vector4 to the given four coordinates.
     *
     * @param x New x coordinate
     * @param y New y coordinate
     * @param z New z coordinate
     * @param w New w coordinate
     *
     * @return This vector
     */
    public Vector4 set(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /**
     * Set the x, y, z and w values of this Vector4 to the four values held within the
     * vals array, starting at offset.
     *
     * @param vals   Array to take 4 component values from
     * @param offset Index of the x coordinate
     *
     * @return This vector
     *
     * @throws NullPointerException           if vals is null
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have four values starting at
     *                                        offset
     */
    public Vector4 set(double[] vals, int offset) {
        return set(vals[offset], vals[offset + 1], vals[offset + 2], vals[offset + 3]);
    }

    /**
     * As {@link #set(double[], int)} but the values are taken from the float array
     * instead of a double array.
     *
     * @param vals
     * @param offset
     *
     * @return This vector
     */
    public Vector4 set(float[] vals, int offset) {
        return set(vals[offset], vals[offset + 1], vals[offset + 2], vals[offset + 3]);
    }

    /**
     * As {@link #set(double[], int)} but the values are taken from the DoubleBuffer.
     *
     * @param vals   The double value source
     * @param offset The index into vals for the x coordinate
     *
     * @return This vector
     *
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have four values starting at
     *                                        offset
     */
    public Vector4 set(DoubleBuffer vals, int offset) {
        return set(vals.get(offset), vals.get(offset + 1), vals.get(offset + 2),
                   vals.get(offset + 3));
    }

    /**
     * As {@link #set(double[], int)} but the values are taken from the FloatBuffer
     *
     * @param vals   The double value source
     * @param offset The index into vals for the x coordinate
     *
     * @return This vector
     *
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have four values starting at
     *                                        offset
     */
    public Vector4 set(FloatBuffer vals, int offset) {
        return set(vals.get(offset), vals.get(offset + 1), vals.get(offset + 2),
                   vals.get(offset + 3));
    }

    /**
     * Get the given component from this vector; index must be 0 (x), 1 (y), 2 (z), or 3
     * (w)
     *
     * @param index The vector component to retrieve
     *
     * @return The component at the given index
     *
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public double get(int index) {
        switch (index) {
        case 0:
            return x;
        case 1:
            return y;
        case 2:
            return z;
        case 3:
            return w;
        default:
            throw new IndexOutOfBoundsException("Index must be in [0, 3]");
        }
    }

    /**
     * Store the four component values of this vector into vals, starting at offset. The
     * components should be placed consecutively, ordered x, y, z, and w. It is assumed
     * that the array has at least four positions available, starting at offset.
     *
     * @param vals   Array to store this vector in
     * @param offset First array index to hold the x value
     *
     * @throws NullPointerException           if vals is null
     * @throws ArrayIndexOutOfBoundsException if there isn't enough room to store this
     *                                        vector at offset
     */
    public void get(double[] vals, int offset) {
        vals[offset] = x;
        vals[offset + 1] = y;
        vals[offset + 2] = z;
        vals[offset + 3] = w;
    }

    /**
     * As {@link #get(double[], int)} except the double coordinates are cast into floats
     * before storing in the array.
     *
     * @param vals
     * @param offset
     */
    public void get(float[] vals, int offset) {
        vals[offset] = (float) x;
        vals[offset + 1] = (float) y;
        vals[offset + 2] = (float) z;
        vals[offset + 3] = (float) w;
    }

    /**
     * As {@link #get(double[], int)}, but with a DoubleBuffer. <tt>offset</tt> is
     * measured from 0, not the buffer's position.
     *
     * @param store  The DoubleBuffer to hold the row values
     * @param offset The first index to use in the store
     *
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space for the
     *                                        vector
     */
    public void get(DoubleBuffer store, int offset) {
        store.put(offset, x);
        store.put(offset + 1, y);
        store.put(offset + 2, z);
        store.put(offset + 3, w);
    }

    /**
     * As {@link #get(double[], int)}, but with a FloatBuffer. <tt>offset</tt> is measured
     * from 0, not the buffer's position.
     *
     * @param store  The FloatBuffer to hold the row values
     * @param offset The first index to use in the store
     *
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space for the
     *                                        vector
     */
    public void get(FloatBuffer store, int offset) {
        store.put(offset, (float) x);
        store.put(offset + 1, (float) y);
        store.put(offset + 2, (float) z);
        store.put(offset + 3, (float) w);
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + ", " + z + ", " + w + "]";
    }

    @Override
    public int hashCode() {
        long result = 17;

        result += 31 * result + Double.doubleToLongBits(x);
        result += 31 * result + Double.doubleToLongBits(y);
        result += 31 * result + Double.doubleToLongBits(z);
        result += 31 * result + Double.doubleToLongBits(w);

        return (int) (((result & 0xffffffff00000000L) >> 32) ^
                      (result & 0x00000000ffffffffL));
    }

    @Override
    public boolean equals(Object o) {
        // this conditional correctly handles null values
        if (!(o instanceof Vector4)) {
            return false;
        }
        Vector4 v = (Vector4) o;
        return x == v.x && y == v.y && z == v.z && w == v.w;
    }

    /**
     * Determine if these two vectors are equal, within an error range of eps. Returns
     * false if v is null
     *
     * @param v   Vector to check approximate equality to
     * @param eps Error tolerance of each component
     *
     * @return True if all component values are within eps of the corresponding component
     *         of v
     */
    public boolean epsilonEquals(@Const Vector4 v, double eps) {
        if (v == null) {
            return false;
        }

        double tx = Math.abs(x - v.x);
        double ty = Math.abs(y - v.y);
        double tz = Math.abs(z - v.z);
        double tw = Math.abs(w - v.w);

        return tx <= eps && ty <= eps && tz <= eps && tw <= eps;
    }
}
