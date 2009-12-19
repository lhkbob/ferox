package com.ferox.renderer.impl;

import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderSurface;

/**
 * RenderPassAction is an Action that, when performed, invokes
 * {@link RenderPass#render(com.ferox.renderer.Renderer, RenderSurface)} using
 * the Renderer associated with the current Context.
 * 
 * @author Michael Ludwig
 */
public class RenderPassAction extends Action {
	private final RenderPass renderPass;

	/**
	 * Create a RenderPassAction that will render the given pass on the surface.
	 * 
	 * @param surface The RenderSurface where pass is rendered to
	 * @param pass The RenderPass that will be rendered
	 * @throws NullPointerException if surface or pass are null
	 */
	public RenderPassAction(RenderSurface surface, RenderPass pass) {
		super(surface);
		if (surface == null || pass == null)
			throw new NullPointerException("Cannot create a RenderPassAction with null arguments");
		renderPass = pass;
	}

	@Override
	public void perform(Context context, Action next) {
		context.getRenderer().reset();
		renderPass.render(context.getRenderer(), getRenderSurface());
	}
}
