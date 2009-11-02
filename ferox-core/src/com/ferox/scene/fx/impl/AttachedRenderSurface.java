package com.ferox.scene.fx.impl;

import com.ferox.renderer.RenderSurface;
import com.ferox.shader.View;

public class AttachedRenderSurface {
	private RenderSurface surface;
	private View view;
	
	public AttachedRenderSurface(RenderSurface surface, View view) {
		this.surface = surface;
		this.view = view;
	}
	
	public RenderSurface getSurface() {
		return surface;
	}
	
	public View getView() {
		return view;
	}
}