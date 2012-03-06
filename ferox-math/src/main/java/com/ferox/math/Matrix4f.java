package com.ferox.math;

/**
 * Matrix4f is the default complete implementation of a 4x4 matrix. It can be
 * used as both a mutable and as a read-only matrix. This implementation stores
 * its data in a 16 element float array. {@link AffineTransform} is another 4x4 matrix
 * implementation that separates the rotation and translation so it can more
 * easily represent an affine transform.
 * 
 * @author Michael Ludwig
 */
public final class Matrix4f extends MutableMatrix4f {
    private final float[] m = new float[16];

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
     * @throws NullPointerException if o is null
     */
    public Matrix4f(ReadOnlyMatrix4f o) {
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
     * @throws NullPointerException if values is null
     * @throws ArrayIndexOutOfBoundsException if values doesn't contain 16
     *             elements starting at offset
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
    
    @Override
    public float get(int row, int col) {
        return m[(col << 2) + row];
    }

    @Override
    public MutableMatrix4f set(int row, int col, float value) {
        m[(col << 2) + row] = value;
        return this;
    }

    @Override
    public MutableMatrix4f set(float m00, float m01, float m02, float m03, 
                               float m10, float m11, float m12, float m13, 
                               float m20, float m21, float m22, float m23,
                               float m30, float m31, float m32, float m33) {
        m[0] = m00;
        m[1] = m10;
        m[2] = m20;
        m[3] = m30;
        
        m[4] = m01;
        m[5] = m11;
        m[6] = m21;
        m[7] = m31;
        
        m[8] = m02;
        m[9] = m12;
        m[10] = m22;
        m[11] = m32;
        
        m[12] = m03;
        m[13] = m13;
        m[14] = m23;
        m[15] = m33;
        
        return this;
    }
}
