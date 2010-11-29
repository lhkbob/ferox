package com.ferox.math;

/**
 * <p>
 * Transform is a special 4x4 matrix that provides fast access to the
 * translation and rotation components. The translation is stored in a
 * {@link Vector3f} and the rotation is stored in a {@link Matrix3f}.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Transform extends MutableMatrix4f {
    private final Vector3f translation;
    private final Matrix3f rotation;

    /** Create an identity transform. */
    public Transform() {
        translation = new Vector3f();
        rotation = new Matrix3f();
    }

    /**
     * Create a transform with no rotation, and the given translation.
     * 
     * @param trans The translation vector to use
     * @throws NullPointerException if trans is null
     */
    public Transform(ReadOnlyVector3f trans) {
        this();
        translation.set(trans);
    }

    /**
     * Create a transform with the given translation and rotation.
     * 
     * @param trans The translation vector to use
     * @param rot The 3x3 matrix representing the orientation
     * @throws NullPointerException if trans or rot are null
     */
    public Transform(ReadOnlyVector3f trans, ReadOnlyMatrix3f rot) {
        this();
        translation.set(trans);
        rotation.set(rot);
    }

    /**
     * Create a transform that takes its initial values from the given affine
     * 4x4 matrix transform.
     * 
     * @param t The matrix to copy
     * @throws NullPointerException if t is null
     */
    public Transform(ReadOnlyMatrix4f t) {
        this();
        set(t);
    }

    /**
     * @return The mutable vector representing the translation component of this
     *         4x4 matrix. Any changes will be reflected in this Transform.
     */
    public Vector3f getTranslation() {
        return translation;
    }

    /**
     * @return The upper 3x3 rotation/scale matrix of this 4x4 matrix. Any
     *         changes will be reflected in this Transform.
     */
    public Matrix3f getRotation() {
        return rotation;
    }
    
    @Override
    public ReadOnlyMatrix3f getUpperMatrix() {
        // we don't need to create a special wrapper matrix
        return rotation;
    }
    
    /*@Override
    public MutableMatrix4f inverse(MutableMatrix4f result) {
        if (result == null)
            result = new Transform();
        
        if (result instanceof Transform) {
            // fast path for inverting Transforms
            Transform t = (Transform) result;
            rotation.transpose(t.rotation);
            translation.scale(-1f, t.translation);
            return t;
        } else {
            return result.set(rotation.get(0, 0), rotation.get(1, 0), rotation.get(2, 0), -translation.getX(),
                              rotation.get(0, 1), rotation.get(1, 1), rotation.get(2, 1), -translation.getY(), 
                              rotation.get(0, 2), rotation.get(1, 2), rotation.get(2, 2), -translation.getZ(), 
                              0f, 0f, 0f, 1f);
        }
    } */
    
    @Override
    public float get(int row, int col) {
        if (row == 3) {
            // last row is either 0 or 1
            return (col == 3 ? 1f : 0f);
        }
        
        if (col == 3) {
            // translation
            return translation.get(row);
        } else {
            // rotation
            return rotation.get(row, col);
        }
    }
    
    @Override
    public Transform set(int row, int col, float value) {
        if (row == 3) {
            // bottom row is immutable so just ignore it
            return this;
        }
        
        if (col == 3) {
            // translation
            translation.set(row, value);
            return this;
        } else {
            // rotation
            rotation.set(row, col, value);
            return this;
        }
    }

    @Override
    public Transform set(float m00, float m01, float m02, float m03, 
                         float m10, float m11, float m12, float m13, 
                         float m20, float m21, float m22, float m23,
                         float m30, float m31, float m32, float m33) {
        translation.set(m03, m13, m23);
        rotation.set(m00, m01, m02,
                     m10, m11, m12,
                     m20, m21, m22);
        // ignore m30, m31, m32 and m33
        return this;
    }
}