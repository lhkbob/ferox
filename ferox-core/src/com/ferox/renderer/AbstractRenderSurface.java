package com.ferox.renderer;

import com.ferox.math.Color4f;

/**
 * An abstract implementation of RenderSurface that only implements the basic
 * methods that are unlikely to require renderer intervention.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractRenderSurface implements RenderSurface {
	private static final Color4f DEFAULT_CLEAR_COLOR = new Color4f();

	private final Color4f clearColor;
	private float clearDepth;
	private int clearStencil;

	public AbstractRenderSurface() {
		clearColor = new Color4f(DEFAULT_CLEAR_COLOR);
		setClearDepth(1f);
		setClearStencil(0);
	}

	@Override
	public Color4f getClearColor() {
		return clearColor;
	}

	@Override
	public void setClearColor(Color4f color) {
		if (color == null)
			color = new Color4f(DEFAULT_CLEAR_COLOR);
		clearColor.set(color);
	}

	@Override
	public float getClearDepth() {
		return clearDepth;
	}

	@Override
	public void setClearDepth(float depth) {
		if (depth < 0f || depth > 1f)
			throw new IllegalArgumentException("Invalid depth clear value: " + depth);
		clearDepth = depth;
	}

	@Override
	public int getClearStencil() {
		return clearStencil;
	}

	@Override
	public void setClearStencil(int stencil) {
		clearStencil = stencil;
	}
}
