package com.ferox.impl.jsr231.peers;

import javax.media.opengl.GL;

import com.ferox.core.states.atoms.ZBuffer;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLZBufferPeer extends SimplePeer<ZBuffer, NoRecord> {	
	public JOGLZBufferPeer(JOGLRenderContext context) {
		super(context);
	}
	
	protected void applyState(ZBuffer prev, NoRecord prevR, ZBuffer next, NoRecord nextR, GL gl) {
		if (prev == null || prev.getDepthTest() != next.getDepthTest())
			gl.glDepthFunc(JOGLRenderContext.getGLFragmentTest(next.getDepthTest()));
		if (prev == null || prev.isZBufferWriteEnabled() != next.isZBufferWriteEnabled()) {
			gl.glDepthMask(next.isZBufferWriteEnabled());
		}
	}
	
	protected void restoreState(ZBuffer zb, NoRecord cleanR, GL gl) {
		gl.glDepthMask(true);
		gl.glDepthFunc(GL.GL_LEQUAL);
	}
}
