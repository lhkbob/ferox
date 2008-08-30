package com.ferox.core.renderer;


public interface RenderPassPeer<R extends RenderPass> {
	public void prepareRenderPass(R pass, RenderContext context);
	public void finishRenderPass(R pass, RenderContext context);
}
