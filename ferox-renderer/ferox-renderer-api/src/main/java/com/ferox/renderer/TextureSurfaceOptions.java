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

public final class TextureSurfaceOptions {
    private final int width;
    private final int height;

    private final Sampler.BaseFormat depthRenderBuffer;

    private final Sampler.RenderTarget[] colorTargets;
    private final Sampler.RenderTarget depthStencilTarget;


    public TextureSurfaceOptions() {
        this(1, 1, null, new Sampler.RenderTarget[0], null);
    }

    private TextureSurfaceOptions(int width, int height,
                                  Sampler.BaseFormat depthRenderBuffer,
                                  Sampler.RenderTarget[] colorTargets,
                                  Sampler.RenderTarget depthStencilTarget) {
        this.width = width;
        this.height = height;

        this.colorTargets = colorTargets;
        this.depthStencilTarget = depthStencilTarget;
        this.depthRenderBuffer = depthRenderBuffer;
    }

    public TextureSurfaceOptions colorBuffers(Sampler.RenderTarget... targets) {
        Sampler.RenderTarget[] copy = (targets == null ? new Sampler.RenderTarget[0]
                                                       : Arrays
                                               .copyOf(targets, targets.length));
        return new TextureSurfaceOptions(width, height, depthRenderBuffer, copy,
                                         depthStencilTarget);
    }

    public TextureSurfaceOptions depthBuffer(Sampler.RenderTarget target) {
        return new TextureSurfaceOptions(width, height, null, colorTargets, target);
    }

    public TextureSurfaceOptions size(int width, int height) {
        return new TextureSurfaceOptions(width, height, depthRenderBuffer, colorTargets,
                                         depthStencilTarget);
    }

    public TextureSurfaceOptions useDepthRenderBuffer() {
        return new TextureSurfaceOptions(width, height, Sampler.BaseFormat.DEPTH,
                                         colorTargets, null);
    }

    public TextureSurfaceOptions useDepthAndStencilRenderBuffer() {
        return new TextureSurfaceOptions(width, height, Sampler.BaseFormat.DEPTH_STENCIL,
                                         colorTargets, null);
    }

    public Sampler.RenderTarget getColorBuffer(int colorBuffer) {
        return (colorBuffer >= colorTargets.length ? null : colorTargets[colorBuffer]);
    }

    public Sampler.RenderTarget getDepthBuffer() {
        return depthStencilTarget;
    }

    public Sampler.BaseFormat getDepthRenderBufferFormat() {
        return depthRenderBuffer;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
