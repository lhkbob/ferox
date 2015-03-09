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
package com.ferox.scene;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ReturnValue;

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
    public ColorRGB getColor(@ReturnValue ColorRGB result);

    /**
     * Set the diffuse color for this entity.
     *
     * @param color The diffuse color
     *
     * @return This component
     */
    public LambertianDiffuseModel setColor(@Const ColorRGB color);
}
