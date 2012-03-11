package com.ferox.math;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * <p>
 * Matrix4 provides an implementation of a 4-by-4 mathematical matrix with many
 * common operations available from linear algebra. It's 16 components are
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
 * In all mathematical functions whose result is a matrix, the Matrix4 calling
 * the method will contain the result. The input matrices will be left
 * unmodified. It is safe for the calling matrix to be any matrix parameter into
 * the function.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Matrix4 implements Cloneable {
    public double m00, m01, m02, m03;
    public double m10, m11, m12, m13;
    public double m20, m21, m22, m23;
    public double m30, m31, m32, m33;
    
    /**
     * Construct a new Matrix4 that's set to the identity matrix.
     */
    public Matrix4() {
        set(0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0);
    }

    /**
     * Construct a new Matrix4 that's copies the values in o.
     * 
     * @see #set(Matrix4)
     * @param o Matrix to clone
     * @throws NullPointerException if o is null
     */
    public Matrix4(Matrix4 o) {
        set(o);
    }
    
    /**
     * Construct a new Matrix4 that assigns each parameter value to the
     * identically named field.
     * 
     * @see #set(float, float, float, float, float, float, float, float, float,
     *      float, float, float, float, float, float, float)
     * @param m00
     * @param m01
     * @param m02
     * @param m03
     * @param m10
     * @param m11
     * @param m12
     * @param m13
     * @param m20
     * @param m21
     * @param m22
     * @param m23
     * @param m30
     * @param m31
     * @param m32
     * @param m33
     */
    public Matrix4(double m00, double m01, double m02, double m03, 
                    double m10, double m11, double m12, double m13,
                    double m20, double m21, double m22, double m23, 
                    double m30, double m31, double m32, double m33) {
        set(m00, m01, m02, m03, 
            m10, m11, m12, m13, 
            m20, m21, m22, m23,
            m30, m31, m32, m33);
    }
    
    @Override
    public Matrix4 clone() {
        return new Matrix4(this);
    }

    /**
     * Compute <code>[a] + [b]</code> and store the result in this matrix.
     * 
     * @param a The left side of the addition
     * @param b The right side of the addition
     * @return This matrix
     * @throws NullPointerException if a and b are null
     */
    public Matrix4 add(@Const Matrix4 a, @Const Matrix4 b) {
        return set(a.m00 + b.m00, a.m01 + b.m01, a.m02 + b.m02, a.m03 + b.m03, 
                   a.m10 + b.m10, a.m11 + b.m11, a.m12 + b.m12, a.m13 + b.m13, 
                   a.m20 + b.m20, a.m21 + b.m21, a.m22 + b.m22, a.m23 + b.m23, 
                   a.m30 + b.m30, a.m31 + b.m31, a.m32 + b.m32, a.m33 + b.m33);
    }

    /**
     * Add <tt>c</tt> to each component of <tt>m</tt> and store the result in
     * this matrix.
     * 
     * @param m The matrix in the addition
     * @param c Constant factor added to each of m's value
     * @return This matrix
     * @throws NullPointerException if m is null
     */
    public Matrix4 add(@Const Matrix4 m, double c) {
        return set(m.m00 + c, m.m01 + c, m.m02 + c, m.m03 + c, 
                   m.m10 + c, m.m11 + c, m.m12 + c, m.m13 + c, 
                   m.m20 + c, m.m21 + c, m.m22 + c, m.m23 + c, 
                   m.m30 + c, m.m31 + c, m.m32 + c, m.m33 + c);
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
        // Thanks to Ardor3D for the determinant expansion
        double ra0 = m00 * m11 - m01 * m10;
        double ra1 = m00 * m12 - m02 * m10;
        double ra2 = m00 * m13 - m03 * m10;
        double ra3 = m01 * m12 - m02 * m11;
        double ra4 = m01 * m13 - m03 * m11;
        double ra5 = m02 * m13 - m03 * m12;
        double rb0 = m20 * m31 - m21 * m30;
        double rb1 = m20 * m32 - m22 * m30;
        double rb2 = m20 * m33 - m23 * m30;
        double rb3 = m21 * m32 - m22 * m31;
        double rb4 = m21 * m33 - m23 * m31;
        double rb5 = m22 * m33 - m23 * m32;

        return ra0 * rb5 - ra1 * rb4 + ra2 * rb3 + ra3 * rb2 - ra4 * rb1 + ra5 * rb0;
    }

    /**
     * Compute the inverse of <tt>m</tt> and store it in this matrix.
     * 
     * @param m The matrix to invert
     * @return This matrix
     * @throws ArithmeticException if this matrix isn't invertible
     * @throws NullPointerException if m is null
     */
    public Matrix4 inverse(@Const Matrix4 m) {
        // Also, thanks to Ardor3D for the inverse code

        // inlined det computation to get at intermediate results
        double ra0 = m.m00 * m.m11 - m.m01 * m.m10;
        double ra1 = m.m00 * m.m12 - m.m02 * m.m10;
        double ra2 = m.m00 * m.m13 - m.m03 * m.m10;
        double ra3 = m.m01 * m.m12 - m.m02 * m.m11;
        double ra4 = m.m01 * m.m13 - m.m03 * m.m11;
        double ra5 = m.m02 * m.m13 - m.m03 * m.m12;
        double rb0 = m.m20 * m.m31 - m.m21 * m.m30;
        double rb1 = m.m20 * m.m32 - m.m22 * m.m30;
        double rb2 = m.m20 * m.m33 - m.m23 * m.m30;
        double rb3 = m.m21 * m.m32 - m.m22 * m.m31;
        double rb4 = m.m21 * m.m33 - m.m23 * m.m31;
        double rb5 = m.m22 * m.m33 - m.m23 * m.m32;

        double invDet = ra0 * rb5 - ra1 * rb4 + ra2 * rb3 + ra3 * rb2 - ra4 * rb1 + ra5 * rb0;
        if (Math.abs(invDet) <= .00001)
            throw new ArithmeticException("Singular m.matrix");
        invDet = 1 / invDet;

        double t00 = m.m11 * rb5 - m.m12 * rb4 + m.m13 * rb3;
        double t10 = m.m12 * rb2 - m.m13 * rb1 - m.m10 * rb5;
        double t20 = m.m10 * rb4 - m.m11 * rb2 + m.m13 * rb0;
        double t30 = m.m11 * rb1 - m.m12 * rb0 - m.m10 * rb3;
        double t01 = m.m02 * rb4 - m.m03 * rb3 - m.m01 * rb5;
        double t11 = m.m00 * rb5 - m.m02 * rb2 + m.m03 * rb1;
        double t21 = m.m01 * rb2 - m.m03 * rb0 - m.m00 * rb4;
        double t31 = m.m00 * rb3 - m.m01 * rb1 + m.m02 * rb0;
        double t02 = m.m31 * ra5 - m.m32 * ra4 + m.m33 * ra3;
        double t12 = m.m32 * ra2 - m.m33 * ra1 - m.m30 * ra5;
        double t22 = m.m30 * ra4 - m.m31 * ra2 + m.m33 * ra0;
        double t32 = m.m31 * ra1 - m.m32 * ra0 - m.m30 * ra3;
        double t03 = m.m22 * ra4 - m.m23 * ra3 - m.m21 * ra5;
        double t13 = m.m20 * ra5 - m.m22 * ra2 + m.m23 * ra1;
        double t23 = m.m21 * ra2 - m.m23 * ra0 - m.m20 * ra4;
        double t33 = m.m20 * ra3 - m.m21 * ra1 + m.m22 * ra0;

        return set(invDet * t00, invDet * t01, invDet * t02, invDet * t03, 
                   invDet * t10, invDet * t11, invDet * t12, invDet * t13, 
                   invDet * t20, invDet * t21, invDet * t22, invDet * t23, 
                   invDet * t30, invDet * t31, invDet * t32, invDet * t33);
    }

    /**
     * Compute and return the length of this matrix. The length of a matrix is
     * defined as the square root of the sum of the squared matrix values.
     * 
     * @return The matrix's length
     */
    public double length() {
        double row1 = m00 * m00 + m01 * m01 + m02 * m02 + m03 * m03;
        double row2 = m10 * m10 + m11 * m11 + m12 * m12 + m13 * m13;
        double row3 = m20 * m20 + m21 * m21 + m22 * m22 + m23 * m23;
        double row4 = m30 * m30 + m31 * m31 + m32 * m32 + m33 * m33;

        return Math.sqrt(row1 + row2 + row3 + row4);
    }

    /**
     * Compute <code>[a] x [b]</code> and store it in this matrix.
     * 
     * @param a The left side of the multiplication
     * @param b The right side of the multiplication
     * @return This matrix
     * @throws NullPointerException if a or b are null
     */
    public Matrix4 mul(@Const Matrix4 a, @Const Matrix4 b) {
        return set(a.m00 * b.m00 + a.m01 * b.m10 + a.m02 * b.m20 + a.m03 * b.m30,
                   a.m00 * b.m01 + a.m01 * b.m11 + a.m02 * b.m21 + a.m03 * b.m31,
                   a.m00 * b.m02 + a.m01 * b.m12 + a.m02 * b.m22 + a.m03 * b.m32, 
                   a.m00 * b.m03 + a.m01 * b.m13 + a.m02 * b.m23 + a.m03 * b.m33, 
                   a.m10 * b.m00 + a.m11 * b.m10 + a.m12 * b.m20 + a.m13 * b.m30, 
                   a.m10 * b.m01 + a.m11 * b.m11 + a.m12 * b.m21 + a.m13 * b.m31, 
                   a.m10 * b.m02 + a.m11 * b.m12 + a.m12 * b.m22 + a.m13 * b.m32, 
                   a.m10 * b.m03 + a.m11 * b.m13 + a.m12 * b.m23 + a.m13 * b.m33, 
                   a.m20 * b.m00 + a.m21 * b.m10 + a.m22 * b.m20 + a.m23 * b.m30, 
                   a.m20 * b.m01 + a.m21 * b.m11 + a.m22 * b.m21 + a.m23 * b.m31, 
                   a.m20 * b.m02 + a.m21 * b.m12 + a.m22 * b.m22 + a.m23 * b.m32, 
                   a.m20 * b.m03 + a.m21 * b.m13 + a.m22 * b.m23 + a.m23 * b.m33, 
                   a.m30 * b.m00 + a.m31 * b.m10 + a.m32 * b.m20 + a.m33 * b.m30, 
                   a.m30 * b.m01 + a.m31 * b.m11 + a.m32 * b.m21 + a.m33 * b.m31, 
                   a.m30 * b.m02 + a.m31 * b.m12 + a.m32 * b.m22 + a.m33 * b.m32, 
                   a.m30 * b.m03 + a.m31 * b.m13 + a.m32 * b.m23 + a.m33 * b.m33);
    }

    /**
     * Multiply <tt>a</tt> by the diagonal matrix that takes it's four diagonal
     * entries from <tt>b</tt>, or compute <code>[a] X [m]</code>, where [m] is
     * all 0s except m00 = b.x, m11 = b.y, m22 = b.z and m33 = b.w
     * 
     * @param a The left matrix in the multiplication
     * @param b Vector holding the four diagonal entries of the other matrix
     * @return This matrix
     * @throws NullPointerException if a or b are null
     */
    public Matrix4 mulDiagonal(@Const Matrix4 a, @Const Vector4 b) {
        return set(a.m00 * b.x, a.m01 * b.y, a.m02 * b.z, a.m03 * b.w, 
                   a.m10 * b.x, a.m11 * b.y, a.m12 * b.z, a.m13 * b.w, 
                   a.m20 * b.x, a.m21 * b.y, a.m22 * b.z, a.m23 * b.w, 
                   a.m30 * b.x, a.m31 * b.y, a.m32 * b.z, a.m33 * b.w);
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
    public Matrix4 mulTransposeBoth(@Const Matrix4 a, @Const Matrix4 b) {
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
    public Matrix4 mulTransposeLeft(@Const Matrix4 a, @Const Matrix4 b) {
        return set(a.m00 * b.m00 + a.m10 * b.m10 + a.m20 * b.m20 + a.m30 * b.m30, 
                   a.m00 * b.m01 + a.m10 * b.m11 + a.m20 * b.m21 + a.m30 * b.m31,
                   a.m00 * b.m02 + a.m10 * b.m12 + a.m20 * b.m22 + a.m30 * b.m32,
                   a.m00 * b.m03 + a.m10 * b.m13 + a.m20 * b.m23 + a.m30 * b.m33,
                   a.m01 * b.m00 + a.m11 * b.m10 + a.m21 * b.m20 + a.m31 * b.m30,
                   a.m01 * b.m01 + a.m11 * b.m11 + a.m21 * b.m21 + a.m31 * b.m31,
                   a.m01 * b.m02 + a.m11 * b.m12 + a.m21 * b.m22 + a.m31 * b.m32, 
                   a.m01 * b.m03 + a.m11 * b.m13 + a.m21 * b.m23 + a.m31 * b.m33, 
                   a.m02 * b.m00 + a.m12 * b.m10 + a.m22 * b.m20 + a.m32 * b.m30, 
                   a.m02 * b.m01 + a.m12 * b.m11 + a.m22 * b.m21 + a.m32 * b.m31,
                   a.m02 * b.m02 + a.m12 * b.m12 + a.m22 * b.m22 + a.m32 * b.m32,
                   a.m02 * b.m03 + a.m12 * b.m13 + a.m22 * b.m23 + a.m32 * b.m33,
                   a.m03 * b.m00 + a.m13 * b.m10 + a.m23 * b.m20 + a.m33 * b.m30,
                   a.m03 * b.m01 + a.m13 * b.m11 + a.m23 * b.m21 + a.m33 * b.m31, 
                   a.m03 * b.m02 + a.m13 * b.m12 + a.m23 * b.m22 + a.m33 * b.m32, 
                   a.m03 * b.m03 + a.m13 * b.m13 + a.m23 * b.m23 + a.m33 * b.m33);
    }

    /**
     * Compute <code>[a] x [b]^T</code> and store the result in this matrix.
     * 
     * @param a The left side of the multiplication
     * @param b The right side of the multiplication, transposed
     * @return This matrix
     * @throws NullPointerException if a or b are null
     */
    public Matrix4 mulTransposeRight(@Const Matrix4 a, @Const Matrix4 b) {
        return set(a.m00 * b.m00 + a.m01 * b.m01 + a.m02 * b.m02 + a.m03 * b.m03, 
                   a.m00 * b.m10 + a.m01 * b.m11 + a.m02 * b.m12 + a.m03 * b.m13, 
                   a.m00 * b.m20 + a.m01 * b.m21 + a.m02 * b.m22 + a.m03 * b.m23,
                   a.m00 * b.m30 + a.m01 * b.m31 + a.m03 * b.m32 + a.m03 * b.m33, 
                   a.m10 * b.m00 + a.m11 * b.m01 + a.m12 * b.m02 + a.m13 * b.m03, 
                   a.m10 * b.m10 + a.m11 * b.m11 + a.m12 * b.m12 + a.m13 * b.m13, 
                   a.m10 * b.m20 + a.m11 * b.m21 + a.m12 * b.m22 + a.m13 * b.m23, 
                   a.m10 * b.m30 + a.m11 * b.m31 + a.m12 * b.m32 + a.m13 * b.m33, 
                   a.m20 * b.m00 + a.m21 * b.m01 + a.m22 * b.m02 + a.m23 * b.m03, 
                   a.m20 * b.m10 + a.m21 * b.m11 + a.m22 * b.m12 + a.m23 * b.m13,
                   a.m20 * b.m20 + a.m21 * b.m21 + a.m22 * b.m22 + a.m23 * b.m23,
                   a.m20 * b.m30 + a.m21 * b.m31 + a.m22 * b.m32 + a.m23 * b.m33,
                   a.m30 * b.m00 + a.m31 * b.m01 + a.m32 * b.m02 + a.m33 * b.m03,
                   a.m30 * b.m10 + a.m31 * b.m11 + a.m32 * b.m12 + a.m33 * b.m13,
                   a.m30 * b.m20 + a.m31 * b.m21 + a.m32 * b.m22 + a.m33 * b.m23, 
                   a.m30 * b.m30 + a.m31 * b.m31 + a.m32 * b.m32 + a.m33 * b.m33);
    }

    /**
     * Compute <code>scalar * [m]</code> and store the result in this matrix.
     * 
     * @param m The matrix being scaled
     * @param scalar The scale factor
     * @return This matrix
     * @throws NullPointerException if m is null
     */
    public Matrix4 scale(@Const Matrix4 m, double scalar) {
        return set(scalar * m.m00, scalar * m.m01, scalar * m.m02, scalar * m.m03, 
                   scalar * m.m10, scalar * m.m11, scalar * m.m12, scalar * m.m13, 
                   scalar * m.m20, scalar * m.m21, scalar * m.m22, scalar * m.m23, 
                   scalar * m.m30, scalar * m.m31, scalar * m.m32, scalar * m.m33);
    }

    /**
     * Compute the trace of this matrix. The trace is defined as the sum of the
     * diagonal entries of the matrix.
     * 
     * @return The matrix's trace
     */
    public double trace() {
        return m00 + m11 + m22 + m33;
    }

    /**
     * Compute the transpose of <tt>m</tt> and store it in this matrix.
     * 
     * @param m The matrix to transpose
     * @return This matrix
     * @throws NullPointerException if m is null
     */
    public Matrix4 transpose(@Const Matrix4 m) {
        return set(m.m00, m.m10, m.m20, m.m30,
                   m.m01, m.m11, m.m21, m.m31, 
                   m.m02, m.m12, m.m22, m.m32, 
                   m.m03, m.m13, m.m23, m.m33);
    }
    
    /**
     * As {@link #add(Matrix4, Matrix4)} with the first parameter being
     * this matrix.
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix4 add(@Const Matrix4 r) {
        return add(this, r);
    }

    /**
     * As {@link #add(Matrix4, double)} with the first parameter being
     * this matrix.
     * 
     * @param c
     * @return This matrix
     */
    public Matrix4 add(double c) {
        return add(this, c);
    }

    /**
     * Invert this matrix in place, equivalent to {@link #inverse(Matrix4)}
     * with the first parameter being this matrix.
     * 
     * @return This matrix
     * @throws ArithmeticException if this matrix isn't invertible
     */
    public Matrix4 inverse() {
        return inverse(this);
    }

    /**
     * As {@link #mul(Matrix4, Matrix4)} with the first parameter being
     * this matrix.
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix4 mul(@Const Matrix4 r) {
        return mul(this, r);
    }

    /**
     * As {@link #mulDiagonal(Matrix4, Vector4)} with the first parameter 
     * being this matrix.
     * 
     * @param diag
     * @return This matrix
     * @throws NullPointerException if diag is null
     */
    public Matrix4 mulDiagonal(@Const Vector4 diag) {
        return mulDiagonal(this, diag);
    }

    /**
     * As {@link #mulTransposeBoth(Matrix4, Matrix4)} with the first parameter being
     * this matrix.
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix4 mulTransposeBoth(@Const Matrix4 r) {
        return mulTransposeBoth(this, r);
    }

    /**
     * As {@link #mulTransposeLeft(Matrix4, Matrix4)} with the first parameter being
     * this matrix.
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix4 mulTransposeLeft(@Const Matrix4 r) {
        return mulTransposeLeft(this, r);
    }

    /**
     * As {@link #mulTransposeRight(Matrix4, Matrix4)} with the first parameter being
     * this matrix.
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix4 mulTransposeRight(@Const Matrix4 r) {
        return mulTransposeRight(this, r);
    }

    /**
     * As {@link #scale(Matrix4, float)} with the first parameter being
     * this matrix.
     * 
     * @param scalar
     * @return This matrix
     */
    public Matrix4 scale(double scalar) {
        return scale(this, scalar);
    }

    /**
     * Transpose this matrix in place, equivalent to
     * {@link #transpose(Matrix4)} with the first parameter being
     * this matrix.
     * 
     * @return This matrix
     */
    public Matrix4 transpose() {
        return transpose(this);
    }

    /**
     * Set the matrix component at the given row and column to value. Both row
     * and col must be in [0, 3].
     * 
     * @param row The matrix row, in [0, 3]
     * @param col The matrix column, in [0, 3]
     * @param value The new value for m[row][col]
     * @return This matrix
     * @throws IndexOutOfBoundsException if row or col is invalid
     */
    public Matrix4 set(int row, int col, double value) {
        if (row == 0) {
            if (col == 0) {
                m00 = value;
            } else if (col == 1) {
                m01 = value;
            } else if (col == 2) {
                m02 = value;
            } else if (col == 3) {
                m03 = value;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, 2, or 3: " + col);
            }
        } else if (row == 1) {
            if (col == 0) {
                m10 = value;
            } else if (col == 1) {
                m11 = value;
            } else if (col == 2) {
                m12 = value;
            } else if (col == 3) {
                m13 = value;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, 2, or 3: " + col);
            }
        } else if (row == 2) {
            if (col == 0) {
                m20 = value;
            } else if (col == 1) {
                m21 = value;
            } else if (col == 2) {
                m22 = value;
            } else if (col == 3) {
                m23 = value;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, 2, or 3: " + col);
            }
        } else if (row == 3) {
            if (col == 0) {
                m30 = value;
            } else if (col == 1) {
                m31 = value;
            } else if (col == 2) {
                m32 = value;
            } else if (col == 3) {
                m33 = value;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, 2, or 3: " + col);
            }
        } else {
            throw new IndexOutOfBoundsException("Row is not 0, 1, 2, or 3: " + row);
        }
        return this;
    }

    /**
     * Set the given matrix column to the four values stored in the vector. The
     * x component is the value used for the 1st row, the y is the 2nd row, the
     * z is the 3rd row, and the w is the 4th row.
     * 
     * @param col Matrix column to use, in [0, 3]
     * @param values Vector source for the four column values
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws IndexOutOfBoundsException if col is invalid
     */
    public Matrix4 setCol(int col, @Const Vector4 values) {
        if (col == 0) {
            m00 = values.x;
            m10 = values.y;
            m20 = values.z;
            m30 = values.w;
        } else if (col == 1) {
            m01 = values.x;
            m11 = values.y;
            m21 = values.z;
            m31 = values.w;
        } else if (col == 2) {
            m02 = values.x;
            m12 = values.y;
            m22 = values.z;
            m32 = values.w;
        } else if (col == 3) {
            m03 = values.x;
            m13 = values.y;
            m23 = values.z;
            m33 = values.w;
        } else {
            throw new IndexOutOfBoundsException("Column is not 0, 1, 2, or 3: " + col);
        }
        return this;
    }

    /**
     * Set the given matrix row to the four values stored in the vector. The x
     * component is the value used for the 1st column, the y is the 2nd column,
     * the z is the 3rd column, and w is the 4th column
     * 
     * @param row Matrix row to use, in [0, 3]
     * @param values Vector source for the four row values
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws IndexOutOfBoundsException if row is invalid
     */
    public Matrix4 setRow(int row, @Const Vector4 values) {
        if (row == 0) {
            m00 = values.x;
            m01 = values.y;
            m02 = values.z;
            m03 = values.w;
        } else if (row == 1) {
            m10 = values.x;
            m11 = values.y;
            m12 = values.z;
            m13 = values.w;
        } else if (row == 2) {
            m20 = values.x;
            m21 = values.y;
            m22 = values.z;
            m23 = values.w;
        } else if (row == 3) {
            m30 = values.x;
            m31 = values.y;
            m32 = values.z;
            m33 = values.w;
        } else {
            throw new IndexOutOfBoundsException("Row is not 0, 1, 2, or 3: " + row);
        }
        return this;
    }

    /**
     * Set all sixteen values of this matrix from the given array values. If
     * rowMajor is true, each set of four floats is treated as a row. If it is
     * false, the values are column major, and each four represents a matrix
     * column. The values are taken from the values array starting at offset; it
     * is assumed that the array is long enough.
     * 
     * @param values Double source for the new matrix data
     * @param offset Start index of the float values
     * @param rowMajor True if values is row-major order
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 16 elements
     *             starting at offset
     */
    public Matrix4 set(double[] values, int offset, boolean rowMajor) {
        if (rowMajor)
            return set(values[offset], values[offset + 1], values[offset + 2], values[offset + 3], 
                       values[offset + 4], values[offset + 5], values[offset + 6], values[offset + 7], 
                       values[offset + 8], values[offset + 9], values[offset + 10], values[offset + 11], 
                       values[offset + 12], values[offset + 13], values[offset + 14], values[offset + 15]);
        else
            return set(values[offset], values[offset + 4], values[offset + 8], values[offset + 12], 
                       values[offset + 1], values[offset + 5], values[offset + 9], values[offset + 13], 
                       values[offset + 2], values[offset + 6], values[offset + 10], values[offset + 14], 
                       values[offset + 3], values[offset + 7], values[offset + 11], values[offset + 15]);
    }

    /**
     * As {@link #set(double[], int, boolean)} except a float[] is the source of
     * values.
     * 
     * @param values Float source for the new matrix data
     * @param offset Start index of the float values
     * @param rowMajor True if values are row-major
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 16 elements
     *             starting at offset
     */
    public Matrix4 set(float[] values, int offset, boolean rowMajor) {
        if (rowMajor)
            return set(values[offset], values[offset + 1], values[offset + 2], values[offset + 3], 
                       values[offset + 4], values[offset + 5], values[offset + 6], values[offset + 7], 
                       values[offset + 8], values[offset + 9], values[offset + 10], values[offset + 11], 
                       values[offset + 12], values[offset + 13], values[offset + 14], values[offset + 15]);
        else
            return set(values[offset], values[offset + 4], values[offset + 8], values[offset + 12], 
                       values[offset + 1], values[offset + 5], values[offset + 9], values[offset + 13], 
                       values[offset + 2], values[offset + 6], values[offset + 10], values[offset + 14], 
                       values[offset + 3], values[offset + 7], values[offset + 11], values[offset + 15]);
    }

    /**
     * As {@link #set(double[], int, boolean)} except a DoubleBuffer is the
     * source of values.
     * 
     * @param values Double source for the new matrix data
     * @param offset Start index of the float values
     * @param rowMajor True if values are row-major
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 16 elements
     *             starting at offset
     */
    public Matrix4 set(DoubleBuffer values, int offset, boolean rowMajor) {
        if (rowMajor)
            return set(values.get(offset), values.get(offset + 1), values.get(offset + 2), values.get(offset + 3), 
                       values.get(offset + 4), values.get(offset + 5), values.get(offset + 6), values.get(offset + 7), 
                       values.get(offset + 8), values.get(offset + 9), values.get(offset + 10), values.get(offset + 11), 
                       values.get(offset + 12), values.get(offset + 13), values.get(offset + 14), values.get(offset + 15));
        else
            return set(values.get(offset), values.get(offset + 4), values.get(offset + 8), values.get(offset + 12), 
                       values.get(offset + 1), values.get(offset + 5), values.get(offset + 9), values.get(offset + 13), 
                       values.get(offset + 2), values.get(offset + 6), values.get(offset + 10), values.get(offset + 14), 
                       values.get(offset + 3), values.get(offset + 7), values.get(offset + 11), values.get(offset + 15));

    }

    /**
     * As {@link #set(double[], int, boolean)} except a FloatBuffer is the
     * source of values.
     * 
     * @param values Float source for the new matrix data
     * @param offset Start index of the float values
     * @param rowMajor True if values are row-major
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 16 elements
     *             starting at offset
     */
    public Matrix4 set(FloatBuffer values, int offset, boolean rowMajor) {
        if (rowMajor)
            return set(values.get(offset), values.get(offset + 1), values.get(offset + 2), values.get(offset + 3), 
                       values.get(offset + 4), values.get(offset + 5), values.get(offset + 6), values.get(offset + 7), 
                       values.get(offset + 8), values.get(offset + 9), values.get(offset + 10), values.get(offset + 11), 
                       values.get(offset + 12), values.get(offset + 13), values.get(offset + 14), values.get(offset + 15));
        else
            return set(values.get(offset), values.get(offset + 4), values.get(offset + 8), values.get(offset + 12), 
                       values.get(offset + 1), values.get(offset + 5), values.get(offset + 9), values.get(offset + 13), 
                       values.get(offset + 2), values.get(offset + 6), values.get(offset + 10), values.get(offset + 14), 
                       values.get(offset + 3), values.get(offset + 7), values.get(offset + 11), values.get(offset + 15));

    }

    /**
     * Set the sixteen values of this matrix in bulk. This is equivalent to
     * assigning each parameter to the matrix's public field of identical name.
     * 
     * @param m00 New value for 1st row and 1st column
     * @param m01 New value for 1st row and 2nd column
     * @param m02 New value for 1st row and 3rd column
     * @param m03 New value for 1st row and 4th column
     * @param m10 New value for 2nd row and 1st column
     * @param m11 New value for 2nd row and 2nd column
     * @param m12 New value for 2nd row and 3rd column
     * @param m13 New value for 2nd row and 4th column
     * @param m20 New value for 3rd row and 1st column
     * @param m21 New value for 3rd row and 2nd column
     * @param m22 New value for 3rd row and 3rd column
     * @param m23 New value for 3rd row and 4th column
     * @param m30 New value for 4th row and 1st column
     * @param m31 New value for 4th row and 2nd column
     * @param m32 New value for 4th row and 3rd column
     * @param m33 New value for 4th row and 4th column
     * @return This matrix
     */
    public Matrix4 set(double m00, double m01, double m02, double m03, 
                        double m10, double m11, double m12, double m13, 
                        double m20, double m21, double m22, double m23, 
                        double m30, double m31, double m32, double m33) {
        this.m00 = m00; this.m01 = m01; this.m02 = m02; this.m03 = m03;
        this.m10 = m10; this.m11 = m11; this.m12 = m12; this.m13 = m13;
        this.m20 = m20; this.m21 = m21; this.m22 = m22; this.m23 = m23;
        this.m30 = m30; this.m31 = m31; this.m32 = m32; this.m33 = m33;
        return this;
    }

    /**
     * Set this matrix to be equal to the given matrix, o.
     * 
     * @param o Matrix whose values are copied into this matrix
     * @return This matrix
     * @throws NullPointerException if o is null
     */
    public Matrix4 set(@Const Matrix4 o) {
        m00 = o.m00; m01 = o.m01; m02 = o.m02; m03 = o.m03;
        m10 = o.m10; m11 = o.m11; m12 = o.m12; m13 = o.m13;
        m20 = o.m20; m21 = o.m21; m22 = o.m22; m23 = o.m23;
        m30 = o.m30; m31 = o.m31; m32 = o.m32; m33 = o.m33;
        return this;
    }

    /**
     * Reset this matrix's values so that it represents the identity matrix.
     * 
     * @return This matrix
     */
    public Matrix4 setIdentity() {
        return set(1, 0, 0, 0, 
                   0, 1, 0, 0, 
                   0, 0, 1, 0,
                   0, 0, 0, 1);
    }

    /**
     * Return the value of the matrix entry at the given row and column. row and
     * col must both be within [0, 3], where 0 represents the 1st row or column.
     * 
     * @param row Row to access, in [0, 3]
     * @param col Col to access, in [0, 3]
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
            } else if (col == 3) {
                return m03;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, 2, or 3: " + col);
            }
        } else if (row == 1) {
            if (col == 0) {
                return m10;
            } else if (col == 1) {
                return m11;
            } else if (col == 2) {
                return m12;
            } else if (col == 3) {
                return m13;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, 2, or 3: " + col);
            }
        } else if (row == 2) {
            if (col == 0) {
                return m20;
            } else if (col == 1) {
                return m21;
            } else if (col == 2) {
                return m22;
            } else if (col == 3) {
                return m23;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, 2, or 3: " + col);
            }
        } else if (row == 3) {
            if (col == 0) {
                return m30;
            } else if (col == 1) {
                return m31;
            } else if (col == 2) {
                return m32;
            } else if (col == 3) {
                return m33;
            } else {
                throw new IndexOutOfBoundsException("Column is not 0, 1, 2, or 3: " + col);
            }
        } else {
            throw new IndexOutOfBoundsException("Row is not 0, 1, 2, or 3: " + row);
        }
    }
    
    /**
     * <p>
     * Store the entire matrix into an array, starting at offset. If rowMajor is
     * true, the matrix should stored in row-major order (i.e. every 4 values
     * constitutes a row). If it is false, then the matrix is stored in
     * column-major order (every 4 values is a column of the matrix).
     * </p>
     * <p>
     * It is assumed that there is enough space in store, starting at offset to
     * hold the sixteen matrix values of this matrix.
     * </p>
     * 
     * @param store Array to hold the sixteen matrix values.
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
            store[offset + 3] = m03;
            store[offset + 4] = m10;
            store[offset + 5] = m11;
            store[offset + 6] = m12;
            store[offset + 7] = m13;
            store[offset + 8] = m20;
            store[offset + 9] = m21;
            store[offset + 10] = m22;
            store[offset + 11] = m23;
            store[offset + 12] = m30;
            store[offset + 13] = m31;
            store[offset + 14] = m32;
            store[offset + 15] = m33;
        } else {
            store[offset + 0] = m00;
            store[offset + 1] = m10;
            store[offset + 2] = m20;
            store[offset + 3] = m30;
            store[offset + 4] = m01;
            store[offset + 5] = m11;
            store[offset + 6] = m21;
            store[offset + 7] = m31;
            store[offset + 8] = m02;
            store[offset + 9] = m12;
            store[offset + 10] = m22;
            store[offset + 11] = m32;
            store[offset + 12] = m03;
            store[offset + 13] = m13;
            store[offset + 14] = m23;
            store[offset + 15] = m33;
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
            store[offset + 3] = (float) m03;
            store[offset + 4] = (float) m10;
            store[offset + 5] = (float) m11;
            store[offset + 6] = (float) m12;
            store[offset + 7] = (float) m13;
            store[offset + 8] = (float) m20;
            store[offset + 9] = (float) m21;
            store[offset + 10] = (float) m22;
            store[offset + 11] = (float) m23;
            store[offset + 12] = (float) m30;
            store[offset + 13] = (float) m31;
            store[offset + 14] = (float) m32;
            store[offset + 15] = (float) m33;
        } else {
            store[offset + 0] = (float) m00;
            store[offset + 1] = (float) m10;
            store[offset + 2] = (float) m20;
            store[offset + 3] = (float) m30;
            store[offset + 4] = (float) m01;
            store[offset + 5] = (float) m11;
            store[offset + 6] = (float) m21;
            store[offset + 7] = (float) m31;
            store[offset + 8] = (float) m02;
            store[offset + 9] = (float) m12;
            store[offset + 10] = (float) m22;
            store[offset + 11] = (float) m32;
            store[offset + 12] = (float) m03;
            store[offset + 13] = (float) m13;
            store[offset + 14] = (float) m23;
            store[offset + 15] = (float) m33;
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
            store.put(offset + 3, m03);
            store.put(offset + 4, m10);
            store.put(offset + 5, m11);
            store.put(offset + 6, m12);
            store.put(offset + 7, m13);
            store.put(offset + 8, m20);
            store.put(offset + 9, m21);
            store.put(offset + 10, m22);
            store.put(offset + 11, m23);
            store.put(offset + 12, m30);
            store.put(offset + 13, m31);
            store.put(offset + 14, m32);
            store.put(offset + 15, m33);
        } else {
            store.put(offset + 0, m00);
            store.put(offset + 1, m10);
            store.put(offset + 2, m20);
            store.put(offset + 3, m30);
            store.put(offset + 4, m01);
            store.put(offset + 5, m11);
            store.put(offset + 6, m21);
            store.put(offset + 7, m31);
            store.put(offset + 8, m02);
            store.put(offset + 9, m12);
            store.put(offset + 10, m22);
            store.put(offset + 11, m32);
            store.put(offset + 12, m03);
            store.put(offset + 13, m13);
            store.put(offset + 14, m23);
            store.put(offset + 15, m33);
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
            store.put(offset + 3, (float) m03);
            store.put(offset + 4, (float) m10);
            store.put(offset + 5, (float) m11);
            store.put(offset + 6, (float) m12);
            store.put(offset + 7, (float) m13);
            store.put(offset + 8, (float) m20);
            store.put(offset + 9, (float) m21);
            store.put(offset + 10, (float) m22);
            store.put(offset + 11, (float) m23);
            store.put(offset + 12, (float) m30);
            store.put(offset + 13, (float) m31);
            store.put(offset + 14, (float) m32);
            store.put(offset + 15, (float) m33);
        } else {
            store.put(offset + 0, (float) m00);
            store.put(offset + 1, (float) m10);
            store.put(offset + 2, (float) m20);
            store.put(offset + 3, (float) m30);
            store.put(offset + 4, (float) m01);
            store.put(offset + 5, (float) m11);
            store.put(offset + 6, (float) m21);
            store.put(offset + 7, (float) m31);
            store.put(offset + 8, (float) m02);
            store.put(offset + 9, (float) m12);
            store.put(offset + 10, (float) m22);
            store.put(offset + 11, (float) m32);
            store.put(offset + 12, (float) m03);
            store.put(offset + 13, (float) m13);
            store.put(offset + 14, (float) m23);
            store.put(offset + 15, (float) m33);
        }
    }

    /**
     * Return a new Matrix3 that contains the current upper 3x3 matrix of this
     * 4x4 matrix.
     * 
     * @return A 3x3 matrix representing the upper 3x3 values of this matrix
     */
    public Matrix3 getUpperMatrix() {
        return new Matrix3(m00, m01, m02,
                           m10, m11, m12,
                           m20, m21, m22);
    }
    
    /**
     * Return a new Vector4 that copies the values of the given column.
     * 
     * @param col The column to fetch
     * @return A new vector equaling the given column
     * @throws IndexOutOfBoundsException if col is not 0, 1, 2, or 3
     */
    public Vector4 getCol(int col) {
        if (col == 0) {
            return new Vector4(m00, m10, m20, m30);
        } else if (col == 1) {
            return new Vector4(m01, m11, m21, m31);
        } else if (col == 2) {
            return new Vector4(m02, m12, m22, m32);
        } else if (col == 3) {
            return new Vector4(m03, m13, m23, m33);
        } else {
            throw new IndexOutOfBoundsException("Column is not 0, 1, 2, or 3:" + col);
        }
    }

    /**
     * Return a new Vector4 that copies the values of the given row.
     * 
     * @param row The row to fetch
     * @return A new vector equaling the given row
     * @throws IndexOutOfBoundsException if row is not 0, 1, 2, or 3
     */
    public Vector4 getRow(int row) {
        if (row == 0) {
            return new Vector4(m00, m01, m02, m03);
        } else if (row == 1) {
            return new Vector4(m10, m11, m12, m13);
        } else if (row == 2) {
            return new Vector4(m20, m21, m22, m23);
        } else if (row == 3) {
            return new Vector4(m30, m31, m32, m33);
        } else {
            throw new IndexOutOfBoundsException("Row is not 0, 1, 2, or 3:" + row);
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
    public boolean epsilonEquals(@Const Matrix4 e, double eps) {
        if (e == null)
            return false;

        return Math.abs(m00 - e.m00) <= eps && Math.abs(m01 - e.m01) <= eps && 
               Math.abs(m02 - e.m02) <= eps && Math.abs(m03 - e.m03) <= eps && 
               Math.abs(m10 - e.m10) <= eps && Math.abs(m11 - e.m11) <= eps &&
               Math.abs(m12 - e.m12) <= eps && Math.abs(m13 - e.m13) <= eps && 
               Math.abs(m20 - e.m20) <= eps && Math.abs(m21 - e.m21) <= eps && 
               Math.abs(m22 - e.m22) <= eps && Math.abs(m23 - e.m23) <= eps && 
               Math.abs(m30 - e.m30) <= eps && Math.abs(m31 - e.m31) <= eps && 
               Math.abs(m32 - e.m32) <= eps && Math.abs(m33 - e.m33) <= eps;
    }

    @Override
    public boolean equals(Object o) {
        // if conditional handles null values
        if (!(o instanceof Matrix4))
            return false;
        Matrix4 e = (Matrix4) o;
        return m00 == e.m00 && m01 == e.m01 && m02 == e.m02 && m03 == e.m03 && 
               m10 == e.m10 && m11 == e.m11 && m12 == e.m12 && m13 == e.m13 && 
               m20 == e.m20 && m21 == e.m21 && m22 == e.m22 && m23 == e.m23 && 
               m30 == e.m30 && m31 == e.m31 && m32 == e.m32 && m33 == e.m33;
    }

    @Override
    public int hashCode() {
        long result = 17;
        result += result * 31 + Double.doubleToLongBits(m00);
        result += result * 31 + Double.doubleToLongBits(m01);
        result += result * 31 + Double.doubleToLongBits(m02);
        result += result * 31 + Double.doubleToLongBits(m03);

        result += result * 31 + Double.doubleToLongBits(m10);
        result += result * 31 + Double.doubleToLongBits(m11);
        result += result * 31 + Double.doubleToLongBits(m12);
        result += result * 31 + Double.doubleToLongBits(m13);

        result += result * 31 + Double.doubleToLongBits(m20);
        result += result * 31 + Double.doubleToLongBits(m21);
        result += result * 31 + Double.doubleToLongBits(m22);
        result += result * 31 + Double.doubleToLongBits(m23);

        result += result * 31 + Double.doubleToLongBits(m30);
        result += result * 31 + Double.doubleToLongBits(m31);
        result += result * 31 + Double.doubleToLongBits(m32);
        result += result * 31 + Double.doubleToLongBits(m33);

        return (int) (((result & 0xffffffff00000000L) >> 32) ^ (result & 0x00000000ffffffffL));
    }
    
    @Override
    public String toString() {
        return "[[ " + m00 + ", " + m01 + ", " + m02 + ", " + m03 + " ]\n" +
               " [ " + m10 + ", " + m11 + ", " + m12 + ", " + m13 + " ]\n" +
               " [ " + m20 + ", " + m21 + ", " + m22 + ", " + m23 + " ]\n" +
               " [ " + m30 + ", " + m31 + ", " + m32 + ", " + m33 + " ]]";
    }
}
