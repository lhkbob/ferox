package com.ferox.renderer;

import com.ferox.math.Const;
import com.ferox.math.Vector4;

/**
 * Texture designates samplers that store color data. They are used as the color render targets when rendering
 * to a texture surface. Textures will can have any base format except DEPTH and DEPTH_STENCIL.
 *
 * @author Michael Ludwig
 */
public interface Texture extends Sampler {
    /**
     * Get the ratio of anisotropic filtering to apply when sampling the color data. The number will be
     * between 0 and 1, where 0 represents no anistropic filtering to be performed and 1 represents the most
     * supported by the current hardware.
     *
     * @return The anistropic filtering level
     */
    public double getAnisotropicFiltering();

    /**
     * Get the border color used when texture coordinates reference the border texel region. The returned
     * instance stores the red, green, blue, and alpha components in the X, Y, Z, and W values of the vector.
     * The returned instance must not be modified. Any mutations will not be reflected in the texture, even
     * after a refresh but may corrupt the value returned by future calls to this method.
     *
     * @return The border color
     */
    @Const
    public Vector4 getBorderColor();
}
