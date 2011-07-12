package com.ferox.scene;

import com.ferox.entity.Component;
import com.ferox.entity.Template;
import com.ferox.entity.TypedId;
import com.ferox.math.ReadOnlyColor3f;
import com.ferox.math.Vector3f;

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
public final class PointLight extends AbstractPlacedLight<PointLight> {
    /**
     * The shared TypedId representing PointLight.
     */
    public static final TypedId<PointLight> ID = Component.getTypedId(PointLight.class);
    
    /**
     * Create a new PointLight with the given color. Energy falloff is initially
     * disabled.
     * 
     * @param color The starting color
     * @throws NullPointerException if color is null
     */
    public PointLight(ReadOnlyColor3f color) {
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
    public PointLight(ReadOnlyColor3f color, float falloffDistance) {
        super(color, new Vector3f(), falloffDistance);
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
    }
}
