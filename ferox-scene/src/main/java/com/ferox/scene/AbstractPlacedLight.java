package com.ferox.scene;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.entreri.Vector3Property;
import com.lhkbob.entreri.Unmanaged;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;

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
    @DefaultDouble(-1.0)
    private DoubleProperty falloffDistance;
    
    private Vector3Property position;
    
    @Unmanaged
    private final Vector3 posCache = new Vector3();

    protected AbstractPlacedLight() { }

    /**
     * Return the local position of this light with respect to the owning
     * entity's coordinate space. If the entity has no defined coordinate space,
     * its in world space. The returned Vector3 instance
     * is reused by this Light instance so it should be cloned before
     * changing which Component is referenced
     * 
     * @return The position of the light
     */
    public final @Const Vector3 getPosition() {
        return posCache;
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
    public final T setPosition(@Const Vector3 pos) {
        if (pos == null)
            throw new NullPointerException("Position cannot be null");
        posCache.set(pos);
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
    public final T setFalloffDistance(double distance) {
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
    public final double getFalloffDistance() {
        return falloffDistance.get(getIndex(), 0);
    }
    
    @Override
    protected void onSet(int index) {
        position.get(index, posCache);
    }
}
