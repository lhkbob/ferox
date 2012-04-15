package com.ferox.scene;

import com.lhkbob.entreri.TypeId;

/**
 * AmbientLight represents a source of ambient light in a scene. Ambient lights
 * contribute an equal amount of light intensity to every rendered object,
 * regardless of direction. AmbientLight does not define any initialization
 * parameters.
 * 
 * @author Michael Ludwig
 */
public final class AmbientLight extends Light<AmbientLight> {
    /**
     * The shared TypedId representing AmbientLight.
     */
    public static final TypeId<AmbientLight> ID = TypeId.get(AmbientLight.class);
    
    private AmbientLight() { }
}
