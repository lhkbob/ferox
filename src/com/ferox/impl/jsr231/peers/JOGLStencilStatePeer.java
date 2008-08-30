package com.ferox.impl.jsr231.peers;

import javax.media.opengl.GL;

import com.ferox.core.states.atoms.StencilState;
import com.ferox.core.states.atoms.StencilState.StencilOp;
import com.ferox.core.util.FeroxException;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLStencilStatePeer extends SimplePeer<StencilState, NoRecord> {
	private static int getGLStencilOp(StencilOp op) {
		switch(op) {
		case DECREMENT: return GL.GL_DECR;
		case DECREMENT_WRAP: return GL.GL_DECR_WRAP;
		case INCREMENT: return GL.GL_INCR;
		case INCREMENT_WRAP: return GL.GL_INCR_WRAP;
		case ZERO: return GL.GL_ZERO;
		case KEEP: return GL.GL_KEEP;
		case REPLACE: return GL.GL_REPLACE;
		case INVERT: return GL.GL_INVERT;
		default:
			throw new FeroxException("Invalid stencil op");
		}
	}
	
	public JOGLStencilStatePeer(JOGLRenderContext context) {
		super(context);
	}
	
	protected void applyState(StencilState prevA, NoRecord prevR, StencilState nextA, NoRecord nextR, GL gl) {
		if (prevA == null || prevA.isStencilEnabled() != nextA.isStencilEnabled()) {
			if (nextA.isStencilEnabled())
				gl.glEnable(GL.GL_STENCIL_TEST);
			else
				gl.glDisable(GL.GL_STENCIL_TEST);
		}
		if (prevA == null || prevA.getStencilFunction() != nextA.getStencilFunction() || prevA.getReferenceValue() != nextA.getReferenceValue()
			|| prevA.getStencilFuncMask() != nextA.getStencilFuncMask())
			gl.glStencilFunc(JOGLRenderContext.getGLFragmentTest(nextA.getStencilFunction()), nextA.getReferenceValue(), nextA.getStencilFuncMask());
		if (prevA == null || prevA.getStencilFailOp() != nextA.getStencilFailOp() || prevA.getDepthFailOp() != nextA.getDepthFailOp()
			|| prevA.getDepthPassOp() != nextA.getDepthPassOp())
			gl.glStencilOp(getGLStencilOp(nextA.getStencilFailOp()), getGLStencilOp(nextA.getDepthFailOp()), getGLStencilOp(nextA.getDepthPassOp()));
		if (prevA == null || prevA.getStencilWriteMask() != nextA.getStencilWriteMask())
			gl.glStencilMask(nextA.getStencilWriteMask());
	}

	protected void restoreState(StencilState cleanA, NoRecord cleanR, GL gl) {	
		gl.glDisable(GL.GL_STENCIL_TEST);
		gl.glStencilMask(Integer.MAX_VALUE - Integer.MIN_VALUE);
	}
}
