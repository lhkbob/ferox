package com.ferox.math;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * <p>
 * Matrix3 provides an implementation of a 3-by-3 mathematical matrix with many
 * common operations available from linear algebra. It's 9 components are
 * exposed as public fields for performance reasons. This means vectors are
 * always mutable. The first number in the field name is the row, the second is
 * the column.
 * </p>
 * <p>
 * The {@link Const} annotation can be used the same way that the 'const' token
 * can modify a type in C++. On input, it declares the method will not modify
 * that instance; on output, it declares that the returned instance should not
 * be modified (internal code might still modify it in some controlled manner).
 * </p>
 * <p>
 * In all mathematical functions whose result is a matrix, the Matrix3 calling
 * the method will contain the result. The input matrices will be left
 * unmodified. It is safe for the calling matrix to be any matrix parameter into
 * the function.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Matrix3 implements Cloneable {
    public double m00, m01, m02;
    public double m10, m11, m12;
    public double m20, m21, m22;
    
    /**
     * Create a new matrix with all components equal to 0.
     */
    public Matrix3() {
        set(0, 0, 0, 
            0, 0, 0, 
            0, 0, 0);
    }

    /**
     * Create a new matrix that copies the values from <tt>m</tt>.
     * 
     * @param m The matrix to copy
     * @throws NullPointerException if m is null
     */
    public Matrix3(@Const Matrix3 m) {
        set(m);
    }

    /**
     * Create a new matrix with the given 9 component values.
     * 
     * @param m00
     * @param m01
     * @param m02
     * @param m10
     * @param m11
     * @param m12
     * @param m20
     * @param m21
     * @param m22
     */
    public Matrix3(double m00, double m01, double m02,
                   double m10, double m11, double m12,
                   double m20, double m21, double m22) {
        set(m00, m01, m02, 
            m10, m11, m12, 
            m20, m21, m22);
    }

    @Override
    public Matrix3 clone() {
        return new Matrix3(this);
    }
    
    /**
     * Compute <code>a + b</code> and store the result in this matrix.
     * 
     * @param a The left side of the addition
     * @param b The right side of the addition
     * @return This matrix
     * @throws NullPointerException if a or b are null
     */
    public Matrix3 add(@Const Matrix3 a, @Const Matrix3 b) {
        return set(a.m00 + b.m00, a.m01 + b.m01, a.m02 + b.m02,
                   a.m10 + b.m10, a.m11 + b.m11, a.m12 + b.m12,
                   a.m20 + b.m20, a.m21 + b.m21, a.m22 + b.m22);
    }

    /**
     * Add the constant to each of a's components and store it in this matrix.
     * 
     * @param a The base matrix
     * @param c Constant factor added to each of matrix value
     * @return This matrix
     * @throws NullPointerException if a is null
     */
    public Matrix3 add(@Const Matrix3 a, double c) {
        return set(a.m00 + c, a.m01 + c, a.m02 + c, 
                   a.m10 + c, a.m11 + c, a.m12 + c, 
                   a.m20 + c, a.m21 + c, a.m22 + c);
    }

    /**
     * Compute and return the determinant for this matrix. If this value is 0,
     * then the matrix is not invertible. If it is very close to 0, then the
     * matrix may be ill-formed and inversions, multiplications and linear
     * solving could be inaccurate.
     * 
     * @return The matrix's determinant
     */
    public double determinant() {
        double t1 = m11 * m22 - m12 * m21;
        double t2 = m10 * m22 - m12 * m20;
        double t3 = m10 * m21 - m11 * m20;

        return m00 * t1 - m01 * t2 + m02 * t3;
    }

    /**
     * Compute the inverse of <tt>a</tt> and store the inverted matrix in this
     * matrix.
     * 
     * @param a The matrix whose inverse is computed
     * @throws NullPointerException if a is null
     * @throws ArithmeticException if this matrix isn't invertible
     */
    public Matrix3 inverse(@Const Matrix3 a) {
        double invDet = a.determinant();
        if (Math.abs(invDet) < .00001)
            throw new ArithmeticException("Singular or ill-formed matrix");
        invDet = 1 / invDet;

        double r00 = a.m22 * a.m11 - a.m21 * a.m12;
        double r01 = a.m21 * a.m02 - a.m22 * a.m01;
        double r02 = a.m12 * a.m01 - a.m11 * a.m02;

        double r10 = a.m20 * a.m12 - a.m22 * a.m10;
        double r11 = a.m22 * a.m00 - a.m20 * a.m02;
        double r12 = a.m10 * a.m02 - a.m12 * a.m00;

        double r20 = a.m21 * a.m10 - a.m20 * a.m11;
        double r21 = a.m20 * a.m01 - a.m21 * a.m00;
        double r22 = a.m11 * a.m00 - a.m10 * a.m01;

        return set(invDet * r00, invDet * r01, invDet * r02, 
                   invDet * r10, invDet * r11, invDet * r12, 
                   invDet * r20, invDet * r21, invDet * r22);
    }

    /**
     * Compute and return the length of this matrix. The length of a matrix is
     * defined as the square root of the sum of the squared matrix values.
     * 
     * @return The matrix's length
     */
    public double length() {
        double row1 = m00 * m00 + m01 * m01 + m02 * m02;
        double row2 = m10 * m10 + m11 * m11 + m12 * m12;
        double row3 = m20 * m20 + m21 * m21 + m22 * m22;

        return Math.sqrt(row1 + row2 + row3);
    }

    /**
     * Compute <code>a X b</code> and store the result in this matrix.
     * 
     * @param a The left side of the multiplication
     * @param b The right side of the multiplication
     * @return This matrix
     * @throws NullPointerException if a or b are null
     */
    public Matrix3 mul(@Const Matrix3 a, @Const Matrix3 b) {
        return set(a.m00 * b.m00 + a.m01 * b.m10 + a.m02 * b.m20, 
                   a.m00 * b.m01 + a.m01 * b.m11 + a.m02 * b.m21, 
                   a.m00 * b.m02 + a.m01 * b.m12 + a.m02 * b.m22, 
                   a.m10 * b.m00 + a.m11 * b.m10 + a.m12 * b.m20, 
                   a.m10 * b.m01 + a.m11 * b.m11 + a.m12 * b.m21, 
                   a.m10 * b.m02 + a.m11 * b.m12 + a.m12 * b.m22, 
                   a.m20 * b.m00 + a.m21 * b.m10 + a.m22 * b.m20, 
                   a.m20 * b.m01 + a.m21 * b.m11 + a.m22 * b.m21, 
                   a.m20 * b.m02 + a.m21 * b.m12 + a.m22 * b.m22);
    }

    /**
     * Multiply <tt>a</tt> by the diagonal matrix that takes it's three diagonal
     * entries from <tt>b</tt>, or compute <code>[a] X [m]</code>, where [m] is
     * all 0s except m00 = b.x, m11 = b.y, and m22 = b.z.
     * 
     * @param a The left matrix in the multiplication
     * @param b Vector holding the three diagonal entries of the other matrix
     * @return This matrix
     * @throws NullPointerException if a or b are null
     */
    public Matrix3 mulDiagonal(@Const Matrix3 a, @Const Vector3 b) {
        return set(a.m00 * b.x, a.m01 * b.y, a.m02 * b.z,
                   a.m10 * b.x, a.m11 * b.y, a.m12 * b.z, 
                   a.m20 * b.x, a.m21 * b.y, a.m22 * b.z); 
    }

    /**
     * <p>
     * Multiply the transpose of <tt>a</tt> by the transpose of <tt>b</tt>, or compute
     * <code>[a]^T x [b]^T</code> and store it in this matrix.
     * </p>
     * <p>
     * Note that <code>[a]^T x [b]^T = ([b] x [a])^T</code>
     * </p>
     * 
     * @param a The left side of the multiplication
     * @param b The right side of the multiplication
     * @return This matrix
     * @throws NullPointerException if a or b are null
     */
    public Matrix3 mulTransposeBoth(@Const Matrix3 a, @Const Matrix3 b) {
        // first compute [b] x [a], and then transpose this matrix in place
        return mul(b, a).transpose();
    }

    /**
     * Compute <code>[a]^T x [b]</code> and store the result in this matrix.
     * 
     * @param a The left side of the multiplication, transposed
     * @param b The right side of the multiplication
     * @return This matrix
     * @throws NullPointerException if a or b are null
     */
    public Matrix3 mulTransposeLeft(@Const Matrix3 a, @Const Matrix3 b) {
        return set(a.m00 * b.m00 + a.m10 * b.m10 + a.m20 * b.m20, 
                   a.m00 * b.m01 + a.m10 * b.m11 + a.m20 * b.m21, 
                   a.m00 * b.m02 + a.m10 * b.m12 + a.m20 * b.m22, 
                   a.m01 * b.m00 + a.m11 * b.m10 + a.m21 * b.m20, 
                   a.m01 * b.m01 + a.m11 * b.m11 + a.m21 * b.m21, 
                   a.m01 * b.m02 + a.m11 * b.m12 + a.m21 * b.m22, 
                   a.m02 * b.m00 + a.m12 * b.m10 + a.m22 * b.m20, 
                   a.m02 * b.m01 + a.m12 * b.m11 + a.m22 * b.m21, 
                   a.m02 * b.m02 + a.m12 * b.m12 + a.m22 * b.m22);
    }

    /**
     * Compute <code>[a] x [b]^T</code> and store the result in this matrix.
     * 
     * @param a The left side of the multiplication
     * @param b The right side of the multiplication, transposed
     * @return This matrix
     * @throws NullPointerException if a or b are null
     */
    public Matrix3 mulTransposeRight(@Const Matrix3 a, @Const Matrix3 b) {
        return set(a.m00 * b.m00 + a.m01 * b.m01 + a.m02 * b.m02, 
                   a.m00 * b.m10 + a.m01 * b.m11 + a.m02 * b.m12, 
                   a.m00 * b.m20 + a.m01 * b.m21 + a.m02 * b.m22, 
                   a.m10 * b.m00 + a.m11 * b.m01 + a.m12 * b.m02, 
                   a.m10 * b.m10 + a.m11 * b.m11 + a.m12 * b.m12, 
                   a.m10 * b.m20 + a.m11 * b.m21 + a.m12 * b.m22, 
                   a.m20 * b.m00 + a.m21 * b.m01 + a.m22 * b.m02, 
                   a.m20 * b.m10 + a.m21 * b.m11 + a.m22 * b.m12,
                   a.m20 * b.m20 + a.m21 * b.m21 + a.m22 * b.m22);
    }

    /**
     * Scale each of <tt>a</tt>'s values by the scalar and store it in this
     * matrix. This effectively computes <code>scalar x [a]</code>
     * 
     * @param a The matrix to scale
     * @param scalar Scale factor applied to each matrix value
     * @return This matrix
     * @throws NullPointerException if a is null
     */
    public Matrix3 scale(@Const Matrix3 a, double scalar) {
        return set(scalar * a.m00, scalar * a.m01, scalar * a.m02, 
                   scalar * a.m10, scalar * a.m11, scalar * a.m12,
                   scalar * a.m20, scalar * a.m21, scalar * a.m22);
    }

    /**
     * Compute the trace of this matrix. The trace is defined as the sum of the
     * diagonal entries of the matrix.
     * 
     * @return The matrix's trace
     */
    public double trace() {
        return m00 + m11 + m22;
    }

    /**
     * Transpose <tt>a</tt> and store the result in this matrix.
     * 
     * @param a The matrix to transpose
     * @return This matrix
     * @throws NullPointerException if a is null
     */
    public Matrix3 transpose(@Const Matrix3 a) {
        return set(a.m00, a.m10, a.m20,
                   a.m01, a.m11, a.m21, 
                   a.m02, a.m12, a.m22);
    }

    /**
     * As {@link #add(Matrix3, Matrix3)} with the first parameter being this
     * matrix.
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix3 add(@Const Matrix3 r) {
        return add(this, r);
    }

    /**
     * As {@link #add(Matrix3, double)} with the first parameter being this
     * matrix.
     * 
     * @param c
     * @return This matrix
     */
    public Matrix3 add(double c) {
        return add(this, c);
    }

    /**
     * Inverse this matrix in place, equivalent to {@link #inverse(Matrix3)}
     * with the first parameter being this matrix.
     * 
     * @return This matrix
     * @throws ArithmeticException if this matrix isn't invertible
     */
    public Matrix3 inverse() {
        return inverse(this);
    }

    /**
     * As {@link #mul(Matrix3, Matrix3)} with the first parameter being
     * this matrix.
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix3 mul(@Const Matrix3 r) {
        return mul(this, r);
    }

    /**
     * As {@link #mulDiagonal(Matrix3, Vector3)} with the first parameter
     * being this matrix.
     * 
     * @param diag
     * @return This matrix
     * @throws NullPointerException if diag is null
     */
    public Matrix3 mulDiagonal(Vector3 diag) {
        return mulDiagonal(this, diag); 
    }

    /**
     * As {@link #mulTransposeBoth(Matrix3, Matrix3)} with the first parameter
     * being this matrix.
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix3 mulTransposeBoth(Matrix3 r) {
        return mulTransposeBoth(this, r);
    }

    /**
     * As {@link #mulTransposeLeft(Matrix3, Matrix3)} with the first parameter
     * being this matrix.
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix3 mulTransposeLeft(Matrix3 r) {
        return mulTransposeLeft(this, r);
    }

    /**
     * As {@link #mulTransposeRight(Matrix3, Matrix3)} with the first
     * parameter being this matrix.
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix3 mulTransposeRight(Matrix3 r) {
        return mulTransposeRight(this, r);
    }

    /**
     * As {@link #scale(Matrix3, double)} with the first parameter being this
     * matrix.
     * 
     * @param scalar
     * @return This matrix
     */
    public Matrix3 scale(double scalar) {
        return scale(this, scalar);
    }

    /**
     * Transpose this matrix in place, equivalent to
     * {@link #transpose(Matrix3)} with the first parameter being this matrix.
     * 
     * @return This matrix
     */
    public Matrix3 transpose() {
        return transpose(this);
    }

    /**
     * Set this Matrix3 to be the rotation matrix representing the same
     * rotation stored by <tt>q</tt>.
     * 
     * @param q The quaternion to convert to matrix form
     * @return This matrix for chaining
     * @throws NullPointerException if q is null
     * @throws ArithmeticException if the length of q is 0
     */
    public Matrix3 set(@Const Quat4 q) {
        double d = q.lengthSquared();
        if (d == 0f)
            throw new ArithmeticException("Quaternion length is 0");
        
        double s = 2 / d;
        
        double xs = q.x * s,  ys = q.y * s, zs = q.z * s;
        double wx = q.w * xs, wy = q.w * ys, wz = q.w * zs;
        double xx = q.x * xs, xy = q.x * ys, xz = q.x * zs;
        double yy = q.y * ys, yz = q.y * zs, zz = q.z * zs;
        
        return set(1f - (yy + zz), xy - wz, xz + wy,
                   xy + wz, 1f - (xx + zz), yz - wx,
                   xz - wy, yz + wx, 1f - (xx + yy));
    }

    

    /**
     * Set the matrix component at the given row and column to value. Both row
     * and col must be in [0, 2].
     * 
     * @param row The matrix row, in [0, 2]
     * @param col The matrix column, in [0, 2]
     * @param value The new value for m[row][col]
     * @return This matrix
     * @throws IndexOutOfBoundsException if row or col is invalid
     */
    public Matrix3 set(int row, int col, double value) {
        if (row == 0) {
            if (col == 0) {
                m00 = value;
            } else if (col == 1) {
                m01 = value;
            } else if (col == 2) {
                m02 = value;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, or 2: " + col);
            }
        } else if (row == 1) {
            if (col == 0) {
                m10 = value;
            } else if (col == 1) {
                m11 = value;
            } else if (col == 2) {
                m12 = value;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, or 2: " + col);
            }
        } else if (row == 2) {
            if (col == 0) {
                m20 = value;
            } else if (col == 1) {
                m21 = value;
            } else if (col == 2) {
                m22 = value;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, or 2: " + col);
            }
        } else {
            throw new IndexOutOfBoundsException("Row is not 0, 1, or 2: " + row);
        }
        return this;
    }

    /**
     * Set the given matrix column to the three values stored in the vector. The
     * x component is the value used for the 1st row, the y is the 2nd row, and
     * the z is the 3rd row.
     * 
     * @param col Matrix column to use, in [0, 2]
     * @param values Vector source for the three column values
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws IndexOutOfBoundsException if col is invalid
     */
    public Matrix3 setCol(int col, @Const Vector3 values) {
        if (col == 0) {
            m00 = values.x;
            m10 = values.y;
            m20 = values.z;
        } else if (col == 1) {
            m01 = values.x;
            m11 = values.y;
            m21 = values.z;
        } else if (col == 2) {
            m02 = values.x;
            m12 = values.y;
            m22 = values.z;
        } else {
            throw new IndexOutOfBoundsException("Column is not 0, 1, or 2: " + col);
        }
        return this;
    }

    /**
     * Set the given matrix row to the three values stored in the vector. The x
     * component is the value used for the 1st column, the y is the 2nd column,
     * and the z is the 3rd column.
     * 
     * @param row Matrix row to use, in [0, 2]
     * @param values Vector source for the three row values
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws IndexOutOfBoundsException if row is invalid
     */
    public Matrix3 setRow(int row, @Const Vector3 values) {
        if (row == 0) {
            m00 = values.x;
            m01 = values.y;
            m02 = values.z;
        } else if (row == 1) {
            m10 = values.x;
            m11 = values.y;
            m12 = values.z;
        } else if (row == 2) {
            m20 = values.x;
            m21 = values.y;
            m22 = values.z;
        } else {
            throw new IndexOutOfBoundsException("Row is not 0, 1, or 2: " + row);
        }
        return this;
    }

    /**
     * Set all nine values of this matrix from the given array values. If
     * rowMajor is true, each set of three doubles is treated as a row. If it is
     * false, the values are column major, and each three represents a matrix
     * column. The values are taken from the values array starting at offset; it
     * is assumed that the array is long enough.
     * 
     * @param values Float source for the new matrix data
     * @param offset Start index of the double values
     * @param rowMajor True if values is row-major order
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 9 values
     *             starting at offset
     */
    public Matrix3 set(double[] values, int offset, boolean rowMajor) {
        if (rowMajor)
            return set(values[offset], values[offset + 1], values[offset + 2], 
                       values[offset + 3], values[offset + 4], values[offset + 5], 
                       values[offset + 6], values[offset + 7], values[offset + 8]);
        else
            return set(values[offset], values[offset + 3], values[offset + 6], 
                       values[offset + 1], values[offset + 4], values[offset + 7], 
                       values[offset + 2], values[offset + 5], values[offset + 8]);
    }

    /**
     * As {@link #set(double[], int, boolean)} except a float[] is the source of
     * values.
     * 
     * @param values Float source for the new matrix data
     * @param offset Start index of the double values
     * @param rowMajor True if values are row-major
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 16 elements
     *             starting at offset
     */
    public Matrix3 set(float[] values, int offset, boolean rowMajor) {
        if (rowMajor)
            return set(values[offset], values[offset + 1], values[offset + 2], 
                       values[offset + 3], values[offset + 4], values[offset + 5], 
                       values[offset + 6], values[offset + 7], values[offset + 8]);
        else
            return set(values[offset], values[offset + 3], values[offset + 6], 
                       values[offset + 1], values[offset + 4], values[offset + 7], 
                       values[offset + 2], values[offset + 5], values[offset + 8]);
    }

    /**
     * As {@link #set(double[], int, boolean)} except a FloatBuffer is the
     * source of values.
     * 
     * @param values Float source for the new matrix data
     * @param offset Start index of the double values
     * @param rowMajor True if values are row-major
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 16 elements
     *             starting at offset
     */
    public Matrix3 set(DoubleBuffer values, int offset, boolean rowMajor) {
        if (rowMajor)
            return set(values.get(offset), values.get(offset + 1), values.get(offset + 2), 
                       values.get(offset + 3), values.get(offset + 4), values.get(offset + 5), 
                       values.get(offset + 6), values.get(offset + 7), values.get(offset + 8));
        else
            return set(values.get(offset), values.get(offset + 3), values.get(offset + 6), 
                       values.get(offset + 1), values.get(offset + 4), values.get(offset + 7), 
                       values.get(offset + 2), values.get(offset + 5), values.get(offset + 8));
    }

    /**
     * As {@link #set(double[], int, boolean)} except a FloatBuffer is the
     * source of values.
     * 
     * @param values Float source for the new matrix data
     * @param offset Start index of the double values
     * @param rowMajor True if values are row-major
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 16 elements
     *             starting at offset
     */
    public Matrix3 set(FloatBuffer values, int offset, boolean rowMajor) {
        if (rowMajor)
            return set(values.get(offset), values.get(offset + 1), values.get(offset + 2), 
                       values.get(offset + 3), values.get(offset + 4), values.get(offset + 5), 
                       values.get(offset + 6), values.get(offset + 7), values.get(offset + 8));
        else
            return set(values.get(offset), values.get(offset + 3), values.get(offset + 6), 
                       values.get(offset + 1), values.get(offset + 4), values.get(offset + 7), 
                       values.get(offset + 2), values.get(offset + 5), values.get(offset + 8));
    }

    /**
     * Set the nine values of this matrix in bulk. This is equivalent to
     * assigning each parameter to the matrix's public field of identical name.
     * 
     * @param m00 New value for 1st row and 1st column
     * @param m01 New value for 1st row and 2nd column
     * @param m02 New value for 1st row and 3rd column
     * @param m10 New value for 2nd row and 1st column
     * @param m11 New value for 2nd row and 2nd column
     * @param m12 New value for 2nd row and 3rd column
     * @param m20 New value for 3rd row and 1st column
     * @param m21 New value for 3rd row and 2nd column
     * @param m22 New value for 3rd row and 3rd column
     * @return This matrix
     */
    public Matrix3 set(double m00, double m01, double m02, 
                       double m10, double m11, double m12, 
                       double m20, double m21, double m22) {
        this.m00 = m00; this.m01 = m01; this.m02 = m02;
        this.m10 = m10; this.m11 = m11; this.m12 = m12;
        this.m20 = m20; this.m21 = m21; this.m22 = m22;
        return this;
    }

    /**
     * Set this matrix to be equal to the given matrix, o.
     * 
     * @param o Matrix whose values are copied into this matrix
     * @return This matrix
     * @throws NullPointerException if o is null
     */
    public Matrix3 set(@Const Matrix3 o) {
        this.m00 = o.m00; this.m01 = o.m01; this.m02 = o.m02;
        this.m10 = o.m10; this.m11 = o.m11; this.m12 = o.m12;
        this.m20 = o.m20; this.m21 = o.m21; this.m22 = o.m22;
        return this;
    }

    /**
     * Reset this matrix's values so that it represents the identity matrix.
     * 
     * @return This matrix
     */
    public Matrix3 setIdentity() {
        return set(1, 0, 0, 
                   0, 1, 0, 
                   0, 0, 1);
    }

    /**
     * Return the value of the matrix entry at the given row and column. row and
     * col must both be within [0, 2], where 0 represents the 1st row or column.
     * 
     * @param row Row to access, in [0, 2]
     * @param col Col to access, in [0, 2]
     * @return Matrix value at (row, col)
     * @throws IndexOutOfBoundsException if row or col are invalid
     */
    public double get(int row, int col) {
        if (row == 0) {
            if (col == 0) {
                return m00;
            } else if (col == 1) {
                return m01;
            } else if (col == 2) {
                return m02;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, or 2: " + col);
            }
        } else if (row == 1) {
            if (col == 0) {
                return m10;
            } else if (col == 1) {
                return m11;
            } else if (col == 2) {
                return m12;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, or 2: " + col);
            }
        } else if (row == 2) {
            if (col == 0) {
                return m20;
            } else if (col == 1) {
                return m21;
            } else if (col == 2) {
                return m22;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, or 2: " + col);
            }
        } else {
            throw new IndexOutOfBoundsException("Row is not 0, 1, or 2: " + row);
        }
    }
    
    /**
     * <p>
     * Store the entire matrix into an array, starting at offset. If rowMajor is
     * true, the matrix should stored in row-major order (i.e. every 3 values
     * constitutes a row). If it is false, then the matrix is stored in
     * column-major order (every 3 values is a column of the matrix).
     * </p>
     * <p>
     * It is assumed that there is enough space in store, starting at offset to
     * hold the nine matrix values of this matrix.
     * </p>
     * 
     * @param store Array to hold the nine matrix values.
     * @param offset First index used to store the matrix data
     * @param rowMajor True if the matrix values are stored as rows, otherwise
     *            as columns
     * @throws NullPointerException if store is null
     * @throws ArrayIndexOutOfBoundsException if there isn't enough space in
     *             store to contain the matrix, starting at offset
     */
    public void get(double[] store, int offset, boolean rowMajor) {
        if (rowMajor) {
            store[offset + 0] = m00;
            store[offset + 1] = m01;
            store[offset + 2] = m02;
            store[offset + 3] = m10;
            store[offset + 4] = m11;
            store[offset + 5] = m12;
            store[offset + 6] = m20;
            store[offset + 7] = m21;
            store[offset + 8] = m22;
        } else {
            store[offset + 0] = m00;
            store[offset + 1] = m10;
            store[offset + 2] = m20;
            store[offset + 3] = m01;
            store[offset + 4] = m11;
            store[offset + 5] = m21;
            store[offset + 6] = m02;
            store[offset + 7] = m12;
            store[offset + 8] = m22;
        }
    }
    
    /**
     * As {@link #get(double[], int, boolean)}, but the data is cast
     * to floats.
     * 
     * @param store The float[] to hold the row values
     * @param offset The first index to use in the store
     * @param rowMajor True if the matrix values are stored as rows, otherwise
     * as columns
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the column
     */
    public void get(float[] store, int offset, boolean rowMajor) {
        if (rowMajor) {
            store[offset + 0] = (float) m00;
            store[offset + 1] = (float) m01;
            store[offset + 2] = (float) m02;
            store[offset + 3] = (float) m10;
            store[offset + 4] = (float) m11;
            store[offset + 5] = (float) m12;
            store[offset + 6] = (float) m20;
            store[offset + 7] = (float) m21;
            store[offset + 8] = (float) m22;
        } else {
            store[offset + 0] = (float) m00;
            store[offset + 1] = (float) m10;
            store[offset + 2] = (float) m20;
            store[offset + 3] = (float) m01;
            store[offset + 4] = (float) m11;
            store[offset + 5] = (float) m21;
            store[offset + 6] = (float) m02;
            store[offset + 7] = (float) m12;
            store[offset + 8] = (float) m22;
        }
    }
    
    /**
     * As {@link #get(double[], int, boolean)}, but with a DoubleBuffer.
     * <tt>offset</tt> is measured from 0, not the buffer's position.
     * 
     * @param store The DoubleBuffer to hold the row values
     * @param offset The first index to use in the store
     * @param rowMajor True if the matrix values are stored as rows, otherwise
     * as columns
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the column
     */
    public void get(DoubleBuffer store, int offset, boolean rowMajor) {
        if (rowMajor) {
            store.put(offset + 0, m00);
            store.put(offset + 1, m01);
            store.put(offset + 2, m02);
            store.put(offset + 3, m10);
            store.put(offset + 4, m11);
            store.put(offset + 5, m12);
            store.put(offset + 6, m20);
            store.put(offset + 7, m21);
            store.put(offset + 8, m22);
        } else {
            store.put(offset + 0, m00);
            store.put(offset + 1, m10);
            store.put(offset + 2, m20);
            store.put(offset + 3, m01);
            store.put(offset + 4, m11);
            store.put(offset + 5, m21);
            store.put(offset + 6, m02);
            store.put(offset + 7, m12);
            store.put(offset + 8, m22);
        }
    }
    
    /**
     * As {@link #get(double[], int, boolean)}, but with a FloatBuffer.
     * <tt>offset</tt> is measured from 0, not the buffer's position.
     * 
     * @param store The FloatBuffer to hold the row values
     * @param offset The first index to use in the store
     * @param rowMajor True if the matrix values are stored as rows, otherwise
     * as columns
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the column
     */
    public void get(FloatBuffer store, int offset, boolean rowMajor) {
        if (rowMajor) {
            store.put(offset + 0, (float) m00);
            store.put(offset + 1, (float) m01);
            store.put(offset + 2, (float) m02);
            store.put(offset + 3, (float) m10);
            store.put(offset + 4, (float) m11);
            store.put(offset + 5, (float) m12);
            store.put(offset + 6, (float) m20);
            store.put(offset + 7, (float) m21);
            store.put(offset + 8, (float) m22);
        } else {
            store.put(offset + 0, (float) m00);
            store.put(offset + 1, (float) m10);
            store.put(offset + 2, (float) m20);
            store.put(offset + 3, (float) m01);
            store.put(offset + 4, (float) m11);
            store.put(offset + 5, (float) m21);
            store.put(offset + 6, (float) m02);
            store.put(offset + 7, (float) m12);
            store.put(offset + 8, (float) m22);
        }
    }

    /**
     * Return a new Vector3 that copies the values of the given column.
     * 
     * @param col The column to fetch
     * @return A new vector equaling the given column
     * @throws IndexOutOfBoundsException if col is not 0, 1 or 2
     */
    public Vector3 getCol(int col) {
        if (col == 0) {
            return new Vector3(m00, m10, m20);
        } else if (col == 1) {
            return new Vector3(m01, m11, m21);
        } else if (col == 2) {
            return new Vector3(m02, m12, m22);
        } else {
            throw new IndexOutOfBoundsException("Column is not 0, 1, or 2:" + col);
        }
    }

    /**
     * Return a new Vector3 that copies the values of the given row.
     * 
     * @param row The row to fetch
     * @return A new vector equaling the given row
     * @throws IndexOutOfBoundsException if row is not 0, 1, or 2
     */
    public Vector3 getRow(int row) {
        if (row == 0) {
            return new Vector3(m00, m01, m02);
        } else if (row == 1) {
            return new Vector3(m10, m11, m12);
        } else if (row == 2) {
            return new Vector3(m20, m21, m22);
        } else {
            throw new IndexOutOfBoundsException("Row is not 0, 1, or 2:" + row);
        }
    }

    /**
     * Determine if these two matrices are equal, within an error range of eps.
     * 
     * @param e Matrix to check approximate equality to
     * @param eps Error tolerance of each component
     * @return True if all component values are within eps of the corresponding
     *         component of e
     */
    public boolean epsilonEquals(@Const Matrix3 e, double eps) {
        if (e == null)
            return false;

        return Math.abs(m00 - e.m00) <= eps && 
               Math.abs(m01 - e.m01) <= eps && 
               Math.abs(m02 - e.m02) <= eps && 
               Math.abs(m10 - e.m10) <= eps && 
               Math.abs(m11 - e.m11) <= eps && 
               Math.abs(m12 - e.m12) <= eps && 
               Math.abs(m20 - e.m20) <= eps && 
               Math.abs(m21 - e.m21) <= eps && 
               Math.abs(m22 - e.m22) <= eps;
    }

    @Override
    public boolean equals(Object o) {
        // if conditional handles null values
        if (!(o instanceof Matrix3))
            return false;
        Matrix3 e = (Matrix3) o;
        return m00 == e.m00 && m01 == e.m01 && m02 == e.m02 && 
               m10 == e.m10 && m11 == e.m11 && m12 == e.m12 && 
               m20 == e.m20 && m21 == e.m21 && m22 == e.m22;
    }

    @Override
    public int hashCode() {
        long result = 17;
        result += result * 31 + Double.doubleToLongBits(m00);
        result += result * 31 + Double.doubleToLongBits(m01);
        result += result * 31 + Double.doubleToLongBits(m02);

        result += result * 31 + Double.doubleToLongBits(m10);
        result += result * 31 + Double.doubleToLongBits(m11);
        result += result * 31 + Double.doubleToLongBits(m12);

        result += result * 31 + Double.doubleToLongBits(m20);
        result += result * 31 + Double.doubleToLongBits(m21);
        result += result * 31 + Double.doubleToLongBits(m22);

        return (int) (((result & 0xffffffff00000000L) >> 32) ^ (result & 0x00000000ffffffffL));
    }
    
    @Override
    public String toString() {
        return "[[ " + m00 + ", " + m01 + ", " + m02 + " ]\n" +
                "[ " + m10 + ", " + m11 + ", " + m12 + " ]\n" +
                "[ " + m20 + ", " + m21 + ", " + m22 + " ]]";
    }
}
