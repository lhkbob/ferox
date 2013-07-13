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

import com.ferox.renderer.texture.Texture;

/**
 * <p/>
 * SpecularColorMap functions like {@link SpecularColor} except that it provides per-texel specular material.
 * Black texel values will effectively disable specular highlights for those regions of the texture. Like
 * {@link EmittedColorMap}, any alpha component has no defined behavior and can be used by the renderer in
 * different ways (possibly as an exponent or shininess factor as used in a {@link BlinnPhongMaterial}).
 * <p/>
 * <p/>
 * It is not defined how the geometry of the Entity is mapped onto the texture, but will likely use texture
 * coordinates stored in the geometry. This should be configured by the rendering controller, or in other
 * component types. Any texture mapping should likely match the texture mapping used for a {@link
 * DiffuseColorMap}.
 *
 * @author Michael Ludwig
 */
public final class SpecularColorMap extends TextureMap<SpecularColorMap> {
    private SpecularColorMap() {
    }

    @Override
    protected void validate(Texture tex) {
        // do nothing
    }
}
