package com.ferox.scene;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.SharedInstance;

/**
 * LambertianDiffuseModel is a component that adds diffuse lighting to an entity. It uses the Lambertian model
 * where contribution is proportional to the cosine between the surface orientation and light direction. When
 * combined with a {@link BlinnPhongSpecularModel}, the entity will be lit with the same lighting equation
 * used by fixed-function OpenGL.
 * <p/>
 * When used in conjunction with a {@link DiffuseColorMap}, the RGB values are modulated with the base diffuse
 * color.
 *
 * @author Michael Ludwig
 */
public interface LambertianDiffuseModel extends Component {
    /**
     * Get the diffuse material color or albedo of the entity. This color is modulated with the light's
     * emitted color and scaled by the computed difufse contribution before finally being added with any
     * specular contribution from the light.
     *
     * @return The diffuse color
     */
    @Const
    @SharedInstance
    public ColorRGB getColor();

    /**
     * Set the diffuse color for this entity.
     *
     * @param color The diffuse color
     *
     * @return This component
     */
    public LambertianDiffuseModel setColor(@Const ColorRGB color);
}
