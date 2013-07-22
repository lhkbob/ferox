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
package com.ferox.renderer.impl.jogl;

import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.Renderer.*;
import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.RendererDelegate;
import com.ferox.renderer.impl.SharedState;
import com.ferox.renderer.impl.resources.BufferImpl;

import javax.media.opengl.GL2GL3;

/**
 * JoglRendererDelegate is a concrete implementation of RendererDelegate that uses the JOGL OpenGL binding.
 *
 * @author Michael Ludwig
 */
public class JoglRendererDelegate extends RendererDelegate {
    // capabilities
    private boolean initialized;

    // state tracking for buffer clearing
    private final Vector4 clearColor = new Vector4(0f, 0f, 0f, 0f);
    private double clearDepth = 1;
    private int clearStencil = 0;

    // state tracking for draw styles
    private boolean cullEnabled = true;
    private int frontPolyMode = GL2GL3.GL_FILL;
    private int backPolyMode = GL2GL3.GL_FILL;

    private GL2GL3 gl;

    /**
     * Create a new delegate that renders for the given context. The SharedState must be the same shared state
     * instance used by all renderers for the given context (so that it is shared).
     *
     * @param context     The context
     * @param sharedState The state
     *
     * @throws NullPointerException if arguments are null
     */
    public JoglRendererDelegate(OpenGLContext context, SharedState sharedState) {
        super(context, sharedState);
    }

    @Override
    protected void glBlendColor(@Const Vector4 color) {
        gl.glBlendColor((float) color.x, (float) color.y, (float) color.z, (float) color.w);
    }

    @Override
    protected void glBlendEquations(BlendFunction funcRgb, BlendFunction funcAlpha) {
        gl.glBlendEquationSeparate(Utils.getGLBlendEquation(funcRgb), Utils.getGLBlendEquation(funcAlpha));
    }

    @Override
    protected void glBlendFactors(BlendFactor srcRgb, BlendFactor dstRgb, BlendFactor srcAlpha,
                                  BlendFactor dstAlpha) {
        gl.glBlendFuncSeparate(Utils.getGLBlendFactor(srcRgb), Utils.getGLBlendFactor(dstRgb),
                               Utils.getGLBlendFactor(srcAlpha), Utils.getGLBlendFactor(dstAlpha));
    }

