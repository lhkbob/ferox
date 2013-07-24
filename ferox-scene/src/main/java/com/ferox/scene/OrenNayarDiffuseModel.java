package com.ferox.scene;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.SharedInstance;

/**
 * OrenNayarDiffuseModel is a component that adds diffuse lighting to an entity. It uses the Oren-Nayar
 * diffuse model that more accurately represents rough surfaces.
 * <p/>
 * When used in conjunction with a {@link DiffuseColorMap}, the RGB values are modulated with the base diffuse
 * color.  The renderer can interpret the alpha channel of the diffuse map as the roughness parameter if the
 * entity does not use a {@link Transparent} component. Otherwise the alpha should be used as the default
 * behavior: per-pixel alpha.
 *
 * @author Michael Ludwig
 */
public interface OrenNayarDiffuseModel extends Component {
    /**
     * Get the roughness parameter for use in the Oren-Nayar equations.
     *
     * @return The roughness parameter
     */
    public double getRoughness();

    /**
     * Set the roughness parameter used in the Oren-Nayar diffuse lighting model.
     *
     * @param roughness The new roughness value
     *
     * @return This component
     */
    public OrenNayarDiffuseModel setRoughness(double roughness);

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
