package com.ferox.scene;

import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.TypedId;

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
    public static final TypedId<AmbientLight> ID = Component.getTypedId(AmbientLight.class);
    
    private AmbientLight(EntitySystem system, int index) {
        super(system, index);
    }
}