    @Override
    protected void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        gl.glColorMask(red, green, blue, alpha);
    }

    @Override
    protected void glDepthMask(boolean mask) {
        gl.glDepthMask(mask);
    }

    @Override
    protected void glDepthOffset(double factor, double units) {
        gl.glPolygonOffset((float) factor, (float) units);
    }

    @Override
    protected void glDepthTest(Comparison test) {
        gl.glDepthFunc(Utils.getGLPixelTest(test));
    }

    @Override
    protected void glDrawStyle(DrawStyle front, DrawStyle back) {
        int cullFace = 0;
        if (front == DrawStyle.NONE && back == DrawStyle.NONE) {
            cullFace = GL2GL3.GL_FRONT_AND_BACK;
        } else if (front == DrawStyle.NONE) {
            cullFace = GL2GL3.GL_FRONT;
        } else if (back == DrawStyle.NONE) {
            cullFace = GL2GL3.GL_BACK;
        }

        if (cullFace == 0) {
            // to show both sides, must disable culling
            if (cullEnabled) {
                cullEnabled = false;
                glEnable(GL2GL3.GL_CULL_FACE, false);
            }
        } else {
            if (!cullEnabled) {
                cullEnabled = true;
                glEnable(GL2GL3.GL_CULL_FACE, true);
            }
            gl.glCullFace(cullFace);
        }

        if (front != DrawStyle.NONE) {
            int frontMode = Utils.getGLPolygonMode(front);
            if (frontPolyMode != frontMode) {
                frontPolyMode = frontMode;
                gl.glPolygonMode(GL2GL3.GL_FRONT, frontMode);
            }
        }

        if (back != DrawStyle.NONE) {
            int backMode = Utils.getGLPolygonMode(back);
            if (backPolyMode != backMode) {
                backPolyMode = backMode;
                gl.glPolygonMode(GL2GL3.GL_BACK, backMode);
            }
        }
    }

    private void glEnable(int flag, boolean enable) {
        if (enable) {
            gl.glEnable(flag);
        } else {
            gl.glDisable(flag);
        }
    }

    @Override
    protected void glEnableBlending(boolean enable) {
        glEnable(GL2GL3.GL_BLEND, enable);
    }

    @Override
    protected void glEnableDepthOffset(boolean enable) {
        glEnable(GL2GL3.GL_POLYGON_OFFSET_LINE, enable);
        glEnable(GL2GL3.GL_POLYGON_OFFSET_POINT, enable);
        glEnable(GL2GL3.GL_POLYGON_OFFSET_FILL, enable);
    }

    @Override
    protected void glEnableStencilTest(boolean enable) {
        glEnable(GL2GL3.GL_STENCIL_TEST, enable);
    }

    @Override
    protected void glStencilMask(boolean front, int mask) {
        int face = (front ? GL2GL3.GL_FRONT : GL2GL3.GL_BACK);
        gl.glStencilMaskSeparate(face, mask);
    }

    @Override
    protected void glStencilTest(Comparison test, int refValue, int mask, boolean isFront) {
        int face = (isFront ? GL2GL3.GL_FRONT : GL2GL3.GL_BACK);
        gl.glStencilFuncSeparate(face, Utils.getGLPixelTest(test), refValue, mask);
    }

    @Override
    protected void glStencilUpdate(StencilUpdate stencilFail, StencilUpdate depthFail,
                                   StencilUpdate depthPass, boolean isFront) {
        int sf = Utils.getGLStencilOp(stencilFail);
        int df = Utils.getGLStencilOp(depthFail);
        int dp = Utils.getGLStencilOp(depthPass);

        int face = (isFront ? GL2GL3.GL_FRONT : GL2GL3.GL_BACK);
        gl.glStencilOpSeparate(face, sf, df, dp);
    }

    @Override
    protected void glViewport(int x, int y, int width, int height) {
        gl.glScissor(x, y, width, height);
        gl.glViewport(x, y, width, height);
    }

    @Override
    public void activate(AbstractSurface surface) {
        gl = ((JoglContext) context).getGLContext().getGL().getGL2GL3();
        if (!initialized) {
            // initial state configuration
            gl.glEnable(GL2GL3.GL_DEPTH_TEST);
            gl.glEnable(GL2GL3.GL_CULL_FACE);
            gl.glEnable(GL2GL3.GL_SCISSOR_TEST);

            initialized = true;
        }

        super.activate(surface);
    }

    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, @Const Vector4 color,
                      double depth, int stencil) {
        if (color == null) {
            throw new NullPointerException("Clear color cannot be null");
        }
        if (depth < 0f || depth > 1f) {
            throw new IllegalArgumentException("Clear depth must be in [0, 1], not: " + depth);
        }

        if (!this.clearColor.equals(color)) {
            this.clearColor.set(color);
            gl.glClearColor((float) color.x, (float) color.y, (float) color.z, (float) color.w);
        }
        if (this.clearDepth != depth) {
            this.clearDepth = depth;
            gl.glClearDepth(depth);
        }
        if (this.clearStencil != stencil) {
            this.clearStencil = stencil;
            gl.glClearStencil(stencil);
        }

        int clearBits = 0;
        if (clearColor) {
            clearBits |= GL2GL3.GL_COLOR_BUFFER_BIT;
        }
        if (clearDepth) {
            clearBits |= GL2GL3.GL_DEPTH_BUFFER_BIT;
        }
        if (clearStencil) {
            clearBits |= GL2GL3.GL_STENCIL_BUFFER_BIT;
        }

        if (clearBits != 0) {
            gl.glClear(clearBits);
        }
    }

    @Override
    protected void glDrawElements(PolygonType type, BufferImpl.BufferHandle h, int offset, int count) {
        int glPolyType = Utils.getGLPolygonConnectivity(type);
        int glDataType = Utils.getGLType(h.type);

        if (h.inmemoryBuffer != null) {
            h.inmemoryBuffer.clear().position(offset * h.type.getByteCount());
            gl.glDrawElements(glPolyType, count, glDataType, h.inmemoryBuffer);
        } else {
            gl.glDrawElements(glPolyType, count, glDataType, offset * h.type.getByteCount());
        }
    }

    @Override
    protected void glDrawArrays(PolygonType type, int first, int count) {
        int glPolyType = Utils.getGLPolygonConnectivity(type);
        gl.glDrawArrays(glPolyType, first, count);
    }
}
