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
import com.ferox.renderer.Renderer;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.renderer.Renderer.StencilUpdate;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.VertexBufferObject;

/**
 * <p>
 * The RendererDelegate is a utility class that exposes the same methods defined
 * in {@link Renderer}, except that it doesn't have responsibility for
 * implementing render(). The public facing methods correctly track OpenGL
 * state, and when necessary delegate to protected functions whose
 * responsibility is to invoke the actual low-level graphics calls.
 * </p>
 * <p>
 * It is recommended that the RendererDelegate is used with an
 * {@link AbstractFixedFunctionRenderer} or an {@link AbstractGlslRenderer} to
 * create the complete functionality of the different Renderer types.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class RendererDelegate {
    protected RendererState state;
    protected RendererState defaultState;

    protected VertexBufferObjectHandle indexBindingHandle;
    protected VertexBufferObject indexBinding;

    protected OpenGLContext context;
    protected ResourceManager resourceManager;

    /**
     * Notify the renderer that the provided surface has been activated and will
     * be using this Renderer. The given context is the context for the current
     * thread and the ResourceManager is the resource manager of the surface's
     * owning framework.
     * 
     * @param active The now active surface
     * @param context The current context
     * @param resourceManager The ResourceManager to use
     */
    public void activate(AbstractSurface surface, OpenGLContext context,
                         ResourceManager resourceManager) {
        this.context = context;
        this.resourceManager = resourceManager;

        if (state == null) {
            // init state
            defaultState = new RendererState(surface);
            state = new RendererState(defaultState);
        }

        defaultState.viewWidth = surface.getWidth();
        defaultState.viewHeight = surface.getHeight();
    }

    public RendererState getCurrentState() {
        return new RendererState(state);
    }

    public void setCurrentState(RendererState state) {
        setBlendColor(state.blendColor);
        setBlendModeRgb(state.blendFuncRgb, state.blendSrcRgb, state.blendDstRgb);
        setBlendModeAlpha(state.blendFuncAlpha, state.blendSrcAlpha, state.blendDstAlpha);
        setBlendingEnabled(state.blendEnabled);

        setColorWriteMask(state.colorMask[0], state.colorMask[1], state.colorMask[2],
                          state.colorMask[3]);

        setDepthOffsets(state.depthOffsetFactor, state.depthOffsetUnits);
        setDepthOffsetsEnabled(state.depthOffsetEnabled);
        setDepthTest(state.depthTest);
        setDepthWriteMask(state.depthMask);

        setDrawStyle(state.styleFront, state.styleBack);

        setStencilTestFront(state.stencilTestFront, state.stencilRefFront,
                            state.stencilTestMaskFront);
        setStencilTestBack(state.stencilTestBack, state.stencilRefBack,
                           state.stencilTestMaskBack);
        setStencilUpdateFront(state.stencilFailFront, state.depthFailFront,
                              state.depthPassFront);
        setStencilUpdateBack(state.stencilFailBack, state.depthFailBack,
                             state.depthPassBack);
        setStencilTestEnabled(state.stencilEnabled);

        setViewport(state.viewX, state.viewY, state.viewWidth, state.viewHeight);
    }

    /**
     * Perform identical operations to
     * {@link Renderer#clear(boolean, boolean, boolean, ReadOnlyVector4f, double, int)}
     * . The color does not need to be clamped because OpenGL performs this for
     * us.
     */
    public abstract void clear(boolean clearColor, boolean clearDepth,
                               boolean clearStencil, @Const Vector4 color, double depth,
                               int stencil);

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
     * Invoke OpenGL calls to set the blend color. The color does not need to be
     * clamped because OpenGL clamps it for us.
     */
    protected abstract void glBlendColor(@Const Vector4 color);

    public void setBlendMode(BlendFunction function, BlendFactor src, BlendFactor dst) {
        setBlendModeAlpha(function, src, dst);
        setBlendModeRgb(function, src, dst);
    }

    public void setBlendModeAlpha(BlendFunction function, BlendFactor src, BlendFactor dst) {
        if (function == null || src == null || dst == null) {
            throw new NullPointerException("Cannot use null arguments: " + function + ", " + src + ", " + dst);
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
            throw new NullPointerException("Cannot use null arguments: " + function + ", " + src + ", " + dst);
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
    protected abstract void glBlendFactors(BlendFactor srcRgb, BlendFactor dstRgb,
                                           BlendFactor srcAlpha, BlendFactor dstAlpha);

    /**
     * Invoke OpenGL calls to set the blend equations.
     */
    protected abstract void glBlendEquations(BlendFunction funcRgb,
                                             BlendFunction funcAlpha);

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
        if (colorMask[0] != red || colorMask[1] != green || colorMask[2] != blue || colorMask[3] != alpha) {
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
    protected abstract void glColorMask(boolean red, boolean green, boolean blue,
                                        boolean alpha);

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
        if (state.stencilTestFront != test || state.stencilRefFront != refValue || state.stencilTestMaskFront != testMask) {
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
        if (state.stencilTestBack != test || state.stencilRefBack != refValue || state.stencilTestMaskBack != testMask) {
            state.stencilTestBack = test;
            state.stencilRefBack = refValue;
            state.stencilTestMaskBack = testMask;
            glStencilTest(test, refValue, testMask, false);
        }
    }

    /**
     * Invoke OpenGL calls to set the stencil test
     */
    protected abstract void glStencilTest(Comparison test, int refValue, int mask,
                                          boolean isFront);

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
            throw new NullPointerException("Cannot have null arguments: " + stencilFail + ", " + depthFail + ", " + depthPass);
        }
        if (state.stencilFailBack != stencilFail || state.depthFailBack != depthFail || state.depthPassBack != depthPass) {
            state.stencilFailBack = stencilFail;
            state.depthFailBack = depthFail;
            state.depthPassBack = depthPass;
            glStencilUpdate(stencilFail, depthFail, depthPass, false);
        }
    }

    public void setStencilUpdateFront(StencilUpdate stencilFail, StencilUpdate depthFail,
                                      StencilUpdate depthPass) {
        if (stencilFail == null || depthFail == null || depthPass == null) {
            throw new NullPointerException("Cannot have null arguments: " + stencilFail + ", " + depthFail + ", " + depthPass);
        }
        if (state.stencilFailFront != stencilFail || state.depthFailFront != depthFail || state.depthPassFront != depthPass) {
            state.stencilFailFront = stencilFail;
            state.depthFailFront = depthFail;
            state.depthPassFront = depthPass;
            glStencilUpdate(stencilFail, depthFail, depthPass, true);
        }
    }

    /**
     * Invoke OpenGL calls to set the StencilOps
     */
    protected abstract void glStencilUpdate(StencilUpdate stencilFail,
                                            StencilUpdate depthFail,
                                            StencilUpdate depthPass, boolean isFront);

    public void setViewport(int x, int y, int width, int height) {
        if (x < 0 || y < 0 || width < 0 || height < 0) {
            throw new IllegalArgumentException("Invalid arguments, all must be positive: " + x + ", " + y + ", " + width + ", " + height);
        }
        if (x != state.viewX || y != state.viewY || width != state.viewWidth || height != state.viewHeight) {
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

    private void setIndexBuffer(VertexBufferObject indices) {
        if (indexBinding != indices) {
            // Must bind a new element vbo
            boolean hadOldIndices = indexBinding != null;
            boolean failTypeCheck = false;

            if (hadOldIndices) {
                // Unlock old vbo first
                resourceManager.unlock(indexBinding);
                indexBinding = null;
                indexBindingHandle = null;
            }

            VertexBufferObjectHandle newHandle = null;
            if (indices != null) {
                newHandle = (VertexBufferObjectHandle) resourceManager.lock(context,
                                                                            indices);
            }

            // check if the actual VBO is of the correct type (must use handle, can't rely
            // on the resource reporting the most up-to-date type)
            if (newHandle != null && newHandle.dataType == DataType.FLOAT) {
                failTypeCheck = true;
                resourceManager.unlock(indices);
                newHandle = null;
            }

            // Handle actual binding of the vbo
            if (newHandle != null) {
                indexBinding = indices;
                indexBindingHandle = newHandle;
                glBindElementVbo(newHandle);
            } else if (hadOldIndices) {
                // Since we can't bind the new vbo, make sure the old
                // one is unbound since we've already unlocked it
                glBindElementVbo(null);
            }

            if (failTypeCheck) {
                throw new IllegalArgumentException("VertexBufferObject cannot have a type of FLOAT");
            }
        }
    }

    public int renderElements(PolygonType polyType, VertexBufferObject indices,
                              int offset, int count) {
        if (polyType == null || indices == null) {
            throw new NullPointerException("PolygonType and indices cannot be null");
        }
        if (offset < 0 || count < 0) {
            throw new IllegalArgumentException("First and count must be at least 0, not: " + offset + ", " + count);
        }

        if (count == 0) {
            return 0; // shortcut
        }

        setIndexBuffer(indices);

        if (indexBinding == null) {
            // No element vbo to work with, so we can't render anything
            return 0;
        } else {
            // check if the actual VBO is of the correct size (must use handle can't
            // rely on the resource reporting the most up-to-date size)
            if ((offset + count) > indexBindingHandle.length) {
                throw new IllegalArgumentException("Index and count access elements outside of VBO range");
            }

            // Element vbo is bound this time (or from a previous rendering)
            glDrawElements(polyType, indexBindingHandle, offset, count);
        }

        return polyType.getPolygonCount(count);
    }

    /**
     * Perform the glDrawElements rendering command. The inputs will be valid.
     */
    protected abstract void glDrawElements(PolygonType type,
                                           VertexBufferObjectHandle handle, int offset,
                                           int count);

    public int renderArray(PolygonType polyType, int first, int count) {
        if (polyType == null) {
            throw new NullPointerException("PolygonType cannot be null");
        }
        if (first < 0 || count < 0) {
            throw new IllegalArgumentException("First and count must be at least 0, not: " + first + ", " + count);
        }

        // short cut
        if (count == 0) {
            return 0;
        }

        glDrawArrays(polyType, first, count);
        return polyType.getPolygonCount(count);
    }

    /**
     * Perform the glDrawArrays rendering command. The inputs will be valid.
     */
    protected abstract void glDrawArrays(PolygonType type, int first, int count);

    /**
     * Bind the given resource handle as the element array vbo. If null, unbind
     * the array vbo.
     */
    protected abstract void glBindElementVbo(VertexBufferObjectHandle handle);
}
