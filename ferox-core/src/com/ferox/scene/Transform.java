package com.ferox.scene;

import com.ferox.entity.Entity;
import com.ferox.entity.Template;
import com.ferox.entity.TypedComponent;
import com.ferox.math.AffineTransform;
import com.ferox.math.ReadOnlyMatrix3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;

/**
 * <p>
 * Transform represents an affine transform that transforms an Entity from its
 * local coordinate space into a coordinate space shared by all Entities within
 * a system (i.e. the world). This can be used to place lights, physics objects,
 * or objects to be rendered.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Transform extends TypedComponent<Transform> {
    private final AffineTransform affineTransform;
    
    /**
     * Create a Transform component starting with the identity transform.
     */
    public Transform() {
        super(null, false);
        affineTransform = new AffineTransform();
    }

    /**
     * Create a new Transform starting with the given 4x4 affine matrix.
     * 
     * @param transform The initial transform
     * @throws NullPointerException if transform is null
     */
    public Transform(ReadOnlyMatrix4f transform) {
        this();
        setMatrix(transform);
    }

    /**
     * Create a new Transform that is a clone of <tt>clone</tt>, for use with a
     * {@link Template}.
     * 
     * @param clone The Transform to clone
     * @throws NullPointerException if clone is null
     */
    public Transform(Transform clone) {
        super(clone, true);
        affineTransform = new AffineTransform(clone.affineTransform);
    }

    /**
     * Copy the given rotation matrix into this Transform's upper 3x3 matrix.
     * 
     * @param m The new rotation matrix
     * @return The new version of the Transform, via {@link #notifyChange()}
     * @throws NullPointerException if m is null
     */
    public int setRotation(ReadOnlyMatrix3f m) {
        affineTransform.getRotation().set(m);
        return notifyChange();
    }

    /**
     * Copy the given translation into the 4th column of the 4x4 matrix of this
     * Transform's affine matrix.
     * 
     * @param t The new translation
     * @return The new version of the Transform, via {@link #notifyChange()}
     * @throws NullPointerException if t is null
     */
    public int setTranslation(ReadOnlyVector3f t) {
        affineTransform.getTranslation().set(t);
        return notifyChange();
    }

    /**
     * Copy the given transform matrix into this Transform's matrix
     * 
     * @param m The new affineTransform
     * @return The new version of the Transform, via {@link #notifyChange()}
     * @throws NullPointerException if m is null
     */
    public int setMatrix(ReadOnlyMatrix4f m) {
        affineTransform.set(m);
        return notifyChange();
    }

    /**
     * Return the matrix that stores the actual world transform. This instance
     * will not change and will reflect any changes to the Transform. Use
     * {@link Entity#getVersion(com.ferox.entity.TypedId)} to determine if it
     * has been modified.
     * 
     * @return The current world affineTransform matrix
     */
    public ReadOnlyMatrix4f getMatrix() {
        return affineTransform;
    }
}
