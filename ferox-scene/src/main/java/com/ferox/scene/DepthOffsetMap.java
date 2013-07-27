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

import com.ferox.renderer.DepthMap;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.NotNull;

/**
 * <p/>
 * DepthOffsetMap provides per-pixel depth offsets to the geometry of an Entity. The depth offsets are
 * distances along each normal of the polygon the pixels are mapped to. The DepthOffsetMap can be used to
 * create parallax normal mapping if combined with a {@link NormalMap}, or could be used to generate a normal
 * map on the fly.
 * <p/>
 * The depth map should not have a depth comparison enabled because the shader will need direct access to the
 * depth values. Positive depth values are assumed to extend along the normal away from the surface. If
 * negative values are necessary, a signed data type should be used.
 *
 * @author Michael Ludwig
 */
public interface DepthOffsetMap extends Component {
    /**
     * Return the non-null Texture that is used by this DepthOffsetMap.
     *
     * @return This TextureMap's texture
     */
    public DepthMap getTexture();

    /**
     * Set the Texture to use with this component.
     *
     * @param texture The new Texture
     *
     * @return This component for chaining purposes
     */
    public DepthOffsetMap setTexture(@NotNull DepthMap texture);
}
