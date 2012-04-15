package com.ferox.scene;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.entreri.Vector3Property;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.annot.Factory;
import com.lhkbob.entreri.annot.Unmanaged;
import com.lhkbob.entreri.property.AbstractPropertyFactory;

/**
 * <p>
 * DirectionLight represents an direction light (or infinite point light), where
 * objects see light coming from the same direction, regardless of their
 * position. An example of a direction light is the sun. Combining a direction
 * light with a transform can be used to transform its direction vector, but any
 * translation will have no affect on the lighting equations. It is possible
 * that a position transform on a DirectionLight could be used to limit its
 * influence, but this all depends on the components used and the controller
 * implementations.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class DirectionLight extends Light<DirectionLight> {
    /**
     * The shared TypedId representing DirectionLight.
     */
    public static final TypeId<DirectionLight> ID = TypeId.get(DirectionLight.class);
    
    @Factory(DefaultDirectionFactory.class)
    private Vector3Property direction;
    
    @Unmanaged
    private final Vector3 dirCache = new Vector3();

    private DirectionLight() { }
    
    /**
     * Set the direction of this light. This copies <tt>dir</tt>, so any future
     * changes to the input vector will not affect this DirectionLight. If this
     * DirectionLight is in an Entity with a transform or rotation-based
     * component, the direction should be interpreted as a local vector.
     * 
     * @param dir The new light direction, before being transformed by any
     *            transform component (if one exists on the owner)
     * @return The new version, via {@link #notifyChange()}
     * @throws NullPointerException if dir is null
     * @throws ArithmeticException if dir cannot be normalized
     */
    public DirectionLight setDirection(@Const Vector3 dir) {
        if (dir == null)
            throw new NullPointerException("Direction vector cannot be null");
        dirCache.set(dir);
        direction.set(dir.normalize(null), getIndex());
        return this;
    }

    /**
     * Get the normalized direction vector for this light. If this
     * DirectionLight is combined with another transform, this vector should be
     * transformed by it before used in lighting calculations. The returned
     * Vector3 instance is reused by this Light instance so it should be cloned
     * before changing which Component is referenced.
     * 
     * @return The normalized direction vector
     */
    public @Const Vector3 getDirection() {
        return dirCache;
    }
    
    @Override
    protected void onSet(int index) {
        direction.get(index, dirCache);
    }
    
    private static class DefaultDirectionFactory extends AbstractPropertyFactory<Vector3Property> {
        public static final Vector3 DEFAULT_DIR = new Vector3(0, 0, 1);
        
        @Override
        public Vector3Property create() {
            return new Vector3Property();
        }

        @Override
        public void setDefaultValue(Vector3Property property, int index) {
            property.set(DEFAULT_DIR, index);
        }
    }
}
