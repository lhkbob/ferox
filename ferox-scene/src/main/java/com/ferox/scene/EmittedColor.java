package com.ferox.scene;

import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.TypedId;

/**
 * <p>
 * EmittedColor causes the Entity to emit a local light. This can be used for
 * glowing eyes or effects like neon lights. Light emitted by these Entities
 * only effect the rendering of the Entity and will not contribute to general
 * scene lighting. Although technically a light, this functions much more like a
 * material. The color stored in this EmittedColor should be considered to have
 * HDR color values when relevant.
 * </p>
 * <p>
 * EmittedColor does not have any initialization parameters.
 * </p>
 * 
 * @see EmittedColorMap
 * @author Michael Ludwig
 */
public final class EmittedColor extends ColorComponent<EmittedColor> {
    /**
     * The shared TypedId representing EmittedColor.
     */
    public static final TypedId<EmittedColor> ID = Component.getTypedId(EmittedColor.class);
    
    private EmittedColor(EntitySystem system, int index) {
        super(system, index);
    }
}
