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

import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.ElementBuffer;
import com.ferox.renderer.Renderer;

/**
 * <p/>
 * AbstractRenderer is the main super class for renderers used by AbstractFrameworks. It takes a single
 * RendererDelegate that handles the actual implementation of the Renderer interface. It is extended by both
 * {@link AbstractFixedFunctionRenderer} and {@link AbstractGlslRenderer}, which complete the implementations
 * for the respective renderer types. Contexts don't need to reuse the same delegate instance between their
 * two renderers because the shared state is tracked independently.
 * <p/>
 * AbstractRenderer adds the {@link #activate(AbstractSurface)} method which is invoked by AbstractSurface
 * when it is activated. This provides a hook for renderers to perform custom initialization of the OpenGL
 * state.
 *
 * @author Michael Ludwig
 */
public abstract class AbstractRenderer implements Renderer, Activateable {
    private static final Vector4 BLACK = new Vector4(0, 0, 0, 0);

    protected final RendererDelegate delegate;
    protected final OpenGLContext context;

    public AbstractRenderer(OpenGLContext context, RendererDelegate delegate) {
        if (delegate == null) {
            throw new NullPointerException("Delegate cannot be null");
        }
        if (delegate.context != context) {
            throw new IllegalArgumentException("Delegate's context is not equal to renderer's context");
        }

        this.context = context;
        this.delegate = delegate;
    }

    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, @Const Vector4 color,
                      double depth, int stencil) {
        delegate.clear(clearColor, clearDepth, clearStencil, color, depth, stencil);
    }

    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil) {
        clear(clearColor, clearDepth, clearStencil, BLACK, 1.0, 0);
    }

    @Override
    public void setBlendColor(@Const Vector4 color) {
        delegate.setBlendColor(color);
    }

    @Override
    public void setBlendMode(BlendFunction function, BlendFactor src, BlendFactor dst) {
        delegate.setBlendMode(function, src, dst);
    }

    @Override
    public void setBlendModeAlpha(BlendFunction function, BlendFactor src, BlendFactor dst) {
        delegate.setBlendModeAlpha(function, src, dst);
    }

    @Override
    public void setBlendModeRgb(BlendFunction function, BlendFactor src, BlendFactor dst) {
        delegate.setBlendModeRgb(function, src, dst);
    }

    @Override
    public void setBlendingEnabled(boolean enable) {
        delegate.setBlendingEnabled(enable);
    }

    @Override
    public void setColorWriteMask(boolean red, boolean green, boolean blue, boolean alpha) {
        delegate.setColorWriteMask(red, green, blue, alpha);
    }

    @Override
    public void setDepthOffsets(double factor, double units) {
        delegate.setDepthOffsets(factor, units);
    }

    @Override
    public void setDepthOffsetsEnabled(boolean enable) {
        delegate.setDepthOffsetsEnabled(enable);
    }

    @Override
    public void setDepthTest(Comparison test) {
        delegate.setDepthTest(test);
    }

    @Override
    public void setDepthWriteMask(boolean mask) {
        delegate.setDepthWriteMask(mask);
    }

    @Override
    public void setDrawStyle(DrawStyle style) {
        delegate.setDrawStyle(style);
    }

    @Override
    public void setDrawStyle(DrawStyle front, DrawStyle back) {
        delegate.setDrawStyle(front, back);
    }

    @Override
    public void setStencilTest(Comparison test, int refValue, int testMask) {
        delegate.setStencilTest(test, refValue, testMask);
    }

    @Override
    public void setStencilTestBack(Comparison test, int refValue, int testMask) {
        delegate.setStencilTestBack(test, refValue, testMask);
    }

    @Override
    public void setStencilTestEnabled(boolean enable) {
        delegate.setStencilTestEnabled(enable);
    }

    @Override
    public void setStencilTestFront(Comparison test, int refValue, int testMask) {
        delegate.setStencilTestFront(test, refValue, testMask);
    }

    @Override
    public void setStencilUpdate(StencilUpdate stencilFail, StencilUpdate depthFail,
                                 StencilUpdate depthPass) {
        delegate.setStencilUpdateFront(stencilFail, depthFail, depthPass);
        delegate.setStencilUpdateBack(stencilFail, depthFail, depthPass);
    }

    @Override
    public void setStencilUpdateFront(StencilUpdate stencilFail, StencilUpdate depthFail,
                                      StencilUpdate depthPass) {
        delegate.setStencilUpdateFront(stencilFail, depthFail, depthPass);
    }

    @Override
    public void setStencilUpdateBack(StencilUpdate stencilFail, StencilUpdate depthFail,
                                     StencilUpdate depthPass) {
        delegate.setStencilUpdateBack(stencilFail, depthFail, depthPass);
    }

    @Override
    public void setStencilWriteMask(int mask) {
        delegate.setStencilWriteMask(mask);
    }

    @Override
    public void setStencilWriteMask(int front, int back) {
        delegate.setStencilWriteMask(front, back);
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        delegate.setViewport(x, y, width, height);
    }

    @Override
    public void setIndices(ElementBuffer indices) {
        delegate.setIndices(indices);
    }

    @Override
    public int render(PolygonType polyType, int first, int count) {
        return delegate.render(polyType, first, count);
    }

    @Override
    public void activate(AbstractSurface active) {
        delegate.activate(active);
    }
}
