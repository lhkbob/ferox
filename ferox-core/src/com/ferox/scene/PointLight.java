package com.ferox.scene;

import com.ferox.entity.Template;
import com.ferox.math.ReadOnlyColor4f;

/**
 * <p>
 * A PointLight is a type of light where light is emitted equally in all
 * directions from a specific position. In many ways it is equivalent to a
 * {@link SpotLight} if the spot light had a cutoff angle equal to 180 degrees
 * (effectively turning the cone into a sphere and removing the direction).
 * </p>
 * <p>
 * A PointLight must be combined with some form of transform component, or other
 * provider of position, to place a PointLight within the scene. This functions
 * identically to how SpotLight and DirectionLight can be modified by a
 * transform to position them within a scene.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class PointLight extends Light<PointLight> {
    private float falloffDistance;

    /**
     * Create a new PointLight with the given color. Energy falloff is initially
     * disabled.
     * 
     * @param color The starting color
     * @throws NullPointerException if color is null
     */
    public PointLight(ReadOnlyColor4f color) {
        this(color, -1f);
    }

    /**
     * Create a new PointLight with the given color and falloff distance. See
     * {@link #setFalloffDistance(float)} for details on energy falloff.
     * 
     * @param color The starting color
     * @param falloffDistance The initial falloff distance
     * @throws NullPointerException if color is null
     */
    public PointLight(ReadOnlyColor4f color, float falloffDistance) {
        setColor(color);
        setFalloffDistance(falloffDistance);
    }

    /**
     * Create a new PointLight that is a clone of <tt>clone</tt> for use with a
     * {@link Template}.
     * 
     * @param clone The light to clone
     * @throws NullPointerException if clone is null
     */
    public PointLight(PointLight clone) {
        super(clone);
        this.falloffDistance = clone.falloffDistance;
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
    public int setFalloffDistance(float distance) {
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
    public float getFalloffDistance() {
        return falloffDistance;
    }
}
