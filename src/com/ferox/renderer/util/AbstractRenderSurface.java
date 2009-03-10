package com.ferox.renderer.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ferox.math.Color;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderSurface;

/**
 * An abstract implementation of RenderSurface that only implements
 * the basic methods that are unlikely to require renderer intervention.
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
	
	private List<RenderPass> passes;
	private List<RenderPass> readOnlyPasses;
	
	public AbstractRenderSurface() {
		this.passes = new ArrayList<RenderPass>();
		this.readOnlyPasses = Collections.unmodifiableList(this.passes);
		
		this.setClearColor(null);
		this.setClearDepth(1f);
		this.setClearStencil(0);
		
		this.setColorBufferCleared(true);
		this.setDepthBufferCleared(true);
		this.setStencilBufferCleared(true);
	}
	
	@Override
	public Color getClearColor() {
		return this.clearColor;
	}

	@Override
	public void setClearColor(Color color) {
		if (color == null)
			color = new Color(DEFAULT_CLEAR_COLOR);
		this.clearColor = color;
	}
	
	@Override
	public float getClearDepth() {
		return this.clearDepth;
	}
	
	@Override
	public void setClearDepth(float depth) {
		this.clearDepth = Math.max(0, Math.min(depth, 1));
	}

	@Override
	public int getClearStencil() {
		return this.clearStencil;
	}

	@Override
	public void setClearStencil(int stencil) {
		this.clearStencil = stencil;
	}

	@Override
	public boolean isColorBufferCleared() {
		return this.enableClearColor;
	}
	
	@Override
	public void setColorBufferCleared(boolean enable) {
		this.enableClearColor = enable;
	}

	@Override
	public boolean isDepthBufferCleared() {
		return this.enableClearDepth;
	}

	@Override
	public void setDepthBufferCleared(boolean enable) {
		this.enableClearDepth = enable;
	}
	
	@Override
	public boolean isStencilBufferCleared() {
		return this.enableClearStencil;
	}
	
	@Override
	public void setStencilBufferCleared(boolean enable) {
		this.enableClearStencil = enable;
	}
	
	@Override
	public void addRenderPass(RenderPass pass) {
		if (!this.passes.contains(pass))
			this.passes.add(pass);
	}

	@Override
	public List<RenderPass> getAllRenderPasses() {
		return this.readOnlyPasses;
	}

	@Override
	public void removeRenderPass(RenderPass pass) {
		this.passes.remove(pass);
	}
}
