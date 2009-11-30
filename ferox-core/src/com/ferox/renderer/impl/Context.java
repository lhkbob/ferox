package com.ferox.renderer.impl;

import com.ferox.math.Color4f;
import com.ferox.renderer.Renderer;

public interface Context {
	public Renderer getRenderer();
	
	public void clearSurface(boolean clearColor, boolean clearDepth, boolean clearStencil,
						     Color4f color, float depth, int stencil);
}
