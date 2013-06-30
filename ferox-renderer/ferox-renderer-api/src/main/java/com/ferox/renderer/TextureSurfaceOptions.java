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

import java.util.Arrays;

/**
 * TextureSurfaceOptions represents the set of configurable parameters used to create an
 * TextureSurface. Each TextureSurfaceOptions instance is immutable, the setters available
 * return new instances that match the calling instance except for the new parameter
 * value.
 *
 * @author Michael Ludwig
 */
public final class TextureSurfaceOptions {
    private final int width;
    private final int height;

    private final Sampler.TexelFormat depthRenderBuffer;

    private final Sampler.RenderTarget[] colorTargets;
    private final Sampler.RenderTarget depthStencilTarget;

    /**
     * Create a default options that is configured for a 1x1 texture, no depth/stencil
     * renderbuffer and no render targets attached by default.
     */
    public TextureSurfaceOptions() {
        this(1, 1, null, new Sampler.RenderTarget[0], null);
    }

    private TextureSurfaceOptions(int width, int height,
                                  Sampler.TexelFormat depthRenderBuffer,
                                  Sampler.RenderTarget[] colorTargets,
                                  Sampler.RenderTarget depthStencilTarget) {
        this.width = width;
        this.height = height;

        this.colorTargets = colorTargets;
        this.depthStencilTarget = depthStencilTarget;
        this.depthRenderBuffer = depthRenderBuffer;
    }

    /**
     * Set the initial color render target attachments for the texture surface. The
     * targets must satisfy the same constraints as if they were to be passed into {@link
     * HardwareAccessLayer#setActiveSurface(TextureSurface, com.ferox.renderer.Sampler.RenderTarget[],
     * com.ferox.renderer.Sampler.RenderTarget)} as the color buffers array.
     *
     * @param targets The initial color buffers
     *
     * @return A new options instance with configured color buffers
     */
    public TextureSurfaceOptions colorBuffers(Sampler.RenderTarget... targets) {
        Sampler.RenderTarget[] copy = (targets == null ? new Sampler.RenderTarget[0]
                                                       : Arrays
                                               .copyOf(targets, targets.length));
        return new TextureSurfaceOptions(width, height, depthRenderBuffer, copy,
                                         depthStencilTarget);
    }

    /**
     * Set the initial depth and/or stencil render target attacment for the surface. This
     * will disable any renderbuffer configuration that may have been previously
     * specified.
     *
     * @param target The initial depth target
     *
     * @return A new options instance with configured depth buffer
     */
    public TextureSurfaceOptions depthBuffer(Sampler.RenderTarget target) {
        return new TextureSurfaceOptions(width, height, null, colorTargets, target);
    }

    /**
     * Set the size of the texture surface. Any render target that is to be attached must
     * have these same dimensions.
     *
     * @param width  The width of the surface
     * @param height The height of the surface
     *
     * @return A new options instance with configured size
     */
    public TextureSurfaceOptions size(int width, int height) {
        return new TextureSurfaceOptions(width, height, depthRenderBuffer, colorTargets,
                                         depthStencilTarget);
    }

    /**
     * Declare that the texture surface should use a renderbuffer that stores only depth
     * information. It will not support a stencil buffer. Any depth buffer render target
     * attachment is cleared.
     *
     * @return The new options instance configured to use a depth renderbuffer
     */
    public TextureSurfaceOptions useDepthRenderBuffer() {
        return new TextureSurfaceOptions(width, height, Sampler.TexelFormat.DEPTH,
                                         colorTargets, null);
    }

    /**
     * Declare that the texture surface should use a renderbuffer that stores both depth
     * and stencil information. Any depth buffer render target attachment is cleared.
     *
     * @return The new options instance configured to use a depth+stencil renderbuffer
     */
    public TextureSurfaceOptions useDepthAndStencilRenderBuffer() {
        return new TextureSurfaceOptions(width, height, Sampler.TexelFormat.DEPTH_STENCIL,
                                         colorTargets, null);
    }

    /**
     * Get the render target configured for the given buffer. If the buffer index is
     * larger than the maximum configured target, then null is returned.
     *
     * @param colorBuffer The color buffer to lookup
     *
     * @return The render target attached by default to the color buffer, or null if none
     *         was specified for the buffer
     *
     * @throws IndexOutOfBoundsException if colorBuffer is less than 0
     */
    public Sampler.RenderTarget getColorBuffer(int colorBuffer) {
        return (colorBuffer >= colorTargets.length ? null : colorTargets[colorBuffer]);
    }

    /**
     * Get the default depth/stencil render target for use with the surface. If no target
     * was specified, null is returned.
     *
     * @return The depth render target
     */
    public Sampler.RenderTarget getDepthBuffer() {
        return depthStencilTarget;
    }

    /**
     * Get the base format of any renderbuffer that must be created for the texture
     * surface. If null is returned the texture surface should not be created with a
     * renderbuffer so that a depth texture can be used instead.
     *
     * @return The base format, one of DEPTH, DEPTH_STENCIL or null
     */
    public Sampler.TexelFormat getDepthRenderBufferFormat() {
        return depthRenderBuffer;
    }

    /**
     * @return The width of the texture surface
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return The height of the texture surface
     */
    public int getHeight() {
        return height;
    }
}
