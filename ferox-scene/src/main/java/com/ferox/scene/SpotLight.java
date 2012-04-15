package com.ferox.scene;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.entreri.Vector3Property;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.annot.DefaultValue;
import com.lhkbob.entreri.annot.Factory;
import com.lhkbob.entreri.annot.Unmanaged;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleProperty;

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
public final class SpotLight extends AbstractPlacedLight<SpotLight> {
    /**
     * The shared TypedId representing SpotLight.
     */
    public static final TypeId<SpotLight> ID = TypeId.get(SpotLight.class);
    
    @Factory(DefaultDirectionFactory.class)
    private Vector3Property direction;
    
    @DefaultValue(defaultDouble=30.0)
    private DoubleProperty cutoffAngle;
    
    @Unmanaged
    private final Vector3 dirCache = new Vector3();

    private SpotLight() { }
    
    /**
     * Return the cutoff angle, in degrees, representing the maximum angle light
     * will spread from the {@link #getDirection() direction vector}. This
     * creates a cone of light that is fat or thin depending on if the angle is
     * large or small.
     * 
     * @return The cutoff angle in degrees, will be in [0, 90]
     */
    public double getCutoffAngle() {
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
    public SpotLight setCutoffAngle(double angle) {
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
    public SpotLight setDirection(@Const Vector3 dir) {
        if (dir == null)
            throw new NullPointerException("Direction vector cannot be null");
        dirCache.set(dir);
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

