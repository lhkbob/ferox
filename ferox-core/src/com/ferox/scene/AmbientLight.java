package com.ferox.scene;

import com.ferox.math.ReadOnlyColor4f;

/**
 * AmbientLight represents a source of ambient light in a scene. Ambient lights
 * contribute an equal amount of light intensity to every rendered object,
 * regardless of direction.
 * 
 * @author Michael Ludwig
 */
public class AmbientLight extends Light<AmbientLight> {
    /**
     * Create an AmbientLight with the given color.
     * 
     * @param color The initial light color
     */
    public AmbientLight(ReadOnlyColor4f color) {
        setColor(color);
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
