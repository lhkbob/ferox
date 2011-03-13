package com.ferox.scene;

import com.ferox.entity.Template;
import com.ferox.math.Color4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

/**
 * <p>
 * SpotLight is a light that shines light in a cone along a specific direction,
 * with the origin of the light (or apex of the cone) located at a specific
 * position. The size of the cone can be configured with a cutoff angle that
 * describes how wide or narrow the cone is.
 * </p>
 * <p>
 * It is intended that SpotLight be combined with a transform component to
 * provide the position and direction information. Like {@link DirectionLight},
 * though, it does provide a direction vector that represents the local
 * direction before being transformed by any transform component. This is
 * provided to make it easier to manipulate the direction of the light.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class SpotLight extends Light<SpotLight> {
    private final Vector3f direction;
    private float cutoffAngle;
    private float falloffDistance;

    /**
     * Create a new SpotLight that uses the given color, a cutoff angle of 30
     * degrees, and a local direction vector of (0, 0, 1). Energy falloff is
     * disabled.
     * 
     * @param color The starting color
     * @throws NullPointerException if color is null
     */
    public SpotLight(Color4f color) {
        this(color, 30f);
    }

    /**
     * Create a new SpotLight that uses the given color and cutoff angle, and a
     * local direction vector of (0, 0, 1). Energy falloff is disabled.
     * 
     * @param color The starting color
     * @param cutoffAngle The starting cutoff angle, in degrees in the range [0,
     *            90]
     * @throws NullPointerException if color is null
     * @throws IllegalArgumentException if cutoffAngle is not in [0, 90]
     */
    public SpotLight(Color4f color, float cutoffAngle) {
        this(color, cutoffAngle, new Vector3f(0f, 0f, 1f));
    }

    /**
     * Create a new SpotLight that uses the given color, cutoff angle, and local
     * direction vector. Energy falloff is disabled.
     * 
     * @param color The color
     * @param cutoffAngle The cutoff angle, in degrees in the range [0, 90]
     * @param direction The local direction vector
     * @throws NullPointerException if color or direction are null
     * @throws IllegalArgumentException if cutoffAngle is not in [0, 90]
     * @throws ArithmeticException if direction cannot be normalized
     */
    public SpotLight(Color4f color, float cutoffAngle, ReadOnlyVector3f direction) {
        this.direction = new Vector3f();
        setColor(color);
        setDirection(direction);
        setCutoffAngle(cutoffAngle);
        setFalloffDistance(-1f);
    }

    /**
     * Create a new SpotLight that is a clone of <tt>clone</tt> for use with a
     * {@link Template}.
     * 
     * @param clone The SpotLight to cloneF
     */
    public SpotLight(SpotLight clone) {
        super(clone);
        direction = new Vector3f(clone.direction);
        cutoffAngle = clone.cutoffAngle;
        falloffDistance = clone.falloffDistance;
    }

    /**
     * Return the cutoff angle, in degrees, representing the maximum angle light
     * will spread from the {@link #getDirection() direction vector}. This
     * creates a cone of light that is fat or thin depending on if the angle is
     * large or small.
     * 
     * @return The cutoff angle in degrees, will be in [0, 90]
     */
    public float getCutoffAngle() {
        return cutoffAngle;
    }

    /**
     * Set the cutoff angle for this SpotLight. The cutoff angle is the maximum
     * angle, in degrees, from the {@link #getDirection() direction vector} that
     * will be affected by the light. Thus an angle value of 90 would create a
     * half-space that is lit.
     * 
     * @param angle The new cutoff angle, in [0, 90]
     * @return The new version of the light, via {@link #notifyChange()}
     * @throws IllegalArgumentException if angle is not between 0 and 90
     */
    public int setCutoffAngle(float angle) {
        if (angle < 0 || angle > 90)
            throw new IllegalArgumentException("Illegal cutoff angle, must be in [0, 90], not: " + angle);
        cutoffAngle = angle;
        return notifyChange();
    }

    /**
     * Set the direction of this light. This copies <tt>dir</tt>, so any future
     * changes to the input vector will not affect this SpotLight. If this
     * SpotLight is in an Entity with a transform or rotation-based component,
     * the direction should be interpreted as a local vector.
     * 
     * @param dir The new light direction, before being transformed by any
     *            transform component (if one exists on the owner)
     * @return The new version, via {@link #notifyChange()}
     * @throws NullPointerException if dir is null
     * @throws ArithmeticException if dir cannot be normalized
     */
    public int setDirection(ReadOnlyVector3f dir) {
        if (dir == null)
            throw new NullPointerException("Direction vector cannot be null");
        dir.normalize(direction);
        return notifyChange();
    }

    /**
     * Get the normalized direction vector for this light. The returned
     * instance's values will be updated in response to
     * {@link #setDirection(ReadOnlyVector3f)}. If this SpotLight is combined
     * with another transform, this vector should be transformed by it before
     * used in lighting calculations.
     * 
     * @return The normalized direction vector
     */
    public ReadOnlyVector3f getDirection() {
        return direction;
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
