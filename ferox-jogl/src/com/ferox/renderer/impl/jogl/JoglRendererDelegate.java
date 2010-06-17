package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;

import com.ferox.math.Color4f;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Renderer.StencilOp;
import com.ferox.renderer.impl.RendererDelegate;

public class JoglRendererDelegate extends RendererDelegate {
    // capabilities
    private final boolean supportsBlending;
    private final boolean supportsSeparateBlending;
    private final boolean supportsSeparateStencil;
    private final boolean supportsStencilWrap;
    
    // cached context so we can get a GL instance a little faster
    private JoglContext context;
    
    // state tracking for buffer clearing
    private final Color4f clearColor = new Color4f(0f, 0f, 0f, 0f);
    private float clearDepth = 1f;
    private int clearStencil = 0;
    
    // state tracking for draw styles
    private boolean cullEnabled = true;
    private int frontPolyMode = GL2GL3.GL_FILL;
    private int backPolyMode = GL2GL3.GL_FILL;
    
    public JoglRendererDelegate(RenderCapabilities caps) {
        supportsBlending = caps.isBlendingSupported();
        supportsSeparateBlending = caps.getSeparateBlendSupport();
        supportsSeparateStencil = caps.getSeparateStencilSupport();
        supportsStencilWrap = caps.getVersion() >= 1.4f;
    }

