package com.ferox.math;

/**
 * <p>
 * Matrix3f is a mutable extension to ReadOnlyMatrix3f. When returned as a
 * ReadOnlyMatrix3f is will function as if it is read-only. However, it exposes
 * a number of ways to modify its four components. Any changes to its component
 * values will then be reflected in the accessors defined in ReadOnlyMatrix3f.
 * </p>
 * <p>
 * In the majority of cases, encountered ReadOnlyMatrix3f's are likely to be
 * Matrix3f's but it should not be considered safe to downcast because certain
 * functions within ReadOnlyMatrix4f can return ReadOnlyMatrix3fs that use a
 * different data source.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Matrix3f extends ReadOnlyMatrix3f implements Cloneable {
    public final float[] m = new float[9];

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
     * @throws NullPointerException if o is null
     */
    public Matrix3f(ReadOnlyMatrix3f o) {
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
     * @throws NullPointerException if values is null
     * @throws ArrayIndexOutOfBoundsException if values does not contain 9
     *             elements starting at offset
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
     * As {@link #add(ReadOnlyMatrix3f, Matrix3f)} where result is this matrix
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix3f add(ReadOnlyMatrix3f r) {
        return add(r, this);
    }

    /**
     * As {@link #add(float, Matrix3f)} where result is this matrix
     * 
     * @param c
     * @return This matrix
     */
    public Matrix3f add(float c) {
        return add(c, this);
    }

    /**
     * Inverse this matrix in place, equivalent to {@link #inverse(Matrix3f)}
     * where result is this matrix
     * 
     * @return This matrix
     * @throws ArithmeticException if this matrix isn't invertible
     */
    public Matrix3f inverse() {
        return inverse(this);
    }

    /**
     * As {@link #mul(ReadOnlyMatrix3f, Matrix3f)} where result is this matrix
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix3f mul(ReadOnlyMatrix3f r) {
        return mul(r, this);
    }

    /**
     * As {@link #mulDiagonal(ReadOnlyVector3f, Matrix3f)} where result is this
     * matrix
     * 
     * @param diag
     * @return This matrix
     * @throws NullPointerException if diag is null
     */
    public Matrix3f mulDiagonal(ReadOnlyVector3f diag) {
        return mulDiagonal(diag, this);
    }

    /**
     * As {@link #mulTransposeBoth(ReadOnlyMatrix3f, Matrix3f)} where result is
     * this matrix
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix3f mulTransposeBoth(ReadOnlyMatrix3f r) {
        return mulTransposeBoth(r, this);
    }

    /**
     * As {@link #mulTransposeLeft(ReadOnlyMatrix3f, Matrix3f)} where result is
     * this matrix
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix3f mulTransposeLeft(ReadOnlyMatrix3f r) {
        return mulTransposeLeft(r, this);
    }

    /**
     * As {@link #mulTransposeRight(ReadOnlyMatrix3f, Matrix3f)} where result is
     * this matrix
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public Matrix3f mulTransposeRight(ReadOnlyMatrix3f r) {
        return mulTransposeRight(r, this);
    }

    /**
     * As {@link #scale(float, Matrix3f)} where result is this matrix
     * 
     * @param scalar
     * @return This matrix
     */
    public Matrix3f scale(float scalar) {
        return scale(scalar, this);
    }

    /**
     * Transpose this matrix in place, equivalent to
     * {@link #transpose(Matrix3f)} where result is this matrix.
     * 
     * @return This matrix
     */
    public Matrix3f transpose() {
        return transpose(this);
    }

    /**
     * Set this Matrix3f to be the rotation matrix representing the same
     * rotation stored by <tt>q</tt>.
     * 
     * @param q The quaternion to convert to matrix form
     * @return This matrix for chaining
     * @throws NullPointerException if q is null
     * @throws ArithmeticException if the length of q is 0
     */
    public Matrix3f set(ReadOnlyQuat4f q) {
        float d = q.lengthSquared();
        if (d == 0f)
            throw new ArithmeticException("Quaternion length is 0");
        
        float s = 2f / d;
        
        float xs = q.getX() * s,  ys = q.getY() * s, zs = q.getZ() * s;
        float wx = q.getW() * xs, wy = q.getW() * ys, wz = q.getW() * zs;
        float xx = q.getX() * xs, xy = q.getX() * ys, xz = q.getX() * zs;
        float yy = q.getY() * ys, yz = q.getY() * zs, zz = q.getZ() * zs;
        
        return set(1f - (yy + zz), xy - wz, xz + wy,
                   xy + wz, 1f - (xx + zz), yz - wx,
                   xz - wy, yz + wx, 1f - (xx + yy));
    }

    @Override
    public float get(int row, int col) {
        return m[col * 3 + row];
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
        m[col * 3 + row] = value;
        return this;
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
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 3 elements
     *             starting at offset
     * @throws IndexOutOfBoundsException if col is invalid
     */
    public Matrix3f setCol(int col, float[] values, int offset) {
        m[col * 3 + 0] = values[offset];
        m[col * 3 + 1] = values[offset + 1];
        m[col * 3 + 2] = values[offset + 2];
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
    public Matrix3f setCol(int col, ReadOnlyVector3f values) {
        m[col * 3 + 0] = values.getX();
        m[col * 3 + 1] = values.getY();
        m[col * 3 + 2] = values.getZ();
        return this;
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
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 3 elements
     *             starting at offset
     * @throws IndexOutOfBoundsException if row is invalid
     */
    public Matrix3f setRow(int row, float[] values, int offset) {
        m[0 + row] = values[offset];
        m[3 + row] = values[offset + 1];
        m[6 + row] = values[offset + 2];
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
    public Matrix3f setRow(int row, ReadOnlyVector3f values) {
        m[0 + row] = values.getX();
        m[3 + row] = values.getY();
        m[6 + row] = values.getZ();
        return this;
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
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 9 values
     *             starting at offset
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
        m[0] = m00;
        m[1] = m10;
        m[2] = m20;
        
        m[3] = m01;
        m[4] = m11;
        m[5] = m21;
        
        m[6] = m02;
        m[7] = m12;
        m[8] = m22;

        return this;
    }

    /**
     * Set this matrix to be equal to the given matrix, o.
     * 
     * @param o Matrix whose values are copied into this matrix
     * @return This matrix
     * @throws NullPointerException if o is null
     */
    public Matrix3f set(ReadOnlyMatrix3f o) {
        return set(o.get(0, 0), o.get(0, 1), o.get(0, 2), 
                   o.get(1, 0), o.get(1, 1), o.get(1, 2), 
                   o.get(2, 0), o.get(2, 1), o.get(2, 2));
    }

    /**
     * Reset this matrix's values so that it represents the identity matrix.
     * 
     * @return This matrix
     */
    public Matrix3f setIdentity() {
        return set(1, 0, 0, 0, 1, 0, 0, 0, 1);
    }
}
