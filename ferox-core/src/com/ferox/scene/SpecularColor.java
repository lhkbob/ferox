package com.ferox.scene;

import com.ferox.entity.Component;
import com.ferox.entity.Template;
import com.ferox.entity.TypedId;
import com.ferox.math.ReadOnlyColor3f;

/**
 * <p>
 * SpecularColor adds specular lighting to the material used to render an
 * Entity. If the lighting model does not support specular lighting, then this
 * will be ignored. Unlike {@link DiffuseColor diffuse lighting}, the specular
 * color also depends on the viewer's relative position to the entity. The view
 * dependency's effect on the final specular contribution depends on the
 * lighting model and should be described in those components (such as
 * {@link BlinnPhongMaterial}).
 * </p>
 * <p>
 * Like DiffuseColor, the color values stored in SpecularColor represent
 * fractions of light that are reflected so the only meaningful values are in
 * the range [0, 1]. HDR color values are not used.
 * </p>
 * 
 * @see SpecularColorMap
 * @author Michael Ludwig
 */
public final class SpecularColor extends ColorComponent<SpecularColor> {
    /**
     * The shared TypedId representing SpecularColor.
     */
    public static final TypedId<SpecularColor> ID = Component.getTypedId(SpecularColor.class);
    
    /**
     * Create a SpecularColor component that copies the given color to use
     * initially.
     * 
     * @param color The initial color
     * @throws NullPointerException if color is null
     */
    public SpecularColor(ReadOnlyColor3f color) {
        super(color);
    }

    /**
     * Create an SpecularColor that is a clone of <tt>clone</tt> for use with a
     * {@link Template}.
     * 
     * @param clone The component to clone
     * @throws NullPointerException if clone is null
     */
    public SpecularColor(SpecularColor clone) {
        super(clone);
    }
}
