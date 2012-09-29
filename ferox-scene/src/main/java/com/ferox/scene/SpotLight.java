package com.ferox.scene;

import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;

/**
 * <p>
 * SpotLight is a light that shines light in a cone along a specific direction,
 * with the origin of the light (or apex of the cone) located at a specific
 * position. The size of the cone can be configured with a cutoff angle that
 * describes how wide or narrow the cone is.
 * </p>
 * <p>
 * A SpotLight should be combined with a {@link Transform} component to specify
 * its position and direction. The direction is stored in the 3rd column of the
 * 4x4 affine matrix. If there is no transform component, the direction vector
 * defaults to the positive z axis.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class SpotLight extends AbstractPlacedLight<SpotLight> {
    /**
     * The shared TypedId representing SpotLight.
     */
    public static final TypeId<SpotLight> ID = TypeId.get(SpotLight.class);

    @DefaultDouble(30.0)
    private DoubleProperty cutoffAngle;

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
        return cutoffAngle.get(getIndex());
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
        if (angle < 0 || angle > 90) {
            throw new IllegalArgumentException("Illegal cutoff angle, must be in [0, 90], not: " + angle);
        }
        cutoffAngle.set(angle, getIndex());
        return this;
    }
}

