package com.ferox.scene;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.math.entreri.Vector3fProperty;
import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.TypedId;

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
 * <p>
 * DirectionLight does not have any initialization parameters.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class DirectionLight extends Light<DirectionLight> {
    /**
     * The shared TypedId representing DirectionLight.
     */
    public static final TypedId<DirectionLight> ID = Component.getTypedId(DirectionLight.class);
    
    public static final ReadOnlyVector3f DEFAULT_DIRECTION = new Vector3f(0f, 0f, 1f);
    
    private Vector3fProperty direction;

    private DirectionLight(EntitySystem system, int index) {
        super(system, index);
    }
    
    @Override
    protected void init(Object... initParams) {
        super.init(initParams);
        setDirection(DEFAULT_DIRECTION);
    }

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
    public DirectionLight setDirection(ReadOnlyVector3f dir) {
        if (dir == null)
            throw new NullPointerException("Direction vector cannot be null");
        direction.set(dir.normalize(null), getIndex());
        return this;
    }

    /**
     * Get the normalized direction vector for this light. If this
     * DirectionLight is combined with another transform, this vector should be
     * transformed by it before used in lighting calculations. The returned
     * vector is a cached instance shared within the component's EntitySystem,
     * so it should be cloned before accessing another component of this type.
     * 
     * @return The normalized direction vector
     */
    public ReadOnlyVector3f getDirection() {
        return direction.get(getIndex());
    }
}
