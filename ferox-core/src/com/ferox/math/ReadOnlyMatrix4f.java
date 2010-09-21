package com.ferox.math;

/**
 * <p>
 * ReadOnlyMatrix4f provides the foundation class for the implementation of a
 * 4x4 matrix. It is read-only in the sense that all operations expose a
 * <tt>result</tt> parameter that stores the computation. The calling matrix is
 * not modified unless it happens to be the result. Additionally, this class
 * only exposes accessors to its data and no mutators. The class
 * {@link Matrix4f} provides a standard implementation of ReadOnlyMatrix4f that
 * exposes mutators for the 16 values of the matrix.
 * </p>
 * <p>
 * In all mathematical functions that compute a new matrix, there is a method
 * parameter, often named result, that will hold the computed value. This result
 * matrix is also returned so that complex mathematical expressions can be
 * chained. It is always safe to use the calling matrix as the result matrix
 * (assuming the calling matrix is of the correct type).
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class ReadOnlyMatrix4f {
    private static ThreadLocal<Matrix4f> inverse = new ThreadLocal<Matrix4f>() {
        @Override
        public Matrix4f initialValue() {
            return new Matrix4f();
        }
    };
    
    /**
     * Add the values of the matrix r to this matrix, component by component and
     * store it in result. This effectively computes the mathematical operation
     * of [result] = [this] + [r].
     * 
     * @param r Matrix to add to this matrix
     * @param result Matrix to hold the addition result
     * @return result, or a new Matrix4f if null, holding the addition
     * @throws NullPointerException if r is null
     */
    public MutableMatrix4f add(ReadOnlyMatrix4f r, MutableMatrix4f result) {
        if (result == null)
            result = new Matrix4f();

        return result.set(get(0, 0) + r.get(0, 0), get(0, 1) + r.get(0, 1), get(0, 2) + r.get(0, 2), get(0, 3) + r.get(0, 3), 
                          get(1, 0) + r.get(1, 0), get(1, 1) + r.get(1, 1), get(1, 2) + r.get(1, 2), get(1, 3) + r.get(1, 3), 
                          get(2, 0) + r.get(2, 0), get(2, 1) + r.get(2, 1), get(2, 2) + r.get(2, 2), get(2, 3) + r.get(2, 3), 
                          get(3, 0) + r.get(3, 0), get(3, 1) + r.get(3, 1), get(3, 2) + r.get(3, 2), get(3, 3) + r.get(3, 3));
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
    public MutableMatrix4f add(float c, MutableMatrix4f result) {
        if (result == null)
            result = new Matrix4f();

        return result.set(get(0, 0) + c, get(0, 1) + c, get(0, 2) + c, get(0, 3) + c, 
                          get(1, 0) + c, get(1, 1) + c, get(1, 2) + c, get(1, 3) + c, 
                          get(2, 0) + c, get(2, 1) + c, get(2, 2) + c, get(2, 3) + c, 
                          get(3, 0) + c, get(3, 1) + c, get(3, 2) + c, get(3, 3) + c);
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
        float ra0 = get(0, 0) * get(1, 1) - get(0, 1) * get(1, 0);
        float ra1 = get(0, 0) * get(1, 2) - get(0, 2) * get(1, 0);
        float ra2 = get(0, 0) * get(1, 3) - get(0, 3) * get(1, 0);
        float ra3 = get(0, 1) * get(1, 2) - get(0, 2) * get(1, 1);
        float ra4 = get(0, 1) * get(1, 3) - get(0, 3) * get(1, 1);
        float ra5 = get(0, 2) * get(1, 3) - get(0, 3) * get(1, 2);
        float rb0 = get(2, 0) * get(3, 1) - get(2, 1) * get(3, 0);
        float rb1 = get(2, 0) * get(3, 2) - get(2, 2) * get(3, 0);
        float rb2 = get(2, 0) * get(3, 3) - get(2, 3) * get(3, 0);
        float rb3 = get(2, 1) * get(3, 2) - get(2, 2) * get(3, 1);
        float rb4 = get(2, 1) * get(3, 3) - get(2, 3) * get(3, 1);
        float rb5 = get(2, 2) * get(3, 3) - get(2, 3) * get(3, 2);

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
    public MutableMatrix4f inverse(MutableMatrix4f result) {
        // Also, thanks to Ardor3D for the inverse code
        if (result == null)
            result = new Matrix4f();

        // inlined det computation to get at intermediate results
        float ra0 = get(0, 0) * get(1, 1) - get(0, 1) * get(1, 0);
        float ra1 = get(0, 0) * get(1, 2) - get(0, 2) * get(1, 0);
        float ra2 = get(0, 0) * get(1, 3) - get(0, 3) * get(1, 0);
        float ra3 = get(0, 1) * get(1, 2) - get(0, 2) * get(1, 1);
        float ra4 = get(0, 1) * get(1, 3) - get(0, 3) * get(1, 1);
        float ra5 = get(0, 2) * get(1, 3) - get(0, 3) * get(1, 2);
        float rb0 = get(2, 0) * get(3, 1) - get(2, 1) * get(3, 0);
        float rb1 = get(2, 0) * get(3, 2) - get(2, 2) * get(3, 0);
        float rb2 = get(2, 0) * get(3, 3) - get(2, 3) * get(3, 0);
        float rb3 = get(2, 1) * get(3, 2) - get(2, 2) * get(3, 1);
        float rb4 = get(2, 1) * get(3, 3) - get(2, 3) * get(3, 1);
        float rb5 = get(2, 2) * get(3, 3) - get(2, 3) * get(3, 2);

        float invDet = ra0 * rb5 - ra1 * rb4 + ra2 * rb3 + ra3 * rb2 - ra4 * rb1 + ra5 * rb0;
        if (Math.abs(invDet) <= .0001f)
            throw new ArithmeticException("Singular matrix");
        invDet = 1f / invDet;

        float t00 = get(1, 1) * rb5 - get(1, 2) * rb4 + get(1, 3) * rb3;
        float t10 = get(1, 2) * rb2 - get(1, 3) * rb1 - get(1, 0) * rb5;
        float t20 = get(1, 0) * rb4 - get(1, 1) * rb2 + get(1, 3) * rb0;
        float t30 = get(1, 1) * rb1 - get(1, 2) * rb0 - get(1, 0) * rb3;
        float t01 = get(0, 2) * rb4 - get(0, 3) * rb3 - get(0, 1) * rb5;
        float t11 = get(0, 0) * rb5 - get(0, 2) * rb2 + get(0, 3) * rb1;
        float t21 = get(0, 1) * rb2 - get(0, 3) * rb0 - get(0, 0) * rb4;
        float t31 = get(0, 0) * rb3 - get(0, 1) * rb1 + get(0, 2) * rb0;
        float t02 = get(3, 1) * ra5 - get(3, 2) * ra4 + get(3, 3) * ra3;
        float t12 = get(3, 2) * ra2 - get(3, 3) * ra1 - get(3, 0) * ra5;
        float t22 = get(3, 0) * ra4 - get(3, 1) * ra2 + get(3, 3) * ra0;
        float t32 = get(3, 1) * ra1 - get(3, 2) * ra0 - get(3, 0) * ra3;
        float t03 = get(2, 2) * ra4 - get(2, 3) * ra3 - get(2, 1) * ra5;
        float t13 = get(2, 0) * ra5 - get(2, 2) * ra2 + get(2, 3) * ra1;
        float t23 = get(2, 1) * ra2 - get(2, 3) * ra0 - get(2, 0) * ra4;
        float t33 = get(2, 0) * ra3 - get(2, 1) * ra1 + get(2, 2) * ra0;

        return result.set(invDet * t00, invDet * t01, invDet * t02, invDet * t03, 
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
    public float length() {
        float row1 = get(0, 0) * get(0, 0) + get(0, 1) * get(0, 1) + get(0, 2) * get(0, 2) + get(0, 3) * get(0, 3);
        float row2 = get(1, 0) * get(1, 0) + get(1, 1) * get(1, 1) + get(1, 2) * get(1, 2) + get(1, 3) * get(1, 3);
        float row3 = get(2, 0) * get(2, 0) + get(2, 1) * get(2, 1) + get(2, 2) * get(2, 2) + get(2, 3) * get(2, 3);
        float row4 = get(3, 0) * get(3, 0) + get(3, 1) * get(3, 1) + get(3, 2) * get(3, 2) + get(3, 3) * get(3, 3);

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
     * @throws NullPointerException if r is null
     */
    public MutableMatrix4f mul(ReadOnlyMatrix4f r, MutableMatrix4f result) {
        if (result == null)
            result = new Matrix4f();
        return result.set(get(0, 0) * r.get(0, 0) + get(0, 1) * r.get(1, 0) + get(0, 2) * r.get(2, 0) + get(0, 3) * r.get(3, 0),
                          get(0, 0) * r.get(0, 1) + get(0, 1) * r.get(1, 1) + get(0, 2) * r.get(2, 1) + get(0, 3) * r.get(3, 1),
                          get(0, 0) * r.get(0, 2) + get(0, 1) * r.get(1, 2) + get(0, 2) * r.get(2, 2) + get(0, 3) * r.get(3, 2), 
                          get(0, 0) * r.get(0, 3) + get(0, 1) * r.get(1, 3) + get(0, 2) * r.get(2, 3) + get(0, 3) * r.get(3, 3), 
                          get(1, 0) * r.get(0, 0) + get(1, 1) * r.get(1, 0) + get(1, 2) * r.get(2, 0) + get(1, 3) * r.get(3, 0), 
                          get(1, 0) * r.get(0, 1) + get(1, 1) * r.get(1, 1) + get(1, 2) * r.get(2, 1) + get(1, 3) * r.get(3, 1), 
                          get(1, 0) * r.get(0, 2) + get(1, 1) * r.get(1, 2) + get(1, 2) * r.get(2, 2) + get(1, 3) * r.get(3, 2), 
                          get(1, 0) * r.get(0, 3) + get(1, 1) * r.get(1, 3) + get(1, 2) * r.get(2, 3) + get(1, 3) * r.get(3, 3), 
                          get(2, 0) * r.get(0, 0) + get(2, 1) * r.get(1, 0) + get(2, 2) * r.get(2, 0) + get(2, 3) * r.get(3, 0), 
                          get(2, 0) * r.get(0, 1) + get(2, 1) * r.get(1, 1) + get(2, 2) * r.get(2, 1) + get(2, 3) * r.get(3, 1), 
                          get(2, 0) * r.get(0, 2) + get(2, 1) * r.get(1, 2) + get(2, 2) * r.get(2, 2) + get(2, 3) * r.get(3, 2), 
                          get(2, 0) * r.get(0, 3) + get(2, 1) * r.get(1, 3) + get(2, 2) * r.get(2, 3) + get(2, 3) * r.get(3, 3), 
                          get(3, 0) * r.get(0, 0) + get(3, 1) * r.get(1, 0) + get(3, 2) * r.get(2, 0) + get(3, 3) * r.get(3, 0), 
                          get(3, 0) * r.get(0, 1) + get(3, 1) * r.get(1, 1) + get(3, 2) * r.get(2, 1) + get(3, 3) * r.get(3, 1), 
                          get(3, 0) * r.get(0, 2) + get(3, 1) * r.get(1, 2) + get(3, 2) * r.get(2, 2) + get(3, 3) * r.get(3, 2), 
                          get(3, 0) * r.get(0, 3) + get(3, 1) * r.get(1, 3) + get(3, 2) * r.get(2, 3) + get(3, 3) * r.get(3, 3));
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
     * @throws NullPointerException if r is null
     */
    public MutableVector4f mul(ReadOnlyVector4f r, MutableVector4f result) {
        if (result == null)
            result = new Vector4f();
        return result.set(get(0, 0) * r.getX() + get(0, 1) * r.getY() + get(0, 2) * r.getZ() + get(0, 3) * r.getW(), 
                          get(1, 0) * r.getX() + get(1, 1) * r.getY() + get(1, 2) * r.getZ() + get(1, 3) * r.getW(),
                          get(2, 0) * r.getX() + get(2, 1) * r.getY() + get(2, 2) * r.getZ() + get(2, 3) * r.getW(),
                          get(3, 0) * r.getX() + get(3, 1) * r.getY() + get(3, 2) * r.getZ() + get(3, 3) * r.getW());
    }

    /**
     * As {@link #mul(ReadOnlyVector4f, Vector4f)} where result is the given
     * vector.
     * 
     * @param r
     * @return r
     * @throws NullPointerException if r is null
     */
    public MutableVector4f mul(MutableVector4f r) {
        return mul(r, r);
    }

    /**
     * Multiply this matrix by the diagonal matrix that takes it's three
     * diagonal entries from diag, or compute [this] x [m], where [m] is all 0s
     * except get(0, 0) = diag.x, get(1, 1) = diag.y, get(2, 2) = diag.z, and get(3, 3) = diag.w. The
     * multiplication is stored in result. If result is null, create and return
     * a new Matrix4f.
     * 
     * @param diag Vector holding the three diagonal entries of the other matrix
     * @param result Matrix holding the multiplication result
     * @return result, or a new Matrix4f if null, holding the multiplication
     * @throws NullPointerException if diag is null
     */
    public MutableMatrix4f mulDiagonal(ReadOnlyVector4f diag, MutableMatrix4f result) {
        if (result == null)
            result = new Matrix4f();
        return result.set(get(0, 0) * diag.getX(), get(0, 1) * diag.getY(), get(0, 2) * diag.getZ(), get(0, 3) * diag.getW(), 
                          get(1, 0) * diag.getX(), get(1, 1) * diag.getY(), get(1, 2) * diag.getZ(), get(1, 3) * diag.getW(), 
                          get(2, 0) * diag.getX(), get(2, 1) * diag.getY(), get(2, 2) * diag.getZ(), get(2, 3) * diag.getW(), 
                          get(3, 0) * diag.getX(), get(3, 1) * diag.getY(), get(3, 2) * diag.getZ(), get(3, 3) * diag.getW());
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
     * @throws NullPointerException if r is null
     */
    public MutableVector4f mulPre(ReadOnlyVector4f r, MutableVector4f result) {
        if (result == null)
            result = new Vector4f();
        return result.set(get(0, 0) * r.getX() + get(1, 0) * r.getY() + get(2, 0) * r.getZ() + get(3, 0) * r.getW(), 
                          get(0, 1) * r.getX() + get(1, 1) * r.getY() + get(2, 1) * r.getZ() + get(3, 1) * r.getW(),
                          get(0, 2) * r.getX() + get(1, 2) * r.getY() + get(2, 2) * r.getZ() + get(3, 2) * r.getW(),
                          get(0, 3) * r.getX() + get(1, 3) * r.getY() + get(2, 3) * r.getZ() + get(3, 3) * r.getW());
    }

    /**
     * As {@link #mulPre(ReadOnlyVector4f, Vector4f)} where result is the given vector.
     * 
     * @param r
     * @return r
     * @throws NullPointerException if r is null
     */
    public MutableVector4f mulPre(MutableVector4f r) {
        return mulPre(r, r);
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
     * @throws NullPointerException if r is null
     */
    public MutableMatrix4f mulTransposeBoth(ReadOnlyMatrix4f r, MutableMatrix4f result) {
        return r.mul(this, result).transpose();
    }

    /**
     * Multiply the transpose of this matrix by r, or compute [this]^T x [r] and
     * store it in result. If result is null, then a new Matrix4f should be
     * created and returned instead.
     * 
     * @param r Matrix used in right-hand operation of the multiplication
     * @param result Matrix storing the result
     * @return result, or a new Matrix4f if null, holding the multiplication
     * @throws NullPointerException if r is null
     */
    public MutableMatrix4f mulTransposeLeft(ReadOnlyMatrix4f r, MutableMatrix4f result) {
        if (result == null)
            result = new Matrix4f();
        return result.set(get(0, 0) * r.get(0, 0) + get(1, 0) * r.get(1, 0) + get(2, 0) * r.get(2, 0) + get(3, 0) * r.get(3, 0), 
                          get(0, 0) * r.get(0, 1) + get(1, 0) * r.get(1, 1) + get(2, 0) * r.get(2, 1) + get(3, 0) * r.get(3, 1),
                          get(0, 0) * r.get(0, 2) + get(1, 0) * r.get(1, 2) + get(2, 0) * r.get(2, 2) + get(3, 0) * r.get(3, 2),
                          get(0, 0) * r.get(0, 3) + get(1, 0) * r.get(1, 3) + get(2, 0) * r.get(2, 3) + get(3, 0) * r.get(3, 3),
                          get(0, 1) * r.get(0, 0) + get(1, 1) * r.get(1, 0) + get(2, 1) * r.get(2, 0) + get(3, 1) * r.get(3, 0),
                          get(0, 1) * r.get(0, 1) + get(1, 1) * r.get(1, 1) + get(2, 1) * r.get(2, 1) + get(3, 1) * r.get(3, 1),
                          get(0, 1) * r.get(0, 2) + get(1, 1) * r.get(1, 2) + get(2, 1) * r.get(2, 2) + get(3, 1) * r.get(3, 2), 
                          get(0, 1) * r.get(0, 3) + get(1, 1) * r.get(1, 3) + get(2, 1) * r.get(2, 3) + get(3, 1) * r.get(3, 3), 
                          get(0, 2) * r.get(0, 0) + get(1, 2) * r.get(1, 0) + get(2, 2) * r.get(2, 0) + get(3, 2) * r.get(3, 0), 
                          get(0, 2) * r.get(0, 1) + get(1, 2) * r.get(1, 1) + get(2, 2) * r.get(2, 1) + get(3, 2) * r.get(3, 1),
                          get(0, 2) * r.get(0, 2) + get(1, 2) * r.get(1, 2) + get(2, 2) * r.get(2, 2) + get(3, 2) * r.get(3, 2),
                          get(0, 2) * r.get(0, 3) + get(1, 2) * r.get(1, 3) + get(2, 2) * r.get(2, 3) + get(3, 2) * r.get(3, 3),
                          get(0, 3) * r.get(0, 0) + get(1, 3) * r.get(1, 0) + get(2, 3) * r.get(2, 0) + get(3, 3) * r.get(3, 0),
                          get(0, 3) * r.get(0, 1) + get(1, 3) * r.get(1, 1) + get(2, 3) * r.get(2, 1) + get(3, 3) * r.get(3, 1), 
                          get(0, 3) * r.get(0, 2) + get(1, 3) * r.get(1, 2) + get(2, 3) * r.get(2, 2) + get(3, 3) * r.get(3, 2), 
                          get(0, 3) * r.get(0, 3) + get(1, 3) * r.get(1, 3) + get(2, 3) * r.get(2, 3) + get(3, 3) * r.get(3, 3));
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
     * @throws NullPointerException if r is null
     */
    public MutableMatrix4f mulTransposeRight(ReadOnlyMatrix4f r, MutableMatrix4f result) {
        if (result == null)
            result = new Matrix4f();
        return result.set(get(0, 0) * r.get(0, 0) + get(0, 1) * r.get(0, 1) + get(0, 2) * r.get(0, 2) + get(0, 3) * r.get(0, 3), 
                          get(0, 0) * r.get(1, 0) + get(0, 1) * r.get(1, 1) + get(0, 2) * r.get(1, 2) + get(0, 3) * r.get(1, 3), 
                          get(0, 0) * r.get(2, 0) + get(0, 1) * r.get(2, 1) + get(0, 2) * r.get(2, 2) + get(0, 3) * r.get(2, 3),
                          get(0, 0) * r.get(3, 0) + get(0, 1) * r.get(3, 1) + get(0, 3) * r.get(3, 2) + get(0, 3) * r.get(3, 3), 
                          get(1, 0) * r.get(0, 0) + get(1, 1) * r.get(0, 1) + get(1, 2) * r.get(0, 2) + get(1, 3) * r.get(0, 3), 
                          get(1, 0) * r.get(1, 0) + get(1, 1) * r.get(1, 1) + get(1, 2) * r.get(1, 2) + get(1, 3) * r.get(1, 3), 
                          get(1, 0) * r.get(2, 0) + get(1, 1) * r.get(2, 1) + get(1, 2) * r.get(2, 2) + get(1, 3) * r.get(2, 3), 
                          get(1, 0) * r.get(3, 0) + get(1, 1) * r.get(3, 1) + get(1, 2) * r.get(3, 2) + get(1, 3) * r.get(3, 3), 
                          get(2, 0) * r.get(0, 0) + get(2, 1) * r.get(0, 1) + get(2, 2) * r.get(0, 2) + get(2, 3) * r.get(0, 3), 
                          get(2, 0) * r.get(1, 0) + get(2, 1) * r.get(1, 1) + get(2, 2) * r.get(1, 2) + get(2, 3) * r.get(1, 3),
                          get(2, 0) * r.get(2, 0) + get(2, 1) * r.get(2, 1) + get(2, 2) * r.get(2, 2) + get(2, 3) * r.get(2, 3),
                          get(2, 0) * r.get(3, 0) + get(2, 1) * r.get(3, 1) + get(2, 2) * r.get(3, 2) + get(2, 3) * r.get(3, 3),
                          get(3, 0) * r.get(0, 0) + get(3, 1) * r.get(0, 1) + get(3, 2) * r.get(0, 2) + get(3, 3) * r.get(0, 3),
                          get(3, 0) * r.get(1, 0) + get(3, 1) * r.get(1, 1) + get(3, 2) * r.get(1, 2) + get(3, 3) * r.get(1, 3),
                          get(3, 0) * r.get(2, 0) + get(3, 1) * r.get(2, 1) + get(3, 2) * r.get(2, 2) + get(3, 3) * r.get(2, 3), 
                          get(3, 0) * r.get(3, 0) + get(3, 1) * r.get(3, 1) + get(3, 2) * r.get(3, 2) + get(3, 3) * r.get(3, 3));
    }

    /**
     * Scale each of this matrix's values by the scalar and store it in result.
     * This effectively computes [result] = scalar*[this].
     * 
     * @param scalar Scale factor applied to each matrix value
     * @param result Matrix to hold the scaled version of this matrix
     * @return result, or a new Matrix4f if null, holding the addition
     */
    public MutableMatrix4f scale(float scalar, MutableMatrix4f result) {
        if (result == null)
            result = new Matrix4f();

        return result.set(scalar * get(0, 0), scalar * get(0, 1), scalar * get(0, 2), scalar * get(0, 3), 
                          scalar * get(1, 0), scalar * get(1, 1), scalar * get(1, 2), scalar * get(1, 3), 
                          scalar * get(2, 0), scalar * get(2, 1), scalar * get(2, 2), scalar * get(2, 3), 
                          scalar * get(3, 0), scalar * get(3, 1), scalar * get(3, 2), scalar * get(3, 3));
    }

    /**
     * Solve the linear system of equations and store the resultant values of
     * (x, y, z, w) into result:
     * 
     * <pre>
     * get(0, 0)*x + get(0, 1)*y + get(0, 2)*z + get(0, 3)*w = ans.x
     * get(1, 0)*x + get(1, 1)*y + get(1, 2)*z + get(1, 3)*w = ans.y
     * get(2, 0)*x + get(2, 1)*y + get(2, 2)*z + get(2, 3)*w = ans.z
     * get(3, 0)*x + get(3, 1)*y + get(3, 2)*z + get(3, 3)*w = ans.w
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
     * @throws NullPointerException if ans is null
     */
    public MutableVector4f solve(ReadOnlyVector4f ans, MutableVector4f result) {
        // the system is b = [A]x and we're solving for x
        // which becomes [A]^-1 b = x
        MutableMatrix4f inv = inverse(inverse.get());
        return inv.mul(ans, result);
    }

    /**
     * Solve the linear system of equations and store the resultant values of
     * (x, y, z, w) into result:
     * 
     * <pre>
     * get(0, 0)*x + get(0, 1)*y + get(0, 2)*z + get(0, 3)*w = ax
     * get(1, 0)*x + get(1, 1)*y + get(1, 2)*z + get(1, 3)*w = ay
     * get(2, 0)*x + get(2, 1)*y + get(2, 2)*z + get(2, 3)*w = az
     * get(3, 0)*x + get(3, 1)*y + get(3, 2)*z + get(3, 3)*w = aw
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
    public MutableVector4f solve(float ax, float ay, float az, float aw, MutableVector4f result) {
        return solve(new Vector4f(ax, ay, az, aw), result);
    }

    /**
     * Compute the trace of this matrix. The trace is defined as the sum of the
     * diagonal entries of the matrix.
     * 
     * @return The matrix's trace
     */
    public float trace() {
        return get(0, 0) + get(1, 1) + get(2, 2) + get(3, 3);
    }

    /**
     * Store the transpose of this matrix into result and return result. If
     * result is null, then a new Matrix4f should be created and returned.
     * 
     * @param result Matrix to hold the transpose
     * @return result, or a new Matrix4f if null, holding the transpose
     */
    public MutableMatrix4f transpose(MutableMatrix4f result) {
        if (result == null)
            result = new Matrix4f();
        return result.set(get(0, 0), get(1, 0), get(2, 0), get(3, 0),
                          get(0, 1), get(1, 1), get(2, 1), get(3, 1), 
                          get(0, 2), get(1, 2), get(2, 2), get(3, 2), 
                          get(0, 3), get(1, 3), get(2, 3), get(3, 3));
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
    public abstract float get(int row, int col);

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
     * Return a new ReadOnlyMatrix3f that wraps the upper 3x3 matrix of this 4x4
     * matrix. Any changes to this matrix will be reflected in the matrix values
     * of the returned 3x3 matrix.
     * 
     * @return A 3x3 matrix representing the upper 3x3 values of this matrix
     */
    public ReadOnlyMatrix3f getUpperMatrix() {
        return new Upper3x3Matrix();
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
    public MutableVector4f getCol(int col, MutableVector4f store) {
        if (store == null)
            store = new Vector4f();
        
        return store.set(get(0, col), get(1, col), get(2, col), get(3, col));
    }
    
    /**
     * Return a ReadOnlyVector4f that wraps the specified column of this matrix.
     * Any changes to the matrice's column, <tt>col</tt> will be reflected by
     * the x, y, z, and w coordinates of the returned vector. Note that unlike
     * {@link #getCol(int, Vector4f)}, this is NOT a copy.
     * 
     * @param col The column to wrap
     * @return A vector wrapping the given column
     */
    public ReadOnlyVector4f getCol(int col) {
        return new ColumnVector(col);
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
        store[offset] = get(0, col);
        store[offset + 1] = get(1, col);
        store[offset + 2] = get(2, col);
        store[offset + 3] = get(3, col);
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
    public MutableVector4f getRow(int row, MutableVector4f store) {
        if (store == null)
            store = new Vector4f();
        return store.set(get(row, 0), get(row, 1), get(row, 2), get(row, 3));
    }
    
    /**
     * Return a ReadOnlyVector4f that wraps the specified row of this matrix.
     * Any changes to the matrice's row, <tt>row</tt> will be reflected by 
     * the x, y, z, and w coordinates of the returned vector. Note that unlike
     * {@link #getRow(int, Vector4f)}, this is NOT a copy.
     * 
     * @param row The row to wrap
     * @return A vector wrapping the given row
     */
    public ReadOnlyVector4f getRow(int row) {
        return new RowVector(row);
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
        store[offset] = get(row, 0);
        store[offset + 1] = get(row, 1);
        store[offset + 2] = get(row, 2);
        store[offset + 3] = get(row, 3);
    }

    /**
     * Determine if these two matrices are equal, within an error range of eps.
     * 
     * @param e Matrix to check approximate equality to
     * @param eps Error tolerance of each component
     * @return True if all component values are within eps of the corresponding
     *         component of e
     */
    public boolean epsilonEquals(ReadOnlyMatrix4f e, float eps) {
        if (e == null)
            return false;

        return Math.abs(get(0, 0) - e.get(0, 0)) <= eps && Math.abs(get(0, 1) - e.get(0, 1)) <= eps && 
               Math.abs(get(0, 2) - e.get(0, 2)) <= eps && Math.abs(get(0, 3) - e.get(0, 3)) <= eps && 
               Math.abs(get(1, 0) - e.get(1, 0)) <= eps && Math.abs(get(1, 1) - e.get(1, 1)) <= eps &&
               Math.abs(get(1, 2) - e.get(1, 2)) <= eps && Math.abs(get(1, 3) - e.get(1, 3)) <= eps && 
               Math.abs(get(2, 0) - e.get(2, 0)) <= eps && Math.abs(get(2, 1) - e.get(2, 1)) <= eps && 
               Math.abs(get(2, 2) - e.get(2, 2)) <= eps && Math.abs(get(2, 3) - e.get(2, 3)) <= eps && 
               Math.abs(get(3, 0) - e.get(3, 0)) <= eps && Math.abs(get(3, 1) - e.get(3, 1)) <= eps && 
               Math.abs(get(3, 2) - e.get(3, 2)) <= eps && Math.abs(get(3, 3) - e.get(3, 3)) <= eps;
    }

    /**
     * Return true if these two matrices are numerically equal. Returns false if
     * e is null
     * 
     * @param e Matrix to test equality with
     * @return True if these vectors are numerically equal
     */
    public boolean equals(ReadOnlyMatrix4f e) {
        return e != null && 
               get(0, 0) == e.get(0, 0) && get(0, 1) == e.get(0, 1) && get(0, 2) == e.get(0, 2) && get(0, 3) == e.get(0, 3) && 
               get(1, 0) == e.get(1, 0) && get(1, 1) == e.get(1, 1) && get(1, 2) == e.get(1, 2) && get(1, 3) == e.get(1, 3) && 
               get(2, 0) == e.get(2, 0) && get(2, 1) == e.get(2, 1) && get(2, 2) == e.get(2, 2) && get(2, 3) == e.get(2, 3) && 
               get(3, 0) == e.get(3, 0) && get(3, 1) == e.get(3, 1) && get(3, 2) == e.get(3, 2) && get(3, 3) == e.get(3, 3);
    }

    @Override
    public boolean equals(Object o) {
        // if conditional handles null values
        if (!(o instanceof ReadOnlyMatrix4f))
            return false;
        else
            return equals((ReadOnlyMatrix4f) o);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += result * 31 + Float.floatToIntBits(get(0, 0));
        result += result * 31 + Float.floatToIntBits(get(0, 1));
        result += result * 31 + Float.floatToIntBits(get(0, 2));
        result += result * 31 + Float.floatToIntBits(get(0, 3));

        result += result * 31 + Float.floatToIntBits(get(1, 0));
        result += result * 31 + Float.floatToIntBits(get(1, 1));
        result += result * 31 + Float.floatToIntBits(get(1, 2));
        result += result * 31 + Float.floatToIntBits(get(1, 3));

        result += result * 31 + Float.floatToIntBits(get(2, 0));
        result += result * 31 + Float.floatToIntBits(get(2, 1));
        result += result * 31 + Float.floatToIntBits(get(2, 2));
        result += result * 31 + Float.floatToIntBits(get(2, 3));

        result += result * 31 + Float.floatToIntBits(get(3, 0));
        result += result * 31 + Float.floatToIntBits(get(3, 1));
        result += result * 31 + Float.floatToIntBits(get(3, 2));
        result += result * 31 + Float.floatToIntBits(get(3, 3));

        return result;
    }
    
    @Override
    public String toString() {
        return "[[ " + get(0, 0) + ", " + get(0, 1) + ", " + get(0, 2) + ", " + get(0, 3) + " ]\n" +
                "[ " + get(1, 0) + ", " + get(1, 1) + ", " + get(1, 2) + ", " + get(1, 3) + " ]\n" +
                "[ " + get(2, 0) + ", " + get(2, 1) + ", " + get(2, 2) + ", " + get(2, 3) + " ]\n" +
                "[ " + get(3, 0) + ", " + get(3, 1) + ", " + get(3, 2) + ", " + get(3, 3) + " ]]";
    }
    
    private class ColumnVector extends ReadOnlyVector4f {
        private final int col;
        
        public ColumnVector(int col) {
            this.col = col;
        }

        @Override
        public float getX() {
            return ReadOnlyMatrix4f.this.get(0, col);
        }

        @Override
        public float getY() {
            return ReadOnlyMatrix4f.this.get(1, col);
        }

        @Override
        public float getZ() {
            return ReadOnlyMatrix4f.this.get(2, col);
        }

        @Override
        public float getW() {
            return ReadOnlyMatrix4f.this.get(3, col);
        }
    }
    
    private class RowVector extends ReadOnlyVector4f {
        private final int row;
        
        public RowVector(int row) {
            this.row = row;
        }

        @Override
        public float getX() {
            return ReadOnlyMatrix4f.this.get(row, 0);
        }

        @Override
        public float getY() {
            return ReadOnlyMatrix4f.this.get(row, 1);
        }

        @Override
        public float getZ() {
            return ReadOnlyMatrix4f.this.get(row, 2);
        }

        @Override
        public float getW() {
            return ReadOnlyMatrix4f.this.get(row, 3);
        }
    }
    
    private class Upper3x3Matrix extends ReadOnlyMatrix3f {
        @Override
        public float get(int row, int col) {
            // fail with reduced max column since normal get() won't catch it
            // but we still rely on their < 0 index check
            if (row > 2 || col > 2)
                throw new IndexOutOfBoundsException();
            return ReadOnlyMatrix4f.this.get(row, col); 
        }
    }
}
