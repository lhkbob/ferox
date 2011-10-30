package com.ferox.scene;

import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.TypedId;

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
 * <p>
 * SpecularColor does not define any initialization parameters.
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
    
    private SpecularColor(EntitySystem system, int index) {
        super(system, index);
    }
}
