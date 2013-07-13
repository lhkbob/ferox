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
import com.ferox.renderer.texture.TextureFormat;

/**
 * <p/>
 * DepthOffsetMap provides per-pixel depth offsets to the geometry of an Entity. The depth offsets are
 * distances along each normal of the polygon the pixels are mapped to. The DepthOffsetMap can be used to
 * create parallax normal mapping if combined with a {@link NormalMap}, or could be used to generate a normal
 * map on the fly.
 * <p/>
 * The textures used by DepthOffsetMap must have single-component texture formats. If the format of the depth
 * map is {@link TextureFormat#R_FLOAT}, then the depth values in the texture are taken as is. If it is any
 * other format, then the depth values are packed into the range [0, 1] and are converted to [-.5, .5] with
 * <code>d - .5</code> when used in a shader.
 *
 * @author Michael Ludwig
 */
public final class DepthOffsetMap extends TextureMap<DepthOffsetMap> {
    private DepthOffsetMap() {
    }

    @Override
    protected void validate(Texture tex) {
        if (tex.getFormat().getComponentCount() != 1) {
            throw new IllegalArgumentException(
                    "Cannot specify a depth map that has more than one component: " + tex.getFormat());
        }
    }
}
