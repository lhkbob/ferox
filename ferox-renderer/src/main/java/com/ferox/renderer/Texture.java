/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
