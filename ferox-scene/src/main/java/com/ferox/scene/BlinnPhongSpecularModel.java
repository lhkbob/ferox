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
import com.lhkbob.entreri.property.Within;

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
    public BlinnPhongSpecularModel setShininess(@Within(min = 0, max = 128) double exponent);

    /**
     * Get the specular material color of the entity. This color is modulated with the light's emitted color
     * and scaled by the computed specular contribution before finally being added with the diffuse
     * contribution from the light.
     *
     * @return The specular color
     */
    public ColorRGB getColor(@ReturnValue ColorRGB result);

    /**
     * Set the specular color for this entity.
     *
     * @param color The specular color
     *
     * @return This component
     */
    public BlinnPhongSpecularModel setColor(@Const ColorRGB color);
}
