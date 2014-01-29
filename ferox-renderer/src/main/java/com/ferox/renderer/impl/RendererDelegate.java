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
import com.ferox.renderer.Renderer.*;
import com.ferox.renderer.ResourceException;
import com.ferox.renderer.impl.resources.BufferImpl;

/**
 * <p/>
 * The RendererDelegate is a utility class that exposes the same methods defined in {@link Renderer}, except
 * that it doesn't have responsibility for implementing render(). The public facing methods correctly track
 * OpenGL state, and when necessary delegate to protected functions whose responsibility is to invoke the
 * actual low-level graphics calls.
 * <p/>
 * It is recommended that the RendererDelegate is used with an {@link AbstractFixedFunctionRenderer} or an
 * {@link AbstractGlslRenderer} to compose the complete functionality of the different Renderer types.
 *
 * @author Michael Ludwig
 */
public abstract class RendererDelegate {
    protected final SharedState state;
    protected final SharedState defaultState;
    protected final OpenGLContext context;

    /**
     * Create a new delegate that renders for the given context. The SharedState must be the same shared state
     * instance used by all renderers for the given context (so that it is shared).
     *
     * @param context     The context
     * @param sharedState The state
     *
     * @throws NullPointerException if arguments are null
     */
    public RendererDelegate(OpenGLContext context, SharedState sharedState) {
        if (context == null || sharedState == null) {
            throw new NullPointerException("Arguments cannot be null");
        }

        this.context = context;
        state = sharedState;
        defaultState = new SharedState(sharedState.textures.length);
    }

    /**
     * Notify the renderer that the provided surface has been activated and will be using this Renderer. The
     * given context is the context for the current thread and the ResourceManager is the resource manager of
     * the surface's owning framework.
     *
     * @param surface The now active surface
     */
    public void activate(AbstractSurface surface) {
        state.viewWidth = surface.getWidth();
        state.viewHeight = surface.getHeight();
        glViewport(0, 0, surface.getWidth(), surface.getHeight());
    }

    public SharedState getCurrentState() {
        return new SharedState(state);
    }

    public void setCurrentState(SharedState state) {
        setBlendColor(state.blendColor);
        setBlendModeRgb(state.blendFuncRgb, state.blendSrcRgb, state.blendDstRgb);
        setBlendModeAlpha(state.blendFuncAlpha, state.blendSrcAlpha, state.blendDstAlpha);
        setBlendingEnabled(state.blendEnabled);

        setColorWriteMask(state.colorMask[0], state.colorMask[1], state.colorMask[2], state.colorMask[3]);

        setDepthOffsets(state.depthOffsetFactor, state.depthOffsetUnits);
        setDepthOffsetsEnabled(state.depthOffsetEnabled);
        setDepthTest(state.depthTest);
        setDepthWriteMask(state.depthMask);

        setDrawStyle(state.styleFront, state.styleBack);

        setStencilTestFront(state.stencilTestFront, state.stencilRefFront, state.stencilTestMaskFront);
        setStencilTestBack(state.stencilTestBack, state.stencilRefBack, state.stencilTestMaskBack);
        setStencilUpdateFront(state.stencilFailFront, state.depthFailFront, state.depthPassFront);
        setStencilUpdateBack(state.stencilFailBack, state.depthFailBack, state.depthPassBack);
        setStencilTestEnabled(state.stencilEnabled);

        setLineAntiAliasingEnabled(state.lineAAEnabled);
        setPointAntiAliasingEnabled(state.pointAAEnabled);
        setPolygonAntiAliasingEnabled(state.pointAAEnabled);

        setLineSize(state.lineWidth);
        setPointSize(state.pointWidth);

        if (state.viewWidth >= 0 && state.viewHeight >= 0) {
            setViewport(state.viewX, state.viewY, state.viewWidth, state.viewHeight);
        }

        // note that these bypass the destroyed check that throws an exception from the public interface
        setIndicesHandle(state.elementVBO);
        context.bindShader(state.shader);
        for (int i = 0; i < state.textures.length; i++) {
            context.bindTexture(i, state.textures[i]);
        }

        // so we have to clean out resource bindings that marked destroyed
        if (state.shader != null && state.shader.isDestroyed()) {
            context.bindShader(null);
        }
        if (state.arrayVBO != null && state.arrayVBO.isDestroyed()) {
            context.bindArrayVBO(null);
        }
        if (state.elementVBO != null && state.elementVBO.isDestroyed()) {
            context.bindElementVBO(null);
        }
        for (int i = 0; i < state.textures.length; i++) {
            if (state.textures[i] != null && state.textures[i].isDestroyed()) {
                context.bindTexture(i, null);
            }
        }
    }

