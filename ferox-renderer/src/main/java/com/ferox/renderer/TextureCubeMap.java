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
package com.ferox.renderer;

/**
 * TextureCubeMap is a color texture that has six separate two dimensional images. Each face can be used as a
 * render target for render-to-texture with a {@link TextureSurface}. All six images have the same width and
 * height so they represent the six faces of a cube. Shaders can refer to a TextureCubeMap in the GLSL code
 * with the 'samplerCube' uniform type.
 *
 * @author Michael Ludwig
 */
public interface TextureCubeMap extends Texture {
    /**
     * @return A RenderTarget that renders into the positive-x face of the cube
     */
    public RenderTarget getPositiveXRenderTarget();

    /**
     * @return A RenderTarget that renders into the negative-x face of the cube
     */
    public RenderTarget getNegativeXRenderTarget();

    /**
     * @return A RenderTarget that renders into the positive-y face of the cube
     */
    public RenderTarget getPositiveYRenderTarget();

    /**
     * @return A RenderTarget that renders into the negative-x face of the cube
     */
    public RenderTarget getNegativeYRenderTarget();

    /**
     * @return A RenderTarget that renders into the positive-z face of the cube
     */
    public RenderTarget getPositiveZRenderTarget();

    /**
     * @return A RenderTarget that renders into the negative-z face of the cube
     */
    public RenderTarget getNegativeZRenderTarget();
}
