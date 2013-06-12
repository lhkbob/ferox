package com.ferox.resource;

import com.ferox.math.Const;
import com.ferox.math.Vector4;

/**
 *
 */
public interface Texture extends Sampler {

    public double getAnisotropicFiltering();

    @Const
    public Vector4 getBorderColor();
}
