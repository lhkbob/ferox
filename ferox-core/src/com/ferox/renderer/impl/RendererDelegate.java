package com.ferox.renderer.impl;

import com.ferox.math.Color4f;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Renderer.StencilOp;

public abstract class RendererDelegate {
	private static final Color4f DEFAULT_BLEND_COLOR = new Color4f(0f, 0f, 0f, 0f);
	
	// blending
	private final Color4f blendColor = new Color4f(DEFAULT_BLEND_COLOR);
	private BlendFunction blendFuncRgb = BlendFunction.ADD;
	private BlendFunction blendFuncAlpha = BlendFunction.ADD;
	
	private BlendFactor blendSrcRgb = BlendFactor.ONE;
	private BlendFactor blendDstRgb = BlendFactor.ZERO;
	private BlendFactor blendSrcAlpha = BlendFactor.ONE;
	private BlendFactor blendDstAlpha = BlendFactor.ZERO;
	
	private boolean blendEnabled = false;
	
	// color masking [red, green, blue, alpha]
	private final boolean[] colorMask = new boolean[] {true, true, true, true};
	
	// depth offsets
	private float depthOffsetFactor = 0f;
	private float depthOffsetUnits = 0f;
	private boolean depthOffsetEnabled = false;
	
	// depth test and mask
	private Comparison depthTest = Comparison.LESS;
	private boolean depthMask = true;
	
	// draw styles
	private DrawStyle styleFront = DrawStyle.SOLID;
	private DrawStyle styleBack = DrawStyle.NONE;
	
	// stencil test
	private Comparison stencilTestFront = Comparison.ALWAYS;
	private int stencilRefFront = 0;
	private int stencilTestMaskFront = ~0;
	
	private StencilOp stencilFailFront = StencilOp.KEEP;
	private StencilOp depthFailFront = StencilOp.KEEP;
	private StencilOp depthPassFront = StencilOp.KEEP;
	
	private Comparison stencilTestBack = Comparison.ALWAYS;
	private int stencilRefBack = 0;
	private int stencilTestMaskBack = ~0;
	
	private StencilOp stencilFailBack = StencilOp.KEEP;
	private StencilOp depthFailBack = StencilOp.KEEP;
	private StencilOp depthPassBack = StencilOp.KEEP;
	
	private boolean stencilEnabled = false;
	
	// stencil mask
	private int stencilMaskFront = ~0;
	private int stencilMaskBack = ~0;
	
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
		setStencilUpdateOps(StencilOp.KEEP, StencilOp.KEEP, StencilOp.KEEP);
		setStencilTestEnabled(false);
		setStencilWriteMask(~0);
	}
	
	/**
	 * Perform identical operations to {@link Renderer#clear()}
	 */
	public abstract void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, Color4f color, float depth, int stencil);

	public void setBlendColor(Color4f color) {
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
	protected abstract void glBlendColor(Color4f color);

	public void setBlendMode(BlendFunction function, BlendFactor src, BlendFactor dst) {
		setBlendModeAlpha(function, src, dst);
		setBlendModeRgb(function, src, dst);
	}

	public void setBlendModeAlpha(BlendFunction function, BlendFactor src, BlendFactor dst) {
		if (blendFuncAlpha != function || blendSrcAlpha != src || blendDstAlpha != dst) {
			blendFuncAlpha = function;
			blendSrcAlpha = src;
			blendDstAlpha = dst;
			glBlendFunction(function, src, dst, false);
		}
	}

	public void setBlendModeRgb(BlendFunction function, BlendFactor src, BlendFactor dst) {
		if (function == null || src == null || dst == null)
			throw new NullPointerException("Cannot use null arguments: " + function + ", " + src + ", " + dst);
		
		if (blendFuncRgb != function || blendSrcRgb != src || blendDstRgb != dst) {
			blendFuncRgb = function;
			blendSrcRgb = src;
			blendDstRgb = dst;
			glBlendFunction(function, src, dst, true);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set the blend functions and factors.  If
	 * isRGB is true, it's for rgb values, else it's for alpha values.
	 */
	protected abstract void glBlendFunction(BlendFunction func, BlendFactor src, BlendFactor dst, boolean isRGB);

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
		if (stencilMaskFront != front || stencilMaskBack != back) {
			stencilMaskFront = front;
			stencilMaskBack = back;
			glStencilMask(front, back);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set the stencil masks
	 */
	protected abstract void glStencilMask(int front, int back);

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
}
