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
 * DiffuseColorMap overrides the albedo used by any diffuse lighting model on the entity on a per-pixel basis.
 * The texture is accessed using the texture coordinates from the entity's Renderable.
 * <p/>
 * Alpha values within the texture map will be treated as per-texel opacity values, with the same definition
 * as {@link Transparent}, although they will be ignored if the Transparent component is not added to the
 * Entity. When the Entity is transparent, the opacity of the Transparent component and the texture are
 * multiplied together to get the final opacity for a pixel.
 *
 * @author Michael Ludwig
 */
public interface DiffuseColorMap extends Component {
    /**
     * Return the non-null Texture that is used by this DiffuseColorMap.
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
    public DiffuseColorMap setTexture(Texture texture);
}
