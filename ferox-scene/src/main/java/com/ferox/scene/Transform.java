package com.ferox.scene;

import com.ferox.math.Matrix4f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.entreri.Matrix4fProperty;
import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.TypedId;

/**
 * <p>
 * Transform represents an affine transform that transforms an Entity from its
 * local coordinate space into a coordinate space shared by all Entities within
 * a system (i.e. the world). This can be used to place lights, physics objects,
 * or objects to be rendered.
 * </p>
 * <p>
 * Transform does not define any initialization parameters.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Transform extends Component {
    /**
     * The shared TypedId representing Transform.
     */
    public static final TypedId<Transform> ID = Component.getTypedId(Transform.class);
    
    public static final ReadOnlyMatrix4f DEFAULT_TRANSFORM = new Matrix4f().setIdentity();
    
    private Matrix4fProperty matrix;
    
    private Transform(EntitySystem system, int index) {
        super(system, index);
    }

    @Override
    protected void init(Object... initParams) {
        setMatrix(DEFAULT_TRANSFORM);
    }

    /**
     * Copy the given transform matrix into this Transform's matrix.
     * 
     * @param m The new affine transform
     * @return This Transform for chaining purposes
     * @throws NullPointerException if m is null
     */
    public Transform setMatrix(ReadOnlyMatrix4f m) {
        matrix.set(m, getIndex());
        return this;
    }

    /**
     * Return the matrix of this Transform. The returned matrix is a cached
     * instance shared within the component's EntitySystem, so it should be
     * cloned before accessing another component of this type.
     * 
     * @return The current world affine transform matrix
     */
    public ReadOnlyMatrix4f getMatrix() {
        return matrix.get(getIndex());
    }
}
