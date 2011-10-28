package com.ferox.renderer.impl.lwjgl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import com.ferox.math.Color3f;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Renderer.StencilOp;
import com.ferox.renderer.impl.RendererDelegate;

public class LwjglRendererDelegate extends RendererDelegate {
    // state tracking for buffer clearing
    private final Color3f clearColor = new Color3f(0f, 0f, 0f, 0f);
    private float clearDepth = 1f;
    private int clearStencil = 0;
    
    // state tracking for draw styles
    private boolean cullEnabled = true;
    private int frontPolyMode = GL11.GL_FILL;
    private int backPolyMode = GL11.GL_FILL;
    
    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, Color3f color, float depth, int stencil) {
        if (color == null)
            throw new NullPointerException("Clear color cannot be null");
        if (depth < 0f || depth > 1f)
            throw new IllegalArgumentException("Clear depht must be in [0, 1], not: " + depth);
        
        if (!this.clearColor.equals(color)) {
            this.clearColor.set(color);
            GL11.glClearColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
        if (this.clearDepth != depth) {
            this.clearDepth = depth;
            GL11.glClearDepth(depth);
        }
        if (this.clearStencil != stencil) {
            this.clearStencil = stencil;
            GL11.glClearStencil(stencil);
        }
        
        int clearBits = 0;
        if (clearColor)
            clearBits |= GL11.GL_COLOR_BUFFER_BIT;
        if (clearDepth)
            clearBits |= GL11.GL_DEPTH_BUFFER_BIT;
        if (clearStencil)
            clearBits |= GL11.GL_STENCIL_BUFFER_BIT;
        
        if (clearBits != 0)
            GL11.glClear(clearBits);
    }

    @Override
    protected void glBlendColor(Color3f color) {
        GL14.glBlendColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    @Override
    protected void glBlendEquations(BlendFunction funcRgb, BlendFunction funcAlpha) {
        // FIXME: this is a high version, must be ready to fallback based on version
        GL20.glBlendEquationSeparate(Utils.getGLBlendEquation(funcRgb), 
                                     Utils.getGLBlendEquation(funcAlpha));
    }

    @Override
    protected void glBlendFactors(BlendFactor srcRgb, BlendFactor dstRgb, BlendFactor srcAlpha, BlendFactor dstAlpha) {
        GL14.glBlendFuncSeparate(Utils.getGLBlendFactor(srcRgb), Utils.getGLBlendFactor(dstRgb), 
                                 Utils.getGLBlendFactor(srcAlpha), Utils.getGLBlendFactor(dstAlpha));
    }

    @Override
    protected void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GL11.glColorMask(red, green, blue, alpha);
    }

    @Override
    protected void glDepthMask(boolean mask) {
        GL11.glDepthMask(mask);
    }

    @Override
    protected void glDepthOffset(float factor, float units) {
        GL11.glPolygonOffset(factor, units);
    }

    @Override
    protected void glDepthTest(Comparison test) {
        GL11.glDepthFunc(Utils.getGLPixelTest(test));
    }

    @Override
    protected void glDrawStyle(DrawStyle front, DrawStyle back) {
        int cullFace = 0;
        if (front == DrawStyle.NONE && back == DrawStyle.NONE) {
            cullFace = GL11.GL_FRONT_AND_BACK;
        } else if (front == DrawStyle.NONE) {
            cullFace = GL11.GL_FRONT;
        } else if (back == DrawStyle.NONE) {
            cullFace = GL11.GL_BACK;
        }
        
        if (cullFace == 0) {
            // to show both sides, must disable culling
            if (cullEnabled) {
                cullEnabled = false;
                glEnable(GL11.GL_CULL_FACE, false);
            }
        } else {
            if (!cullEnabled) {
                cullEnabled = true;
                glEnable(GL11.GL_CULL_FACE, true);
            }
            GL11.glCullFace(cullFace);
        }
        
        if (front != DrawStyle.NONE) {
            int frontMode = Utils.getGLPolygonMode(front);
            if (frontPolyMode != frontMode) {
                frontPolyMode = frontMode;
                GL11.glPolygonMode(GL11.GL_FRONT, frontMode);
            }
        }
        
        if (back != DrawStyle.NONE) {
            int backMode = Utils.getGLPolygonMode(back);
            if (backPolyMode != backMode) {
                backPolyMode = backMode;
                GL11.glPolygonMode(GL11.GL_BACK, backMode);
            }
        }
    }
    
    private void glEnable(int flag, boolean enable) {
        if (enable)
            GL11.glEnable(flag);
        else
            GL11.glDisable(flag);
    }

    @Override
    protected void glEnableBlending(boolean enable) {
        glEnable(GL11.GL_BLEND, enable);
    }

    @Override
    protected void glEnableDepthOffset(boolean enable) {
        glEnable(GL11.GL_POLYGON_OFFSET_LINE, enable);
        glEnable(GL11.GL_POLYGON_OFFSET_POINT, enable);
        glEnable(GL11.GL_POLYGON_OFFSET_FILL, enable);
    }

    @Override
    protected void glEnableStencilTest(boolean enable) {
        glEnable(GL11.GL_STENCIL_TEST, enable);
    }

    @Override
    protected void glStencilMask(boolean front, int mask) {
        int face = (front ? GL11.GL_FRONT : GL11.GL_BACK);
        // FIXME: also too high, need to be able to fall back
        GL20.glStencilMaskSeparate(face, mask);
    }

    @Override
    protected void glStencilTest(Comparison test, int refValue, int mask, boolean isFront) {
        int face = (isFront ? GL11.GL_FRONT : GL11.GL_BACK);
        GL20.glStencilFuncSeparate(face, Utils.getGLPixelTest(test), refValue, mask);
    }

    @Override
    protected void glStencilUpdate(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass, boolean isFront) {
        int sf = Utils.getGLStencilOp(stencilFail);
        int df = Utils.getGLStencilOp(depthFail);
        int dp = Utils.getGLStencilOp(depthPass);
        
        int face = (isFront ? GL11.GL_FRONT : GL11.GL_BACK);
        GL20.glStencilOpSeparate(face, sf, df, dp);
    }

    @Override
    protected void glViewport(int x, int y, int width, int height) {
        GL11.glScissor(x, y, width, height);
        GL11.glViewport(x, y, width, height);
    }

    @Override
    protected void init() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }
}
