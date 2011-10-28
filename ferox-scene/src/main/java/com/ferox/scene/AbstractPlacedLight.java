package com.ferox.scene;

import com.ferox.math.ReadOnlyColor3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

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
    private float falloffDistance;
    private final Vector3f position;

    /**
     * Create a new light that uses the given color, position, and falloff.
     * 
     * @param color The starting color
     * @param position The starting position
     * @param falloff The starting falloff
     * @throws NullPointerException if color or position are null
     */
    protected AbstractPlacedLight(ReadOnlyColor3f color, ReadOnlyVector3f position, float falloff) {
        super(color);
        this.position = new Vector3f();
        
        setFalloffDistance(falloff);
        setPosition(position);
    }
    
    /**
     * Cloning constructor
     * @param clone The clone
     */
    protected AbstractPlacedLight(T clone) {
        super(clone);
        falloffDistance = clone.falloffDistance;
        position = new Vector3f(clone.position);
    }

    /**
     * Return the local position of this light with respect to the owning
     * entity's coordinate space. If the entity has no defined coordinate space,
     * its in world space.
     * 
     * @return The position of the light
     */
    public final ReadOnlyVector3f getPosition() {
        return position;
    }

    /**
     * Copy <tt>pos</tt> into this component's position vector. Anything
     * monitoring the instance returned by {@link #getPosition()} will see the
     * changes. The provided position is in the entity's local space, which is
     * usually defined by a {@link Transform} component.
     * 
     * @param pos The new position
     * @return The new version of the component
     * @throws NullPointerException if pos is null
     */
    public final int setPosition(ReadOnlyVector3f pos) {
        if (pos == null)
            throw new NullPointerException("Position cannot be null");
        position.set(pos);
        return notifyChange();
    }

    /**
     * Set the distance to where the light's energy has fallen to zero and no
     * longer contributes to the lighting of a scene. If this is negative, the
     * light has no energy falloff and all lit objects will be lit with the same
     * energy. When enabled, a light's energy falls off acoording to an inverse
     * square law based on the distance to an object.
     * 
     * @param distance The new falloff distance
     * @return The new version of the light, via {@link #notifyChange()}
     */
    public final int setFalloffDistance(float distance) {
        // No argument checking, a negative distance disables
        // light falloff so every value is supported
        falloffDistance = distance;
        return notifyChange();
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
        return falloffDistance;
    }
}
