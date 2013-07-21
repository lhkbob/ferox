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
package com.ferox.renderer.impl;

import com.ferox.renderer.Sampler;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.resources.TextureImpl;

import java.util.EnumSet;

/**
 * AbstractTextureSurface is a mostly complete implementation of TextureSurface that is also an
 * AbstractSurface. It handles the creation and updating of the necessary Texture resources for the
 * TextureSurface, based on the input TextureSurfaceOptions.
 *
 * @author Michael Ludwig
 */
public abstract class AbstractTextureSurface extends AbstractSurface implements TextureSurface {
    private static final EnumSet<Sampler.TexelFormat> VALID_COLOR_FORMATS = EnumSet
            .of(Sampler.TexelFormat.R, Sampler.TexelFormat.RG, Sampler.TexelFormat.RGB,
                Sampler.TexelFormat.RGBA);
    private static final EnumSet<Sampler.TexelFormat> VALID_DEPTH_FORMATS = EnumSet
            .of(Sampler.TexelFormat.DEPTH, Sampler.TexelFormat.DEPTH_STENCIL);
    private final int width;
    private final int height;

    private final Sampler.TexelFormat depthRenderBuffer;

    private final TextureImpl.RenderTargetImpl[] colorTargets;
    private TextureImpl.RenderTargetImpl depthTarget;

    /**
     * Create a new surface with the given texture options.
     *
     * @param framework The framework the surface is owned by
     * @param options   The options that configure this surface
     */
    public AbstractTextureSurface(FrameworkImpl framework, TextureSurfaceOptions options) {
        int maxDimension = framework.getCapabilities().getMaxTextureSurfaceSize();
        if (options.getWidth() > maxDimension || options.getHeight() > maxDimension) {
            throw new SurfaceCreationException(
                    "Cannot create texture surface of requested size: beyond max supported dimension");
        }

        this.width = options.getWidth();
        this.height = options.getHeight();
        this.depthRenderBuffer = options.getDepthRenderBufferFormat();

        colorTargets = new TextureImpl.RenderTargetImpl[framework.getCapabilities().getMaxColorBuffers()];

        if (options.getColorBufferCount() > colorTargets.length) {
            throw new SurfaceCreationException("Too many color buffers specified");
        }
        Sampler.RenderTarget[] array = new Sampler.RenderTarget[options.getColorBufferCount()];
        for (int i = 0; i < array.length; i++) {
            array[i] = options.getColorBuffer(i);
        }
        setRenderTargets(array, options.getDepthBuffer());
    }

    @Override
    public Sampler.TexelFormat getDepthRenderBufferFormat() {
        return depthRenderBuffer;
    }

    @Override
    public TextureImpl.RenderTargetImpl getDepthBuffer() {
        return depthTarget;
    }

    @Override
    public TextureImpl.RenderTargetImpl getColorBuffer(int buffer) {
        return colorTargets[buffer];
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    private int mipDim(int dimension, int mipmap) {
        return Math.max(dimension >> mipmap, 1);
    }

    /**
     * Update the render targets of this surface. Validation is performed to ensure that the dimensions and
     * formats all match and all mipmap layers are valid. If colorTargets is shorter than the maximum number
     * of color targets, then the remaining targets are bound to null.
     *
     * @param colorTargets The color targets
     * @param depthTarget  The depth target
     *
     * @throws IllegalArgumentException if the targets are misconfigured for the surface
     */
    public void setRenderTargets(Sampler.RenderTarget[] colorTargets, Sampler.RenderTarget depthTarget) {
        // first validate new targets, before delegating to the subclass to perform the OpenGL
        // operations necessary
        if (depthRenderBuffer != null && depthTarget != null) {
            throw new IllegalArgumentException(
                    "Cannot specify a depth render target when a depth renderbuffer is used");
        }

        TextureImpl.FullFormat colorFormat = null;
        for (int i = 0; i < colorTargets.length; i++) {
            if (colorTargets[i] != null) {
                TextureImpl t = (TextureImpl) colorTargets[i].getSampler();
                int m = t.getBaseMipmap();
                if (mipDim(t.getWidth(), m) != width || mipDim(t.getHeight(), m) != height) {
                    throw new IllegalArgumentException(String.format(
                            "Color buffer %d does not match surface dimensions, expected %d x %d but was %d x %d",
                            i, width, height, mipDim(t.getWidth(), m), mipDim(t.getHeight(), m)));
                }
                if (!VALID_COLOR_FORMATS.contains(t.getFormat())) {
                    throw new IllegalArgumentException(
                            "Texture format is not valid for color target, was: " + t.getFormat() +
                            ", but must be one of: " + VALID_COLOR_FORMATS);
                }

                if (colorFormat == null) {
                    colorFormat = t.getFullFormat();
                } else if (t.getFullFormat() != colorFormat) {
                    throw new IllegalArgumentException(String.format(
                            "Color buffer %d uses different color format, expected %s but was %s", i,
                            colorFormat, t.getFullFormat()));
                }
            }
        }
        if (depthTarget != null) {
            if (!VALID_DEPTH_FORMATS.contains(depthTarget.getSampler().getFormat())) {
                throw new IllegalArgumentException("Texture format is not valid for depth target, was: " +
                                                   depthTarget.getSampler().getFormat() +
                                                   ", but must be one of: " + VALID_DEPTH_FORMATS);
            }
            int m = depthTarget.getSampler().getBaseMipmap();
            if (mipDim(depthTarget.getSampler().getWidth(), m) != width ||
                mipDim(depthTarget.getSampler().getHeight(), m) != height) {
                throw new IllegalArgumentException(String.format(
                        "Depth buffer does not match surface dimensions, expected %d x %d but was %d x %d",
                        width, height, depthTarget.getSampler().getWidth(),
                        depthTarget.getSampler().getHeight()));
            }
        }

        // about as valid as we can guarantee at this point
        for (int i = 0; i < this.colorTargets.length; i++) {
            if (i >= colorTargets.length) {
                this.colorTargets[i] = null;
            } else {
                this.colorTargets[i] = (TextureImpl.RenderTargetImpl) colorTargets[i];
            }
        }
        this.depthTarget = (TextureImpl.RenderTargetImpl) depthTarget;
    }
}
