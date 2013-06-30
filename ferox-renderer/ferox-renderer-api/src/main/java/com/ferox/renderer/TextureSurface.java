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
 * TextureSurface is a Surface that provides render-to-texture functionality. A texture
 * surface has a fixed dimension, and is optionally created with a depth/stencil
 * renderbuffer if only color render targets are used.
 * <p/>
 * To render into a texture, the TextureSurface must be created or activated with the
 * render targets produced by the textures of interest.  If multiple render targets are
 * rendered into simultaneously, they must have the same dimension and be of the same
 * color format if they are color targets.  Depth textures can be used when the surface
 * has no depth renderbuffer to receive the depth state.
 *
 * @author Michael Ludwig
 * @see HardwareAccessLayer#setActiveSurface(TextureSurface, com.ferox.renderer.Sampler.RenderTarget[],
 *      com.ferox.renderer.Sampler.RenderTarget)
 */
public interface TextureSurface extends Surface {
    /**
     * Get the base format of the renderbuffer used for depth and stencil tests. If the
     * surface was created without a renderbuffer, this will return null. Otherwise it
     * will be one of DEPTH or DEPTH_STENCIL. A depth/stencil texture can only be used as
     * a render target for a surface if this returns null.
     *
     * @return The depth/stencil renderbuffer format of the surface
     */
    public Sampler.TexelFormat getDepthRenderBufferFormat();

    /**
     * @return The last configured depth render target, or null if no target was
     *         activated, or when a depth renderbuffer is used
     */
    public Sampler.RenderTarget getDepthBuffer();

    /**
     * Get the last configured color render target for the particular draw buffer. The
     * buffer parameter must be at least 0 and less than the number of supported draw
     * buffers defined in the Framework's capabilities.
     *
     * @param buffer The color buffer to lookup
     *
     * @return The last configured color render target for the given buffer, or null if
     *         that draw buffer is disabled for this surface
     *
     * @throws IndexOutOfBoundsException if buffer refers to an unsupported or illegal
     *                                   buffer
     * @see com.ferox.renderer.Capabilities#getMaxColorBuffers()
     */
    public Sampler.RenderTarget getColorBuffer(int buffer);
}
