package com.ferox.renderer.impl2;

import com.ferox.math.ReadOnlyVector4f;
import com.ferox.math.Vector4f;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Renderer.StencilOp;

/**
 * <p>
 * The RendererDelegate is a utility class that exposes the same methods defined
 * in {@link Renderer}, except that it doesn't have responsibility for
 * implementing render(). The public facing methods correctly track OpenGL
 * state, and when necessary delegate to protected functions whose
 * responsibility is to invoke the actual low-level graphics calls.
 * </p>
 * <p>
 * It is recommended that the RendererDelegate is used with a
 * {@link AbstractFixedFunctionRenderer} or a {@link GlslRendererDelegate} to
 * create the complete functionality of the different Renderer types.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class RendererDelegate {
    private static final ReadOnlyVector4f DEFAULT_BLEND_COLOR = new Vector4f(0f, 0f, 0f, 0f);
    
    // blending
    protected final Vector4f blendColor = new Vector4f();
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
    protected float depthOffsetFactor = 0f;
    protected float depthOffsetUnits = 0f;
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
    
    protected StencilOp stencilFailFront = StencilOp.KEEP;
    protected StencilOp depthFailFront = StencilOp.KEEP;
    protected StencilOp depthPassFront = StencilOp.KEEP;
    
    protected Comparison stencilTestBack = Comparison.ALWAYS;
    protected int stencilRefBack = 0;
    protected int stencilTestMaskBack = ~0;
    
    protected StencilOp stencilFailBack = StencilOp.KEEP;
    protected StencilOp depthFailBack = StencilOp.KEEP;
    protected StencilOp depthPassBack = StencilOp.KEEP;
    
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
    
    private boolean initialized = false;
    
    protected abstract void init();
    
    public void reset() {
        if (!initialized) {
            init();
            initialized = true;
        }
        
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
        setStencilUpdateOps(StencilOp.KEEP, StencilOp.KEEP, StencilOp.KEEP);
        setStencilTestEnabled(false);
        setStencilWriteMask(~0);
        
        // only reset viewport if we've been assigned valid dimensions
        if (viewSurfaceHeight >= 0 && viewSurfaceWidth >= 0)
            setViewport(0, 0, viewSurfaceWidth, viewSurfaceHeight);
    }
    
    public void setSurfaceSize(int width, int height) {
        viewSurfaceWidth = width;
        viewSurfaceHeight = height;
    }
    
    /**
     * Perform identical operations to {@link Renderer#clear(boolean, boolean, boolean, ReadOnlyVector4f, float, int)}
     */
    public abstract void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, ReadOnlyVector4f color, float depth, int stencil);

    public void setBlendColor(ReadOnlyVector4f color) {
        if (color == null)
            throw new NullPointerException("Null blend color");
        
        if (!blendColor.equals(color)) {
            blendColor.set(color);
            glBlendColor(color);
        }
    }
    
    /**
     * Invoke OpenGL calls to set the blend color.
     */
    protected abstract void glBlendColor(ReadOnlyVector4f color);

    public void setBlendMode(BlendFunction function, BlendFactor src, BlendFactor dst) {
        setBlendModeAlpha(function, src, dst);
        setBlendModeRgb(function, src, dst);
    }

    public void setBlendModeAlpha(BlendFunction function, BlendFactor src, BlendFactor dst) {
        if (function == null || src == null || dst == null)
            throw new NullPointerException("Cannot use null arguments: " + function + ", " + src + ", " + dst);
        if (dst == BlendFactor.SRC_ALPHA_SATURATE)
            throw new IllegalArgumentException("Cannot use SRC_ALPHA_SATURATE for dest BlendFactor");
        
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
        if (function == null || src == null || dst == null)
            throw new NullPointerException("Cannot use null arguments: " + function + ", " + src + ", " + dst);
        if (dst == BlendFactor.SRC_ALPHA_SATURATE)
            throw new IllegalArgumentException("Cannot use SRC_ALPHA_SATURATE for dest BlendFactor");

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
    protected abstract void glBlendFactors(BlendFactor srcRgb, BlendFactor dstRgb, BlendFactor srcAlpha, BlendFactor dstAlpha);

    /**
     * Invoke OpenGL calls to set the blend equations. 
     */
    protected abstract void glBlendEquations(BlendFunction funcRgb, BlendFunction funcAlpha);
    
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
        if (colorMask[0] != red || colorMask[1] != green 
            || colorMask[2] != blue || colorMask[3] != alpha) {
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
    
    public void setDepthOffsets(float factor, float units) {
        if (depthOffsetFactor != factor || depthOffsetUnits != units) {
            depthOffsetFactor = factor;
            depthOffsetUnits = units;
            glDepthOffset(factor, units);
        }
    }
    
    /**
     * Invoke OpenGL calls to configure depth offsets.
     */
    protected abstract void glDepthOffset(float factor, float units);

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
        if (test == null)
            throw new NullPointerException("Null depth test");
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
        if (front == null || back == null)
            throw new NullPointerException("Null DrawStyle: " + front + ", " + back);
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
        if (test == null)
            throw new NullPointerException("Stencil test");
        if (stencilTestFront != test || stencilRefFront != refValue || stencilTestMaskFront != testMask) {
            stencilTestFront = test;
            stencilRefFront = refValue;
            stencilTestMaskFront = testMask;
            glStencilTest(test, refValue, testMask, true);
        }
    }
    
    public void setStencilTestFront(Comparison test, int refValue, int testMask) {
        if (test == null)
            throw new NullPointerException("Stencil test");
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
    protected abstract void glStencilTest(Comparison test, int refValue, int mask, boolean isFront);
    
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

    public void setStencilUpdateOps(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass) {
        setStencilUpdateOpsFront(stencilFail, depthFail, depthPass);
        setStencilUpdateOpsBack(stencilFail, depthFail, depthPass);
    }

    public void setStencilUpdateOpsBack(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass) {
        if (stencilFail == null || depthFail == null || depthPass == null)
            throw new NullPointerException("Cannot have null arguments: " + stencilFail + ", " + depthFail + ", " + depthPass);
        if (stencilFailBack != stencilFail || depthFailBack != depthFail || depthPassBack != depthPass) {
            stencilFailBack = stencilFail;
            depthFailBack = depthFail;
            depthPassBack = depthPass;
            glStencilUpdate(stencilFail, depthFail, depthPass, false);
        }
    }

    public void setStencilUpdateOpsFront(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass) {
        if (stencilFail == null || depthFail == null || depthPass == null)
            throw new NullPointerException("Cannot have null arguments: " + stencilFail + ", " + depthFail + ", " + depthPass);
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
    protected abstract void glStencilUpdate(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass, boolean isFront);
    
    public void setViewport(int x, int y, int width, int height) {
        if (x < 0 || y < 0 || width < 0 || height < 0)
            throw new IllegalArgumentException("Invalid arguments, all must be positive: " + x + ", " + y + ", " + width + ", " + height);
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
}
