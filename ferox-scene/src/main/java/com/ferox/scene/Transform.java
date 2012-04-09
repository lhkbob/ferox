package com.ferox.scene;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.entreri.Matrix4Property;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.annot.Factory;
import com.lhkbob.entreri.annot.Unmanaged;
import com.lhkbob.entreri.property.AbstractPropertyFactory;

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
public final class Transform extends ComponentData<Transform> {
    /**
     * The shared TypedId representing Transform.
     */
    public static final TypeId<Transform> ID = TypeId.get(Transform.class);
    
    @Factory(IdentityPropertyFactory.class)
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
    
    private static class IdentityPropertyFactory extends AbstractPropertyFactory<Matrix4Property> {
        private static final Matrix4 IDENTITY = new Matrix4().setIdentity();
        
        @Override
        public Matrix4Property create() {
            return new Matrix4Property();
        }

        @Override
        public void setDefaultValue(Matrix4Property property, int index) {
            property.set(IDENTITY, index);
        }
    }
}
