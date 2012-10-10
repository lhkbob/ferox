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

import com.ferox.resource.Texture;
import com.lhkbob.entreri.TypeId;

/**
 * <p>
 * DiffuseColorMap provides diffuse material colors, much like
 * {@link DiffuseColor}, except that it uses a {@link Texture} to have per-texel
 * coloration instead of a single color across the entire Entity. It is not
 * defined how the geometry of the Entity is mapped onto the texture, but will
 * likely use texture coordinates stored in the geometry. This should be
 * configured by the rendering controller, or in other component types.
 * </p>
 * <p>
 * Alpha values within the texture map will be treated as per-texel opacity
 * values, with the same definition as {@link Transparent}, although they will
 * be ignored if the Transparent component is not added to the Entity. When the
 * Entity is transparent, the opacity of the Transparent component and the
 * texture are multiplied together to get the final opacity for a pixel.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class DiffuseColorMap extends TextureMap<DiffuseColorMap> {
    /**
     * The shared TypedId representing DiffuseColorMap.
     */
    public static final TypeId<DiffuseColorMap> ID = TypeId.get(DiffuseColorMap.class);

    private DiffuseColorMap() {}

    @Override
    protected void validate(Texture tex) {
        // do nothing
    }
}
