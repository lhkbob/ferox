package com.ferox.renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ferox.math.Color;

/**
 * An abstract implementation of RenderSurface that only implements the basic
 * methods that are unlikely to require renderer intervention.
 * 
 * @author Michael Ludwig
 * 
 */
public abstract class AbstractRenderSurface implements RenderSurface {
	private static final Color DEFAULT_CLEAR_COLOR = new Color();

	private Color clearColor;
	private float clearDepth;
	private int clearStencil;

	private boolean enableClearColor;
	private boolean enableClearDepth;
	private boolean enableClearStencil;

	private final List<RenderPass> passes;
	private final List<RenderPass> readOnlyPasses;

	public AbstractRenderSurface() {
		passes = new ArrayList<RenderPass>();
		readOnlyPasses = Collections.unmodifiableList(passes);

		setClearColor(null);
		setClearDepth(1f);
		setClearStencil(0);

		setColorBufferCleared(true);
		setDepthBufferCleared(true);
		setStencilBufferCleared(true);
	}

	@Override
	public Color getClearColor() {
		return clearColor;
	}

	@Override
	public void setClearColor(Color color) {
		if (color == null) {
			color = new Color(DEFAULT_CLEAR_COLOR);
		}
		clearColor = color;
	}

	@Override
	public float getClearDepth() {
		return clearDepth;
	}

	@Override
	public void setClearDepth(float depth) {
		clearDepth = Math.max(0, Math.min(depth, 1));
	}

	@Override
	public int getClearStencil() {
		return clearStencil;
	}

	@Override
	public void setClearStencil(int stencil) {
		clearStencil = stencil;
	}

	@Override
	public boolean isColorBufferCleared() {
		return enableClearColor;
	}

	@Override
	public void setColorBufferCleared(boolean enable) {
		enableClearColor = enable;
	}

	@Override
	public boolean isDepthBufferCleared() {
		return enableClearDepth;
	}

	@Override
	public void setDepthBufferCleared(boolean enable) {
		enableClearDepth = enable;
	}

	@Override
	public boolean isStencilBufferCleared() {
		return enableClearStencil;
	}

	@Override
	public void setStencilBufferCleared(boolean enable) {
		enableClearStencil = enable;
	}

	@Override
	public void addRenderPass(RenderPass pass) {
		if (!passes.contains(pass)) {
			passes.add(pass);
		}
	}

	@Override
	public List<RenderPass> getAllRenderPasses() {
		return readOnlyPasses;
	}

	@Override
	public void removeRenderPass(RenderPass pass) {
		passes.remove(pass);
	}
}
