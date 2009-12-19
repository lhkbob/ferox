package com.ferox.renderer.impl;

import com.ferox.math.Color4f;
import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.Renderer;

/**
 * ClearSurfaceAction is an Action that clears the entire framebuffer of it's
 * associated RenderSurface. It exposes parameters to control which buffers are
 * cleared and what they are cleared to, in the same manner way that
 * {@link Framework#queue(RenderSurface, RenderPass, boolean, boolean, boolean, Color4f, float, int)}
 * exposes clearing configuration parameters.
 * 
 * @author Michael Ludwig
 */
public class ClearSurfaceAction extends Action {
	private final boolean clearDepth;
	private final boolean clearColor;
	private final boolean clearStencil;
	
	private final Color4f color;
	private final float depth;
	private final int stencil;

	/**
	 * Create a new ClearSurfaceAction for the given RenderSurface. The
	 * RenderSurface cannot be null, but it is assumed that all other
	 * pararameters are valid to pass directly to
	 * {@link Renderer#clear(boolean, boolean, boolean, Color4f, float, int)}.
	 * 
	 * @param surface The RenderSurface to be cleared
	 * @param clearDepth
	 * @param clearColor
	 * @param clearStencil
	 * @param color
	 * @param depth
	 * @param stencil
	 * @throws NullPointerException if surface is null
	 */
	public ClearSurfaceAction(RenderSurface surface, boolean clearDepth, boolean clearColor, boolean clearStencil,
		 					  Color4f color, float depth, int stencil) {
		super(surface);
		if (surface == null)
			throw new NullPointerException("Cannot create a ClearSurfaceAction without a RenderSurface");
		
		this.clearColor = clearColor;
		this.clearDepth = clearDepth;
		this.clearStencil = clearStencil;
		
		this.color = color;
		this.depth = depth;
		this.stencil = stencil;
	}

	@Override
	public void perform(Context context, Action next) {
		Renderer renderer = context.getRenderer();
		// make sure the entire buffer will be cleared, 
		// without doing a full reset
		renderer.setDepthWriteMask(true);
		renderer.setColorWriteMask(true, true, true, true);
		renderer.setStencilWriteMask(~0);
		
		renderer.clear(clearColor, clearDepth, clearStencil, color, depth, stencil);
	}
}
