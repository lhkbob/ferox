package com.ferox.scene;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.SharedInstance;

/**
 * BlinnPhongSpecularModel is a component that adds specular lighting to an entity. It uses the Blinn-Phong
 * specular equation.  When combined with a {@link LambertianDiffuseModel}, the entity will be lit with the
 * same lighting equation used by fixed-function OpenGL.
 * <p/>
 * When used in conjunction with a {@link SpecularColorMap}, the RGB values are modulated with the base
 * specular color, and any alpha value is added to the base shininess.
 *
 * @author Michael Ludwig
 */
public interface BlinnPhongSpecularModel extends Component {
    /**
     * Get the shininess exponent used in the Blinn-Phong specular equation. Lower values imply a larger and
     * dimmer highlight while higher values create a brighter but more contained specular highlight.
     *
     * @return The shininess of the material
     */
    public double getShininess();

    /**
     * Set the shininess exponent to use in the Blinn-Phong specular lighting model. This is the same
     * parameter that is passed to {@link com.ferox.renderer.FixedFunctionRenderer#setMaterialShininess(double)}
     * when OpenGL's fixed-function lighting is used.
     *
     * @param exponent The shininess exponent
     *
     * @return This component
     */
    public BlinnPhongSpecularModel setShininess(double exponent);

    /**
     * Get the specular material color of the entity. This color is modulated with the light's emitted color
     * and scaled by the computed specular contribution before finally being added with the diffuse
     * contribution from the light.
     *
     * @return The specular color
     */
    @Const
    @SharedInstance
    public ColorRGB getColor();

    /**
     * Set the specular color for this entity.
     *
     * @param color The specular color
     *
     * @return This component
     */
    public BlinnPhongSpecularModel setColor(@Const ColorRGB color);
}
