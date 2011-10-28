package com.ferox.scene;

import com.ferox.entity2.Component;
import com.ferox.entity2.TypedId;
import com.ferox.math.ReadOnlyColor3f;

/**
 * AmbientLight represents a source of ambient light in a scene. Ambient lights
 * contribute an equal amount of light intensity to every rendered object,
 * regardless of direction.
 * 
 * @author Michael Ludwig
 */
public final class AmbientLight extends Light<AmbientLight> {
    /**
     * The shared TypedId representing AmbientLight.
     */
    public static final TypedId<AmbientLight> ID = Component.getTypedId(AmbientLight.class);

    /**
     * Create an AmbientLight with the given color.
     * 
     * @param color The initial light color
     * @throws NullPointerException if color is null
     */
    public AmbientLight(ReadOnlyColor3f color) {
        super(color);
    }

    /**
     * Create an AmbientLight that is a clone of the given AmbientLight.
     * 
     * @param clone The light to clone
     * @throws NullPointerException if clone is null
     */
    public AmbientLight(AmbientLight clone) {
        super(clone);
        // Don't need to clone any more properties for AmbientLight
    }
}
