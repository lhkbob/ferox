package com.ferox.scene;

import com.ferox.entity.Component;
import com.ferox.entity.Template;
import com.ferox.entity.TypedId;
import com.ferox.math.ReadOnlyColor3f;

/**
 * EmittedColor causes the Entity to emit a local light. This can be used for
 * glowing eyes or effects like neon lights. Light emitted by these Entities
 * only effect the rendering of the Entity and will not contribute to general
 * scene lighting. Although technically a light, this functions much more like a
 * material. The color stored in this EmittedColor should be considered to have
 * HDR color values when relevant.
 * 
 * @see EmittedColorMap
 * @author Michael Ludwig
 */
public final class EmittedColor extends ColorComponent<EmittedColor> {
    /**
     * The shared TypedId representing EmittedColor.
     */
    public static final TypedId<EmittedColor> ID = Component.getTypedId(EmittedColor.class);
    
    /**
     * Create a EmittedColor component that copies the given color to use
     * initially.
     * 
     * @param color The initial color
     * @throws NullPointerException if color is null
     */
    public EmittedColor(ReadOnlyColor3f color) {
        super(color);
    }

    /**
     * Create an EmittedColor that is a clone of <tt>clone</tt> for use with a
     * {@link Template}
     * 
     * @param clone The component to clone
     * @throws NullPointerException if clone is null
     */
    public EmittedColor(EmittedColor clone) {
        super(clone);
    }
}
