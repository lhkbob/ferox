package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PixelOpRecord;
import com.ferox.state.BlendMode;

/** State driver that provides a backbone for the BLEND_MODE role
 * based off of BlendMode instances.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglBlendModeStateDriver extends SingleStateDriver<BlendMode> {
	public JoglBlendModeStateDriver(JoglSurfaceFactory factory) {
		super(null, BlendMode.class, factory);
	}
	
	@Override
	protected void restore(GL gl, JoglStateRecord record) {
		PixelOpRecord pr = record.pixelOpRecord;
		
		if (pr.enableBlend) {
			pr.enableBlend = false;
			gl.glDisable(GL.GL_BLEND);
		}
		
		if (pr.blendEquationAlpha != GL.GL_FUNC_ADD || pr.blendEquationRgb != GL.GL_FUNC_ADD) {
			pr.blendEquationAlpha = GL.GL_FUNC_ADD;
			pr.blendEquationRgb = GL.GL_FUNC_ADD;
			gl.glBlendEquation(GL.GL_FUNC_ADD);
		}
		
		if (pr.blendDstAlpha != GL.GL_ZERO || pr.blendDstRgb != GL.GL_ZERO ||
			pr.blendSrcAlpha != GL.GL_ONE || pr.blendSrcRgb != GL.GL_ONE) {
			pr.blendDstAlpha = GL.GL_ZERO;
			pr.blendDstRgb = GL.GL_ZERO;
			pr.blendSrcAlpha = GL.GL_ZERO;
			pr.blendSrcRgb = GL.GL_ZERO;
			gl.glBlendFunc(GL.GL_ONE, GL.GL_ZERO);
		}
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, BlendMode nextState) {
		PixelOpRecord pr = record.pixelOpRecord;
		
		if (nextState == null) {
			// disable blending
			if (pr.enableBlend) {
				pr.enableBlend = false;
				gl.glDisable(GL.GL_BLEND);
			}
		} else {
			// enable and configure blending
			if (!pr.enableBlend) {
				pr.enableBlend = true;
				gl.glEnable(GL.GL_BLEND);
			}
			
			// equation
			int eq = EnumUtil.getGLBlendEquation(nextState.getEquation());
			if (eq != pr.blendEquationRgb || eq != pr.blendEquationAlpha) {
				pr.blendEquationRgb = eq;
				pr.blendEquationAlpha = eq;
				gl.glBlendEquation(eq);
			}
			
			// factors
			int funcSrc = EnumUtil.getGLBlendFactor(nextState.getSourceFactor());
			int funcDst = EnumUtil.getGLBlendFactor(nextState.getDestFactor());
			if (funcSrc != pr.blendSrcRgb || funcSrc != pr.blendSrcAlpha ||
				funcDst != pr.blendDstRgb || funcDst != pr.blendDstAlpha) {
				pr.blendSrcRgb = funcSrc; pr.blendSrcAlpha = funcSrc;
				pr.blendDstRgb = funcDst; pr.blendDstAlpha = funcSrc;
				gl.glBlendFunc(funcSrc, funcDst);
			}
			
			// because the blend factor doesn't allow CONSTANT_X, ONE_MINUS_CONSTANT_X
			// we don't have to bother checking or updating the blend color
		}
	}
}
