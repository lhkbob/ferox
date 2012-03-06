package com.ferox.scene;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.math.entreri.Vector3fProperty;
import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.TypedId;
import com.googlecode.entreri.property.FloatProperty;

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
 * <p>
 * SpotLight does not define any initialization parameters.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class SpotLight extends AbstractPlacedLight<SpotLight> {
    /**
     * The shared TypedId representing SpotLight.
     */
    public static final TypedId<SpotLight> ID = Component.getTypedId(SpotLight.class);
    
    public static final ReadOnlyVector3f DEFAULT_DIRECTION = new Vector3f(0f, 0f, 1f);
    
    private Vector3fProperty direction;
    private FloatProperty cutoffAngle;

    private SpotLight(EntitySystem system, int index) {
        super(system, index);
    }
    
    @Override
    protected void init(Object... initParams) {
        super.init(initParams);
        setDirection(DEFAULT_DIRECTION);
        setCutoffAngle(30f);
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
        return cutoffAngle.get(getIndex(), 0);
    }

    /**
     * Set the cutoff angle for this SpotLight. The cutoff angle is the maximum
     * angle, in degrees, from the {@link #getDirection() direction vector} that
     * will be affected by the light. Thus an angle value of 90 would create a
     * half-space that is lit.
     * 
     * @param angle The new cutoff angle, in [0, 90]
     * @return This light for chaining purposes
     * @throws IllegalArgumentException if angle is not between 0 and 90
     */
    public SpotLight setCutoffAngle(float angle) {
        if (angle < 0 || angle > 90)
            throw new IllegalArgumentException("Illegal cutoff angle, must be in [0, 90], not: " + angle);
        cutoffAngle.set(angle, getIndex(), 0);
        return this;
    }

    /**
     * Set the direction of this light. This copies <tt>dir</tt>, so any future
     * changes to the input vector will not affect this SpotLight. If this
     * SpotLight is in an Entity with a transform or rotation-based component,
     * the direction should be interpreted as a local vector.
     * 
     * @param dir The new light direction, before being transformed by any
     *            transform component (if one exists on the owner)
     * @return This light for chaining purposes
     * @throws NullPointerException if dir is null
     * @throws ArithmeticException if dir cannot be normalized
     */
    public SpotLight setDirection(ReadOnlyVector3f dir) {
        if (dir == null)
            throw new NullPointerException("Direction vector cannot be null");
        direction.set(dir.normalize(null), getIndex());
        return this;
    }

    /**
     * Get the normalized direction vector for this light. If this SpotLight is
     * combined with another transform, this vector should be transformed by it
     * before used in lighting calculations. The returned vector is a cached
     * instance shared within the component's EntitySystem, so it should be
     * cloned before accessing another component of this type.
     * 
     * @return The normalized direction vector
     */
    public ReadOnlyVector3f getDirection() {
        return direction.get(getIndex());
    }

    /**
     * Return the local direction of this light in <tt>store</tt>. If store is
     * null, a new vector is created to hold the direction and returned.
     * 
     * @param store The result vector to hold the direction
     * @return The local direction in store, or a new vector if store was null
     */
    public final MutableVector3f getDirection(MutableVector3f store) {
        return direction.get(getIndex(), store);
    }
}

