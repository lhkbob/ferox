package com.ferox.math;

/**
 * <p>
 * Matrix4f provides a final implementation of a 4x4 matrix. The sixteen
 * components of the matrix are available as public fields. There is no need for
 * further abstraction because a 4x4 matrix is just 16 values. The public fields
 * are labeled m[row][col], where the rows and columns go from 0 to 3.
 * </p>
 * <p>
 * In all mathematical functions that compute a new matrix, there is a method
 * parameter, often named result, that will hold the computed value. This result
 * matrix is also returned so that complex mathematical expressions can be
 * chained. It is always safe to use the calling matrix as the result matrix.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Matrix4f implements Cloneable {
    public float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23,
        m30, m31, m32, m33;

    /**
     * Construct a new Matrix4f that's set to the identity matrix.
     */
    public Matrix4f() {
        setIdentity();
    }

    /**
     * Construct a new Matrix4f that's copies the values in o.
     * 
     * @see #set(Matrix4f)
     * @param o Matrix to clone
     */
    public Matrix4f(Matrix4f o) {
        set(o);
    }

    /**
     * Construct a new Matrix4f which takes its sixteen values from the given
     * array, starting at offset. The values are either given row-major or
     * column-major, depending on the rowMajor value.
     * 
     * @see #set(float[], int, boolean)
     * @param values Value source
     * @param offset Start index of matrix component values
     * @param rowMajor Ordering of the matrix data
     */
    public Matrix4f(float[] values, int offset, boolean rowMajor) {
        set(values, offset, rowMajor);
    }

    /**
     * Construct a new Matrix4f that assigns each parameter value to the
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
    public Matrix4f(float m00, float m01, float m02, float m03, 
                    float m10, float m11, float m12, float m13,
                    float m20, float m21, float m22, float m23, 
                    float m30, float m31, float m32, float m33) {
        set(m00, m01, m02, m03, 
            m10, m11, m12, m13, 
            m20, m21, m22, m23,
            m30, m31, m32, m33);
    }

    /**
     * Add the values of the matrix r to this matrix, component by component and
     * store it in result. This effectively computes the mathematical operation
     * of [result] = [this] + [r].
     * 
     * @param r Matrix to add to this matrix
     * @param result Matrix to hold the addition result
     * @return result, or a new Matrix4f if null, holding the addition
     */
    public Matrix4f add(Matrix4f r, Matrix4f result) {
        if (result == null)
            result = new Matrix4f();

        return result.set(m00 + r.m00, m01 + r.m01, m02 + r.m02, m03 + r.m03, 
                          m10 + r.m10, m11 + r.m11, m12 + r.m12, m13 + r.m13, 
                          m20 + r.m20, m21 + r.m21, m22 + r.m22, m23 + r.m23, 
                          m30 + r.m30, m31 + r.m31, m32 + r.m32, m33 + r.m33);
    }

    /**
     * As add(r, result) where result is this matrix
     * 
     * @param r
     * @return This matrix
     */
    public Matrix4f add(Matrix4f r) {
        return add(r, this);
    }

    /**
     * Add the given constant to each of this matrix's values and store it in
     * result. If result is null, then a new Matrix4f should be created and
     * returned.
     * 
     * @param c Constant factor added to each of matrix value
     * @param result Matrix to hold the addition result
     * @return result, or a new Matrix4f if null, holding the addition
     */
    public Matrix4f add(float c, Matrix4f result) {
        if (result == null)
            result = new Matrix4f();

        return result.set(m00 + c, m01 + c, m02 + c, m03 + c, 
                          m10 + c, m11 + c, m12 + c, m13 + c, 
                          m20 + c, m21 + c, m22 + c, m23 + c, 
                          m30 + c, m31 + c, m32 + c, m33 + c);
    }

    /**
     * As add(c, result) where result is this matrix
     * 
     * @param c
     * @return This matrix
     */
    public Matrix4f add(float c) {
        return add(c, this);
    }

    /**
     * Compute and return the determinant for this matrix. If this value is 0,
     * then the matrix is not invertible. If it is very close to 0, then the
     * matrix may be ill-formed and inversions, multiplications and linear
     * solving could be inaccurate.
     * 
     * @return The matrix's determinant
     */
    public float determinant() {
        // Thanks to Ardor3D for the determinant expansion
        float ra0 = m00 * m11 - m01 * m10;
        float ra1 = m00 * m12 - m02 * m10;
        float ra2 = m00 * m13 - m03 * m10;
        float ra3 = m01 * m12 - m02 * m11;
        float ra4 = m01 * m13 - m03 * m11;
        float ra5 = m02 * m13 - m03 * m12;
        float rb0 = m20 * m31 - m21 * m30;
        float rb1 = m20 * m32 - m22 * m30;
        float rb2 = m20 * m33 - m23 * m30;
        float rb3 = m21 * m32 - m22 * m31;
        float rb4 = m21 * m33 - m23 * m31;
        float rb5 = m22 * m33 - m23 * m32;

        return ra0 * rb5 - ra1 * rb4 + ra2 * rb3 + ra3 * rb2 - ra4 * rb1 + ra5 * rb0;
    }

    /**
     * Store the inverse of this matrix into result and return result. If result
     * is null, then a new Matrix4f should be created and returned.
     * 
     * @param result Matrix to hold the inverse
     * @return result, or a new Matrix4f if null, holding the inverse
     * @throws ArithmeticException if this matrix isn't invertible
     */
    public Matrix4f inverse(Matrix4f result) {
        // Also, thanks to Ardor3D for the inverse code
        if (result == null)
            result = new Matrix4f();

        // inlined det computation to get at intermediate results
        float ra0 = m00 * m11 - m01 * m10;
        float ra1 = m00 * m12 - m02 * m10;
        float ra2 = m00 * m13 - m03 * m10;
        float ra3 = m01 * m12 - m02 * m11;
        float ra4 = m01 * m13 - m03 * m11;
        float ra5 = m02 * m13 - m03 * m12;
        float rb0 = m20 * m31 - m21 * m30;
        float rb1 = m20 * m32 - m22 * m30;
        float rb2 = m20 * m33 - m23 * m30;
        float rb3 = m21 * m32 - m22 * m31;
        float rb4 = m21 * m33 - m23 * m31;
        float rb5 = m22 * m33 - m23 * m32;

        float invDet = ra0 * rb5 - ra1 * rb4 + ra2 * rb3 + ra3 * rb2 - ra4 * rb1 + ra5 * rb0;
        if (Math.abs(invDet) <= .0001f)
            throw new ArithmeticException("Singular matrix");
        invDet = 1f / invDet;

        float t00 = m11 * rb5 - m12 * rb4 + m13 * rb3;
        float t10 = m12 * rb2 - m13 * rb1 - m10 * rb5;
        float t20 = m10 * rb4 - m11 * rb2 + m13 * rb0;
        float t30 = m11 * rb1 - m12 * rb0 - m10 * rb3;
        float t01 = m02 * rb4 - m03 * rb3 - m01 * rb5;
        float t11 = m00 * rb5 - m02 * rb2 + m03 * rb1;
        float t21 = m01 * rb2 - m03 * rb0 - m00 * rb4;
        float t31 = m00 * rb3 - m01 * rb1 + m02 * rb0;
        float t02 = m31 * ra5 - m32 * ra4 + m33 * ra3;
        float t12 = m32 * ra2 - m33 * ra1 - m30 * ra5;
        float t22 = m30 * ra4 - m31 * ra2 + m33 * ra0;
        float t32 = m31 * ra1 - m32 * ra0 - m30 * ra3;
        float t03 = m22 * ra4 - m23 * ra3 - m21 * ra5;
        float t13 = m20 * ra5 - m22 * ra2 + m23 * ra1;
        float t23 = m21 * ra2 - m23 * ra0 - m20 * ra4;
        float t33 = m20 * ra3 - m21 * ra1 + m22 * ra0;

        return result.set(invDet * t00, invDet * t01, invDet * t02, invDet * t03, 
                          invDet * t10, invDet * t11, invDet * t12, invDet * t13, 
                          invDet * t20, invDet * t21, invDet * t22, invDet * t23, 
                          invDet * t30, invDet * t31, invDet * t32, invDet * t33);
    }

    /**
     * Invert this matrix in place
     * 
     * @return This matrix
     */
    public Matrix4f inverse() {
        return inverse(this);
    }

    /**
     * Compute and return the length of this matrix. The length of a matrix is
     * defined as the square root of the sum of the squared matrix values.
     * 
     * @return The matrix's length
     */
    public float length() {
        float row1 = m00 * m00 + m01 * m01 + m02 * m02 + m03 * m03;
        float row2 = m10 * m10 + m11 * m11 + m12 * m12 + m13 * m13;
        float row3 = m20 * m20 + m21 * m21 + m22 * m22 + m23 * m23;
        float row4 = m30 * m30 + m31 * m31 + m32 * m32 + m33 * m33;

        return (float) Math.sqrt(row1 + row2 + row3 + row4);
    }

    /**
     * Multiply this matrix by r, or compute [this] x [r] and store it in
     * result. If result is null, then a new Matrix4f should be created and
     * returned instead.
     * 
     * @param r Matrix used in the right-hand operation of the multiplication
     * @param result Matrix storing the result
     * @return result, or a new Matrix4f if null, holding the multiplication
     */
    public Matrix4f mul(Matrix4f r, Matrix4f result) {
        if (result == null)
            result = new Matrix4f();
        return result.set(m00 * r.m00 + m01 * r.m10 + m02 * r.m20 + m03 * r.m30,
                          m00 * r.m01 + m01 * r.m11 + m02 * r.m21 + m03 * r.m31,
                          m00 * r.m02 + m01 * r.m12 + m02 * r.m22 + m03 * r.m32, 
                          m00 * r.m03 + m01 * r.m13 + m02 * r.m23 + m03 * r.m33, 
                          m10 * r.m00 + m11 * r.m10 + m12 * r.m20 + m13 * r.m30, 
                          m10 * r.m01 + m11 * r.m11 + m12 * r.m21 + m13 * r.m31, 
                          m10 * r.m02 + m11 * r.m12 + m12 * r.m22 + m13 * r.m32, 
                          m10 * r.m03 + m11 * r.m13 + m12 * r.m23 + m13 * r.m33, 
                          m20 * r.m00 + m21 * r.m10 + m22 * r.m20 + m23 * r.m30, 
                          m20 * r.m01 + m21 * r.m11 + m22 * r.m21 + m23 * r.m31, 
                          m20 * r.m02 + m21 * r.m12 + m22 * r.m22 + m23 * r.m32, 
                          m20 * r.m03 + m21 * r.m13 + m22 * r.m23 + m23 * r.m33, 
                          m30 * r.m00 + m31 * r.m10 + m32 * r.m20 + m33 * r.m30, 
                          m30 * r.m01 + m31 * r.m11 + m32 * r.m21 + m33 * r.m31, 
                          m30 * r.m02 + m31 * r.m12 + m32 * r.m22 + m33 * r.m32, 
                          m30 * r.m03 + m31 * r.m13 + m32 * r.m23 + m33 * r.m33);
    }

    /**
     * As mul(r, result) where result is this matrix
     * 
     * @param r
     * @return This matrix
     */
    public Matrix4f mul(Matrix4f r) {
        return mul(r, this);
    }

    /**
     * Multiply this matrix by the 4x1 matrix represented by the four values in
     * r, or compute [this] x [r]. The result of this operation is another 4x1
     * matrix, which can then be interpreted as the transformed vector of r. The
     * computation is stored in result; if result is null, then create a new
     * Vector4f to hold it and return.
     * 
     * @param r Vector to be interpreted as a 4x1 matrix in the multiplication
     * @param result Vector holding the resultant transformed vector
     * @return result, or a new Vector4f if null, holding [this] x [r]
     */
    public Vector4f mul(Vector4f r, Vector4f result) {
        return mul(r.x, r.y, r.z, r.w, result);
    }

    /**
     * As mul(r, result) where result is the given vector.
     * 
     * @param r
     * @return r
     */
    public Vector4f mul(Vector4f r) {
        return mul(r, r);
    }

    /**
     * Multiply this matrix by the 4x1 matrix [[x][y][z][w]] or compute [this] x
     * [[x][y][z][w]]. The result of this operation is another 4x1 matrix, which
     * can then be interpreted as the transformed vector of (x,y,z,w). The
     * computation is stored in result; if result is null, then create a new
     * Vector4f to hold it and return.
     * 
     * @param x 1st row value of the 4x1 matrix
     * @param y 2nd row value of the 4x1 matrix
     * @param z 3rd row value of the 4x1 matrix
     * @param w 4th row value of the 4x1 matrix
     * @param result Vector holding the resultant transformed vector
     * @return result, or a new Vector4f if null, holding [this] x
     *         [[x][y][z][w]]
     */
    public Vector4f mul(float x, float y, float z, float w, Vector4f result) {
        if (result == null)
            result = new Vector4f();
        return result.set(m00 * x + m01 * y + m02 * z + m03 * w, 
                          m10 * x + m11 * y + m12 * z + m13 * w,
                          m20 * x + m21 * y + m22 * z + m23 * w,
                          m30 * x + m31 * y + m32 * z + m33 * w);
    }

    /**
     * Multiply this matrix by the diagonal matrix that takes it's three
     * diagonal entries from diag, or compute [this] x [m], where [m] is all 0s
     * except m00 = diag.x, m11 = diag.y, m22 = diag.z, and m33 = diag.w. The
     * multiplication is stored in result. If result is null, create and return
     * a new Matrix4f.
     * 
     * @param diag Vector holding the three diagonal entries of the other matrix
     * @param result Matrix holding the multiplication result
     * @return result, or a new Matrix4f if null, holding the multiplication
     */
    public Matrix4f mulDiagonal(Vector4f diag, Matrix4f result) {
        return mulDiagonal(diag.x, diag.y, diag.z, diag.w, result);
    }

    /**
     * As mulDiagonal(diag, result) where result is this matrix
     * 
     * @param diag
     * @return This matrix
     */
    public Matrix4f mulDiagonal(Vector4f diag) {
        return mulDiagonal(diag, this);
    }

    /**
     * Multiply this matrix by the diagonal matrix that takes it's four diagonal
     * entries from (x, y, z, w), or compute [this] x [m], where [m] is all 0s
     * except m00 = x, m11 = y, m22 = z, and m33 = w. The multiplication is
     * stored in result. If result is null, create and return a new Matrix4f.
     * 
     * @param x The m00 value used for the diagonal matrix
     * @param y The m11 value used for the diagonal matrix
     * @param z The m22 value used for the diagonal matrix
     * @param w The m33 value used for the diagonal matrix
     * @param result Matrix holding the multiplication result
     * @return result, or a new Matrix4f if null, holding the multiplication
     */
    public Matrix4f mulDiagonal(float x, float y, float z, float w, Matrix4f result) {
        if (result == null)
            result = new Matrix4f();
        return result.set(m00 * x, m01 * y, m02 * z, m03 * w, 
                          m10 * x, m11 * y, m12 * z, m13 * w, 
                          m20 * x, m21 * y, m22 * z, m23 * w, 
                          m30 * x, m31 * y, m32 * z, m33 * w);
    }

    /**
     * Multiply the 1x4 matrix represented by the transpose of r by this matrix,
     * or compute [r]^T x [this]. The result of this operation is another 1x4
     * matrix, which can then be re-transposed and stored in result; if result
     * is null, then create a new Vector4f to hold it and return.
     * 
     * @param r Vector to be interpreted as a 1x4 matrix in the multiplication
     * @param result Vector holding the resultant computation
     * @return result, or a new Vector4f if null, holding [r]^T x [this]
     */
    public Vector4f mulPre(Vector4f r, Vector4f result) {
        return mulPre(r.x, r.y, r.z, r.w, result);
    }

    /**
     * As mul(r, r) where result is the given vector.
     * 
     * @param r
     * @return r
     */
    public Vector4f mulPre(Vector4f r) {
        return mulPre(r, r);
    }

    /**
     * Multiply the 1x4 matrix represented by [x, y, z, w] by this matrix, or
     * compute [x, y, z, w] x [this]. The result of this operation is another
     * 1x4 matrix, which can then be re-transposed and stored in result; if
     * result is null, then create a new Vector4f to hold it and return.
     * 
     * @param x The 1st column's value in the 1x3 matrix
     * @param y The 2nd column's value in the 1x3 matrix
     * @param z The 3rd column's value in the 1x3 matrix
     * @param w The 4th column's value in the 1x4 matrix
     * @param result Vector holding the resultant computation
     * @return result, or a new Vector4f if null, holding [x, y, z, w] x [this]
     */
    public Vector4f mulPre(float x, float y, float z, float w, Vector4f result) {
        if (result == null)
            result = new Vector4f();
        return result.set(m00 * x + m10 * y + m20 * z + m30 * w, 
                          m01 * x + m11 * y + m21 * z + m31 * w,
                          m02 * x + m12 * y + m22 * z + m32 * w,
                          m03 * x + m13 * y + m23 * z + m33 * w);
    }

    /**
     * <p>
     * Multiply the transpose of this matrix by the transpose of r, or compute
     * [this]^T x [r]^T and store it in result. If result is null, then a new
     * Matrix4f should be created and returned instead.
     * </p>
     * <p>
     * Note that [this]^T x [r]^T = ([r] x [this])^T
     * </p>
     * 
     * @param r Matrix whose transpose is used in right-hand operation of the
     *            multiplication
     * @param result Matrix storing the result
     * @return result, or a new Matrix4f if null, holding the multiplication
     */
    public Matrix4f mulTransposeBoth(Matrix4f r, Matrix4f result) {
        result = r.mul(this, result);
        return result.transpose(result);
    }

    /**
     * As mulTransposeBoth(r, result) where result is this matrix
     * 
     * @param r
     * @return This matrix
     */
    public Matrix4f mulTransposeBoth(Matrix4f r) {
        return mulTransposeBoth(r, this);
    }

    /**
     * Multiply the transpose of this matrix by r, or compute [this]^T x [r] and
     * store it in result. If result is null, then a new Matrix4f should be
     * created and returned instead.
     * 
     * @param r Matrix used in right-hand operation of the multiplication
     * @param result Matrix storing the result
     * @return result, or a new Matrix4f if null, holding the multiplication
     */
    public Matrix4f mulTransposeLeft(Matrix4f r, Matrix4f result) {
        if (result == null)
            result = new Matrix4f();
        return result.set(m00 * r.m00 + m10 * r.m10 + m20 * r.m20 + m30 * r.m30, 
                          m00 * r.m01 + m10 * r.m11 + m20 * r.m21 + m30 * r.m31,
                          m00 * r.m02 + m10 * r.m12 + m20 * r.m22 + m30 * r.m32,
                          m00 * r.m03 + m10 * r.m13 + m20 * r.m23 + m30 * r.m33,
                          m01 * r.m00 + m11 * r.m10 + m21 * r.m20 + m31 * r.m30,
                          m01 * r.m01 + m11 * r.m11 + m21 * r.m21 + m31 * r.m31,
                          m01 * r.m02 + m11 * r.m12 + m21 * r.m22 + m31 * r.m32, 
                          m01 * r.m03 + m11 * r.m13 + m21 * r.m23 + m31 * r.m33, 
                          m02 * r.m00 + m12 * r.m10 + m22 * r.m20 + m32 * r.m30, 
                          m02 * r.m01 + m12 * r.m11 + m22 * r.m21 + m32 * r.m31,
                          m02 * r.m02 + m12 * r.m12 + m22 * r.m22 + m32 * r.m32,
                          m02 * r.m03 + m12 * r.m13 + m22 * r.m23 + m32 * r.m33,
                          m03 * r.m00 + m13 * r.m10 + m23 * r.m20 + m33 * r.m30,
                          m03 * r.m01 + m13 * r.m11 + m23 * r.m21 + m33 * r.m31, 
                          m03 * r.m02 + m13 * r.m12 + m23 * r.m22 + m33 * r.m32, 
                          m03 * r.m03 + m13 * r.m13 + m23 * r.m23 + m33 * r.m33);
    }

    /**
     * As mulTransposeLeft(r, result) where result is this matrix
     * 
     * @param r
     * @return This matrix
     */
    public Matrix4f mulTransposeLeft(Matrix4f r) {
        return mulTransposeLeft(r, this);
    }

    /**
     * Multiply this matrix by the transpose of r, or compute [this] x [r]^T and
     * store it in result. If result is null, then a new Matrix4f should be
     * created and returned instead.
     * 
     * @param r Matrix whose transpose is used in right-hand operation of the
     *            multiplication
     * @param result Matrix storing the result
     * @return result, or a new Matrix4f if null, holding the multiplication
     */
    public Matrix4f mulTransposeRight(Matrix4f r, Matrix4f result) {
        if (result == null)
            result = new Matrix4f();
        return result.set(m00 * r.m00 + m01 * r.m01 + m02 * r.m02 + m03 * r.m03, 
                          m00 * r.m10 + m01 * r.m11 + m02 * r.m12 + m03 * r.m13, 
                          m00 * r.m20 + m01 * r.m21 + m02 * r.m22 + m03 * r.m23,
                          m00 * r.m30 + m01 * r.m31 + m03 * r.m32 + m03 * r.m33, 
                          m10 * r.m00 + m11 * r.m01 + m12 * r.m02 + m13 * r.m03, 
                          m10 * r.m10 + m11 * r.m11 + m12 * r.m12 + m13 * r.m13, 
                          m10 * r.m20 + m11 * r.m21 + m12 * r.m22 + m13 * r.m23, 
                          m10 * r.m30 + m11 * r.m31 + m12 * r.m32 + m13 * r.m33, 
                          m20 * r.m00 + m21 * r.m01 + m22 * r.m02 + m23 * r.m03, 
                          m20 * r.m10 + m21 * r.m11 + m22 * r.m12 + m23 * r.m13,
                          m20 * r.m20 + m21 * r.m21 + m22 * r.m22 + m23 * r.m23,
                          m20 * r.m30 + m21 * r.m31 + m22 * r.m32 + m23 * r.m33,
                          m30 * r.m00 + m31 * r.m01 + m32 * r.m02 + m33 * r.m03,
                          m30 * r.m10 + m31 * r.m11 + m32 * r.m12 + m33 * r.m13,
                          m30 * r.m20 + m31 * r.m21 + m32 * r.m22 + m33 * r.m23, 
                          m30 * r.m30 + m31 * r.m31 + m32 * r.m32 + m33 * r.m33);
    }

    /**
     * As mulTransposeRight(r, result) where result is this matrix
     * 
     * @param r
     * @return This matrix
     */
    public Matrix4f mulTransposeRight(Matrix4f r) {
        return mulTransposeRight(r, this);
    }

    /**
     * Scale each of this matrix's values by the scalar and store it in result.
     * This effectively computes [result] = scalar*[this].
     * 
     * @param scalar Scale factor applied to each matrix value
     * @param result Matrix to hold the scaled version of this matrix
     * @return result, or a new Matrix4f if null, holding the addition
     */
    public Matrix4f scale(float scalar, Matrix4f result) {
        if (result == null)
            result = new Matrix4f();

        return result.set(scalar * m00, scalar * m01, scalar * m02, scalar * m03, 
                          scalar * m10, scalar * m11, scalar * m12, scalar * m13, 
                          scalar * m20, scalar * m21, scalar * m22, scalar * m23, 
                          scalar * m30, scalar * m31, scalar * m32, scalar * m33);
    }

    /**
     * As scale(scalar, result) where result is this matrix
     * 
     * @param scalar
     * @return This matrix
     */
    public Matrix4f scale(float scalar) {
        return scale(scalar, this);
    }

    /**
     * Solve the linear system of equations and store the resultant values of
     * (x, y, z, w) into result:
     * 
     * <pre>
     * m00*x + m01*y + m02*z + m03*w = ans.x
     * m10*x + m11*y + m12*z + m13*w = ans.y
     * m20*x + m21*y + m22*z + m23*w = ans.z
     * m30*x + m31*y + m32*z + m33*w = ans.w
     * </pre>
     * 
     * If result is null, then a new Vector4f should be created and returned.
     * 
     * @param ans Vector holding the constraint values for the linear system of
     *            equations represented by this matrix
     * @param result Vector to hold the solution to the linear system of
     *            equations
     * @return result, or a new Vector4f if null, holding the solutions
     * @throws ArithmeticException if no solution or an infinite solutions exist
     */
    public Vector4f solve(Vector4f ans, Vector4f result) {
        return solve(ans.x, ans.y, ans.z, ans.w, result);
    }

    /**
     * Solve the linear system of equations and store the resultant values of
     * (x, y, z, w) into result:
     * 
     * <pre>
     * m00*x + m01*y + m02*z + m03*w = ax
     * m10*x + m11*y + m12*z + m13*w = ay
     * m20*x + m21*y + m22*z + m23*w = az
     * m30*x + m31*y + m32*z + m33*w = aw
     * </pre>
     * 
     * If result is null, then a new Vector4f should be created and returned.
     * 
     * @param ax Constraint value for the 1st function
     * @param ay Constraint value for the 2nd function
     * @param az Constraint value for the 3rd function
     * @param aw Constraint value for the 4th function
     * @param result Vector to hold the solution to the linear system of
     *            equations
     * @return result, or a new Vector4f if null, holding the solutions
     * @throws ArithmeticException if no solution or an infinite solutions exist
     */
    public Vector4f solve(float ax, float ay, float az, float aw, Vector4f result) {
        // the system is b = [A]x and we're solving for x
        // which becomes [A]^-1 b = x
        Matrix4f inv = inverse.get();
        this.inverse(inv);
        return inv.mul(ax, ay, az, aw, result);
    }

    private static ThreadLocal<Matrix4f> inverse = new ThreadLocal<Matrix4f>() {
        @Override
        public Matrix4f initialValue() {
            return new Matrix4f();
        }
    };

    /**
     * Compute the trace of this matrix. The trace is defined as the sum of the
     * diagonal entries of the matrix.
     * 
     * @return The matrix's trace
     */
    public float trace() {
        return m00 + m11 + m22 + m33;
    }

    /**
     * Store the transpose of this matrix into result and return result. If
     * result is null, then a new Matrix4f should be created and returned.
     * 
     * @param result Matrix to hold the transpose
     * @return result, or a new Matrix4f if null, holding the transpose
     */
    public Matrix4f transpose(Matrix4f result) {
        if (result == null)
            result = new Matrix4f();
        return result.set(m00, m10, m20, m30,
                          m01, m11, m21, m31, 
                          m02, m12, m22, m32, 
                          m03, m13, m23, m33);
    }

    /**
     * Transpose this matrix in place.
     * 
     * @return This matrix
     */
    public Matrix4f transpose() {
        return transpose(this);
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
    public float get(int row, int col) {
        if (row == 0) {
            if (col == 0)
                return m00;
            else if (col == 1)
                return m01;
            else if (col == 2)
                return m02;
            else if (col == 3)
                return m03;
        } else if (row == 1) {
            if (col == 0)
                return m10;
            else if (col == 1)
                return m11;
            else if (col == 2)
                return m12;
            else if (col == 3)
                return m13;
        } else if (row == 2) {
            if (col == 0)
                return m20;
            else if (col == 1)
                return m21;
            else if (col == 2)
                return m22;
            else if (col == 3)
                return m23;
        } else if (row == 3)
            if (col == 0)
                return m30;
            else if (col == 1)
                return m31;
            else if (col == 2)
                return m32;
            else if (col == 3)
                return m33;

        throw new IndexOutOfBoundsException("Illegal row or column: " + row + ", " + col);
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
    public void get(float[] store, int offset, boolean rowMajor) {
        if (rowMajor) {
            getRow(0, store, offset);
            getRow(1, store, offset + 4);
            getRow(2, store, offset + 8);
            getRow(3, store, offset + 12);
        } else {
            getCol(0, store, offset);
            getCol(1, store, offset + 4);
            getCol(2, store, offset + 8);
            getCol(3, store, offset + 12);
        }
    }

    /**
     * Store the four column values for the given column index into the vector
     * store. If store is null, a new Vector4f should be created and returned.
     * 
     * @param col Column to access, in [0, 3]
     * @param store Vector to hold the retrieved row
     * @return store, or a new Vector4f if null, holding the matrix column
     * @throws IndexOutOfBoundsException if col is invalid
     */
    public Vector4f getCol(int col, Vector4f store) {
        if (store == null)
            store = new Vector4f();

        switch (col) {
        case 0:
            return store.set(m00, m10, m20, m30);
        case 1:
            return store.set(m01, m11, m21, m31);
        case 2:
            return store.set(m02, m12, m22, m32);
        case 3:
            return store.set(m03, m13, m23, m33);
        default:
            throw new IndexOutOfBoundsException("Invalid column value: " + col);
        }
    }

    /**
     * Store the four column values for the given column index into the float
     * array. The four values will be stored consecutively, starting at the
     * offset index. It is assumed that store has at least four elements
     * remaining, starting at offset.
     * 
     * @param col The col to retrieve, in [0, 3]
     * @param store The array to hold the column values
     * @param offset First index to use in store
     * @throws NullPointerException if store is null
     * @throws IndexOutOfBoundsException if col is invalid
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the column
     */
    public void getCol(int col, float[] store, int offset) {
        switch (col) {
        case 0:
            store[offset] = m00;
            store[offset + 1] = m10;
            store[offset + 2] = m20;
            store[offset + 3] = m30;
            break;
        case 1:
            store[offset] = m01;
            store[offset + 1] = m11;
            store[offset + 2] = m21;
            store[offset + 3] = m31;
            break;
        case 2:
            store[offset] = m02;
            store[offset + 1] = m12;
            store[offset + 2] = m22;
            store[offset + 3] = m32;
            break;
        case 3:
            store[offset] = m03;
            store[offset + 1] = m13;
            store[offset + 2] = m23;
            store[offset + 3] = m33;
            break;
        default:
            throw new IndexOutOfBoundsException("Invalid column value: " + col);
        }
    }

    /**
     * Store the four row values for the given row index into the vector store.
     * If store is null, a new Vector4f should be created and returned.
     * 
     * @param row Row to access, in [0, 3]
     * @param store Vector to hold the retrieved row
     * @return store, or a new Vector4f if null, holding the matrix row
     * @throws IndexOutOfBoundsException if row is invalid
     */
    public Vector4f getRow(int row, Vector4f store) {
        if (store == null)
            store = new Vector4f();

        switch (row) {
        case 0:
            return store.set(m00, m01, m02, m03);
        case 1:
            return store.set(m10, m11, m12, m13);
        case 2:
            return store.set(m20, m21, m22, m23);
        case 3:
            return store.set(m30, m31, m32, m33);
        default:
            throw new IndexOutOfBoundsException("Invalid row value: " + row);
        }
    }

    /**
     * Store the four row values for the given row index into the float array.
     * The four values will be stored consecutively, starting at the offset
     * index. It is assumed that store has at least four elements remaining,
     * starting at offset.
     * 
     * @param row The row to retrieve, in [0, 3]
     * @param store The array to hold the row values
     * @param offset First index to use in store
     * @throws NullPointerException if store is null
     * @throws IndexOutOfBoundsException if row is invalid
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the row
     */
    public void getRow(int row, float[] store, int offset) {
        switch (row) {
        case 0:
            store[offset] = m00;
            store[offset + 1] = m01;
            store[offset + 2] = m02;
            store[offset + 3] = m03;
            break;
        case 1:
            store[offset] = m10;
            store[offset + 1] = m11;
            store[offset + 2] = m12;
            store[offset + 3] = m13;
            break;
        case 2:
            store[offset] = m20;
            store[offset + 1] = m21;
            store[offset + 2] = m22;
            store[offset + 3] = m23;
            break;
        case 3:
            store[offset] = m30;
            store[offset + 1] = m31;
            store[offset + 2] = m32;
            store[offset + 3] = m33;
            break;
        default:
            throw new IndexOutOfBoundsException("Invalid row value: " + row);
        }
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
    public Matrix4f set(int row, int col, float value) {
        if (row == 0) {
            if (col == 0) {
                m00 = value;
                return this;
            } else if (col == 1) {
                m01 = value;
                return this;
            } else if (col == 2) {
                m02 = value;
                return this;
            } else if (col == 3) {
                m03 = value;
                return this;
            }
        } else if (row == 1) {
            if (col == 0) {
                m10 = value;
                return this;
            } else if (col == 1) {
                m11 = value;
                return this;
            } else if (col == 2) {
                m12 = value;
                return this;
            } else if (col == 3) {
                m13 = value;
                return this;
            }
        } else if (row == 2) {
            if (col == 0) {
                m20 = value;
                return this;
            } else if (col == 1) {
                m21 = value;
                return this;
            } else if (col == 2) {
                m22 = value;
                return this;
            } else if (col == 3) {
                m23 = value;
                return this;
            }
        } else if (row == 3)
            if (col == 0) {
                m30 = value;
                return this;
            } else if (col == 1) {
                m31 = value;
                return this;
            } else if (col == 2) {
                m32 = value;
                return this;
            } else if (col == 3) {
                m33 = value;
                return this;
            }

        throw new IndexOutOfBoundsException("Invalid row or column: " + row + ", " + col);
    }

    /**
     * Set the given matrix column to the values in the values array, starting
     * at offset. From offset, four floats are taken; it is assumed that values
     * has enough space for four floats.
     * 
     * @param col The column to update, in [0, 3]
     * @param values The source for the new column values
     * @param offset The offset to get the 1st column value from
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws ArrayIndexOutOfBoundsException if values doesn't long enough
     * @throws IndexOutOfBoundsException if col is invalid
     */
    public Matrix4f setCol(int col, float[] values, int offset) {
        switch (col) {
        case 0:
            m00 = values[offset];
            m10 = values[offset + 1];
            m20 = values[offset + 2];
            m30 = values[offset + 3];
            return this;
        case 1:
            m01 = values[offset];
            m11 = values[offset + 1];
            m21 = values[offset + 2];
            m31 = values[offset + 3];
            return this;
        case 2:
            m02 = values[offset];
            m12 = values[offset + 1];
            m22 = values[offset + 2];
            m32 = values[offset + 3];
            return this;
        case 3:
            m03 = values[offset];
            m13 = values[offset + 1];
            m23 = values[offset + 2];
            m33 = values[offset + 3];
            return this;
        default:
            throw new IndexOutOfBoundsException("Invalid column value: " + col);
        }
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
    public Matrix4f setCol(int col, Vector4f values) {
        switch (col) {
        case 0:
            m00 = values.x;
            m10 = values.y;
            m20 = values.z;
            m30 = values.w;
            return this;
        case 1:
            m01 = values.x;
            m11 = values.y;
            m21 = values.z;
            m31 = values.w;
            return this;
        case 2:
            m02 = values.x;
            m12 = values.y;
            m22 = values.z;
            m32 = values.w;
            return this;
        case 3:
            m03 = values.x;
            m13 = values.y;
            m23 = values.z;
            m33 = values.w;
            return this;
        default:
            throw new IndexOutOfBoundsException("Invalid column value: " + col);
        }
    }

    /**
     * Set the given matrix row to the values in the values array, starting at
     * offset. From offset, four floats are taken; it is assumed that values has
     * enough space for four floats.
     * 
     * @param row The row to update, in [0, 3]
     * @param values The source for the new row values
     * @param offset The offset to get the 1st row value from
     * @return This matrix
     * @throws NullPointerException if values is null
     * @throws ArrayIndexOutOfBoundsException if values doesn't long enough
     * @throws IndexOutOfBoundsException if row is invalid
     */
    public Matrix4f setRow(int row, float[] values, int offset) {
        switch (row) {
        case 0:
            m00 = values[offset];
            m01 = values[offset + 1];
            m02 = values[offset + 2];
            m03 = values[offset + 3];
            return this;
        case 1:
            m10 = values[offset];
            m11 = values[offset + 1];
            m12 = values[offset + 2];
            m13 = values[offset + 3];
            return this;
        case 2:
            m20 = values[offset];
            m21 = values[offset + 1];
            m22 = values[offset + 2];
            m23 = values[offset + 3];
            return this;
        case 3:
            m30 = values[offset];
            m31 = values[offset + 1];
            m32 = values[offset + 2];
            m33 = values[offset + 3];
            return this;
        default:
            throw new IndexOutOfBoundsException("Invalid row value: " + row);
        }
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
    public Matrix4f setRow(int row, Vector4f values) {
        switch (row) {
        case 0:
            m00 = values.x;
            m01 = values.y;
            m02 = values.z;
            m03 = values.w;
            return this;
        case 1:
            m10 = values.x;
            m11 = values.y;
            m12 = values.z;
            m13 = values.w;
            return this;
        case 2:
            m20 = values.x;
            m21 = values.y;
            m22 = values.z;
            m23 = values.w;
            return this;
        case 3:
            m30 = values.x;
            m31 = values.y;
            m32 = values.z;
            m33 = values.w;
            return this;
        default:
            throw new IndexOutOfBoundsException("Invalid row value: " + row);
        }
    }

    /**
     * Set all sixteen values of this matrix from the given array values. If
     * rowMajor is true, each set of four floats is treated as a row. If it is
     * false, the values are column major, and each four represents a matrix
     * column. The values are taken from the values array starting at offset; it
     * is assumed that the array is long enough.
     * 
     * @param values Float source for the new matrix data
     * @param offset Start index of the float values
     * @param rowMajor True if values is row-major order
     * @return This matrix
     * @throws NullPointerException if values is null
     */
    public Matrix4f set(float[] values, int offset, boolean rowMajor) {
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
    public Matrix4f set(float m00, float m01, float m02, float m03, 
                        float m10, float m11, float m12, float m13, 
                        float m20, float m21, float m22, float m23, 
                        float m30, float m31, float m32, float m33) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;

        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;

        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;

        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;

        return this;
    }

    /**
     * Set this matrix to be equal to the given matrix, o.
     * 
     * @param o Matrix whose values are copied into this matrix
     * @return This matrix
     * @throws NullPointerException if o is null
     */
    public Matrix4f set(Matrix4f o) {
        return set(o.m00, o.m01, o.m02, o.m03, 
                   o.m10, o.m11, o.m12, o.m13, 
                   o.m20, o.m21, o.m22, o.m23,
                   o.m30, o.m31, o.m32, o.m33);
    }

    /**
     * Reset this matrix's values so that it represents the identity matrix.
     * 
     * @return This matrix
     */
    public Matrix4f setIdentity() {
        return set(1, 0, 0, 0, 
                   0, 1, 0, 0, 
                   0, 0, 1, 0,
                   0, 0, 0, 1);
    }

    /**
     * Determine if these two matrices are equal, within an error range of eps.
     * 
     * @param e Matrix to check approximate equality to
     * @param eps Error tolerance of each component
     * @return True if all component values are within eps of the corresponding
     *         component of e
     */
    public boolean epsilonEquals(Matrix4f e, float eps) {
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

    /**
     * Return true if these two matrices are numerically equal. Returns false if
     * e is null
     * 
     * @param e Matrix to test equality with
     * @return True if these vectors are numerically equal
     */
    public boolean equals(Matrix4f e) {
        return e != null && 
               m00 == e.m00 && m01 == e.m01 && m02 == e.m02 && m03 == e.m03 && 
               m10 == e.m10 && m11 == e.m11 && m12 == e.m12 && m13 == e.m13 && 
               m20 == e.m20 && m21 == e.m21 && m22 == e.m22 && m23 == e.m23 && 
               m30 == e.m30 && m31 == e.m31 && m32 == e.m32 && m33 == e.m33;
    }

    @Override
    public Matrix4f clone() {
        return new Matrix4f(this);
    }

    @Override
    public boolean equals(Object o) {
        // if conditional handles null values
        if (!(o instanceof Matrix4f))
            return false;
        else
            return equals((Matrix4f) o);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += result * 31 + Float.floatToIntBits(m00);
        result += result * 31 + Float.floatToIntBits(m01);
        result += result * 31 + Float.floatToIntBits(m02);
        result += result * 31 + Float.floatToIntBits(m03);

        result += result * 31 + Float.floatToIntBits(m10);
        result += result * 31 + Float.floatToIntBits(m11);
        result += result * 31 + Float.floatToIntBits(m12);
        result += result * 31 + Float.floatToIntBits(m13);

        result += result * 31 + Float.floatToIntBits(m20);
        result += result * 31 + Float.floatToIntBits(m21);
        result += result * 31 + Float.floatToIntBits(m22);
        result += result * 31 + Float.floatToIntBits(m23);

        result += result * 31 + Float.floatToIntBits(m30);
        result += result * 31 + Float.floatToIntBits(m31);
        result += result * 31 + Float.floatToIntBits(m32);
        result += result * 31 + Float.floatToIntBits(m33);

        return result;
    }
    
    @Override
    public String toString() {
        return "[[ " + m00 + ", " + m01 + ", " + m02 + ", " + m03 + " ]\n" +
                "[ " + m10 + ", " + m11 + ", " + m12 + ", " + m13 + " ]\n" +
                "[ " + m20 + ", " + m21 + ", " + m22 + ", " + m23 + " ]\n" +
                "[ " + m30 + ", " + m31 + ", " + m32 + ", " + m33 + " ]]";
    }
}
