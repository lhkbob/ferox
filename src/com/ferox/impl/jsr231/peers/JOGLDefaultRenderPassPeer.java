package com.ferox.impl.jsr231.peers;

import com.ferox.core.renderer.RenderContext;
import com.ferox.core.renderer.RenderPass;
import com.ferox.core.renderer.RenderPassPeer;

public class JOGLDefaultRenderPassPeer implements RenderPassPeer<RenderPass> {

	public void finishRenderPass(RenderPass pass, RenderContext context) {
		// do nothing
	}

	public void prepareRenderPass(RenderPass pass, RenderContext context) {
		// do nothing
	}
}
