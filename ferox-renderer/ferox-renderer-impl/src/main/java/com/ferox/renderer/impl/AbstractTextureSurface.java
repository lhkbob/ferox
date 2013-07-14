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
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 * AbstractTextureSurface is a mostly complete implementation of TextureSurface that is also an
 * AbstractSurface. It handles the creation and updating of the necessary Texture resources for the
 * TextureSurface, based on the input TextureSurfaceOptions.
 *
 * @author Michael Ludwig
 */
public abstract class AbstractTextureSurface extends AbstractSurface implements TextureSurface {
    private final int width;
    private final int height;

    private final Sampler.TexelFormat depthRenderBuffer;

    private final TextureImpl.RenderTargetImpl[] colorTargets;
    private TextureImpl.RenderTargetImpl depthTarget;


    public AbstractTextureSurface(FrameworkImpl framework, int width, int height,
                                  Sampler.TexelFormat depthRenderBuffer) {
        super(framework);

        int maxDimension = framework.getCapabilities().getMaxTextureSurfaceSize();
        if (width > maxDimension || height > maxDimension) {
            throw new SurfaceCreationException(
                    "Cannot create texture surface of requested size: beyond max supported dimension");
        }

        this.width = width;
        this.height = height;
        this.depthRenderBuffer = depthRenderBuffer;

        colorTargets = new TextureImpl.RenderTargetImpl[framework.getCapabilities().getMaxColorBuffers()];
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

    public void setRenderTargets(OpenGLContext ctx, Sampler.RenderTarget[] colorTargets,
                                 Sampler.RenderTarget depthTarget) {
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
                if (t.getWidth() != width || t.getHeight() != height) {
                    throw new IllegalArgumentException(String.format(
                            "Color buffer %d does not match surface dimensions, expected %d x %d but was %d x %d",
                            i, width, height, t.getWidth(), t.getHeight()));
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
            if (depthTarget.getSampler().getWidth() != width ||
                depthTarget.getSampler().getHeight() != height) {
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

        // notify the implementation to re-bind textures if necessary
        updateRenderTargets(ctx);
    }

    protected abstract void updateRenderTargets(OpenGLContext ctx);
}
