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

import com.ferox.renderer.*;
import com.ferox.renderer.impl.resources.AbstractResource;

/**
 * HardwareAccessLayerImpl is a simple implementation of {@link HardwareAccessLayer} for use with the
 * AbstractFramework. It uses the ContextManager and ResourceManager of the framework to manage the active
 * surfaces and handle resource operations.
 *
 * @author Michael Ludwig
 */
public class HardwareAccessLayerImpl implements HardwareAccessLayer {
    private final FrameworkImpl framework;

    private ContextImpl currentContext;

    public HardwareAccessLayerImpl(FrameworkImpl framework) {
        this.framework = framework;
    }

    @Override
    public Context setActiveSurface(Surface surface) {
        if (surface.getFramework() != framework) {
            throw new IllegalArgumentException("Surface not created by this framework");
        }

        AbstractSurface s = (AbstractSurface) surface;
        OpenGLContext context = framework.getContextManager().setActiveSurface(s);
        if (context == null) {
            currentContext = null;
        } else {
            currentContext = new ContextImpl(context, s);
        }

        return currentContext;
    }

    @Override
    public Context setActiveSurface(TextureSurface surface, Sampler.RenderTarget singleColorBuffer) {
        return setActiveSurface(surface, singleColorBuffer, null);
    }

    @Override
    public Context setActiveSurface(TextureSurface surface, Sampler.RenderTarget singleColorBuffer,
                                    Sampler.RenderTarget depthBuffer) {
        Sampler.RenderTarget[] targets = new Sampler.RenderTarget[] { singleColorBuffer };
        return setActiveSurface(surface, targets, depthBuffer);
    }

    @Override
    public Context setActiveSurface(TextureSurface surface, Sampler.RenderTarget[] colorBuffers,
                                    Sampler.RenderTarget depthBuffer) {
        Context ctx = setActiveSurface(surface);
        if (ctx != null) {
            ((AbstractTextureSurface) surface).setRenderTargets(colorBuffers, depthBuffer);
        }

        return ctx;
    }

    @Override
    public Context getCurrentContext() {
        return currentContext;
    }

    @Override
    public void refresh(Resource resource) {
        OpenGLContext ctx = (currentContext == null ? framework.getContextManager().ensureContext()
                                                    : currentContext.context);
        framework.getResourceFactory().refresh(ctx, (AbstractResource<?>) resource);
    }

    @Override
    public void destroy(Resource resource) {
        OpenGLContext ctx = (currentContext == null ? framework.getContextManager().ensureContext()
                                                    : currentContext.context);
        ((AbstractResource<?>) resource).getHandle().destroy(ctx);
    }

    private class ContextImpl implements Context {
        private final OpenGLContext context;
        private final AbstractSurface surface;

        private Renderer selectedRenderer;

        public ContextImpl(OpenGLContext context, AbstractSurface surface) {
            this.context = context;
            this.surface = surface;
        }

        @Override
        public GlslRenderer getGlslRenderer() {
            if (selectedRenderer == null) {
                // need to select a renderer
                selectedRenderer = context.getGlslRenderer();
            }

            if (selectedRenderer instanceof FixedFunctionRenderer) {
                throw new IllegalStateException("FixedFunctionRenderer already selected");
            } else {
                return (GlslRenderer) selectedRenderer; // may be null
            }
        }

        @Override
        public FixedFunctionRenderer getFixedFunctionRenderer() {
            if (selectedRenderer == null) {
                // need to select a renderer
                selectedRenderer = context.getFixedFunctionRenderer();
            }

            if (selectedRenderer instanceof GlslRenderer) {
                throw new IllegalStateException("GlslRenderer already selected");
            } else {
                return (FixedFunctionRenderer) selectedRenderer; // may be null
            }
        }

        @Override
        public void flush() {
            surface.flush(context);
        }

        @Override
        public Surface getSurface() {
            return surface;
        }
    }
}