    /**
     * Perform identical operations to {@link Renderer#clear(boolean, boolean, boolean, Vector4, double, int)}
     * . The color does not need to be clamped because OpenGL performs this for us.
     */
    public abstract void clear(boolean clearColor, boolean clearDepth, boolean clearStencil,
                               @Const Vector4 color, double depth, int stencil);

    public void setLineAntiAliasingEnabled(boolean enable) {
        if (state.lineAAEnabled != enable) {
            state.lineAAEnabled = enable;
            glEnableLineAntiAliasing(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable line aa
     */
    protected abstract void glEnableLineAntiAliasing(boolean enable);

    public void setLineSize(double width) {
        if (width < 1f) {
            throw new IllegalArgumentException("Line width must be at least 1, not: " + width);
        }
        if (state.lineWidth != width) {
            state.lineWidth = width;
            glLineWidth(width);
        }
    }

    /**
     * Invoke OpenGL calls to set line width
     */
    protected abstract void glLineWidth(double width);

    public void setPointAntiAliasingEnabled(boolean enable) {
        if (state.pointAAEnabled != enable) {
            state.pointAAEnabled = enable;
            glEnablePointAntiAliasing(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable point aa
     */
    protected abstract void glEnablePointAntiAliasing(boolean enable);

    public void setPointSize(double width) {
        if (width < 1.0) {
            throw new IllegalArgumentException("Point width must be at least 1, not: " + width);
        }
        if (state.pointWidth != width) {
            state.pointWidth = width;
            glPointWidth(width);
        }
    }

    /**
     * Invoke OpenGL calls to set point width
     */
    protected abstract void glPointWidth(double width);

    public void setPolygonAntiAliasingEnabled(boolean enable) {
        if (state.polyAAEnabled != enable) {
            state.polyAAEnabled = enable;
            glEnablePolyAntiAliasing(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable polygon aa
     */
    protected abstract void glEnablePolyAntiAliasing(boolean enable);

    public void setBlendColor(@Const Vector4 color) {
        if (color == null) {
            throw new NullPointerException("Null blend color");
        }

        if (!state.blendColor.equals(color)) {
            state.blendColor.set(color);
            glBlendColor(color);
        }
    }

    /**
     * Invoke OpenGL calls to set the blend color. The color does not need to be clamped because OpenGL clamps
     * it for us.
     */
    protected abstract void glBlendColor(@Const Vector4 color);

    public void setBlendMode(BlendFunction function, BlendFactor src, BlendFactor dst) {
        setBlendModeAlpha(function, src, dst);
        setBlendModeRgb(function, src, dst);
    }

    public void setBlendModeAlpha(BlendFunction function, BlendFactor src, BlendFactor dst) {
        if (function == null || src == null || dst == null) {
            throw new NullPointerException("Cannot use null arguments: " + function + ", " + src + ", " +
                                           dst);
        }
        if (dst == BlendFactor.SRC_ALPHA_SATURATE) {
            throw new IllegalArgumentException("Cannot use SRC_ALPHA_SATURATE for dest BlendFactor");
        }

        if (state.blendFuncAlpha != function) {
            state.blendFuncAlpha = function;
            glBlendEquations(state.blendFuncRgb, function);
        }

        if (state.blendSrcAlpha != src || state.blendDstAlpha != dst) {
            state.blendSrcAlpha = src;
            state.blendDstAlpha = dst;
            glBlendFactors(state.blendSrcRgb, state.blendDstRgb, src, dst);
        }
    }

    public void setBlendModeRgb(BlendFunction function, BlendFactor src, BlendFactor dst) {
        if (function == null || src == null || dst == null) {
            throw new NullPointerException("Cannot use null arguments: " + function + ", " + src + ", " +
                                           dst);
        }
        if (dst == BlendFactor.SRC_ALPHA_SATURATE) {
            throw new IllegalArgumentException("Cannot use SRC_ALPHA_SATURATE for dest BlendFactor");
        }

        if (state.blendFuncRgb != function) {
            state.blendFuncRgb = function;
            glBlendEquations(function, state.blendFuncAlpha);
        }

        if (state.blendSrcRgb != src || state.blendDstRgb != dst) {
            state.blendSrcRgb = src;
            state.blendDstRgb = dst;
            glBlendFactors(src, dst, state.blendSrcAlpha, state.blendDstAlpha);
        }
    }

    /**
     * Invoke OpenGL calls to set the blend factors.
     */
    protected abstract void glBlendFactors(BlendFactor srcRgb, BlendFactor dstRgb, BlendFactor srcAlpha,
                                           BlendFactor dstAlpha);

    /**
     * Invoke OpenGL calls to set the blend equations.
     */
    protected abstract void glBlendEquations(BlendFunction funcRgb, BlendFunction funcAlpha);

    public void setBlendingEnabled(boolean enable) {
        if (state.blendEnabled != enable) {
            state.blendEnabled = enable;
            glEnableBlending(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable blending.
     */
    protected abstract void glEnableBlending(boolean enable);

    public void setColorWriteMask(boolean red, boolean green, boolean blue, boolean alpha) {
        boolean[] colorMask = state.colorMask;
        if (colorMask[0] != red || colorMask[1] != green || colorMask[2] != blue ||
            colorMask[3] != alpha) {
            colorMask[0] = red;
            colorMask[1] = green;
            colorMask[2] = blue;
            colorMask[3] = alpha;

            glColorMask(red, green, blue, alpha);
        }
    }

    /**
     * Invoke OpenGL calls to configure the color mask.
     */
    protected abstract void glColorMask(boolean red, boolean green, boolean blue, boolean alpha);

    public void setDepthOffsets(double factor, double units) {
        if (state.depthOffsetFactor != factor || state.depthOffsetUnits != units) {
            state.depthOffsetFactor = factor;
            state.depthOffsetUnits = units;
            glDepthOffset(factor, units);
        }
    }

    /**
     * Invoke OpenGL calls to configure depth offsets.
     */
    protected abstract void glDepthOffset(double factor, double units);

    public void setDepthOffsetsEnabled(boolean enable) {
        if (state.depthOffsetEnabled != enable) {
            state.depthOffsetEnabled = enable;
            glEnableDepthOffset(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable depth offsets.
     */
    protected abstract void glEnableDepthOffset(boolean enable);

    public void setDepthTest(Comparison test) {
        if (test == null) {
            throw new NullPointerException("Null depth test");
        }
        if (state.depthTest != test) {
            state.depthTest = test;
            glDepthTest(test);
        }
    }

    /**
     * Invoke OpenGL calls to set the depth test
     */
    protected abstract void glDepthTest(Comparison test);

    public void setDepthWriteMask(boolean mask) {
        if (state.depthMask != mask) {
            state.depthMask = mask;
            glDepthMask(mask);
        }
    }

    /**
     * Invoke OpenGL calls to set the depth mask
     */
    protected abstract void glDepthMask(boolean mask);

    public void setDrawStyle(DrawStyle style) {
        setDrawStyle(style, style);
    }

    public void setDrawStyle(DrawStyle front, DrawStyle back) {
        if (front == null || back == null) {
            throw new NullPointerException("Null DrawStyle: " + front + ", " + back);
        }
        if (state.styleFront != front || state.styleBack != back) {
            state.styleFront = front;
            state.styleBack = back;
            glDrawStyle(front, back);
        }
    }

    /**
     * Invoke OpenGL calls to set the draw styles
     */
    protected abstract void glDrawStyle(DrawStyle front, DrawStyle back);

    public void setStencilWriteMask(int mask) {
        setStencilWriteMask(mask, mask);
    }

    public void setStencilWriteMask(int front, int back) {
        if (state.stencilMaskFront != front) {
            state.stencilMaskFront = front;
            glStencilMask(true, front);
        }

        if (state.stencilMaskBack != back) {
            state.stencilMaskBack = back;
            glStencilMask(false, back);
        }
    }

    /**
     * Invoke OpenGL calls to set the stencil masks
     */
    protected abstract void glStencilMask(boolean front, int mask);

    public void setStencilTest(Comparison test, int refValue, int testMask) {
        setStencilTestFront(test, refValue, testMask);
        setStencilTestBack(test, refValue, testMask);
    }

    public void setStencilTestBack(Comparison test, int refValue, int testMask) {
        if (test == null) {
            throw new NullPointerException("Stencil test comparison can't be null");
        }
        if (state.stencilTestFront != test || state.stencilRefFront != refValue ||
            state.stencilTestMaskFront != testMask) {
            state.stencilTestFront = test;
            state.stencilRefFront = refValue;
            state.stencilTestMaskFront = testMask;
            glStencilTest(test, refValue, testMask, true);
        }
    }

    public void setStencilTestFront(Comparison test, int refValue, int testMask) {
        if (test == null) {
            throw new NullPointerException("Stencil test comparison can't be null");
        }
        if (state.stencilTestBack != test || state.stencilRefBack != refValue ||
            state.stencilTestMaskBack != testMask) {
            state.stencilTestBack = test;
            state.stencilRefBack = refValue;
            state.stencilTestMaskBack = testMask;
            glStencilTest(test, refValue, testMask, false);
        }
    }

    /**
     * Invoke OpenGL calls to set the stencil test
     */
    protected abstract void glStencilTest(Comparison test, int refValue, int mask, boolean isFront);

    public void setStencilTestEnabled(boolean enable) {
        if (state.stencilEnabled != enable) {
            state.stencilEnabled = enable;
            glEnableStencilTest(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable the stencil test
     */
    protected abstract void glEnableStencilTest(boolean enable);

    public void setStencilUpdateBack(StencilUpdate stencilFail, StencilUpdate depthFail,
                                     StencilUpdate depthPass) {
        if (stencilFail == null || depthFail == null || depthPass == null) {
            throw new NullPointerException("Cannot have null arguments: " + stencilFail + ", " + depthFail +
                                           ", " + depthPass);
        }
        if (state.stencilFailBack != stencilFail || state.depthFailBack != depthFail ||
            state.depthPassBack != depthPass) {
            state.stencilFailBack = stencilFail;
            state.depthFailBack = depthFail;
            state.depthPassBack = depthPass;
            glStencilUpdate(stencilFail, depthFail, depthPass, false);
        }
    }

    public void setStencilUpdateFront(StencilUpdate stencilFail, StencilUpdate depthFail,
                                      StencilUpdate depthPass) {
        if (stencilFail == null || depthFail == null || depthPass == null) {
            throw new NullPointerException("Cannot have null arguments: " + stencilFail + ", " + depthFail +
                                           ", " + depthPass);
        }
        if (state.stencilFailFront != stencilFail || state.depthFailFront != depthFail ||
            state.depthPassFront != depthPass) {
            state.stencilFailFront = stencilFail;
            state.depthFailFront = depthFail;
            state.depthPassFront = depthPass;
            glStencilUpdate(stencilFail, depthFail, depthPass, true);
        }
    }

    /**
     * Invoke OpenGL calls to set the StencilOps
     */
    protected abstract void glStencilUpdate(StencilUpdate stencilFail, StencilUpdate depthFail,
                                            StencilUpdate depthPass, boolean isFront);

    public void setViewport(int x, int y, int width, int height) {
        if (x < 0 || y < 0 || width < 0 || height < 0) {
            throw new IllegalArgumentException("Invalid arguments, all must be positive: " + x + ", " + y +
                                               ", " +
                                               width + ", " + height);
        }
        if (x != state.viewX || y != state.viewY || width != state.viewWidth ||
            height != state.viewHeight) {
            state.viewX = x;
            state.viewY = y;
            state.viewWidth = width;
            state.viewHeight = height;
            glViewport(x, y, width, height);
        }
    }

    /**
     * Invoke OpenGL calls to set the viewport
     */
    protected abstract void glViewport(int x, int y, int width, int height);

    public void setIndices(ElementBuffer indices) {
        if (indices == null) {
            setIndicesHandle(null);
        } else {
            if (indices.isDestroyed()) {
                throw new ResourceException("Cannot use a destroyed resource");
            }
            setIndicesHandle(((BufferImpl) indices).getHandle());
        }
    }

    private void setIndicesHandle(BufferImpl.BufferHandle indices) {
        if (state.elementVBO != indices) {
            // Must bind a new element vbo
            context.bindElementVBO(indices);
        }
    }

    public int render(PolygonType polyType, int offset, int count) {
        if (polyType == null) {
            throw new NullPointerException("PolygonType cannot be null");
        }
        if (offset < 0 || count < 0) {
            throw new IllegalArgumentException("First and count must be at least 0, not: " + offset + ", " +
                                               count);
        }

        if (state.elementVBO != null) {
            // Element vbo is bound this time (or from a previous rendering)
            glDrawElements(polyType, state.elementVBO, offset, count);
        } else {
            // use glDrawArrays
            glDrawArrays(polyType, offset, count);
        }

        return polyType.getPolygonCount(count);
    }

    /**
     * Perform the glDrawElements rendering command. The inputs will be valid.
     */
    protected abstract void glDrawElements(PolygonType type, BufferImpl.BufferHandle handle, int offset,
                                           int count);

    /**
     * Perform the glDrawArrays rendering command. The inputs will be valid.
     */
    protected abstract void glDrawArrays(PolygonType type, int first, int count);
}
