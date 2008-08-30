package com.ferox.impl.jsr231.peers;

import java.util.Arrays;

import javax.media.opengl.GL;

import com.ferox.core.scene.states.Fog;
import com.ferox.core.scene.states.Fog.FogFunction;
import com.ferox.core.states.StateManager;
import com.ferox.core.util.FeroxException;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLFogPeer extends SimplePeer<Fog, NoRecord> {		
	private static int getGLFogEqn(FogFunction eqn) {
		switch(eqn) {
		case EXP: return GL.GL_EXP;
		case EXP_SQUARED: return GL.GL_EXP2;
		case LINEAR: return GL.GL_LINEAR;
		}
		throw new FeroxException("Invalid fog equation");
	}
		
	public JOGLFogPeer(JOGLRenderContext context) {
		super(context);
	}
	
	public void prepareManager(StateManager curr, StateManager prev) {
		if (prev == null)
			this.context.getGL().glEnable(GL.GL_FOG);
	}
	
	public void disableManager(StateManager curr) {
		this.context.getGL().glDisable(GL.GL_FOG);
	}
	
	protected void applyState(Fog prevA, NoRecord prevR, Fog nextA, NoRecord nextR, GL gl) {
		if (prevA == null || !Arrays.equals(prevA.getFogColor(), nextA.getFogColor()))
			gl.glFogfv(GL.GL_FOG_COLOR, nextA.getFogColor(), 0);
		if (prevA == null || prevA.getDensity() != nextA.getDensity())
			gl.glFogf(GL.GL_FOG_DENSITY, nextA.getDensity());
		if (prevA == null || prevA.getFogStart() != nextA.getFogStart())
			gl.glFogf(GL.GL_FOG_START, nextA.getFogStart());
		if (prevA == null || prevA.getFogEnd() != nextA.getFogEnd())
			gl.glFogf(GL.GL_FOG_END, nextA.getFogEnd());
		if (prevA == null || prevA.getFogEquation() != nextA.getFogEquation())
			gl.glFogi(GL.GL_FOG_MODE, getGLFogEqn(nextA.getFogEquation()));
		if (prevA == null || prevA.getQuality() != nextA.getQuality())
			gl.glHint(GL.GL_FOG_HINT, JOGLRenderContext.getGLQuality(nextA.getQuality()));
	}

	protected void restoreState(Fog cleanA, NoRecord cleanR, GL gl) {		
		// do nothing
	}
}
