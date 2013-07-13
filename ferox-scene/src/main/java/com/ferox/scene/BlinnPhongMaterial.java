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

import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;

/**
 * <p/>
 * The BlinnPhongMaterial is a Component that specifies that the renderable Entity should be rendered using a
 * Blinn-Phong lighting model. This is the lighting model used by OpenGL's fixed-function pipeline, although
 * that is per-vertex lighting. If possible, renderers should use per-pixel Phong shading to achieve better
 * rendering quality.
 * <p/>
 * <p/>
 * The Blinn-Phong model supports diffuse, specular and emitted light colors. This Component does not specify
 * values for these, but is expected to be combined with {@link DiffuseColor}, {@link DiffuseColorMap}, {@link
 * SpecularColor}, {@link SpecularColorMap} and the like. It does provide a shininess exponent that describes
 * the shape of specular highlights on objects. This separation was done so that other lighting models can be
 * added but still enable the use of the color providing components.
 *
 * @author Michael Ludwig
 */
public final class BlinnPhongMaterial extends Material<BlinnPhongMaterial> {
    @DefaultDouble(1.0)
    private DoubleProperty shininess;

    private BlinnPhongMaterial() {
    }

    /**
     * Set the shininess exponent to use with this material. The shininess exponent controls how sharp or
     * shiny the specular highlights of an object are. A high value implies a shinier surface with brighter,
     * sharper highlights. The minimum value is 0, and although there is no explicit maximum, the OpenGL
     * fixed-function pipeline imposes a maximum of 128 that this might get clamped to when rendering.
     *
     * @param shiny The new shininess exponent
     *
     * @return This component
     *
     * @throws IllegalArgumentException if shiny is less than 0
     */
    public BlinnPhongMaterial setShininess(double shiny) {
        if (shiny < 0f) {
            throw new IllegalArgumentException("Shininess must be positive, not: " + shiny);
        }
        shininess.set(shiny, getIndex());
        updateVersion();
        return this;
    }

    /**
     * Return the shininess exponent of the Blinn-Phong material. A higher value means a very shiny surface
     * with bright, small specular highlights. Lower values have fainter and more diffuse specular
     * highlights.
     *
     * @return The shininess exponent, will be at least 0
     */
    public double getShininess() {
        return shininess.get(getIndex());
    }
}
