package com.ferox.impl.jsr231.peers;

import com.ferox.core.renderer.RenderPass;
import com.ferox.core.renderer.RenderPassPeer;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLRenderPassPeer implements RenderPassPeer {
	protected JOGLRenderContext context;
	
	public JOGLRenderPassPeer(JOGLRenderContext context) {
		this.context = context;
	}

	public void finishRenderPass(RenderPass pass) {
		// do nothing
	}

	public void prepareRenderPass(RenderPass pass) {
		// do nothing
	}
}
