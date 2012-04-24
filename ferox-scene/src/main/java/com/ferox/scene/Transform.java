package com.ferox.scene;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.entreri.Matrix4Property;
import com.ferox.math.entreri.Matrix4Property.DefaultMatrix4;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.Unmanaged;

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
public final class Transform extends ComponentData<Transform> {
    /**
     * The shared TypedId representing Transform.
     */
    public static final TypeId<Transform> ID = TypeId.get(Transform.class);
    
    // the identity matrix
    @DefaultMatrix4(m00=1.0, m01=0.0, m02=0.0, m03=0.0,
                    m10=0.0, m11=1.0, m12=0.0, m13=0.0,
                    m20=0.0, m21=0.0, m22=1.0, m23=0.0,
                    m30=0.0, m31=0.0, m32=0.0, m33=1.0)
    private Matrix4Property matrix;
    
    @Unmanaged
    private final Matrix4 cache = new Matrix4();
    
    private Transform() { }

    /**
     * Copy the given transform matrix into this Transform's matrix.
     * 
     * @param m The new affine transform
     * @return This Transform for chaining purposes
     * @throws NullPointerException if m is null
     */
    public Transform setMatrix(@Const Matrix4 m) {
        // set the cache to m as well so that it is always valid
        cache.set(m);
        matrix.set(m, getIndex());
        return this;
    }

    /**
     * Return the matrix of this Transform. The returned Matrix4 instance
     * is reused by this Transform instance so it should be cloned before
     * changing which Component is referenced
     * 
     * @return The current world affine transform matrix
     */
    public @Const Matrix4 getMatrix() {
        return cache;
    }
    
    @Override
    protected void onSet(int index) {
        matrix.get(index, cache);
    }
}
