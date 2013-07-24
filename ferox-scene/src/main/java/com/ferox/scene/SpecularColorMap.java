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

/**
 * SpecularColorMap servers to override the specular properties on a per-pixel level. The RGB values represent
 * the overridden specular color, which should be modulated with any specular material color provided with the
 * lighting model. If the alpha value is present, its values are interpreted in a lighting model dependent
 * manner, generally encoding the other configurable property of the specular model (such as shininess or
 * roughness).
 * <p/>
 * The texture is accessed by texture coordinates from the entity's associated Renderable.
 *
 * @author Michael Ludwig
 */
public interface SpecularColorMap extends Component {
    /**
     * Return the non-null Texture that is used by this SpecularColorMap.
     *
     * @return This TextureMap's texture
     */
    public Texture getTexture();

    /**
     * Set the Texture to use with this component.
     *
     * @param texture The new Texture
     *
     * @return This component for chaining purposes
     */
    public SpecularColorMap setTexture(Texture texture);
}
