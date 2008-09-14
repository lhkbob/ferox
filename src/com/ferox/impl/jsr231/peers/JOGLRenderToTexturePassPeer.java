package com.ferox.impl.jsr231.peers;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.renderer.RenderPass;
import com.ferox.core.renderer.RenderToTexturePass;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLRenderToTexturePassPeer extends JOGLRenderPassPeer {
	public static interface RTTPeer {
		public void finish(RenderToTexturePass pass, JOGLRenderContext context);
		public void prepare(RenderToTexturePass pass, JOGLRenderContext context);
	}
	
	private RTTPeer peer;
	
	public JOGLRenderToTexturePassPeer(JOGLRenderContext context) {
		super(context);
		this.peer = null;
	}
	
	public void finishRenderPass(RenderPass pass) {
		this.peer.finish((RenderToTexturePass)pass, this.context);
	}

	public void prepareRenderPass(RenderPass pass) {
		if (this.peer == null) {
			if (RenderManager.getSystemCapabilities().isFBOSupported())
				this.peer = new CopyTexturePeer();
			else
				this.peer = new CopyTexturePeer();
		}
		this.peer.prepare((RenderToTexturePass)pass, this.context);
	}
}
