package com.ferox.renderer.impl;

import com.ferox.renderer.RenderSurface;

public abstract class Action {
	private final RenderSurface surface;
	
	public Action(RenderSurface surface) {
		this.surface = surface;
	}
	
	public RenderSurface getRenderSurface() {
		return surface;
	}
	
	public boolean prepare() {
		return surface == null || !surface.isDestroyed();
	}
	
	public abstract void perform(Context context, Action next);
}
