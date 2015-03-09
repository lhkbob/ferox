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

import com.ferox.renderer.Texture;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.Reference;

/**
 * <p/>
 * EmittedColorMap functions much like {@link EmittedColor} except that it defines the emitted light in a
 * texture map to allow for more detail in the final rendering. Like EmittedColor, the emitted light will not
 * influence other objects in the system and is a purely local effect.
 * <p/>
 * Alpha values in the texture map do not have an explicit behavior. Renderers may support encoding of
 * additional data within that channel, or use it as an exponent to have a higher range of values.
 *
 * @author Michael Ludwig
 */
public interface EmittedColorMap extends Component {
    /**
     * Return the non-null Texture that is used by this EmittedColorMap.
     *
     * @return This TextureMap's texture
     */
    @Reference(nullable = false)
    public Texture getTexture();

    /**
     * Set the Texture to use with this component.
     *
     * @param texture The new Texture
     *
     * @return This component for chaining purposes
     */
    public EmittedColorMap setTexture(Texture texture);
}
