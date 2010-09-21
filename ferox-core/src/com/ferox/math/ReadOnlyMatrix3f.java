package com.ferox.math;

/**
 * <p>
 * ReadOnlyMatrix3f provides the foundation class for the implementation of a
 * 3x3 matrix. It is read-only in the sense that all operations expose a
 * <tt>result</tt> parameter that stores the computation. The calling matrix is
 * not modified unless it happens to be the result. Additionally, this class
 * only exposes accessors to its data and no mutators. The class
 * {@link Matrix3f} provides a standard implementation of ReadOnlyMatrix3f that
 * exposes mutators for the 9 values of the matrix.
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
public abstract class ReadOnlyMatrix3f {
    /**
     * Add the values of the matrix r to this matrix, component by component and
     * store it in result. This effectively computes the mathematical operation
     * of [result] = [this] + [r].
     * 
     * @param r Matrix to add to this matrix
     * @param result Matrix to hold the addition result
     * @return result, or a new Matrix3f if null, holding the addition
     * @throws NullPointerException if r is null
     */
    public MutableMatrix3f add(ReadOnlyMatrix3f r, MutableMatrix3f result) {
        if (result == null)
            result = new Matrix3f();

        return result.set(get(0, 0) + r.get(0, 0), get(0, 1) + r.get(0, 1), get(0, 2) + r.get(0, 2), 
                          get(1, 0) + r.get(1, 0), get(1, 1) + r.get(1, 1), get(1, 2) + r.get(1, 2),
                          get(2, 0) + r.get(2, 0), get(2, 1) + r.get(2, 1), get(2, 2) + r.get(2, 2));
    }

    /**
     * Add the given constant to each of this matrix's values and store it in
     * result. If result is null, then a new Matrix3f should be created and
     * returned.
     * 
     * @param c Constant factor added to each of matrix value
     * @param result Matrix to hold the addition result
     * @return result, or a new Matrix3f if null, holding the addition
     */
    public MutableMatrix3f add(float c, MutableMatrix3f result) {
        if (result == null)
            result = new Matrix3f();

        return result.set(get(0, 0) + c, get(0, 1) + c, get(0, 2) + c, 
                          get(1, 0) + c, get(1, 1) + c, get(1, 2) + c, 
                          get(2, 0) + c, get(2, 1) + c, get(2, 2) + c);
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
        float t1 = get(1, 1) * get(2, 2) - get(1, 2) * get(2, 1);
        float t2 = get(1, 0) * get(2, 2) - get(1, 2) * get(2, 0);
        float t3 = get(1, 0) * get(2, 1) - get(1, 1) * get(2, 0);

        return get(0, 0) * t1 - get(0, 1) * t2 + get(0, 2) * t3;
    }

    /**
     * Store the inverse of this matrix into result and return result. If result
     * is null, then a new Matrix3f should be created and returned.
     * 
     * @param result Matrix to hold the inverse
     * @return result, or a new Matrix3f if null, holding the inverse
     * @throws ArithmeticException if this matrix isn't invertible
     */
    public MutableMatrix3f inverse(MutableMatrix3f result) {
        float invDet = determinant();
        if (Math.abs(invDet) < .0001f)
            throw new ArithmeticException("Singular or ill-formed matrix");
        invDet = 1f / invDet;

        if (result == null)
            result = new Matrix3f();

        float r00 = get(2, 2) * get(1, 1) - get(2, 1) * get(1, 2);
        float r01 = get(2, 1) * get(0, 2) - get(2, 2) * get(0, 1);
        float r02 = get(1, 2) * get(0, 1) - get(1, 1) * get(0, 2);

        float r10 = get(2, 0) * get(1, 2) - get(2, 2) * get(1, 0);
        float r11 = get(2, 2) * get(0, 0) - get(2, 0) * get(0, 2);
        float r12 = get(1, 0) * get(0, 2) - get(1, 2) * get(0, 0);

        float r20 = get(2, 1) * get(1, 0) - get(2, 0) * get(1, 1);
        float r21 = get(2, 0) * get(0, 1) - get(2, 1) * get(0, 0);
        float r22 = get(1, 1) * get(0, 0) - get(1, 0) * get(0, 1);

        return result.set(invDet * r00, invDet * r01, invDet * r02, 
                          invDet * r10, invDet * r11, invDet * r12, 
                          invDet * r20, invDet * r21, invDet * r22);
    }

    /**
     * Compute and return the length of this matrix. The length of a matrix is
     * defined as the square root of the sum of the squared matrix values.
     * 
     * @return The matrix's length
     */
    public float length() {
        float row1 = get(0, 0) * get(0, 0) + get(0, 1) * get(0, 1) + get(0, 2) * get(0, 2);
        float row2 = get(1, 0) * get(1, 0) + get(1, 1) * get(1, 1) + get(1, 2) * get(1, 2);
        float row3 = get(2, 0) * get(2, 0) + get(2, 1) * get(2, 1) + get(2, 2) * get(2, 2);

        return (float) Math.sqrt(row1 + row2 + row3);
    }

    /**
     * Multiply this matrix by r, or compute [this] x [r] and store it in
     * result. If result is null, then a new Matrix3f should be created and
     * returned instead.
     * 
     * @param r Matrix used in the right-hand operation of the multiplication
     * @param result Matrix storing the result
     * @return result, or a new Matrix3f if null, holding the multiplication
     * @throws NullPointerException if r is null
     */
    public MutableMatrix3f mul(ReadOnlyMatrix3f r, MutableMatrix3f result) {
        if (result == null)
            result = new Matrix3f();
        return result.set(get(0, 0) * r.get(0, 0) + get(0, 1) * r.get(1, 0) + get(0, 2) * r.get(2, 0), 
                          get(0, 0) * r.get(0, 1) + get(0, 1) * r.get(1, 1) + get(0, 2) * r.get(2, 1), 
                          get(0, 0) * r.get(0, 2) + get(0, 1) * r.get(1, 2) + get(0, 2) * r.get(2, 2), 
                          get(1, 0) * r.get(0, 0) + get(1, 1) * r.get(1, 0) + get(1, 2) * r.get(2, 0), 
                          get(1, 0) * r.get(0, 1) + get(1, 1) * r.get(1, 1) + get(1, 2) * r.get(2, 1), 
                          get(1, 0) * r.get(0, 2) + get(1, 1) * r.get(1, 2) + get(1, 2) * r.get(2, 2), 
                          get(2, 0) * r.get(0, 0) + get(2, 1) * r.get(1, 0) + get(2, 2) * r.get(2, 0), 
                          get(2, 0) * r.get(0, 1) + get(2, 1) * r.get(1, 1) + get(2, 2) * r.get(2, 1), 
                          get(2, 0) * r.get(0, 2) + get(2, 1) * r.get(1, 2) + get(2, 2) * r.get(2, 2));
    }

    /**
     * Multiply this matrix by the 3x1 matrix represented by the three values in
     * r, or compute [this] x [r]. The result of this operation is another 3x1
     * matrix, which can then be interpreted as the transformed vector of r. The
     * computation is stored in result; if result is null, then create a new
     * Vector3f to hold it and return.
     * 
     * @param r Vector to be interpreted as a 3x1 matrix in the multiplication
     * @param result Vector holding the resultant transformed vector
     * @return result, or a new Vector3f if null, holding [this] x [r]
     * @throws NullPointerException if r is null
     */
    public MutableVector3f mul(ReadOnlyVector3f r, MutableVector3f result) {
        if (result == null)
            result = new Vector3f();
        return result.set(get(0, 0) * r.getX() + get(0, 1) * r.getY() + get(0, 2) * r.getZ(), 
                          get(1, 0) * r.getX() + get(1, 1) * r.getY() + get(1, 2) * r.getZ(), 
                          get(2, 0) * r.getX() + get(2, 1) * r.getY() + get(2, 2) * r.getZ());
    }

    /**
     * As {@link #mul(ReadOnlyVector3f, Vector3f)} where the result is
     * <tt>r</tt>
     * 
     * @param r The vector to transform
     * @return The input vector r, after being transformed by this matrix
     * @throws NullPointerException if r is null
     */
    public MutableVector3f mul(MutableVector3f r) {
        return mul(r, r);
    }

    /**
     * Multiply this matrix by the diagonal matrix that takes it's three
     * diagonal entries from diag, or compute [this] x [m], where [m] is all 0s
     * except get(0, 0) = diag.x, get(1, 1) = diag.y, and get(2, 2) = diag.z. The multiplication
     * is stored in result. If result is null, create and return a new Matrix3f.
     * 
     * @param diag Vector holding the three diagonal entries of the other matrix
     * @param result Matrix holding the multiplication result
     * @return result, or a new Matrix3f if null, holding the multiplication
     * @throws NullPointerException if diag is null
     */
    public MutableMatrix3f mulDiagonal(ReadOnlyVector3f diag, MutableMatrix3f result) {
        if (result == null)
            result = new Matrix3f();
        return result.set(get(0, 0) * diag.getX(), get(0, 1) * diag.getY(), get(0, 2) * diag.getZ(),
                          get(1, 0) * diag.getX(), get(1, 1) * diag.getY(), get(1, 2) * diag.getZ(), 
                          get(2, 0) * diag.getX(), get(2, 1) * diag.getY(), get(2, 2) * diag.getZ()); 
    }


    /**
     * Multiply the 1x3 matrix represented by [x, y, z] by this matrix, or
     * compute [x, y, z] x [this]. The result of this operation is another 1x3
     * matrix, which can then be re-transposed and stored in result; if result
     * is null, then create a new Vector3f to hold it and return.
     * 
     * @param r Vector to be interpreted as a 1x3 matrix in the multiplication
     * @param result Vector holding the resultant computation
     * @return result, or a new Vector3f if null, holding [x, y, z] x [this]
     * @throws NullPointerException if r is null
     */
    public MutableVector3f mulPre(ReadOnlyVector3f r, MutableVector3f result) {
        if (result == null)
            result = new Vector3f();
        return result.set(get(0, 0) * r.getX() + get(1, 0) * r.getY() + get(2, 0) * r.getZ(), 
                          get(0, 1) * r.getX() + get(1, 1) * r.getY() + get(2, 1) * r.getZ(), 
                          get(0, 2) * r.getX() + get(1, 2) * r.getY() + get(2, 2) * r.getZ());
    }

    /**
     * As {@link #mulPre(ReadOnlyVector3f, Vector3f)} where result is the given
     * vector
     * 
     * @param r
     * @return r
     * @throws NullPointerException if r is null
     */
    public MutableVector3f mulPre(MutableVector3f r) {
        return mulPre(r, r);
    }

    /**
     * <p>
     * Multiply the transpose of this matrix by the transpose of r, or compute
     * [this]^T x [r]^T and store it in result. If result is null, then a new
     * Matrix3f should be created and returned instead.
     * </p>
     * <p>
     * Note that [this]^T x [r]^T = ([r] x [this])^T
     * </p>
     * 
     * @param r Matrix whose transpose is used in right-hand operation of the
     *            multiplication
     * @param result Matrix storing the result
     * @return result, or a new Matrix3f if null, holding the multiplication
     * @throws NullPointerException if r is null
     */
    public MutableMatrix3f mulTransposeBoth(ReadOnlyMatrix3f r, MutableMatrix3f result) {
        return r.mul(this, result).transpose();
    }

    /**
     * Multiply the transpose of this matrix by r, or compute [this]^T x [r] and
     * store it in result. If result is null, then a new Matrix3f should be
     * created and returned instead.
     * 
     * @param r Matrix used in right-hand operation of the multiplication
     * @param result Matrix storing the result
     * @return result, or a new Matrix3f if null, holding the multiplication
     * @throws NullPointerException if r is null
     */
    public MutableMatrix3f mulTransposeLeft(ReadOnlyMatrix3f r, MutableMatrix3f result) {
        if (result == null)
            result = new Matrix3f();
        return result.set(get(0, 0) * r.get(0, 0) + get(1, 0) * r.get(1, 0) + get(2, 0) * r.get(2, 0), 
                          get(0, 0) * r.get(0, 1) + get(1, 0) * r.get(1, 1) + get(2, 0) * r.get(2, 1), 
                          get(0, 0) * r.get(0, 2) + get(1, 0) * r.get(1, 2) + get(2, 0) * r.get(2, 2), 
                          get(0, 1) * r.get(0, 0) + get(1, 1) * r.get(1, 0) + get(2, 1) * r.get(2, 0), 
                          get(0, 1) * r.get(0, 1) + get(1, 1) * r.get(1, 1) + get(2, 1) * r.get(2, 1), 
                          get(0, 1) * r.get(0, 2) + get(1, 1) * r.get(1, 2) + get(2, 1) * r.get(2, 2), 
                          get(0, 2) * r.get(0, 0) + get(1, 2) * r.get(1, 0) + get(2, 2) * r.get(2, 0), 
                          get(0, 2) * r.get(0, 1) + get(1, 2) * r.get(1, 1) + get(2, 2) * r.get(2, 1), 
                          get(0, 2) * r.get(0, 2) + get(1, 2) * r.get(1, 2) + get(2, 2) * r.get(2, 2));
    }

    /**
     * Multiply this matrix by the transpose of r, or compute [this] x [r]^T and
     * store it in result. If result is null, then a new Matrix3f should be
     * created and returned instead.
     * 
     * @param r Matrix whose transpose is used in right-hand operation of the
     *            multiplication
     * @param result Matrix storing the result
     * @return result, or a new Matrix3f if null, holding the multiplication
     * @throws NullPointerException if r is null
     */
    public MutableMatrix3f mulTransposeRight(ReadOnlyMatrix3f r, MutableMatrix3f result) {
        if (result == null)
            result = new Matrix3f();
        return result.set(get(0, 0) * r.get(0, 0) + get(0, 1) * r.get(0, 1) + get(0, 2) * r.get(0, 2), 
                          get(0, 0) * r.get(1, 0) + get(0, 1) * r.get(1, 1) + get(0, 2) * r.get(1, 2), 
                          get(0, 0) * r.get(2, 0) + get(0, 1) * r.get(2, 1) + get(0, 2) * r.get(2, 2), 
                          get(1, 0) * r.get(0, 0) + get(1, 1) * r.get(0, 1) + get(1, 2) * r.get(0, 2), 
                          get(1, 0) * r.get(1, 0) + get(1, 1) * r.get(1, 1) + get(1, 2) * r.get(1, 2), 
                          get(1, 0) * r.get(2, 0) + get(1, 1) * r.get(2, 1) + get(1, 2) * r.get(2, 2), 
                          get(2, 0) * r.get(0, 0) + get(2, 1) * r.get(0, 1) + get(2, 2) * r.get(0, 2), 
                          get(2, 0) * r.get(1, 0) + get(2, 1) * r.get(1, 1) + get(2, 2) * r.get(1, 2),
                          get(2, 0) * r.get(2, 0) + get(2, 1) * r.get(2, 1) + get(2, 2) * r.get(2, 2));
    }

    /**
     * Scale each of this matrix's values by the scalar and store it in result.
     * This effectively computes [result] = scalar*[this].
     * 
     * @param scalar Scale factor applied to each matrix value
     * @param result Matrix to hold the scaled version of this matrix
     * @return result, or a new Matrix3f if null, holding the addition
     */
    public MutableMatrix3f scale(float scalar, MutableMatrix3f result) {
        if (result == null)
            result = new Matrix3f();

        return result.set(scalar * get(0, 0), scalar * get(0, 1), scalar * get(0, 2), 
                          scalar * get(1, 0), scalar * get(1, 1), scalar * get(1, 2),
                          scalar * get(2, 0), scalar * get(2, 1), scalar * get(2, 2));
    }

    /**
     * Solve the linear system of equations and store the resultant values of
     * (x, y, z) into result:
     * 
     * <pre>
     * get(0, 0)*x + get(0, 1)*y + get(0, 2)*z = ans.x
     * get(1, 0)*x + get(1, 1)*y + get(1, 2)*z = ans.y
     * get(2, 0)*x + get(2, 1)*y + get(2, 2)*z = ans.z
     * </pre>
     * 
     * If result is null, then a new Vector3f should be created and returned.
     * 
     * @param ans Vector holding the constraint values for the linear system of
     *            equations represented by this matrix
     * @param result Vector to hold the solution to the linear system of
     *            equations
     * @return result, or a new Vector3f if null, holding the solutions
     * @throws ArithmeticException if no solution or an infinite solutions exist
     * @throws NullPointerException if ans is null
     */
    public MutableVector3f solve(ReadOnlyVector3f ans, MutableVector3f result) {
        return solve(ans.getX(), ans.getY(), ans.getZ(), result);
    }

    /**
     * Solve the linear system of equations and store the resultant values of
     * (x, y, z) into result:
     * 
     * <pre>
     * get(0, 0)*x + get(0, 1)*y + get(0, 2)*z = ax
     * get(1, 0)*x + get(1, 1)*y + get(1, 2)*z = ay
     * get(2, 0)*x + get(2, 1)*y + get(2, 2)*z = az
     * </pre>
     * 
     * If result is null, then a new Vector3f should be created and returned.
     * 
     * @param ax Constraint value for the 1st function
     * @param ay Constraint value for the 2nd function
     * @param az Constraint value for the 3rd function
     * @param result Vector to hold the solution to the linear system of
     *            equations
     * @return result, or a new Vector3f if null, holding the solutions
     * @throws ArithmeticException if no solution or an infinite solutions exist
     */
    public MutableVector3f solve(float ax, float ay, float az, MutableVector3f result) {
        if (result == null)
            result = new Vector3f();
        float invDet = determinant();
        if (Math.abs(invDet) < .0001f)
            throw new ArithmeticException("No solution, or infinite solutions, cannot solve system");
        invDet = 1f / invDet;

        result.setX(invDet * (ax * (get(1, 1) * get(2, 2) - get(1, 2) * get(2, 1)) - ay * (get(0, 1) * get(2, 2) - get(0, 2) * get(2, 1)) + az * (get(0, 1) * get(1, 2) - get(0, 2) * get(1, 1))));
        result.setY(invDet * (ay * (get(0, 0) * get(2, 2) - get(0, 2) * get(2, 0)) - az * (get(0, 0) * get(1, 2) - get(0, 2) * get(1, 0)) - ax * (get(1, 0) * get(2, 2) - get(1, 2) * get(2, 0))));
        result.setZ(invDet * (ax * (get(1, 0) * get(2, 1) - get(1, 1) * get(2, 0)) - ay * (get(0, 0) * get(2, 1) - get(0, 1) * get(2, 0)) + az * (get(0, 0) * get(1, 1) - get(0, 1) * get(1, 0))));

        return result;
    }

    /**
     * Compute the trace of this matrix. The trace is defined as the sum of the
     * diagonal entries of the matrix.
     * 
     * @return The matrix's trace
     */
    public float trace() {
        return get(0, 0) + get(1, 1) + get(2, 2);
    }

    /**
     * Store the transpose of this matrix into result and return result. If
     * result is null, then a new Matrix3f should be created and returned.
     * 
     * @param result Matrix to hold the transpose
     * @return result, or a new Matrix3f if null, holding the transpose
     */
    public MutableMatrix3f transpose(MutableMatrix3f result) {
        if (result == null)
            result = new Matrix3f();
        return result.set(get(0, 0), get(1, 0), get(2, 0),
                          get(0, 1), get(1, 1), get(2, 1), 
                          get(0, 2), get(1, 2), get(2, 2));
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
            getRow(1, store, offset + 3);
            getRow(2, store, offset + 6);
        } else {
            getCol(0, store, offset);
            getCol(1, store, offset + 3);
            getCol(2, store, offset + 6);
        }
    }

    /**
     * Store the three column values for the given column index into the vector
     * store. If store is null, a new Vector3f should be created and returned.
     * 
     * @param col Column to access, in [0, 2]
     * @param store Vector to hold the retrieved row
     * @return store, or a new Vector3f if null, holding the matrix column
     * @throws IndexOutOfBoundsException if col is invalid
     */
    public MutableVector3f getCol(int col, MutableVector3f store) {
        if (store == null)
            store = new Vector3f();
        return store.set(get(0, col), get(1, col), get(2, col));
    }

    /**
     * Return a ReadOnlyVector3f that wraps the specified column of this matrix.
     * Any changes to the matrice's column, <tt>col</tt> will be reflected by
     * the x, y, and z coordinates of the returned vector. Note that unlike
     * {@link #getCol(int, Vector3f)}, this is NOT a copy.
     * 
     * @param col The column to wrap
     * @return A vector wrapping the given column
     */
    public ReadOnlyVector3f getCol(int col) {
        return new ColumnVector(col);
    }

    /**
     * Store the three column values for the given column index into the float
     * array. The three values will be stored consecutively, starting at the
     * offset index. It is assumed that store has at least three elements
     * remaining, starting at offset.
     * 
     * @param col The col to retrieve, in [0, 2]
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
    }

    /**
     * Store the three row values for the given row index into the vector store.
     * If store is null, a new Vector3f should be created and returned.
     * 
     * @param row Row to access, in [0, 2]
     * @param store Vector to hold the retrieved row
     * @return store, or a new Vector3f if null, holding the matrix row
     * @throws IndexOutOfBoundsException if row is invalid
     */
    public MutableVector3f getRow(int row, MutableVector3f store) {
        if (store == null)
            store = new Vector3f();
        return store.set(get(row, 0), get(row, 1), get(row, 2));
    }
    
    /**
     * Return a ReadOnlyVector3f that wraps the specified row of this matrix.
     * Any changes to the matrice's row, <tt>row</tt> will be reflected by 
     * the x, y, and z coordinates of the returned vector. Note that unlike
     * {@link #getRow(int, Vector3f)}, this is NOT a copy.
     * 
     * @param row The row to wrap
     * @return A vector wrapping the given row
     */
    public ReadOnlyVector3f getRow(int row) {
        return new RowVector(row);
    }

    /**
     * Store the three row values for the given row index into the float array.
     * The three values will be stored consecutively, starting at the offset
     * index. It is assumed that store has at least three elements remaining,
     * starting at offset.
     * 
     * @param row The row to retrieve, in [0, 2]
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
    }

    /**
     * Determine if these two matrices are equal, within an error range of eps.
     * 
     * @param e Matrix to check approximate equality to
     * @param eps Error tolerance of each component
     * @return True if all component values are within eps of the corresponding
     *         component of e
     */
    public boolean epsilonEquals(ReadOnlyMatrix3f e, float eps) {
        if (e == null)
            return false;

        return Math.abs(get(0, 0) - e.get(0, 0)) <= eps && 
               Math.abs(get(0, 1) - e.get(0, 1)) <= eps && 
               Math.abs(get(0, 2) - e.get(0, 2)) <= eps && 
               Math.abs(get(1, 0) - e.get(1, 0)) <= eps && 
               Math.abs(get(1, 1) - e.get(1, 1)) <= eps && 
               Math.abs(get(1, 2) - e.get(1, 2)) <= eps && 
               Math.abs(get(2, 0) - e.get(2, 0)) <= eps && 
               Math.abs(get(2, 1) - e.get(2, 1)) <= eps && 
               Math.abs(get(2, 2) - e.get(2, 2)) <= eps;
    }

    /**
     * Return true if these two matrices are numerically equal. Returns false if
     * e is null
     * 
     * @param e Matrix to test equality with
     * @return True if these vectors are numerically equal
     */
    public boolean equals(ReadOnlyMatrix3f e) {
        return e != null && 
               get(0, 0) == e.get(0, 0) && get(0, 1) == e.get(0, 1) && get(0, 2) == e.get(0, 2) && 
               get(1, 0) == e.get(1, 0) && get(1, 1) == e.get(1, 1) && get(1, 2) == e.get(1, 2) && 
               get(2, 0) == e.get(2, 0) && get(2, 1) == e.get(2, 1) && get(2, 2) == e.get(2, 2);
    }

    @Override
    public boolean equals(Object o) {
        // if conditional handles null values
        if (!(o instanceof ReadOnlyMatrix3f))
            return false;
        else
            return equals((ReadOnlyMatrix3f) o);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += result * 31 + Float.floatToIntBits(get(0, 0));
        result += result * 31 + Float.floatToIntBits(get(0, 1));
        result += result * 31 + Float.floatToIntBits(get(0, 2));

        result += result * 31 + Float.floatToIntBits(get(1, 0));
        result += result * 31 + Float.floatToIntBits(get(1, 1));
        result += result * 31 + Float.floatToIntBits(get(1, 2));

        result += result * 31 + Float.floatToIntBits(get(2, 0));
        result += result * 31 + Float.floatToIntBits(get(2, 1));
        result += result * 31 + Float.floatToIntBits(get(2, 2));

        return result;
    }
    
    @Override
    public String toString() {
        return "[[ " + get(0, 0) + ", " + get(0, 1) + ", " + get(0, 2) + " ]\n" +
                "[ " + get(1, 0) + ", " + get(1, 1) + ", " + get(1, 2) + " ]\n" +
                "[ " + get(2, 0) + ", " + get(2, 1) + ", " + get(2, 2) + " ]]";
    }
    
    private class ColumnVector extends ReadOnlyVector3f {
        private final int col;
        
        public ColumnVector(int col) {
            this.col = col;
        }

        @Override
        public float getX() {
            return ReadOnlyMatrix3f.this.get(0, col);
        }

        @Override
        public float getY() {
            return ReadOnlyMatrix3f.this.get(1, col);
        }

        @Override
        public float getZ() {
            return ReadOnlyMatrix3f.this.get(2, col);
        }
    }
    
    private class RowVector extends ReadOnlyVector3f {
        private final int row;
        
        public RowVector(int row) {
            this.row = row;
        }

        @Override
        public float getX() {
            return ReadOnlyMatrix3f.this.get(row, 0);
        }

        @Override
        public float getY() {
            return ReadOnlyMatrix3f.this.get(row, 1);
        }

        @Override
        public float getZ() {
            return ReadOnlyMatrix3f.this.get(row, 2);
        }
    }
}
