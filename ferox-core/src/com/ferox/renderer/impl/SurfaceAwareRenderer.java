package com.ferox.renderer.impl;

import com.ferox.renderer.RenderSurface;

/**
 * SurfaceAwareRenderer is a decorator on Renderers that allows them to know of
 * the current RenderSurface that the Renderer will render into.
 * 
 * @author Michael Ludwig
 */
public interface SurfaceAwareRenderer {
	/**
	 * Notify the SurfaceAwareRenderer of the current RenderSurface. It is the
	 * responsibility of the Framework implementation to invoke this when
	 * necessary so that all SurfaceAwareRenderer's know of the RenderSurface
	 * which they render into to. This may be called many times with different
	 * surfaces, depending on how a Context's renderer must be used.
	 * 
	 * @param surface The RenderSurface which this Renderer will render into
	 * @throws NullPointerException if surface is null
	 */
	public void setRenderSurface(RenderSurface surface);
}
