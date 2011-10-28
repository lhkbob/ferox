package com.ferox.math;

/**
 * Matrix3f is the default complete implementation of a 3x3 matrix. It can be
 * used as a mutable matrix and as a read-only matrix depending on how it is
 * exposed. It stores its values in a float array of length 9.
 * 
 * @author Michael Ludwig
 */
public final class Matrix3f extends MutableMatrix3f {
    private final float[] m = new float[9];

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
    
    @Override
    public float get(int row, int col) {
        return m[col * 3 + row];
    }

    @Override
    public MutableMatrix3f set(int row, int col, float value) {
        m[col * 3 + row] = value;
        return this;
    }

    @Override
    public MutableMatrix3f set(float m00, float m01, float m02, 
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
}
