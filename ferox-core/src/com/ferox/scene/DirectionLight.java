package com.ferox.scene;

import com.ferox.entity.Template;
import com.ferox.math.ReadOnlyColor3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

/**
 * DirectionLight represents an direction light (or infinite point light), where
 * objects see light coming from the same direction, regardless of their
 * position. An example of a direction light is the sun. Combining a direction
 * light with a transform can be used to transform its direction vector, but any
 * translation will have no affect on the lighting equations. It is possible
 * that a position transform on a DirectionLight could be used to limit its
 * influence, but this all depends on the components used and the controller
 * implementations.
 * 
 * @author Michael Ludwig
 */
public final class DirectionLight extends Light<DirectionLight> {
    private final Vector3f direction;

    /**
     * Create a new DirectionLight with the given color and an initial direction
     * aligned with the positive z axis.
     * 
     * @param color The starting light color
     * @throws NullPointerException if color is null
     */
    public DirectionLight(ReadOnlyColor3f color) {
        this(color, new Vector3f(0f, 0f, 1f));
    }

    /**
     * Create a new DirectionLight with the given color and direction.
     * 
     * @param color The starting color
     * @param direction The starting direction
     * @throws NullPointerException if color or direction are null
     * @throws ArithmeticException if direction cannot be normalized
     */
    public DirectionLight(ReadOnlyColor3f color, ReadOnlyVector3f direction) {
        this.direction = new Vector3f();
        setDirection(direction);
        setColor(color);
    }

    /**
     * Create a new DirectionLight that is a clone of <tt>clone</tt> for use
     * with a {@link Template}.
     * 
     * @param clone The light to clone
     * @throws NullPointerException if clone is null
     */
    public DirectionLight(DirectionLight clone) {
        super(clone);
        direction = new Vector3f();
        setDirection(clone.direction);
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
    public int setDirection(ReadOnlyVector3f dir) {
        if (dir == null)
            throw new NullPointerException("Direction vector cannot be null");
        dir.normalize(direction);
        return notifyChange();
    }

    /**
     * Get the normalized direction vector for this light. The returned
     * instance's values will be updated in response to
     * {@link #setDirection(ReadOnlyVector3f)}. If this DirectionLight is
     * combined with another transform, this vector should be transformed by it
     * before used in lighting calculations.
     * 
     * @return The normalized direction vector
     */
    public ReadOnlyVector3f getDirection() {
        return direction;
    }
}
