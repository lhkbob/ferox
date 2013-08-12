package com.ferox.scene;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.SharedInstance;

/**
 * WardSpecularModel is a component that adds specular lighting to an entity. It uses the Ward anisotropic
 * specular equation.  Like {@link CookTorrenceSpecularModel} and {@link BlinnPhongSpecularModel} it must
 * still be combined with a diffuse lighting model to get acceptable results. Because the Ward model computes
 * anisotropic specular highlights, the entity's geometry must have tangent vectors available.
 * <p/>
 * When used in conjunction with a {@link SpecularColorMap}, the RGB values are modulated with the base
 * specular color, and any alpha value is used to determine the specular orientation.
 *
 * @author Michael Ludwig
 */
public interface WardSpecularModel extends Component {
    // FIXME add the user controls for highlight size and shape. Also figure out how to encode them
    // into a single parameter instead of in two (not sure if angle will be valid anymore

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
    public WardSpecularModel setColor(@Const ColorRGB color);
}
