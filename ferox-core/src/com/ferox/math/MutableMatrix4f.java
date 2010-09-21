package com.ferox.math;

/**
 * <p>
 * MutableMatrix4f is a mutable extension to ReadOnlyMatrix4f. When returned as
 * a ReadOnlyMatrix4f is will function as if it is read-only. However, it
 * exposes a number of ways to modify its four components. Any changes to its
 * component values will then be reflected in the accessors defined in
 * ReadOnlyMatrix4f.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class MutableMatrix4f extends ReadOnlyMatrix4f {
    /**
     * As {@link #add(ReadOnlyMatrix4f, Matrix4f)} where result is this matrix
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public MutableMatrix4f add(ReadOnlyMatrix4f r) {
        return add(r, this);
    }

    /**
     * As {@link #add(float, Matrix4f)} where result is this matrix
     * 
     * @param c
     * @return This matrix
     */
    public MutableMatrix4f add(float c) {
        return add(c, this);
    }

    /**
     * Invert this matrix in place, equivalent to {@link #inverse(Matrix4f)}
     * where result is this matrix.
     * 
     * @return This matrix
     * @throws ArithmeticException if this matrix isn't invertible
     */
    public MutableMatrix4f inverse() {
        return inverse(this);
    }

    /**
     * As {@link #mul(ReadOnlyMatrix4f, Matrix4f)} where result is this matrix
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public MutableMatrix4f mul(ReadOnlyMatrix4f r) {
        return mul(r, this);
    }

    /**
     * As {@link #mulDiagonal(ReadOnlyVector4f, Matrix4f)} where result is this
     * matrix
     * 
     * @param diag
     * @return This matrix
     * @throws NullPointerException if diag is null
     */
    public MutableMatrix4f mulDiagonal(ReadOnlyVector4f diag) {
        return mulDiagonal(diag, this);
    }

    /**
     * As {@link #mulTransposeBoth(ReadOnlyMatrix4f, Matrix4f)} where result is
     * this matrix
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public MutableMatrix4f mulTransposeBoth(ReadOnlyMatrix4f r) {
        return mulTransposeBoth(r, this);
    }

    /**
     * As {@link #mulTransposeLeft(ReadOnlyMatrix4f, Matrix4f)} where result is
     * this matrix
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public MutableMatrix4f mulTransposeLeft(ReadOnlyMatrix4f r) {
        return mulTransposeLeft(r, this);
    }

    /**
     * As {@link #mulTransposeRight(ReadOnlyMatrix4f, Matrix4f)} where result is
     * this matrix
     * 
     * @param r
     * @return This matrix
     * @throws NullPointerException if r is null
     */
    public MutableMatrix4f mulTransposeRight(ReadOnlyMatrix4f r) {
        return mulTransposeRight(r, this);
    }

    /**
     * As {@link #scale(float, Matrix4f)} where result is this matrix
     * 
     * @param scalar
     * @return This matrix
     */
    public MutableMatrix4f scale(float scalar) {
        return scale(scalar, this);
    }

    /**
     * Transpose this matrix in place, equivalent to
     * {@link #transpose(Matrix4f)} where result is this matrix.
     * 
     * @return This matrix
     */
    public MutableMatrix4f transpose() {
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
    public abstract MutableMatrix4f set(int row, int col, float value);

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
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 4 elements
     *             starting at offset
     * @throws IndexOutOfBoundsException if col is invalid
     */
    public MutableMatrix4f setCol(int col, float[] values, int offset) {
        return set(0, col, values[offset])
              .set(1, col, values[offset + 1])
              .set(2, col, values[offset + 2])
              .set(3, col, values[offset + 3]);
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
    public MutableMatrix4f setCol(int col, ReadOnlyVector4f values) {
        return set(0, col, values.getX())
              .set(1, col, values.getY())
              .set(2, col, values.getZ())
              .set(3, col, values.getW());
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
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 4 elements
     *             starting at offset
     * @throws IndexOutOfBoundsException if row is invalid
     */
    public MutableMatrix4f setRow(int row, float[] values, int offset) {
        return set(row, 0, values[offset])
              .set(row, 1, values[offset + 1])
              .set(row, 2, values[offset + 2])
              .set(row, 3, values[offset + 3]);
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
    public MutableMatrix4f setRow(int row, ReadOnlyVector4f values) {
        return set(row, 0, values.getX())
              .set(row, 1, values.getY())
              .set(row, 2, values.getZ())
              .set(row, 3, values.getW());
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
     * @throws ArrayIndexOutOfBoundsException if values doesn't have 16 elements
     *             starting at offset
     */
    public MutableMatrix4f set(float[] values, int offset, boolean rowMajor) {
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
    public abstract MutableMatrix4f set(float m00, float m01, float m02, float m03, 
                                        float m10, float m11, float m12, float m13, 
                                        float m20, float m21, float m22, float m23, 
                                        float m30, float m31, float m32, float m33);

    /**
     * Set this matrix to be equal to the given matrix, o.
     * 
     * @param o Matrix whose values are copied into this matrix
     * @return This matrix
     * @throws NullPointerException if o is null
     */
    public MutableMatrix4f set(ReadOnlyMatrix4f o) {
        return set(o.get(0, 0), o.get(0, 1), o.get(0, 2), o.get(0, 3), 
                   o.get(1, 0), o.get(1, 1), o.get(1, 2), o.get(1, 3), 
                   o.get(2, 0), o.get(2, 1), o.get(2, 2), o.get(2, 3),
                   o.get(3, 0), o.get(3, 1), o.get(3, 2), o.get(3, 3));
    }

    /**
     * Reset this matrix's values so that it represents the identity matrix.
     * 
     * @return This matrix
     */
    public MutableMatrix4f setIdentity() {
        return set(1, 0, 0, 0, 
                   0, 1, 0, 0, 
                   0, 0, 1, 0,
                   0, 0, 0, 1);
    }
}