    private GL2GL3 getGL() {
        // we cache the context the first time we need it,
        // a renderer will not be passed around amongst contexts
        if (context == null)
            context = JoglContext.getCurrent();
        return context.getGL();
    }
    
    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, Color4f color, float depth, int stencil) {
        if (color == null)
            throw new NullPointerException("Clear color cannot be null");
        if (depth < 0f || depth > 1f)
            throw new IllegalArgumentException("Clear depht must be in [0, 1], not: " + depth);
        
        GL2GL3 gl = getGL();
        
        if (!this.clearColor.equals(color)) {
            this.clearColor.set(color);
            gl.glClearColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
        if (this.clearDepth != depth) {
            this.clearDepth = depth;
            gl.glClearDepthf(depth);
        }
        if (this.clearStencil != stencil) {
            this.clearStencil = stencil;
            gl.glClearStencil(stencil);
        }
        
        int clearBits = 0;
        if (clearColor)
            clearBits |= GL.GL_COLOR_BUFFER_BIT;
        if (clearDepth)
            clearBits |= GL.GL_DEPTH_BUFFER_BIT;
        if (clearStencil)
            clearBits |= GL.GL_STENCIL_BUFFER_BIT;
        
        if (clearBits != 0)
            gl.glClear(clearBits);
    }

    @Override
    protected void glBlendColor(Color4f color) {
        if (supportsBlending)
            getGL().glBlendColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    @Override
    protected void glBlendEquations(BlendFunction funcRgb, BlendFunction funcAlpha) {
        if (supportsBlending) {
            if (supportsSeparateBlending)
                getGL().glBlendEquationSeparate(Utils.getGLBlendEquation(funcRgb), 
                                                Utils.getGLBlendEquation(funcAlpha));
            else
                getGL().glBlendEquation(Utils.getGLBlendEquation(funcRgb));
        }
    }

    @Override
    protected void glBlendFactors(BlendFactor srcRgb, BlendFactor dstRgb, BlendFactor srcAlpha, BlendFactor dstAlpha) {
        if (supportsBlending) {
            // separate blend functions were supported before separate blend equations
            getGL().glBlendFuncSeparate(Utils.getGLBlendFactor(srcRgb), Utils.getGLBlendFactor(dstRgb), 
                                        Utils.getGLBlendFactor(srcAlpha), Utils.getGLBlendFactor(dstAlpha));
        }
    }

    @Override
    protected void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        getGL().glColorMask(red, green, blue, alpha);
    }

    @Override
    protected void glDepthMask(boolean mask) {
        getGL().glDepthMask(mask);
    }

    @Override
    protected void glDepthOffset(float factor, float units) {
        getGL().glPolygonOffset(factor, units);
    }

    @Override
    protected void glDepthTest(Comparison test) {
        getGL().glDepthFunc(Utils.getGLPixelTest(test));
    }

    @Override
    protected void glDrawStyle(DrawStyle front, DrawStyle back) {
        GL2GL3 gl = getGL();
        
        int cullFace = 0;
        if (front == DrawStyle.NONE && back == DrawStyle.NONE) {
            cullFace = GL.GL_FRONT_AND_BACK;
        } else if (front == DrawStyle.NONE) {
            cullFace = GL.GL_FRONT;
        } else if (back == DrawStyle.NONE) {
            cullFace = GL.GL_BACK;
        }
        
        if (cullFace == 0) {
            // to show both sides, must disable culling
            if (cullEnabled) {
                cullEnabled = false;
                glEnable(GL.GL_CULL_FACE, false);
            }
        } else {
            if (!cullEnabled) {
                cullEnabled = true;
                glEnable(GL.GL_CULL_FACE, true);
            }
            gl.glCullFace(cullFace);
        }
        
        if (front != DrawStyle.NONE) {
            int frontMode = Utils.getGLPolygonMode(front);
            if (frontPolyMode != frontMode) {
                frontPolyMode = frontMode;
                gl.glPolygonMode(GL.GL_FRONT, frontMode);
            }
        }
        
        if (back != DrawStyle.NONE) {
            int backMode = Utils.getGLPolygonMode(back);
            if (backPolyMode != backMode) {
                backPolyMode = backMode;
                gl.glPolygonMode(GL.GL_BACK, backMode);
            }
        }
    }
    
    private void glEnable(int flag, boolean enable) {
        if (enable)
            getGL().glEnable(flag);
        else
            getGL().glDisable(flag);
    }

    @Override
    protected void glEnableBlending(boolean enable) {
        glEnable(GL.GL_BLEND, enable);
    }

    @Override
    protected void glEnableDepthOffset(boolean enable) {
        glEnable(GL2GL3.GL_POLYGON_OFFSET_LINE, enable);
        glEnable(GL2GL3.GL_POLYGON_OFFSET_POINT, enable);
        glEnable(GL2GL3.GL_POLYGON_OFFSET_FILL, enable);
    }

    @Override
    protected void glEnableStencilTest(boolean enable) {
        glEnable(GL.GL_STENCIL_TEST, enable);
    }

    @Override
    protected void glStencilMask(boolean front, int mask) {
        if (supportsSeparateStencil) {
            int face = (front ? GL.GL_FRONT : GL.GL_BACK);
            getGL().glStencilMaskSeparate(face, mask);
        } else if (front) { 
            // fallback to use front mask
            getGL().glStencilMask(mask);
        }
    }

    @Override
    protected void glStencilTest(Comparison test, int refValue, int mask, boolean isFront) {
        if (supportsSeparateStencil) {
            int face = (isFront ? GL.GL_FRONT : GL.GL_BACK);
            getGL().glStencilFuncSeparate(face, Utils.getGLPixelTest(test), refValue, mask);
        } else if (isFront) { 
            // fallback to use front mask
            getGL().glStencilFunc(Utils.getGLPixelTest(test), refValue, mask);
        }
    }

    @Override
    protected void glStencilUpdate(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass, boolean isFront) {
        int sf = Utils.getGLStencilOp(stencilFail, supportsStencilWrap);
        int df = Utils.getGLStencilOp(depthFail, supportsStencilWrap);
        int dp = Utils.getGLStencilOp(depthPass, supportsStencilWrap);
        
        if (supportsSeparateStencil) {
            int face = (isFront ? GL.GL_FRONT : GL.GL_BACK);
            getGL().glStencilOpSeparate(face, sf, df, dp);
        } else if (isFront) {
            // fallback to use the front mask
            getGL().glStencilOp(sf, df, dp);
        }
    }

    @Override
    protected void glViewport(int x, int y, int width, int height) {
        GL2GL3 gl = getGL();
        gl.glScissor(x, y, width, height);
        gl.glViewport(x, y, width, height);
    }

    @Override
    protected void init() {
        GL2GL3 gl = getGL();
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_SCISSOR_TEST);
    }
}
