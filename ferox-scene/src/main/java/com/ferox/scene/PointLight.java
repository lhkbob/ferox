package com.ferox.scene;

import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.TypedId;

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
 * <p>
 * PointLight defines no initialization parameters.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class PointLight extends AbstractPlacedLight<PointLight> {
    /**
     * The shared TypedId representing PointLight.
     */
    public static final TypedId<PointLight> ID = Component.getTypedId(PointLight.class);
    
    private PointLight(EntitySystem system, int index) {
        super(system, index);
    }
}
