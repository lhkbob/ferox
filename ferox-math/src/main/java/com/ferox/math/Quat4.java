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

import com.ferox.math.bounds.Plane;

/**
 * <p>
 * Quat4 is an implementation of a 4-component quaternion.
 * </p>
 * <p>
 * More information on the math and theory behind quaternions can be found <a
 * href="http://en.wikipedia.org/wiki/Quaternion">here</a>.
 * </p>
 * <p>
 * In all mathematical functions whose result is a quaternion, the Quat4 calling
 * the method will contain the result. The input quaternions will be left
 * unmodified. It is safe for the calling quaternion to be any quaternion
 * parameter into the function.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Quat4 implements Cloneable {
    public double x;
    public double y;
    public double z;
    public double w;

    /**
     * Create a new Quat4 initialized to the identity quaternion.
     */
    public Quat4() {
        setIdentity();
    }

    /**
     * Create a new Quat4 that copies its values from those in <tt>q</tt>.
     * 
     * @param q The quaternion to clone
     * @throws NullPointerException if q is null
     */
    public Quat4(@Const Quat4 q) {
        set(q);
    }

    /**
     * Create a new Quat4 that takes its initial values as (x, y, z, w).
     * 
     * @param x
     * @param y
     * @param z
     * @param w
     */
    public Quat4(double x, double y, double z, double w) {
        set(x, y, z, w);
    }

    @Override
    public Quat4 clone() {
        return new Quat4(this);
    }

    /**
     * Compute the shortest rotation arc from <tt>a</tt> to <tt>b</tt>, and
     * store the rotation in this quaternion.
     * 
     * @param a The starting vector
     * @param b The target, ending vector
     * @return This quaternion
     * @throws NullPointerException if a or b are null
     */
    public Quat4 arc(@Const Vector3 a, @Const Vector3 b) {
        // make sure that both this and v are normalized
        // if they aren't unit length, get the normalized version as new vectors
        double da = a.lengthSquared();
        double db = b.lengthSquared();
        Vector3 na = (Math.abs(da - 1.0) < .00001 ? a : new Vector3().scale(a,
                                                                            1 / Math.sqrt(da)));
        Vector3 nb = (Math.abs(db - 1.0) < .00001 ? b : new Vector3().scale(b,
                                                                            1 / Math.sqrt(db)));

        double d = na.dot(nb);
        if (d < 1.0) {
            Vector3 n = new Vector3();
            Plane.getTangentSpace(na, n, new Vector3()); // FIXME

            return set(n.x, n.y, n.z, 0.0);
        } else {
            Vector3 c = new Vector3().cross(na, nb);
            double s = Math.sqrt(2.0 * (1.0 + d));
            double rs = 1.0 / s;

            return set(c.x * rs, c.y * rs, c.z * rs, .5 * s);
        }
    }

    /**
     * Compute <code>a + b</code> and store it in this quaternion.
     * 
     * @param a The left side of the addition
     * @param b The right side of the addition
     * @return This quaternion
     * @throws NullPointerException if a or b are null
     */
    public Quat4 add(@Const Quat4 a, @Const Quat4 b) {
        return set(a.x + b.x, a.y + b.y, a.z + b.z, a.w + b.w);
    }

    /**
     * Compute <code>a - b</code> and store it in this quaternion.
     * 
     * @param a The left side of the subtraction
     * @param b The right side of the subtraction
     * @return This quaternion
     * @throws NullPointerException if a or b are null
     */
    public Quat4 sub(@Const Quat4 a, @Const Quat4 b) {
        return set(a.x - b.x, a.y - b.y, a.z - b.z, a.w - b.w);
    }

    /**
     * Compute <code>[a] x [b]</code> and store it in this quaternion. This is
     * similar to matrix multiplication in that it is not commutative and can be
     * thought of as concatenation of transforms.
     * 
     * @param a The quaternion on the left side of the multiplication
     * @param b The quaternion on the right side of the multiplication
     * @return This quaternion
     * @throws NullPointerException if a or b are null
     */
    public Quat4 mul(@Const Quat4 a, @Const Quat4 b) {
        return set(a.w * b.x + a.x * b.w + a.y * b.z - a.z * b.y,
                   a.w * b.y + a.y * b.w + a.z * b.x - a.x * b.z,
                   a.w * b.z + a.z * b.w + a.x * b.y - a.y * b.x,
                   a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z);
    }

    /**
     * Multiply <tt>a</tt> by the quaternion implicitly formed by <tt>b</tt>,
     * where b holds the first 3 coordinates of the quaternion and its fourth is
     * considered to be 0. The multiplication is stored in this quaternion
     * 
     * @param a The left side of the multiplication
     * @param v The first three coordinates of the implicit quaternion, for the
     *            right side of the multiplication
     * @return This quaternion
     * @throws NullPointerException if a or b are null
     */
    public Quat4 mul(@Const Quat4 a, @Const Vector3 b) {
        return set(a.w * b.x + a.y * b.z - a.z * b.y, a.w * b.y + a.z * b.x - a.x * b.z,
                   a.w * b.z + a.x * b.y - a.y * b.x,
                   -(a.x * b.x + a.y * b.y + a.z * b.z));
    }

    /**
     * Scale the components of <tt>q</tt> by <tt>s</tt> and store them in this
     * quaternion.
     * 
     * @param q The quaternion to scale
     * @param s The scale factor
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public Quat4 scale(@Const Quat4 q, double s) {
        return set(s * q.x, s * q.y, s * q.z, s * q.w);
    }

    /**
     * Normalize the quaternion <tt>a</tt> and store it in this quaternion
     * 
     * @param a The quaternion being normalized
     * @return This quaternion
     * @throws ArithmeticException if a cannot be normalized
     * @throws NullPointerException if a is null
     */
    public Quat4 normalize(@Const Quat4 a) {
        double d = a.length();
        if (d == 0f) {
            throw new ArithmeticException("Cannot normalize quaternion with 0 length");
        }
        return scale(a, 1.0 / d);
    }

    /**
     * @return The length of this quaternion (identical to a 4-vector with the
     *         same components)
     */
    public double length() {
        return Math.sqrt(lengthSquared());
    }

    /**
     * @return The squared length of this quaternion (identical to a 4-vector
     *         with the same components)
     */
    public double lengthSquared() {
        return x * x + y * y + z * z + w * w;
    }

    /**
     * Compute the dot product between this quaternion and <tt>q</tt>.
     * 
     * @param q The other quaternion involved in the dot product
     * @return The dot product between this and <tt>q</tt>, which is the same as
     *         if both were treated as 4-vectors.
     * @throws NullPointerException if q is null
     */
    public double dot(Quat4 q) {
        return x * q.x + y * q.y + z * q.z + w * q.w;
    }

    /**
     * @return The rotation, in radians, about the conceptual axis of rotation
     *         representing this quaternions transform
     */
    public double getAxisAngle() {
        return (2 * Math.acos(w));
    }

    /**
     * Compute and return the axis of rotation for this quaternion. This creates
     * a new Vector3f.
     * 
     * @return The axis of rotation in a new vector
     */
    public Vector3 getAxis() {
        double s2 = 1.0 - w * w;
        if (s2 < .0001) {
            return new Vector3(1, 0, 0); // arbitrary
        }

        double s = Math.sqrt(s2);
        return new Vector3(x / s, y / s, z / s);
    }

    /**
     * Compute the angle between this quaternion and <tt>q</tt>
     * 
     * @param q The other quaternion
     * @return The angle of rotation, in radians, between this quaternion and q
     * @throws NullPointerException if q is null
     * @throws ArithmeticException if there is a singularity when computing the
     *             angle between this and q (i.e. if either have 0-length)
     */
    public double angle(@Const Quat4 q) {
        double s = Math.sqrt(lengthSquared() * q.lengthSquared());
        if (s == 0) {
            throw new ArithmeticException("Undefined angle between two quaternions");
        }

        return Math.acos(dot(q) / s);
    }

    /**
     * Compute the inverse of <tt>q</tt> and store it in this quaternion.
     * 
     * @param q The quaternion being inverted
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public Quat4 inverse(@Const Quat4 q) {
        return set(-q.x, -q.y, -q.z, q.w);
    }

    /**
     * Perform spherical linear interpolation between <tt>a</tt> and <tt>b</tt>
     * and store the interpolated quaternion in this quaternion. The parameter
     * <tt>t</tt> should be in the range <code>[0, 1]</code> where a value of 0
     * represents <tt>a</tt> and a value of 1 represents <tt>b</tt>.
     * 
     * @param a The initial quaternion
     * @param b The end quaternion
     * @param t Interpolation factor, from 0 to 1
     * @return This quaternion
     * @throws IllegalArgumentException if t is not in the range [0, 1]
     * @throws NullPointerException if a or b are null
     */
    public Quat4 slerp(@Const Quat4 a, @Const Quat4 b, double t) {
        if (t < 0 || t > 1) {
            throw new IllegalArgumentException("t must be in [0, 1], not: " + t);
        }

        double theta = a.angle(b);
        if (theta != 0) {
            double d = 1f / Math.sin(theta);
            double s0 = Math.sin((1 - t) * theta);
            double s1 = Math.sin(t * theta);

            if (a.dot(b) < 0) {
                s1 *= -1;
            }

            return set((a.x * s0 + b.x * s1) * d, (a.y * s0 + b.y * s1) * d,
                       (a.z * s0 + b.z * s1) * d, (a.w * s0 + b.w * s1) * d);
        } else {
            return set(a.x, a.y, a.z, a.w);
        }
    }

    /**
     * As {@link #add(Quat4, Quat4)} with the first argument this quaternion.
     * 
     * @param q
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public Quat4 add(@Const Quat4 q) {
        return add(this, q);
    }

    /**
     * As {@link #sub(Quat4, Quat4)} with the first argument this quaternion.
     * 
     * @param q
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public Quat4 sub(@Const Quat4 q) {
        return sub(this, q);
    }

    /**
     * As {@link #mul(Quat4, Quat4)} with the first argument this quaternion.
     * 
     * @param q
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public Quat4 mul(@Const Quat4 q) {
        return mul(this, q);
    }

    /**
     * As {@link #mul(Quat4, Vector3)} with the first argument this quaternion.
     * 
     * @param v
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public Quat4 mul(@Const Vector3 v) {
        return mul(this, v);
    }

    /**
     * As {@link #scale(Quat4, double)} with the first argument this quaternion.
     * 
     * @param s
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public Quat4 scale(double s) {
        return scale(this, s);
    }

    /**
     * Normalize this quaternion in place, equivalent to
     * {@link #normalize(Quat4)} with the first argument this quaternion.
     * 
     * @return This quaternion
     * @throws ArithmeticException if this quaternion cannot be normalized
     */
    public Quat4 normalize() {
        return normalize(this);
    }

    /**
     * Invert this quaternion in place, equivalent to {@link #inverse(Quat4)}
     * with the first argument this quaternion.
     * 
     * @return This quaternion
     */
    public Quat4 inverse() {
        return inverse(this);
    }

    /**
     * Set this quaternion to the identity quaternion, which is (0, 0, 0, 1).
     * 
     * @return This quaternion
     */
    public Quat4 setIdentity() {
        return set(0, 0, 0, 1);
    }

    /**
     * Set this quaternion to be the rotation of <tt>angle</tt> radians about
     * the specified <tt>axis</tt>.
     * 
     * @param axis The axis of rotation, should not be the 0 vector
     * @param angle The angle of rotation, in radians
     * @return This quaternion
     * @throws NullPointerException if axis is null
     */
    public Quat4 setAxisAngle(@Const Vector3 axis, double angle) {
        double d = axis.length();
        double s = Math.sin(.5 * angle) / d;
        return set(axis.x * s, axis.y * s, axis.z * s, Math.cos(.5 * angle));
    }

    /**
     * Set this quaternion to be the rotation described by the Euler angles:
     * <tt>yaw</tt>, <tt>pitch</tt> and <tt>roll</tt>. Yaw is the rotation
     * around the y-axis, pitch is the rotation around the x-axis, and roll is
     * the rotation around the z-axis. The final rotation is formed by rotating
     * first Y, then X and then Z.
     * 
     * @param yaw Rotation around the y-axis in radians
     * @param pitch Rotation around the x-axis in radians
     * @param roll Rotation around the z-axis in radians
     * @return This quaternion
     */
    public Quat4 setEuler(double yaw, double pitch, double roll) {
        double cosYaw = Math.cos(.5 * yaw);
        double sinYaw = Math.sin(.5 * yaw);

        double cosPitch = Math.cos(.5 * pitch);
        double sinPitch = Math.sin(.5 * pitch);

        double cosRoll = Math.cos(.5 * roll);
        double sinRoll = Math.sin(.5 * roll);

        return set(cosRoll * sinPitch * cosYaw + sinRoll * cosPitch * sinYaw,
                   cosRoll * cosPitch * sinYaw - sinRoll * sinPitch * cosYaw,
                   sinRoll * cosPitch * cosYaw - cosRoll * sinPitch * sinYaw,
                   cosRoll * cosPitch * cosYaw + sinRoll * sinPitch * sinYaw);
    }

    /**
     * Set the value of this quaternion to represent the rotation of the given
     * matrix, <tt>e</tt>. It is assumed that <tt>e</tt> contains a rotation
     * matrix and does not include scale factors, or other form of 3x3 matrix.
     * 
     * @param e The rotation matrix to convert to quaternion form
     * @return This quaternion
     * @throws NullPointerException if e is null
     */
    public Quat4 set(@Const Matrix3 e) {
        double trace = e.trace();
        if (trace > 0.0) {
            double s = Math.sqrt(trace + 1.0);
            w = .5 * s;
            s = .5 / s;

            x = s * (e.m21 - e.m12);
            y = s * (e.m02 - e.m20);
            z = s * (e.m10 - e.m01);
        } else {
            // get the column that has the highest diagonal element
            int i = (e.m00 < e.m11 ? (e.m11 < e.m22 ? 2 : 1) : (e.m00 < e.m22 ? 2 : 0));
            int j = (i + 1) % 3;
            int k = (i + 2) % 3;

            double s = Math.sqrt(e.get(i, i) - e.get(j, j) - e.get(k, k) + 1.0);
            set(i, .5 * s);
            s = .5 / s;

            set(3, (e.get(k, j) - e.get(j, k)) * s);
            set(j, (e.get(j, i) + e.get(i, j)) * s);
            set(k, (e.get(k, i) + e.get(i, k)) * s);
        }

        return this;
    }

    /**
     * Set the quaternion coordinate at index to the given value. index must be
     * one of 0 (x), 1 (y), 2 (z), or 3 (w).
     * 
     * @param index Coordinate to modify
     * @param val New value for coordinate
     * @return This quaternion
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public Quat4 set(int index, double val) {
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
     * Set the x, y, z, and w values of this Quat4 to the values held in q.
     * 
     * @param q Quaternion to be copied into this
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public Quat4 set(@Const Quat4 q) {
        return set(q.x, q.y, q.z, q.w);
    }

    /**
     * Set the x, y, z, and w values of this Quat4 to the given four
     * coordinates.
     * 
     * @param x New x coordinate
     * @param y New y coordinate
     * @param z New z coordinate
     * @param w New w coordinate
     * @return This quaternion
     */
    public Quat4 set(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /**
     * Set the x, y, z and w values of this Quat4 to the four values held within
     * the vals array, starting at offset.
     * 
     * @param vals Array to take 4 component values from
     * @param offset Index of the x coordinate
     * @return This quaternion
     * @throws NullPointerException if vals is null
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have four values
     *             starting at offset
     */
    public Quat4 set(double[] vals, int offset) {
        return set(vals[offset], vals[offset + 1], vals[offset + 2], vals[offset + 3]);
    }

    /**
     * As {@link #set(double[], int)} but the values are taken from the float[].
     * 
     * @param vals The double value source
     * @param offset The index into vals for the x coordinate
     * @return This vector
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have four values
     *             starting at offset
     */
    public Quat4 set(float[] vals, int offset) {
        return set(vals[offset], vals[offset + 1], vals[offset + 2], vals[offset + 3]);
    }

    /**
     * As {@link #set(double[], int)} but the values are taken from the
     * DoubleBuffer.
     * 
     * @param vals The double value source
     * @param offset The index into vals for the x coordinate
     * @return This vector
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have four values
     *             starting at offset
     */
    public Quat4 set(DoubleBuffer vals, int offset) {
        return set(vals.get(offset), vals.get(offset + 1), vals.get(offset + 2),
                   vals.get(offset + 3));
    }

    /**
     * As {@link #set(double[], int)} but the values are taken from the
     * FloatBuffer.
     * 
     * @param vals The double value source
     * @param offset The index into vals for the x coordinate
     * @return This vector
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have four values
     *             starting at offset
     */
    public Quat4 set(FloatBuffer vals, int offset) {
        return set(vals.get(offset), vals.get(offset + 1), vals.get(offset + 2),
                   vals.get(offset + 3));
    }

    /**
     * Get the given component from this quaternion; index must be 0 (x), 1 (y),
     * 2 (z), or 3 (w)
     * 
     * @param index The quaternion component to retrieve
     * @return The component at the given index
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
     * Store the four component values of this quaternion into vals, starting at
     * offset. The components should be placed consecutively, ordered x, y, z,
     * and w. It is assumed that the array has at least four positions
     * available, starting at offset.
     * 
     * @param vals Array to store this quaternion in
     * @param offset First array index to hold the x value
     * @throws NullPointerException if vals is null
     * @throws ArrayIndexOutOfBoundsException if there isn't enough room to
     *             store this quaternion at offset
     */
    public void get(double[] vals, int offset) {
        vals[offset] = x;
        vals[offset + 1] = y;
        vals[offset + 2] = z;
        vals[offset + 3] = w;
    }

    /**
     * As {@link #get(double[], int)}, but with a float[]. The double values are
     * cast to floats in order to store them.
     * 
     * @param store The float[] to hold the row values
     * @param offset The first index to use in the store
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the quaternion
     */
    public void get(float[] vals, int offset) {
        vals[offset] = (float) x;
        vals[offset + 1] = (float) y;
        vals[offset + 2] = (float) z;
        vals[offset + 3] = (float) w;
    }

    /**
     * As {@link #get(double[], int)}, but with a DoubleBuffer. <tt>offset</tt>
     * is measured from 0, not the buffer's position.
     * 
     * @param store The DoubleBuffer to hold the row values
     * @param offset The first index to use in the store
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the quaternion
     */
    public void get(DoubleBuffer store, int offset) {
        store.put(offset, x);
        store.put(offset + 1, y);
        store.put(offset + 2, z);
        store.put(offset + 3, w);
    }

    /**
     * As {@link #get(double[], int)}, but with a FloatBuffer. <tt>offset</tt>
     * is measured from 0, not the buffer's position.
     * 
     * @param store The FloatBuffer to hold the row values
     * @param offset The first index to use in the store
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the quaternion
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

        return (int) (((result & 0xffffffff00000000L) >> 32) ^ (result & 0x00000000ffffffffL));
    }

    @Override
    public boolean equals(Object o) {
        // this conditional correctly handles null values
        if (!(o instanceof Quat4)) {
            return false;
        }
        Quat4 v = (Quat4) o;
        return x == v.x && y == v.y && z == v.z && w == v.w;
    }

    /**
     * Determine if these two quaternions are equal, within an error range of
     * eps. Returns false if q is null.
     * 
     * @param q Quaternion to check approximate equality to
     * @param eps Error tolerance of each component
     * @return True if all component values are within eps of the corresponding
     *         component of q
     */
    public boolean epsilonEquals(Quat4 q, double eps) {
        if (q == null) {
            return false;
        }

        double tx = Math.abs(x - q.x);
        double ty = Math.abs(y - q.y);
        double tz = Math.abs(z - q.z);
        double tw = Math.abs(w - q.w);

        return tx <= eps && ty <= eps && tz <= eps && tw <= eps;
    }
}
