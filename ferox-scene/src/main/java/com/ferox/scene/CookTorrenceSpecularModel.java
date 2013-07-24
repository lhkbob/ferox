package com.ferox.scene;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.SharedInstance;

/**
 * CookTorrenceSpecularModel is a component that adds specular lighting to an entity. It uses the
 * Cook-Torrence specular equation. It should be combined with a {@link LambertianDiffuseModel} or {@link
 * OrenNayarDiffuseModel} component to fully specify an entity's material.
 * <p/>
 * When used in conjunction with a {@link SpecularColorMap}, the RGB values are modulated with the base
 * specular color, and any alpha value is added to the base roughness.
 *
 * @author Michael Ludwig
 */
public interface CookTorrenceSpecularModel extends Component {
    /**
     * Get the roughness parameter for use in the Cook-Torrence equations.
     *
     * @return The roughness parameter
     */
    public double getRoughness();

    /**
     * Set the roughness parameter used in the Cook-Torrence specular lighting model.
     *
     * @param roughness The new roughness value
     *
     * @return This component
     */
    public CookTorrenceSpecularModel setRoughness(double roughness);

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
    public CookTorrenceSpecularModel setColor(@Const ColorRGB color);
}
