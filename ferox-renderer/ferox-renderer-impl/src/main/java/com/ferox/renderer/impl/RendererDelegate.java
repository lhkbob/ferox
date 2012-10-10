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
    private static final Vector4 DEFAULT_BLEND_COLOR = new Vector4(0f, 0f, 0f, 0f);

    // blending
    protected final Vector4 blendColor = new Vector4();
    protected BlendFunction blendFuncRgb = BlendFunction.ADD;
    protected BlendFunction blendFuncAlpha = BlendFunction.ADD;

    protected BlendFactor blendSrcRgb = BlendFactor.ONE;
    protected BlendFactor blendDstRgb = BlendFactor.ZERO;
    protected BlendFactor blendSrcAlpha = BlendFactor.ONE;
    protected BlendFactor blendDstAlpha = BlendFactor.ZERO;

    protected boolean blendEnabled = false;

    // color masking [red, green, blue, alpha]
    protected final boolean[] colorMask = new boolean[] {true, true, true, true};

    // depth offsets
    protected double depthOffsetFactor = 0;
    protected double depthOffsetUnits = 0;
    protected boolean depthOffsetEnabled = false;

    // depth test and mask
    protected Comparison depthTest = Comparison.LESS;
    protected boolean depthMask = true;

    // draw styles
    protected DrawStyle styleFront = DrawStyle.SOLID;
    protected DrawStyle styleBack = DrawStyle.NONE;

    // stencil test
    protected Comparison stencilTestFront = Comparison.ALWAYS;
    protected int stencilRefFront = 0;
    protected int stencilTestMaskFront = ~0;

    protected StencilUpdate stencilFailFront = StencilUpdate.KEEP;
    protected StencilUpdate depthFailFront = StencilUpdate.KEEP;
    protected StencilUpdate depthPassFront = StencilUpdate.KEEP;

    protected Comparison stencilTestBack = Comparison.ALWAYS;
    protected int stencilRefBack = 0;
    protected int stencilTestMaskBack = ~0;

    protected StencilUpdate stencilFailBack = StencilUpdate.KEEP;
    protected StencilUpdate depthFailBack = StencilUpdate.KEEP;
    protected StencilUpdate depthPassBack = StencilUpdate.KEEP;

    protected boolean stencilEnabled = false;

    // stencil mask
    protected int stencilMaskFront = ~0;
    protected int stencilMaskBack = ~0;

    // viewport
    protected int viewX = 0;
    protected int viewY = 0;
    protected int viewWidth = -1;
    protected int viewHeight = -1;

    protected int viewSurfaceWidth = -1;
    protected int viewSurfaceHeight = -1;

    // rendering
    protected VertexBufferObject indexBinding = null;
    protected VertexBufferObjectHandle indexBindingHandle = null;

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

        viewSurfaceWidth = surface.getWidth();
        viewSurfaceHeight = surface.getHeight();
    }

    public void reset() {
        // reset the portion of state described in Renderer
        setBlendColor(DEFAULT_BLEND_COLOR);
        setBlendMode(BlendFunction.ADD, BlendFactor.ONE, BlendFactor.ZERO);

        setColorWriteMask(true, true, true, true);

        setDepthOffsets(0f, 0f);
        setDepthOffsetsEnabled(false);

        setDepthTest(Comparison.LESS);
        setDepthWriteMask(true);

        setDrawStyle(DrawStyle.SOLID, DrawStyle.NONE);

        setStencilTest(Comparison.ALWAYS, 0, ~0);
        setStencilUpdateFront(StencilUpdate.KEEP, StencilUpdate.KEEP, StencilUpdate.KEEP);
        setStencilUpdateBack(StencilUpdate.KEEP, StencilUpdate.KEEP, StencilUpdate.KEEP);
        setStencilTestEnabled(false);
        setStencilWriteMask(~0);

        // manually unbind the index vbo
        if (indexBinding != null) {
            glBindElementVbo(null);
            resourceManager.unlock(indexBinding);
            indexBinding = null;
            indexBindingHandle = null;
        }

        // only reset viewport if we've been assigned valid dimensions
        if (viewSurfaceHeight >= 0 && viewSurfaceWidth >= 0) {
            setViewport(0, 0, viewSurfaceWidth, viewSurfaceHeight);
        }
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

        if (!blendColor.equals(color)) {
            blendColor.set(color);
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

        if (blendFuncAlpha != function) {
            blendFuncAlpha = function;
            glBlendEquations(blendFuncRgb, function);
        }

        if (blendSrcAlpha != src || blendDstAlpha != dst) {
            blendSrcAlpha = src;
            blendDstAlpha = dst;
            glBlendFactors(blendSrcRgb, blendDstRgb, src, dst);
        }
    }

    public void setBlendModeRgb(BlendFunction function, BlendFactor src, BlendFactor dst) {
        if (function == null || src == null || dst == null) {
            throw new NullPointerException("Cannot use null arguments: " + function + ", " + src + ", " + dst);
        }
        if (dst == BlendFactor.SRC_ALPHA_SATURATE) {
            throw new IllegalArgumentException("Cannot use SRC_ALPHA_SATURATE for dest BlendFactor");
        }

        if (blendFuncRgb != function) {
            blendFuncRgb = function;
            glBlendEquations(function, blendFuncAlpha);
        }

        if (blendSrcRgb != src || blendDstRgb != dst) {
            blendSrcRgb = src;
            blendDstRgb = dst;
            glBlendFactors(src, dst, blendSrcAlpha, blendDstAlpha);
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
        if (blendEnabled != enable) {
            blendEnabled = enable;
            glEnableBlending(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable blending.
     */
    protected abstract void glEnableBlending(boolean enable);

    public void setColorWriteMask(boolean red, boolean green, boolean blue, boolean alpha) {
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
        if (depthOffsetFactor != factor || depthOffsetUnits != units) {
            depthOffsetFactor = factor;
            depthOffsetUnits = units;
            glDepthOffset(factor, units);
        }
    }

    /**
     * Invoke OpenGL calls to configure depth offsets.
     */
    protected abstract void glDepthOffset(double factor, double units);

    public void setDepthOffsetsEnabled(boolean enable) {
        if (depthOffsetEnabled != enable) {
            depthOffsetEnabled = enable;
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
        if (depthTest != test) {
            depthTest = test;
            glDepthTest(test);
        }
    }

    /**
     * Invoke OpenGL calls to set the depth test
     */
    protected abstract void glDepthTest(Comparison test);

    public void setDepthWriteMask(boolean mask) {
        if (depthMask != mask) {
            depthMask = mask;
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
        if (styleFront != front || styleBack != back) {
            styleFront = front;
            styleBack = back;
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
        if (stencilMaskFront != front) {
            stencilMaskFront = front;
            glStencilMask(true, front);
        }

        if (stencilMaskBack != back) {
            stencilMaskBack = back;
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
            throw new NullPointerException("Stencil test");
        }
        if (stencilTestFront != test || stencilRefFront != refValue || stencilTestMaskFront != testMask) {
            stencilTestFront = test;
            stencilRefFront = refValue;
            stencilTestMaskFront = testMask;
            glStencilTest(test, refValue, testMask, true);
        }
    }

    public void setStencilTestFront(Comparison test, int refValue, int testMask) {
        if (test == null) {
            throw new NullPointerException("Stencil test");
        }
        if (stencilTestBack != test || stencilRefBack != refValue || stencilTestMaskBack != testMask) {
            stencilTestBack = test;
            stencilRefBack = refValue;
            stencilTestMaskBack = testMask;
            glStencilTest(test, refValue, testMask, false);
        }
    }

    /**
     * Invoke OpenGL calls to set the stencil test
     */
    protected abstract void glStencilTest(Comparison test, int refValue, int mask,
                                          boolean isFront);

    public void setStencilTestEnabled(boolean enable) {
        if (stencilEnabled != enable) {
            stencilEnabled = enable;
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
        if (stencilFailBack != stencilFail || depthFailBack != depthFail || depthPassBack != depthPass) {
            stencilFailBack = stencilFail;
            depthFailBack = depthFail;
            depthPassBack = depthPass;
            glStencilUpdate(stencilFail, depthFail, depthPass, false);
        }
    }

    public void setStencilUpdateFront(StencilUpdate stencilFail, StencilUpdate depthFail,
                                      StencilUpdate depthPass) {
        if (stencilFail == null || depthFail == null || depthPass == null) {
            throw new NullPointerException("Cannot have null arguments: " + stencilFail + ", " + depthFail + ", " + depthPass);
        }
        if (stencilFailFront != stencilFail || depthFailFront != depthFail || depthPassFront != depthPass) {
            stencilFailFront = stencilFail;
            depthFailFront = depthFail;
            depthPassFront = depthPass;
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
        if (x != viewX || y != viewY || width != viewWidth || height != viewHeight) {
            viewX = x;
            viewY = y;
            viewWidth = width;
            viewHeight = height;
            glViewport(x, y, width, height);
        }
    }

    /**
     * Invoke OpenGL calls to set the viewport
     */
    protected abstract void glViewport(int x, int y, int width, int height);

    public int render(PolygonType polyType, VertexBufferObject indices, int offset,
                      int count) {
        if (polyType == null || indices == null) {
            throw new NullPointerException("PolygonType and indices cannot be null");
        }
        if (offset < 0 || count < 0) {
            throw new IllegalArgumentException("First and count must be at least 0, not: " + offset + ", " + count);
        }

        if (count == 0) {
            return 0; // shortcut
        }

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

            VertexBufferObjectHandle newHandle = (VertexBufferObjectHandle) resourceManager.lock(context,
                                                                                                 indices);

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

    public int render(PolygonType polyType, int first, int count) {
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
