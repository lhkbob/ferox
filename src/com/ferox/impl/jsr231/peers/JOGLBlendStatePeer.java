package com.ferox.impl.jsr231.peers;

import javax.media.opengl.GL;

import com.ferox.core.states.atoms.BlendState;
import com.ferox.core.states.atoms.BlendState.BlendFactor;
import com.ferox.core.states.atoms.BlendState.BlendFunction;
import com.ferox.core.util.FeroxException;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLBlendStatePeer extends SimplePeer<BlendState, NoRecord> {
	private static int getGLBlendFunction(BlendFunction func) {
		switch(func) {
		case ADD: return GL.GL_FUNC_ADD;
		case MAX: return GL.GL_MAX;
		case MIN: return GL.GL_MIN;
		case REVERSE_SUBTRACT: return GL.GL_FUNC_REVERSE_SUBTRACT;
		case SUBTRACT: return GL.GL_FUNC_SUBTRACT;
		default:
			throw new FeroxException("Illegal blend function for an BlendState");
		}
	}
	
	private static int getGLBlendFactor(BlendFactor src) {
		switch(src) {
		case ZERO: return GL.GL_ZERO;
		case ONE: return GL.GL_ONE;
		case SRC_COLOR: return GL.GL_SRC_COLOR;
		case ONE_MINUS_SRC_COLOR: return GL.GL_ONE_MINUS_SRC_COLOR;
		case SRC_ALPHA: return GL.GL_SRC_ALPHA;
		case ONE_MINUS_SRC_ALPHA: return GL.GL_ONE_MINUS_SRC_ALPHA;
		case SRC_ALPHA_SATURATE: return GL.GL_SRC_ALPHA_SATURATE;
		default:
			throw new FeroxException("Illegal blend factor for an BlendState");
		}
	}
	
	public JOGLBlendStatePeer(JOGLRenderContext context) {
		super(context);
	}

	protected void applyState(BlendState prev, NoRecord prevR, BlendState next, NoRecord nextR, GL gl) {
		if (prev == null || prev.isBlendEnabled() != next.isBlendEnabled()) {
			if (next.isBlendEnabled())
				gl.glEnable(GL.GL_BLEND);
			else
				gl.glDisable(GL.GL_BLEND);
		}
		if (prev == null || prev.getBlendFunction() != next.getBlendFunction())
			gl.glBlendEquation(getGLBlendFunction(next.getBlendFunction()));
		if (prev == null || prev.getSourceBlendFactor() != next.getSourceBlendFactor() 
			|| prev.getDestBlendFactor() != next.getDestBlendFactor())
			gl.glBlendFunc(getGLBlendFactor(next.getSourceBlendFactor()), getGLBlendFactor(next.getDestBlendFactor()));		

	}

	protected void restoreState(BlendState clean, NoRecord cleanR, GL gl) {
		gl.glDisable(GL.GL_BLEND);
	}
}
