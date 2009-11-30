package com.ferox.renderer.impl;

import com.ferox.math.Color4f;
import com.ferox.renderer.RenderSurface;

public class ClearSurfaceAction extends Action {
	private final boolean clearDepth;
	private final boolean clearColor;
	private final boolean clearStencil;
	
	private final Color4f color;
	private final float depth;
	private final int stencil;
	
	public ClearSurfaceAction(RenderSurface surface, boolean clearDepth, boolean clearColor, boolean clearStencil,
		 					  Color4f color, float depth, int stencil) {
		super(surface);
		this.clearColor = clearColor;
		this.clearDepth = clearDepth;
		this.clearStencil = clearStencil;
		
		this.color = (color == null ? new Color4f() : color);
		this.depth = depth;
		this.stencil = stencil;
	}

	@Override
	public void perform(Context context, Action next) {
		context.clearSurface(clearColor, clearDepth, clearStencil, color, depth, stencil);
	}
}
