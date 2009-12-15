package com.ferox.renderer.impl;

import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderSurface;

public class RenderPassAction extends Action {
	private final RenderPass renderPass;
	
	public RenderPassAction(RenderSurface surface, RenderPass pass) {
		super(surface);
		renderPass = pass;
	}

	@Override
	public void perform(Context context, Action next) {
		context.getRenderer().reset();
		renderPass.render(context.getRenderer(), getRenderSurface());
	}
}
