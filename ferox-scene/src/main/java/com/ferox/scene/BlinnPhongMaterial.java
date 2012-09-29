package com.ferox.scene;

import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;

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
 * 
 * @author Michael Ludwig
 */
public final class BlinnPhongMaterial extends Material<BlinnPhongMaterial> {
    /**
     * The shared TypedId representing BlinnPhongMaterial.
     */
    public static final TypeId<BlinnPhongMaterial> ID = TypeId.get(BlinnPhongMaterial.class);

    @DefaultDouble(1.0)
    private DoubleProperty shininess;

    private BlinnPhongMaterial() { }

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
    public void setShininess(double shiny) {
        if (shiny < 0f) {
            throw new IllegalArgumentException("Shininess must be positive, not: " + shiny);
        }
        shininess.set(shiny, getIndex());
    }

    /**
     * Return the shininess exponent of the Blinn-Phong material. A higher value
     * means a very shiny surface with bright, small specular highlights. Lower
     * values have fainter and more diffuse specular highlights.
     * 
     * @return The shininess exponent, will be at least 0
     */
    public double getShininess() {
        return shininess.get(getIndex());
    }
}
