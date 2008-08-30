package com.ferox.impl.jsr231.peers;

import javax.media.opengl.GL;

import com.ferox.core.states.atoms.DrawMode;
import com.ferox.core.states.atoms.DrawMode.DrawFace;
import com.ferox.core.states.atoms.DrawMode.DrawStyle;
import com.ferox.core.states.atoms.DrawMode.Winding;
import com.ferox.core.util.FeroxException;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLDrawModePeer extends SimplePeer<DrawMode, NoRecord> {	
	private static int getGLPolygonMode(DrawStyle fill) {
		switch(fill) {
		case WIREFRAME: return GL.GL_LINE;
		case FILLED: return GL.GL_FILL;
		}
		throw new FeroxException("Illegal draw face mode");
	}
	
	private static int getGLWinding(Winding wind) {
		switch(wind) {
		case COUNTER_CLOCKWISE: return GL.GL_CCW;
		case CLOCKWISE: return GL.GL_CW;
		}
		throw new FeroxException("Illegal winding in draw face");
	}
	
	public JOGLDrawModePeer(JOGLRenderContext context) {
		super(context);
	}
	
	protected void applyState(DrawMode prev, NoRecord prevR, DrawMode next, NoRecord nextR, GL gl) {
		if (prev == null || prev.getDrawFace() != next.getDrawFace()) {
			if (next.getDrawFace() == DrawFace.FRONT_AND_BACK) {
				gl.glDisable(GL.GL_CULL_FACE);
			} else {
				if (prev == null || prev.getDrawFace() == DrawFace.FRONT_AND_BACK) {
					gl.glEnable(GL.GL_CULL_FACE);
				}
				if (next.getDrawFace() == DrawFace.BACK)
					gl.glCullFace(GL.GL_FRONT);
				else if (next.getDrawFace() == DrawFace.FRONT)
					gl.glCullFace(GL.GL_BACK);
			}
			
			if (next.getDrawFace() == DrawFace.BACK || next.getDrawFace() == DrawFace.FRONT_AND_BACK)
				gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_TRUE);
			else
				gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_FALSE);
		}
		
		if (prev == null || prev.getBackMode() != next.getBackMode() || prev.getFrontMode() != next.getFrontMode()) {
			if (next.getBackMode() == next.getFrontMode()) 
				gl.glPolygonMode(GL.GL_FRONT_AND_BACK, getGLPolygonMode(next.getFrontMode()));
			else {
				gl.glPolygonMode(GL.GL_FRONT, getGLPolygonMode(next.getFrontMode()));
				gl.glPolygonMode(GL.GL_BACK, getGLPolygonMode(next.getBackMode()));
			}
		}
		
		if (prev == null || prev.getWinding() != next.getWinding()) {
			gl.glFrontFace(getGLWinding(next.getWinding()));
		}
	}

	protected void restoreState(DrawMode dr, NoRecord r, GL gl) {
		if (dr.getBackMode() != DrawStyle.FILLED || dr.getFrontMode() == DrawStyle.FILLED)
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
		if (dr.getWinding() != Winding.COUNTER_CLOCKWISE)
			gl.glFrontFace(GL.GL_CCW);
		if (dr.getDrawFace() != DrawFace.FRONT) {
			gl.glCullFace(GL.GL_BACK);
			gl.glEnable(GL.GL_CULL_FACE);
			gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_FALSE);
		}	
	}
}
