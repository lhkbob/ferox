package com.ferox.math;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * <p>
 * Vector3 provides an implementation of a 3-element mathematical vector with
 * many common operations available from linear algebra. It's 3 components are
 * exposed as public fields for performance reasons. This means vectors are
 * always mutable.
 * </p>
 * <p>
 * The {@link Const} annotation can be used the same way that the 'const' token
 * can modify a type in C++. On input, it declares the method will not modify
 * that instance; on output, it declares that the returned instance should not
 * be modified (internal code might still modify it in some controlled manner).
 * </p>
 * <p>
 * In all mathematical functions whose result is a vector, the Vector3 calling
 * the method will contain the result. The input vectors will be left
 * unmodified. It is safe for the calling vector to be any vector parameter into
 * the function.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Vector3 implements Cloneable {
    public double x;
    public double y;
    public double z;

    /**
     * Create a new vector with all components equal to 0.
     */
    public Vector3() {
        this(0, 0, 0);
    }

    /**
     * Create a new vector that copies the x, y, and z values from <tt>v</tt>.
     * 
     * @param v The vector to copy
     * @throws NullPointerException if v is null
     */
    public Vector3(@Const Vector3 v) {
        this(v.x, v.y, v.z);
    }

    /**
     * Create a new vector with the given x, y, and z values.
     * 
     * @param x The x value
     * @param y The y value
     * @param z The z value
     */
    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Vector3 clone() {
        return new Vector3(this);
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
     * Compute the length of this vector, squared. Often, when only length
     * comparisons are necessary, the sauared length is useful because its
     * computation doesn't involve a slow square root operation.
     * 
     * @return The squared length of this vector
     */
    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    /**
     * Compute the distance between this vector and <tt>v</tt>, treating both
     * vectors as 3D points.
     * 
     * @param v The other vector
     * @return The distance between this and v
     * @throws NullPointerException if v is null
     */
    public double distance(@Const Vector3 v) {
        return Math.sqrt(distanceSquared(v));
    }

    /**
     * Compute the square of the distance between this and <tt>v</tt>, treating
     * both vectors as 3D points.
     * 
     * @param v The other vector
     * @return The distance between this and v
     * @throws NullPointerException if v is null
     */
    public double distanceSquared(@Const Vector3 v) {
        double dx = x - v.x;
        double dy = y - v.y;
        double dz = z - v.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Compute and return the shortest angle between this vector and v. The
     * returned angle must be in radians.
     * 
     * @param v The other vector involved
     * @return The smallest angle, in radians, between this and v
     * @throws NullPointerException if v is null
     */
    public double angle(@Const Vector3 v) {
        return Math.acos(dot(v) / (length() * v.length()));
    }

    /**
     * Compute and return the dot product between this vector and the given
     * vector, v.
     * 
     * @param v Vector to compute dot product with
     * @return The dot product between v and this
     * @throws NullPointerException if v is null
     */
    public double dot(@Const Vector3 v) {
        return x * v.x + y * v.y + z * v.z;
    }

    /**
     * Compute a vector orthogonal to <tt>b</tt> and store the result in this
     * vector. The orthogonal vector will be in the plane formed by <tt>a</tt>
     * and <tt>b</tt>, and it will be in the same half-plane as <tt>a</tt>
     * formed by a line through <tt>b</tt>. This uses the Gram-Schmidt process.
     * 
     * @param a The vector that helps form the plane and chooses the result's
     *            orientation
     * @param b The vector the result is orthogonal to
     * @return This vector
     * @throws NullPointerException if a or b are null
     */
    public Vector3 ortho(@Const Vector3 a, @Const Vector3 b) {
        double tx = a.x;
        double ty = a.y;
        double tz = a.z;

        // if this == a, project modifies a, which is why we preserve tx, ty, tz
        project(a, b);
        return set(tx - x, ty - y, tz - z);
    }

    /**
     * Transform the vector <tt>v</tt> by <tt>q</tt> and store it in this
     * vector.
     * 
     * @param v The vector to rotate or transform
     * @param q The quaternion to rotate by
     * @return This vector
     * @throws NullPointerException if v or q are null
     */
    public Vector3 rotate(@Const Vector3 v, @Const Quat4 q) {
        Quat4 o = new Quat4().mul(q, v).mul(new Quat4().inverse(q));
        return set(o.x, o.y, o.z);
    }

    /**
     * As {@link #rotate(Vector3, Quat4)} where the first argument is this
     * vector.
     * 
     * @param q
     * @return This vector
     */
    public Vector3 rotate(@Const Quat4 q) {
        return rotate(this, q);
    }

    /**
     * Project <tt>a</tt> onto <tt>b</tt> and store the projected vector in this
     * vector.
     * 
     * @param a The vector being projected
     * @param b The vector that is projected onto
     * @return This vector
     * @throws NullPointerException if a or b are null
     */
    public Vector3 project(@Const Vector3 a, @Const Vector3 b) {
        return scale(b, a.dot(b) / b.lengthSquared());
    }

    /**
     * Compute the cross product <code>a X b</code> and store the result in this
     * vector.
     * 
     * @param a The left side of the cross product
     * @param b The right side of the cross product
     * @return This vector
     * @throws NullPointerException if a or b are null
     */
    public Vector3 cross(@Const Vector3 a, @Const Vector3 b) {
        return set(a.y * b.z - b.y * a.z, a.z * b.x - b.z * a.x, a.x * b.y - b.x * a.y);
    }

    /**
     * Compute <code>a + b</code> and store the result in this vector.
     * 
     * @param a The left side of the addition
     * @param b The right side of the addition
     * @return This vector
     * @throws NullPointerException if a or b are null
     */
    public Vector3 add(@Const Vector3 a, @Const Vector3 b) {
        return set(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    /**
     * Compute <code>a + (scalar * b)</code> and store the result in this
     * vector.
     * 
     * @param a The left side of the addition
     * @param scalar The scaling factor applied to
     * @param b The vector to be scaled and then added to a
     * @return This vector
     * @throws NullPointerException if a or b are null
     */
    public Vector3 addScaled(@Const Vector3 a, double scalar, @Const Vector3 b) {
        return set(a.x + scalar * b.x, a.y + scalar * b.y, a.z + scalar * b.z);
    }

    /**
     * Compute <code>a - b</code> and store the result in this vector.
     * 
     * @param a The left side of the subtraction
     * @param b The right side of the subtraction
     * @return This vector
     * @throws NullPointerException if a or b are null
     */
    public Vector3 sub(@Const Vector3 a, @Const Vector3 b) {
        return set(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    /**
     * Scale v by the given scalar (scalar*v) and store it in this vector.
     * 
     * @param v The vector to be scaled
     * @param scalar Scaling factor to use
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public Vector3 scale(@Const Vector3 v, double scalar) {
        return set(scalar * v.x, scalar * v.y, scalar * v.z);
    }

    /**
     * Normalize <tt>v</tt> to be of length 1 and store it in this vector. The
     * vector can't be normalized if it's length is 0. If it's length is very
     * close to 0, the results may suffer from loss of precision.
     * 
     * @param v The vector to normalize
     * @return This vector
     * @throws ArithmeticException if v can't be normalized
     * @throws NullPointerException if v is null
     */
    public Vector3 normalize(@Const Vector3 v) {
        double d = v.length();
        if (d == 0f) {
            throw new ArithmeticException("Can't normalize 0 vector");
        }
        return scale(v, 1 / d);
    }

    /**
     * Compute <code>[m] x [b]</code> and store the resulting 3x1 matrix in this
     * vector. For sake of multiplication, [b] is considered to be a 3x1 matrix
     * where x is the first row, y is the second, and z is the third.
     * 
     * @param m The matrix on the left side of the multiplication
     * @param b The vector to interpret as a 3x1 matrix
     * @return This vector
     * @throws NullPointerException if m or b are null
     */
    public Vector3 mul(@Const Matrix3 m, @Const Vector3 b) {
        return set(m.m00 * b.x + m.m01 * b.y + m.m02 * b.z,
                   m.m10 * b.x + m.m11 * b.y + m.m12 * b.z,
                   m.m20 * b.x + m.m21 * b.y + m.m22 * b.z);
    }

    /**
     * <p>
     * Compute <code>[b] x [m]</code> and store the resulting 1x3 matrix in this
     * vector. For sake of multiplication, [b] is considered to be a 1x3 matrix,
     * where x is the first column, y is the second, and z is the third.
     * </p>
     * <p>
     * Note that this uses the reverse order of {@link #mul(Matrix3, Vector3)}.
     * </p>
     * 
     * @param b Vector to be interpreted as a 1x3 matrix in the multiplication
     * @param m The matrix in the right side of the multiplication
     * @return This vector
     * @throws NullPointerException if m or b are null
     */
    public Vector3 mul(@Const Vector3 b, @Const Matrix3 m) {
        return set(m.m00 * b.x + m.m10 * b.y + m.m20 * b.z,
                   m.m01 * b.x + m.m11 * b.y + m.m21 * b.z,
                   m.m02 * b.x + m.m12 * b.y + m.m22 * b.z);
    }

    /**
     * As {@link #transform(Matrix4, Vector3, double)} where the w-component is
     * set to 1. Thus, the resulting computation is the transformation of
     * <tt>a</tt> by <tt>m</tt>, interpreting <tt>a</tt> as a point in space.
     * 
     * @param m The transform matrix
     * @param a The point to transform
     * @return This vector
     * @throws NullPointerException if m or a are null
     */
    public Vector3 transform(@Const Matrix4 m, @Const Vector3 a) {
        return transform(m, a, 1.0);
    }

    /**
     * <p>
     * Compute <code>[m] x [a']</code> where <tt>[a']</tt> is the 4x1 matrix
     * formed by the three values of <tt>a</tt> and the specified <tt>w</tt>.
     * <p>
     * Common values of <tt>w</tt> are:
     * <ul>
     * <li><code>w = 0</code> transforms <tt>a</tt> by the upper 3x3 of
     * <tt>m</tt>, treating <tt>a</tt> as a direction vector.</li>
     * <li><code>w = 1</code> transforms <tt>a</tt> as if it were a point in
     * 3-space (assuming m is an affine transform).</li>
     * </ul>
     * 
     * @param m The affine transform to transform a with
     * @param a The vector being transformed by m
     * @param w The fourth component of a
     * @return This vector
     * @throws NullPointerException if m or a are null
     */
    public Vector3 transform(@Const Matrix4 m, @Const Vector3 a, double w) {
        return set(m.m00 * a.x + m.m01 * a.y + m.m02 * a.z + m.m03 * w,
                   m.m10 * a.x + m.m11 * a.y + m.m12 * a.z + m.m13 * w,
                   m.m20 * a.x + m.m21 * a.y + m.m22 * a.z + m.m23 * w);
    }

    /**
     * As {@link #transform(Matrix4, Vector3)} where the calling vector is
     * transformed by the matrix <tt>m</tt>.
     * 
     * @param m The matrix transforming this vector
     * @return This vector
     * @throws NullPointerException if m is null
     */
    public Vector3 transform(@Const Matrix4 m) {
        return transform(m, this);
    }

    /**
     * Solve the linear system of equations, <code>[m]*[this] = [a]</code> and
     * store the resultant values of (x, y, z) into this vector:
     * 
     * <pre>
     * m.m00*x + m.m01*y + m.m02*z = a.x
     * m.m10*x + m.m11*y + m.m12*z = a.y
     * m.m20*x + m.m21*y + m.m22*z = a.z
     * </pre>
     * 
     * @param m The matrix describing the coefficients of the 3 equations
     * @param a The vector constraining the values that solve the linear system
     *            of equations
     * @return This vector
     * @throws ArithmeticException if no solution or an infinite solutions exist
     * @throws NullPointerException if m or a are null
     */
    public Vector3 solve(@Const Matrix3 m, @Const Vector3 a) {
        double invDet = m.determinant();
        if (Math.abs(invDet) < .00001) {
            throw new ArithmeticException("No solution, or infinite solutions, cannot solve system");
        }
        invDet = 1f / invDet;

        return set(invDet * (a.x * (m.m11 * m.m22 - m.m12 * m.m21) - a.y * (m.m01 * m.m22 - m.m02 * m.m21) + a.z * (m.m01 * m.m12 - m.m02 * m.m11)),
                   invDet * (a.y * (m.m00 * m.m22 - m.m02 * m.m20) - a.z * (m.m00 * m.m12 - m.m02 * m.m10) - a.x * (m.m10 * m.m22 - m.m12 * m.m20)),
                   invDet * (a.x * (m.m10 * m.m21 - m.m11 * m.m20) - a.y * (m.m00 * m.m21 - m.m01 * m.m20) + a.z * (m.m00 * m.m11 - m.m01 * m.m10)));
    }

    /**
     * As {@link #ortho(Vector3, Vector3)}, where the first argument is this
     * vector.
     * 
     * @param proj
     * @return This vector
     * @throws NullPointerException if proj is null
     */
    public Vector3 ortho(@Const Vector3 proj) {
        return ortho(this, proj);
    }

    /**
     * As {@link #project(Vector3, Vector3)} where the first argument is this
     * vector.
     * 
     * @param proj
     * @return This vector
     * @throws NullPointerException if proj is null
     */
    public Vector3 project(@Const Vector3 proj) {
        return project(this, proj);
    }

    /**
     * As {@link #cross(Vector3, Vector3)}, where the first argument is this
     * vector.
     * 
     * @param v
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public Vector3 cross(@Const Vector3 v) {
        return cross(this, v);
    }

    /**
     * As {@link #add(Vector3, Vector3)}, where the first argument is this
     * vector.
     * 
     * @param v
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public Vector3 add(@Const Vector3 v) {
        return add(this, v);
    }

    /**
     * As {@link #addScaled(Vector3, double, Vector3)} where the first argument
     * is the calling Vector3.
     * 
     * @param scale Scale factor applied to v
     * @param v The vector scaled then added to this vector
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public Vector3 addScaled(double scale, @Const Vector3 v) {
        return addScaled(this, scale, v);
    }

    /**
     * As {@link #sub(Vector3, Vector3)}, where the first argument is this
     * vector.
     * 
     * @param v
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public Vector3 sub(@Const Vector3 v) {
        return sub(this, v);
    }

    /**
     * As {@link #scale(Vector3, double)}, where the first argument is this
     * vector.
     * 
     * @param scalar
     * @return This vector
     */
    public Vector3 scale(double scalar) {
        return scale(this, scalar);
    }

    /**
     * Normalize this vector in place, equivalent to {@link #normalize(Vector3)}
     * , where the first argument is this vector.
     * 
     * @return This vector
     * @throws ArithmeticException if the vector cannot be normalized
     */
    public Vector3 normalize() {
        return normalize(this);
    }

    /**
     * Compute the corner of the box that is most extended along the given
     * direction vector and store it in this vector. This will return one of the
     * 8 possible corners of the box depending on the signs of the vector, it
     * will not return points along the edge or face of the box.
     * 
     * @param bounds The bounds
     * @param dir The query direction vector
     * @return This vector, updated to hold the far extent
     * @throws NullPointerException if bounds or dir is null
     */
    public Vector3 farExtent(@Const AxisAlignedBox bounds, @Const Vector3 dir) {
        return extent(bounds.min, bounds.max, dir);
    }

    /**
     * Compute the corner of the box that is least extended along the given
     * direction vector and store it in this vector. This is equivalent to
     * calling {@link #farExtent(AxisAlignedBox, Vector3)} with a negated
     * direction vector but avoids computing the negation.
     * 
     * @param bounds The bounds
     * @param dir The query direction vector
     * @return This vector, updated to hold the near extent
     * @throws NullPointerException if bounds or dir is null
     */
    public Vector3 nearExtent(@Const AxisAlignedBox bounds, @Const Vector3 dir) {
        return extent(bounds.max, bounds.min, dir);
    }

    private Vector3 extent(@Const Vector3 min, @Const Vector3 max, @Const Vector3 dir) {
        x = (dir.x > 0 ? max.x : min.x);
        y = (dir.y > 0 ? max.y : min.y);
        z = (dir.z > 0 ? max.z : min.z);
        return this;
    }

    /**
     * Set the vector coordinate at index to the given value. index must be one
     * of 0 (x), 1 (y), or 2 (z).
     * 
     * @param index Coordinate to modify
     * @param val New value for coordinate
     * @return This vector
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public Vector3 set(int index, double val) {
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
        default:
            throw new IndexOutOfBoundsException("Index must be in [0, 3]");
        }
        return this;
    }

    /**
     * Set the x, y, and z values of this Vector3 to be equal to <tt>v</tt>
     * 
     * @param v Vector to be copied into this
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public Vector3 set(@Const Vector3 v) {
        return set(v.x, v.y, v.z);
    }

    /**
     * Set the x, y, and z values of this Vector3 to the given three
     * coordinates.
     * 
     * @param x New x coordinate
     * @param y New y coordinate
     * @param z New z coordinate
     * @return This vector
     */
    public Vector3 set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Set the x, y, and z values of this Vector3 to the three values held
     * within the vals array, starting at offset.
     * 
     * @param vals Array to take 3 component values from
     * @param offset Index of the x coordinate
     * @return This vector
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have three values
     *             starting at offset
     * @throws NullPointerException if vals is null
     */
    public Vector3 set(double[] vals, int offset) {
        return set(vals[offset], vals[offset + 1], vals[offset + 2]);
    }

    /**
     * As {@link #set(double[], int)} but the values are taken from the float
     * array instead of a double array.
     * 
     * @param vals
     * @param offset
     * @return This vector
     */
    public Vector3 set(float[] vals, int offset) {
        return set(vals[offset], vals[offset + 1], vals[offset + 2]);
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
    public Vector3 set(DoubleBuffer vals, int offset) {
        return set(vals.get(offset), vals.get(offset + 1), vals.get(offset + 2));
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
    public Vector3 set(FloatBuffer vals, int offset) {
        return set(vals.get(offset), vals.get(offset + 1), vals.get(offset + 2));
    }

    /**
     * Get the given component from this vector; index must be 0 (x), 1 (y), or
     * 2 (z).
     * 
     * @param index The vector component to retrieve
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
        default:
            throw new IndexOutOfBoundsException("Index must be in [0, 2]");
        }
    }

    /**
     * Store the three component values of this vector into vals, starting at
     * offset. The components should be placed consecutively, ordered x, y, and
     * z. It is assumed that the array has at least three positions available,
     * starting at offset.
     * 
     * @param vals Array to store this vector in
     * @param offset First array index to hold the x value
     * @throws NullPointerException if vals is null
     * @throws ArrayIndexOutOfBoundsException if there isn't enough room to
     *             store this vector at offset
     */
    public void get(double[] vals, int offset) {
        vals[offset] = x;
        vals[offset + 1] = y;
        vals[offset + 2] = z;
    }

    /**
     * As {@link #get(double[], int)} except the double coordinates are cast
     * into floats before storing in the array.
     * 
     * @param vals
     * @param offset
     */
    public void get(float[] vals, int offset) {
        vals[offset] = (float) x;
        vals[offset + 1] = (float) y;
        vals[offset + 2] = (float) z;
    }

    /**
     * As {@link #get(double[], int)}, but with a DoubleBuffer. <tt>offset</tt>
     * is measured from 0, not the buffer's position.
     * 
     * @param store The DoubleBuffer to hold the row values
     * @param offset The first index to use in the store
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the vector
     */
    public void get(DoubleBuffer store, int offset) {
        store.put(offset, x);
        store.put(offset + 1, y);
        store.put(offset + 2, z);
    }

    /**
     * As {@link #get(double[], int)}, but with a FloatBuffer. <tt>offset</tt>
     * is measured from 0, not the buffer's position.
     * 
     * @param store The FloatBuffer to hold the row values
     * @param offset The first index to use in the store
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the vector
     */
    public void get(FloatBuffer store, int offset) {
        store.put(offset, (float) x);
        store.put(offset + 1, (float) y);
        store.put(offset + 2, (float) z);
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + ", " + z + "]";
    }

    @Override
    public int hashCode() {
        long result = 17;

        result += 31 * result + Double.doubleToLongBits(x);
        result += 31 * result + Double.doubleToLongBits(y);
        result += 31 * result + Double.doubleToLongBits(z);

        return (int) (((result & 0xffffffff00000000L) >> 32) ^ (result & 0x00000000ffffffffL));
    }

    @Override
    public boolean equals(Object v) {
        // this conditional correctly handles null values
        if (!(v instanceof Vector3)) {
            return false;
        }
        Vector3 e = (Vector3) v;
        return x == e.x && y == e.y && z == e.z;
    }

    /**
     * Determine if these two vectors are equal, within an error range of eps.
     * Returns false if v is null.
     * 
     * @param v Vector to check approximate equality to
     * @param eps Error tolerance of each component
     * @return True if all component values are within eps of the corresponding
     *         component of v
     */
    public boolean epsilonEquals(@Const Vector3 v, double eps) {
        if (v == null) {
            return false;
        }

        if (Math.abs(x - v.x) > eps) {
            return false;
        }
        if (Math.abs(y - v.y) > eps) {
            return false;
        }
        if (Math.abs(z - v.z) > eps) {
            return false;
        }

        return true;
    }
}
