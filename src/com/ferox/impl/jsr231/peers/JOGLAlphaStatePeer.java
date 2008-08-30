package com.ferox.impl.jsr231.peers;

import javax.media.opengl.GL;

import com.ferox.core.states.atoms.AlphaState;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLAlphaStatePeer extends SimplePeer<AlphaState, NoRecord> {	
	public JOGLAlphaStatePeer(JOGLRenderContext context) {
		super(context);
	}
	
	protected void applyState(AlphaState prevA, NoRecord prevR, AlphaState nextA, NoRecord nextR, GL gl) {
		if (prevA == null || prevA.isAlphaEnabled() != nextA.isAlphaEnabled()) {
			if (nextA.isAlphaEnabled())
				gl.glEnable(GL.GL_ALPHA_TEST);
			else
				gl.glDisable(GL.GL_ALPHA_TEST);
		}
		if (prevA == null || prevA.getAlphaTest() != nextA.getAlphaTest() || prevA.getAlphaReferenceValue() != nextA.getAlphaReferenceValue())
			gl.glAlphaFunc(JOGLRenderContext.getGLFragmentTest(nextA.getAlphaTest()), nextA.getAlphaReferenceValue());
	}

	protected void restoreState(AlphaState cleanA, NoRecord cleanR, GL gl) {
		gl.glDisable(GL.GL_ALPHA_TEST);
	}
}
