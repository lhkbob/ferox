package com.ferox.renderer.impl;

import com.ferox.renderer.RenderPass;
import com.ferox.renderer.Surface;

/**
 * RenderPassAction is an Action that, when performed, invokes
 * {@link RenderPass#render(com.ferox.renderer.Renderer, Surface)} using
 * the Renderer associated with the current Context.
 * 
 * @author Michael Ludwig
 */
public class RenderPassAction extends Action {
	private final RenderPass renderPass;

    /**
     * Create a RenderPassAction that will render the given pass on the surface.
     * This constructor restricts the Surface to a valid, non-null Surface.
     * 
     * @param surface The Surface where pass is rendered to
     * @param pass The RenderPass that will be rendered
     * @throws NullPointerException if surface or pass are null
     */
	public RenderPassAction(Surface surface, RenderPass pass) {
		super(surface);
		if (surface == null || pass == null)
			throw new NullPointerException("Cannot queue a null Surface or RenderPass");
		renderPass = pass;
	}

	@Override
	public void perform(Context context, Action next) {
		context.getRenderer().reset();
		renderPass.render(context.getRenderer(), getSurface());
	}
}
