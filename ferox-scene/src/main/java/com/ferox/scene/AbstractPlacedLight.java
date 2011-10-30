package com.ferox.scene;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.math.entreri.Vector3fProperty;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.property.FloatProperty;

/**
 * An intermediate light type that is shared by {@link PointLight} and
 * {@link SpotLight}. Although it is common to represent a point and spot light
 * as the same object (such as by using a cutoff angle of 180 degrees to
 * represent a point light), it was decided to have them be separate components
 * because they are often treated fundamentally different in rendering engines.
 * 
 * @author Michael Ludwig
 * @param <T> The component light type
 */
public abstract class AbstractPlacedLight<T extends AbstractPlacedLight<T>> extends Light<T> {
    public static final ReadOnlyVector3f DEFAULT_POSITION = new Vector3f();
    
    private FloatProperty falloffDistance;
    private Vector3fProperty position;

    protected AbstractPlacedLight(EntitySystem system, int index) {
        super(system, index);
    }

    @Override
    protected void init(Object... initParams) {
        setFalloffDistance(-1f);
        setPosition(DEFAULT_POSITION);
    }

    /**
     * Return the local position of this light with respect to the owning
     * entity's coordinate space. If the entity has no defined coordinate space,
     * its in world space. The returned vector is a cached instance shared
     * within the component's EntitySystem, so it should be cloned before
     * accessing another component of this type.
     * 
     * @return The position of the light
     */
    public final ReadOnlyVector3f getPosition() {
        return position.get(getIndex());
    }

    /**
     * Copy <tt>pos</tt> into this component's position vector. The provided
     * position is in the entity's local space, which is usually defined by a
     * {@link Transform} component.
     * 
     * @param pos The new position
     * @return This light for chaining purposes
     * @throws NullPointerException if pos is null
     */
    @SuppressWarnings("unchecked")
    public final T setPosition(ReadOnlyVector3f pos) {
        if (pos == null)
            throw new NullPointerException("Position cannot be null");
        position.set(pos, getIndex());
        return (T) this;
    }

    /**
     * Set the distance to where the light's energy has fallen to zero and no
     * longer contributes to the lighting of a scene. If this is negative, the
     * light has no energy falloff and all lit objects will be lit with the same
     * energy. When enabled, a light's energy falls off acoording to an inverse
     * square law based on the distance to an object.
     * 
     * @param distance The new falloff distance
     * @return This light for chaining purposes
     */
    @SuppressWarnings("unchecked")
    public final T setFalloffDistance(float distance) {
        // No argument checking, a negative distance disables
        // light falloff so every value is supported
        falloffDistance.set(distance, getIndex(), 0);
        return (T) this;
    }

    /**
     * Return the distance to where the light's energy has fallen off to zero
     * and no longer contributes to lighting. If this is negative, then the
     * light has no energy falloff. When enabled, a light's energy falls off
     * according to an inverse square law based on the distance to an object
     * 
     * @return The falloff distance
     */
    public final float getFalloffDistance() {
        return falloffDistance.get(getIndex(), 0);
    }
}
