package com.ferox.scene;

import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;

/**
 * <p>
 * An intermediate light type that is shared by {@link PointLight} and
 * {@link SpotLight}. Although it is common to represent a point and spot light
 * as the same object (such as by using a cutoff angle of 180 degrees to
 * represent a point light), it was decided to have them be separate components
 * because they are often treated fundamentally different in rendering engines.
 * </p>
 * <p>
 * Placed lights should be combined with a {@link Transform} component to
 * control the position (and possibly direction). A placed light without a
 * transform defaults to the default transform.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The component light type
 */
public abstract class AbstractPlacedLight<T extends AbstractPlacedLight<T>> extends Light<T> {
    @DefaultDouble(-1.0)
    private DoubleProperty falloffDistance;

    protected AbstractPlacedLight() { }

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
}
