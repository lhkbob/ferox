package com.ferox.math;

/**
 * <p>
 * Matrix3f provides a final implementation of a 3x3 matrix. The nine components
 * of the matrix are available as public fields. There is no need for further
 * abstraction because a 3x3 matrix is just 9 values. The public fields are
 * labeled m[row][col], where the rows and columns go from 0 to 2.
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
public final class Matrix3f implements Cloneable {
	public float m00, m01, m02, m10, m11, m12, m20, m21, m22;

	/**
	 * Construct a new Matrix3f that's set to the identity matrix.
	 */
	public Matrix3f() {
		setIdentity();
	}

	/**
	 * Construct a new Matrix3f that's copies the values in o.
	 * 
	 * @see #set(Matrix3f)
	 * @param o Matrix to clone
	 */
	public Matrix3f(Matrix3f o) {
		set(o);
	}

	/**
	 * Construct a new Matrix3f which takes its nine values from the given
	 * array, starting at offset. The values are either given row-major or
	 * column-major, depending on the rowMajor value.
	 * 
	 * @see #set(float[], int, boolean)
	 * @param values Value source
	 * @param offset Start index of matrix component values
	 * @param rowMajor Ordering of the matrix data
	 */
	public Matrix3f(float[] values, int offset, boolean rowMajor) {
		set(values, offset, rowMajor);
	}

	/**
	 * Construct a new Matrix3f that assigns each parameter value to the
	 * identically named field.
	 * 
	 * @see #set(float, float, float, float, float, float, float, float, float)
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
	public Matrix3f(float m00, float m01, float m02, 
					float m10, float m11, float m12, 
					float m20, float m21, float m22) {
		set(m00, m01, m02, m10, m11, m12, m20, m21, m22);
	}

	/**
	 * Add the values of the matrix r to this matrix, component by component and
	 * store it in result. This effectively computes the mathematical operation
	 * of [result] = [this] + [r].
	 * 
	 * @param r Matrix to add to this matrix
	 * @param result Matrix to hold the addition result
	 * @return result, or a new Matrix3f if null, holding the addition
	 */
	public Matrix3f add(Matrix3f r, Matrix3f result) {
		if (result == null)
			result = new Matrix3f();

		return result.set(m00 + r.m00, m01 + r.m01, m02 + r.m02, 
						  m10 + r.m10, m11 + r.m11, m12 + r.m12,
						  m20 + r.m20, m21 + r.m21, m22 + r.m22);
	}

	/**
	 * As add(r, result) where result is this matrix
	 * 
	 * @param r
	 * @return This matrix
	 */
	public Matrix3f add(Matrix3f r) {
		return add(r, this);
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
	public Matrix3f add(float c, Matrix3f result) {
		if (result == null)
			result = new Matrix3f();

		return result.set(m00 + c, m01 + c, m02 + c, 
						  m10 + c, m11 + c, m12 + c, 
						  m20 + c, m21 + c, m22 + c);
	}

	/**
	 * As add(c, result) where result is this matrix
	 * 
	 * @param c
	 * @return This matrix
	 */
	public Matrix3f add(float c) {
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
		float t1 = m11 * m22 - m12 * m21;
		float t2 = m10 * m22 - m12 * m20;
		float t3 = m10 * m21 - m11 * m20;

		return m00 * t1 - m01 * t2 + m02 * t3;
	}

	/**
	 * Store the inverse of this matrix into result and return result. If result
	 * is null, then a new Matrix3f should be created and returned.
	 * 
	 * @param result Matrix to hold the inverse
	 * @return result, or a new Matrix3f if null, holding the inverse
	 * @throws ArithmeticException if this matrix isn't invertible
	 */
	public Matrix3f inverse(Matrix3f result) {
		float invDet = determinant();
		if (Math.abs(invDet) < .0001f)
			throw new ArithmeticException("Singular or ill-formed matrix");
		invDet = 1f / invDet;

		if (result == null)
			result = new Matrix3f();

		float r00 = m22 * m11 - m21 * m12;
		float r01 = m21 * m02 - m22 * m01;
		float r02 = m12 * m01 - m11 * m02;

		float r10 = m20 * m12 - m22 * m10;
		float r11 = m22 * m00 - m20 * m02;
		float r12 = m10 * m02 - m12 * m00;

		float r20 = m21 * m10 - m20 * m11;
		float r21 = m20 * m01 - m21 * m00;
		float r22 = m11 * m00 - m10 * m01;

		return result.set(invDet * r00, invDet * r01, invDet * r02, 
						  invDet * r10, invDet * r11, invDet * r12, 
						  invDet * r20, invDet * r21, invDet * r22);
	}

	/**
	 * Inverse this matrix in place
	 * 
	 * @return This matrix
	 */
	public Matrix3f inverse() {
		return inverse(this);
	}

	/**
	 * Compute and return the length of this matrix. The length of a matrix is
	 * defined as the square root of the sum of the squared matrix values.
	 * 
	 * @return The matrix's length
	 */
	public float length() {
		float row1 = m00 * m00 + m01 * m01 + m02 * m02;
		float row2 = m10 * m10 + m11 * m11 + m12 * m12;
		float row3 = m20 * m20 + m21 * m21 + m22 * m22;

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
	 */
	public Matrix3f mul(Matrix3f r, Matrix3f result) {
		if (result == null)
			result = new Matrix3f();
		return result.set(m00 * r.m00 + m01 * r.m10 + m02 * r.m20, 
						  m00 * r.m01 + m01 * r.m11 + m02 * r.m21, 
						  m00 * r.m02 + m01 * r.m12 + m02 * r.m22, 
						  m10 * r.m00 + m11 * r.m10 + m12 * r.m20, 
						  m10 * r.m01 + m11 * r.m11 + m12 * r.m21, 
						  m10 * r.m02 + m11 * r.m12 + m12 * r.m22, 
						  m20 * r.m00 + m21 * r.m10 + m22 * r.m20, 
						  m20 * r.m01 + m21 * r.m11 + m22 * r.m21, 
						  m20 * r.m02 + m21 * r.m12 + m22 * r.m22);
	}

	/**
	 * As mul(r, result) where result is this matrix
	 * 
	 * @param r
	 * @return This matrix
	 */
	public Matrix3f mul(Matrix3f r) {
		return mul(r, this);
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
	 */
	public Vector3f mul(Vector3f r, Vector3f result) {
		return mul(r.x, r.y, r.z, result);
	}

	/**
	 * As mul(r, result) where result is the given vector
	 * 
	 * @param r
	 * @return r
	 */
	public Vector3f mul(Vector3f r) {
		return mul(r, r);
	}

	/**
	 * Multiply this matrix by the 3x1 matrix [[x][y][z]], or compute [this] x
	 * [[x][y][z]]. The result of this operation is another 3x1 matrix, which
	 * can then be interpreted as the transformed vector of (x,y,z). The
	 * computation is stored in result; if result is null, then create a new
	 * Vector3f to hold it and return.
	 * 
	 * @param r Vector to be interpreted as a 3x1 matrix in the multiplication
	 * @param result Vector holding the resultant transformed vector
	 * @return result, or a new Vector3f if null, holding [this] x [[x][y][z]]
	 */
	public Vector3f mul(float x, float y, float z, Vector3f result) {
		if (result == null)
			result = new Vector3f();
		return result.set(m00 * x + m01 * y + m02 * z, 
						  m10 * x + m11 * y + m12 * z, 
						  m20 * x + m21 * y + m22 * z);
	}

	/**
	 * Multiply this matrix by the diagonal matrix that takes it's three
	 * diagonal entries from diag, or compute [this] x [m], where [m] is all 0s
	 * except m00 = diag.x, m11 = diag.y, and m22 = diag.z. The multiplication
	 * is stored in result. If result is null, create and return a new Matrix3f.
	 * 
	 * @param diag Vector holding the three diagonal entries of the other matrix
	 * @param result Matrix holding the multiplication result
	 * @return result, or a new Matrix3f if null, holding the multiplication
	 */
	public Matrix3f mulDiagonal(Vector3f diag, Matrix3f result) {
		return mulDiagonal(diag.x, diag.y, diag.z, result);
	}

	/**
	 * As mulDiagonal(diag, result) where result is this matrix
	 * 
	 * @param diag
	 * @return This matrix
	 */
	public Matrix3f mulDiagonal(Vector3f diag) {
		return mulDiagonal(diag, this);
	}

	/**
	 * Multiply this matrix by the diagonal matrix that takes it's three
	 * diagonal entries from (x, y, z), or compute [this] x [m], where [m] is
	 * all 0s except m00 = x, m11 = y, and m22 = z. The multiplication is stored
	 * in result. If result is null, create and return a new Matrix3f.
	 * 
	 * @param x The m00 value used for the diagonal matrix
	 * @param y The m11 value used for the diagonal matrix
	 * @param z The m22 value used for the diagonal matrix
	 * @param result Matrix holding the multiplication result
	 * @return result, or a new Matrix3f if null, holding the multiplication
	 */
	public Matrix3f mulDiagonal(float x, float y, float z, Matrix3f result) {
		if (result == null)
			result = new Matrix3f();
		return result.set(m00 * x, m01 * y, m02 * z,
						  m10 * x, m11 * y, m12 * z, 
						  m20 * x, m21 * y, m22 * z);
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
	 */
	public Vector3f mulPre(Vector3f r, Vector3f result) {
		return mulPre(r.x, r.y, r.z, result);
	}

	/**
	 * Multiply the 1x3 matrix represented by the transpose of r by this matrix,
	 * or compute [r]^T x [this]. The result of this operation is another 1x3
	 * matrix, which can then be re-transposed and stored in result; if result
	 * is null, then create a new Vector3f to hold it and return.
	 * 
	 * @param x The 1st column's value in the 1x3 matrix
	 * @param y The 2nd column's value in the 1x3 matrix
	 * @param z The 3rd column's value in the 1x3 matrix
	 * @param result Vector holding the resultant computation
	 * @return result, or a new Vector3f if null, holding [r]^T x [this]
	 */
	public Vector3f mulPre(float x, float y, float z, Vector3f result) {
		if (result == null)
			result = new Vector3f();
		return result.set(m00 * x + m10 * y + m20 * z, 
						  m01 * x + m11 * y + m21 * z, 
						  m02 * x + m12 * y + m22 * z);
	}

	/**
	 * As mulPre(r, result) where result is the given vector
	 * 
	 * @param r
	 * @return r
	 */
	public Vector3f mulPre(Vector3f r) {
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
	 */
	public Matrix3f mulTransposeBoth(Matrix3f r, Matrix3f result) {
		result = r.mul(this, result);
		return result.transpose(result);
	}

	/**
	 * As mulTransposeBoth(r, result) where result is this matrix
	 * 
	 * @param r
	 * @return This matrix
	 */
	public Matrix3f mulTransposeBoth(Matrix3f r) {
		return mulTransposeBoth(r, this);
	}

	/**
	 * Multiply the transpose of this matrix by r, or compute [this]^T x [r] and
	 * store it in result. If result is null, then a new Matrix3f should be
	 * created and returned instead.
	 * 
	 * @param r Matrix used in right-hand operation of the multiplication
	 * @param result Matrix storing the result
	 * @return result, or a new Matrix3f if null, holding the multiplication
	 */
	public Matrix3f mulTransposeLeft(Matrix3f r, Matrix3f result) {
		if (result == null)
			result = new Matrix3f();
		return result.set(m00 * r.m00 + m10 * r.m10 + m20 * r.m20, 
						  m00 * r.m01 + m10 * r.m11 + m20 * r.m21, 
						  m00 * r.m02 + m10 * r.m12 + m20 * r.m22, 
						  m01 * r.m00 + m11 * r.m10 + m21 * r.m20, 
						  m01 * r.m01 + m11 * r.m11 + m21 * r.m21, 
						  m01 * r.m02 + m11 * r.m12 + m21 * r.m22, 
						  m02 * r.m00 + m12 * r.m10 + m22 * r.m20, 
						  m02 * r.m01 + m12 * r.m11 + m22 * r.m21, 
						  m02 * r.m02 + m12 * r.m12 + m22 * r.m22);
	}

	/**
	 * As mulTransposeLeft(r, result) where result is this matrix
	 * 
	 * @param r
	 * @return This matrix
	 */
	public Matrix3f mulTransposeLeft(Matrix3f r) {
		return mulTransposeLeft(r, this);
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
	 */
	public Matrix3f mulTransposeRight(Matrix3f r, Matrix3f result) {
		if (result == null)
			result = new Matrix3f();
		return result.set(m00 * r.m00 + m01 * r.m01 + m02 * r.m02, 
						  m00 * r.m10 + m01 * r.m11 + m02 * r.m12, 
						  m00 * r.m20 + m01 * r.m21 + m02 * r.m22, 
						  m10 * r.m00 + m11 * r.m01 + m12 * r.m02, 
						  m10 * r.m10 + m11 * r.m11 + m12 * r.m12, 
						  m10 * r.m20 + m11 * r.m21 + m12 * r.m22, 
						  m20 * r.m00 + m21 * r.m01 + m22 * r.m02, 
						  m20 * r.m10 + m21 * r.m11 + m22 * r.m12,
						  m20 * r.m20 + m21 * r.m21 + m22 * r.m22);
	}

	/**
	 * As mulTransposeRight(r, result) where result is this matrix
	 * 
	 * @param r
	 * @return This matrix
	 */
	public Matrix3f mulTransposeRight(Matrix3f r) {
		return mulTransposeRight(r, this);
	}

	/**
	 * Scale each of this matrix's values by the scalar and store it in result.
	 * This effectively computes [result] = scalar*[this].
	 * 
	 * @param scalar Scale factor applied to each matrix value
	 * @param result Matrix to hold the scaled version of this matrix
	 * @return result, or a new Matrix3f if null, holding the addition
	 */
	public Matrix3f scale(float scalar, Matrix3f result) {
		if (result == null)
			result = new Matrix3f();

		return result.set(scalar * m00, scalar * m01, scalar * m02, 
						  scalar * m10, scalar * m11, scalar * m12,
						  scalar * m20, scalar * m21, scalar * m22);
	}

	/**
	 * As scale(scalar, result) where result is this matrix
	 * 
	 * @param scalar
	 * @return This matrix
	 */
	public Matrix3f scale(float scalar) {
		return scale(scalar, this);
	}

	/**
	 * Solve the linear system of equations and store the resultant values of
	 * (x, y, z) into result:
	 * 
	 * <pre>
	 * m00*x + m01*y + m02*z = ans.x
	 * m10*x + m11*y + m12*z = ans.y
	 * m20*x + m21*y + m22*z = ans.z
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
	 */
	public Vector3f solve(Vector3f ans, Vector3f result) {
		return solve(ans.x, ans.y, ans.z, result);
	}

	/**
	 * Solve the linear system of equations and store the resultant values of
	 * (x, y, z) into result:
	 * 
	 * <pre>
	 * m00*x + m01*y + m02*z = ax
	 * m10*x + m11*y + m12*z = ay
	 * m20*x + m21*y + m22*z = az
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
	public Vector3f solve(float ax, float ay, float az, Vector3f result) {
		if (result == null)
			result = new Vector3f();
		float invDet = determinant();
		if (Math.abs(invDet) < .0001f)
			throw new ArithmeticException("No solution, or infinite solutions, cannot solve system");
		invDet = 1f / invDet;

		result.x = invDet * (ax * (m11 * m22 - m12 * m21) - ay * (m01 * m22 - m02 * m21) + az * (m01 * m12 - m02 * m11));
		result.y = invDet * (ay * (m00 * m22 - m02 * m20) - az * (m00 * m12 - m02 * m10) - ax * (m10 * m22 - m12 * m20));
		result.z = invDet * (ax * (m10 * m21 - m11 * m20) - ay * (m00 * m21 - m01 * m20) + az * (m00 * m11 - m01 * m10));

		return result;
	}

	/**
	 * Compute the trace of this matrix. The trace is defined as the sum of the
	 * diagonal entries of the matrix.
	 * 
	 * @return The matrix's trace
	 */
	public float trace() {
		return m00 + m11 + m22;
	}

	/**
	 * Store the transpose of this matrix into result and return result. If
	 * result is null, then a new Matrix3f should be created and returned.
	 * 
	 * @param result Matrix to hold the transpose
	 * @return result, or a new Matrix3f if null, holding the transpose
	 */
	public Matrix3f transpose(Matrix3f result) {
		if (result == null)
			result = new Matrix3f();
		return result.set(m00, m10, m20, m01, m11, m21, m02, m12, m22);
	}

	/**
	 * Transpose this matrix in place
	 * 
	 * @return This matrix
	 */
	public Matrix3f transpose() {
		return transpose(this);
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
	public float get(int row, int col) {
		if (row == 0) {
			if (col == 0)
				return m00;
			else if (col == 1)
				return m01;
			else if (col == 2)
				return m02;
		} else if (row == 1) {
			if (col == 0)
				return m10;
			else if (col == 1)
				return m11;
			else if (col == 2)
				return m12;
		} else if (row == 2)
			if (col == 0)
				return m20;
			else if (col == 1)
				return m21;
			else if (col == 2)
				return m22;

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
	public Vector3f getCol(int col, Vector3f store) {
		if (store == null)
			store = new Vector3f();

		switch (col) {
		case 0:
			return store.set(m00, m10, m20);
		case 1:
			return store.set(m01, m11, m21);
		case 2:
			return store.set(m02, m12, m22);
		default:
			throw new IndexOutOfBoundsException("Invalid column value: " + col);
		}
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
		switch (col) {
		case 0:
			store[offset] = m00;
			store[offset + 1] = m10;
			store[offset + 2] = m20;
			break;
		case 1:
			store[offset] = m01;
			store[offset + 1] = m11;
			store[offset + 2] = m21;
			break;
		case 2:
			store[offset] = m02;
			store[offset + 1] = m12;
			store[offset + 2] = m22;
			break;
		default:
			throw new IndexOutOfBoundsException("Invalid column value: " + col);
		}
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
	public Vector3f getRow(int row, Vector3f store) {
		if (store == null)
			store = new Vector3f();

		switch (row) {
		case 0:
			return store.set(m00, m01, m02);
		case 1:
			return store.set(m10, m11, m12);
		case 2:
			return store.set(m20, m21, m22);
		default:
			throw new IndexOutOfBoundsException("Invalid row value: " + row);
		}
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
		switch (row) {
		case 0:
			store[offset] = m00;
			store[offset + 1] = m01;
			store[offset + 2] = m02;
			break;
		case 1:
			store[offset] = m10;
			store[offset + 1] = m11;
			store[offset + 2] = m12;
			break;
		case 2:
			store[offset] = m20;
			store[offset + 1] = m21;
			store[offset + 2] = m22;
			break;
		default:
			throw new IndexOutOfBoundsException("Invalid row value: " + row);
		}
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
	public Matrix3f set(int row, int col, float value) {
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
			}
		} else if (row == 2)
			if (col == 0) {
				m20 = value;
				return this;
			} else if (col == 1) {
				m21 = value;
				return this;
			} else if (col == 2) {
				m22 = value;
				return this;
			}

		throw new IndexOutOfBoundsException("Invalid row or column: " + row + ", " + col);
	}

	/**
	 * Set the given matrix column to the values in the values array, starting
	 * at offset. From offset, three floats are taken; it is assumed that values
	 * has enough space for three floats.
	 * 
	 * @param col The column to update, in [0, 2]
	 * @param values The source for the new column values
	 * @param offset The offset to get the 1st column value from
	 * @return This matrix
	 * @throws NullPointerException if values is null
	 * @throws ArrayIndexOutOfBoundsException if values doesn't long enough
	 * @throws IndexOutOfBoundsException if col is invalid
	 */
	public Matrix3f setCol(int col, float[] values, int offset) {
		switch (col) {
		case 0:
			m00 = values[offset];
			m10 = values[offset + 1];
			m20 = values[offset + 2];
			return this;
		case 1:
			m01 = values[offset];
			m11 = values[offset + 1];
			m21 = values[offset + 2];
			return this;
		case 2:
			m02 = values[offset];
			m12 = values[offset + 1];
			m22 = values[offset + 2];
			return this;
		default:
			throw new IndexOutOfBoundsException("Invalid column value: " + col);
		}
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
	public Matrix3f setCol(int col, Vector3f values) {
		switch (col) {
		case 0:
			m00 = values.x;
			m10 = values.y;
			m20 = values.z;
			return this;
		case 1:
			m01 = values.x;
			m11 = values.y;
			m21 = values.z;
			return this;
		case 2:
			m02 = values.x;
			m12 = values.y;
			m22 = values.z;
			return this;
		default:
			throw new IndexOutOfBoundsException("Invalid column value: " + col);
		}
	}

	/**
	 * Set the given matrix row to the values in the values array, starting at
	 * offset. From offset, three floats are taken; it is assumed that values
	 * has enough space for three floats.
	 * 
	 * @param row The row to update, in [0, 2]
	 * @param values The source for the new row values
	 * @param offset The offset to get the 1st row value from
	 * @return This matrix
	 * @throws NullPointerException if values is null
	 * @throws ArrayIndexOutOfBoundsException if values doesn't long enough
	 * @throws IndexOutOfBoundsException if row is invalid
	 */
	public Matrix3f setRow(int row, float[] values, int offset) {
		switch (row) {
		case 0:
			m00 = values[offset];
			m01 = values[offset + 1];
			m02 = values[offset + 2];
			return this;
		case 1:
			m10 = values[offset];
			m11 = values[offset + 1];
			m12 = values[offset + 2];
			return this;
		case 2:
			m20 = values[offset];
			m21 = values[offset + 1];
			m22 = values[offset + 2];
			return this;
		default:
			throw new IndexOutOfBoundsException("Invalid row value: " + row);
		}
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
	public Matrix3f setRow(int row, Vector3f values) {
		switch (row) {
		case 0:
			m00 = values.x;
			m01 = values.y;
			m02 = values.z;
			return this;
		case 1:
			m10 = values.x;
			m11 = values.y;
			m12 = values.z;
			return this;
		case 2:
			m20 = values.x;
			m21 = values.y;
			m22 = values.z;
			return this;
		default:
			throw new IndexOutOfBoundsException("Invalid row value: " + row);
		}
	}

	/**
	 * Set all nine values of this matrix from the given array values. If
	 * rowMajor is true, each set of three floats is treated as a row. If it is
	 * false, the values are column major, and each three represents a matrix
	 * column. The values are taken from the values array starting at offset; it
	 * is assumed that the array is long enough.
	 * 
	 * @param values Float source for the new matrix data
	 * @param offset Start index of the float values
	 * @param rowMajor True if values is row-major order
	 * @return This matrix
	 * @throws NullPointerException if values is null
	 */
	public Matrix3f set(float[] values, int offset, boolean rowMajor) {
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
	public Matrix3f set(float m00, float m01, float m02, 
						float m10, float m11, float m12, 
						float m20, float m21, float m22) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;

		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;

		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;

		return this;
	}

	/**
	 * Set this matrix to be equal to the given matrix, o.
	 * 
	 * @param o Matrix whose values are copied into this matrix
	 * @return This matrix
	 * @throws NullPointerException if o is null
	 */
	public Matrix3f set(Matrix3f o) {
		return set(o.m00, o.m01, o.m02, o.m10, o.m11, o.m12, o.m20, o.m21, o.m22);
	}

	/**
	 * Reset this matrix's values so that it represents the identity matrix.
	 * 
	 * @return This matrix
	 */
	public Matrix3f setIdentity() {
		return set(1, 0, 0, 0, 1, 0, 0, 0, 1);
	}

	/**
	 * Determine if these two matrices are equal, within an error range of eps.
	 * 
	 * @param e Matrix to check approximate equality to
	 * @param eps Error tolerance of each component
	 * @return True if all component values are within eps of the corresponding
	 *         component of e
	 */
	public boolean epsilonEquals(Matrix3f e, float eps) {
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

	/**
	 * Return true if these two matrices are numerically equal. Returns false if
	 * e is null
	 * 
	 * @param e Matrix to test equality with
	 * @return True if these vectors are numerically equal
	 */
	public boolean equals(Matrix3f e) {
		return e != null && 
			   m00 == e.m00 && m01 == e.m01 && m02 == e.m02 && 
			   m10 == e.m10 && m11 == e.m11 && m12 == e.m12 && 
			   m20 == e.m20 && m21 == e.m21 && m22 == e.m22;
	}

	@Override
	public Matrix3f clone() {
		return new Matrix3f(this);
	}

	@Override
	public boolean equals(Object o) {
		// if conditional handles null values
		if (!(o instanceof Matrix3f))
			return false;
		else
			return equals((Matrix3f) o);
	}

	@Override
	public int hashCode() {
		int result = 17;
		result += result * 31 + Float.floatToIntBits(m00);
		result += result * 31 + Float.floatToIntBits(m01);
		result += result * 31 + Float.floatToIntBits(m02);

		result += result * 31 + Float.floatToIntBits(m10);
		result += result * 31 + Float.floatToIntBits(m11);
		result += result * 31 + Float.floatToIntBits(m12);

		result += result * 31 + Float.floatToIntBits(m20);
		result += result * 31 + Float.floatToIntBits(m21);
		result += result * 31 + Float.floatToIntBits(m22);

		return result;
	}
	
	@Override
	public String toString() {
		return "[[ " + m00 + ", " + m01 + ", " + m02 + " ]\n" +
				"[ " + m10 + ", " + m11 + ", " + m12 + " ]\n" +
				"[ " + m20 + ", " + m21 + ", " + m22 + " ]]";
	}
}
