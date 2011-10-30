package com.ferox.scene;

import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.TypedId;
import com.googlecode.entreri.property.FloatProperty;

/**
 * <p>
 * The BlinnPhongMaterial is a Component that specifies that the renderable
 * Entity should be rendered using a Blinn-Phong lighting model. This is the
 * lighting model used by OpenGL's fixed-function pipeline, although that is
 * per-vertex lighting. If possible, renderers should use per-pixel Phong
 * shading to achieve better rendering quality.
 * </p>
 * <p>
 * The Blinn-Phong model supports diffuse, specular and emitted light colors.
 * This Component does not specify values for these, but is expected to be
 * combined with {@link DiffuseColor}, {@link DiffuseColorMap},
 * {@link SpecularColor}, {@link SpecularColorMap} and the like. It does provide
 * a shininess exponent that describes the shape of specular highlights on
 * objects. This separation was done so that other lighting models can be added
 * but still enable the use of the color providing components.
 * </p>
 * <p>
 * BlinnPhongMaterial inherits Material's single initialization parameter of a
 * VertexAttribute for its normals.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class BlinnPhongMaterial extends Material<BlinnPhongMaterial> {
    /**
     * The shared TypedId representing BlinnPhongMaterial.
     */
    public static final TypedId<BlinnPhongMaterial> ID = Component.getTypedId(BlinnPhongMaterial.class);
    
    private FloatProperty shininess;

    protected BlinnPhongMaterial(EntitySystem system, int index) {
        super(system, index);
    }
    
    @Override
    protected void init(Object... initParams) {
        super.init(initParams); // just pass as-is, since we don't define any more
        setShininess(1f);
    }

    /**
     * Set the shininess exponent to use with this material. The shininess
     * exponent controls how sharp or shiny the specular highlights of an object
     * are. A high value implies a shinier surface with brighter, sharper
     * highlights. The minimum value is 0, and although there is no explicit
     * maximum, the OpenGL fixed-function pipeline imposes a maximum of 128 that
     * this might get clamped to when rendering.
     * 
     * @param shiny The new shininess exponent
     * @throws IllegalArgumentException if shiny is less than 0
     */
    public void setShininess(float shiny) {
        if (shiny < 0f)
            throw new IllegalArgumentException("Shininess must be positive, not: " + shiny);
        shininess.set(shiny, getIndex(), 0);
    }

    /**
     * Return the shininess exponent of the Blinn-Phong material. A higher value
     * means a very shiny surface with bright, small specular highlights. Lower
     * values have fainter and more diffuse specular highlights.
     * 
     * @return The shininess exponent, will be at least 0
     */
    public float getShininess() {
        return shininess.get(getIndex(), 0);
    }
}
