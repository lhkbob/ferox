package com.ferox.renderer.impl.jogl.drivers.effect;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLBase;

import com.ferox.effect.BlendMode;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PixelOpRecord;

/**
 * Effect driver that provides a backbone for the BLEND_MODE role based off of
 * BlendMode instances.
 * 
 * @author Michael Ludwig
 */
public class JoglBlendModeEffectDriver extends SingleEffectDriver<BlendMode, GL2ES2> {
	public JoglBlendModeEffectDriver(JoglContextManager factory) {
		super(null, BlendMode.class, factory);
	}
	
	@Override
	protected GL2ES2 convert(GLBase gl) {
		return gl.getGL2ES2();
	}

	@Override
	protected void apply(GL2ES2 gl, JoglStateRecord record, BlendMode nextState) {
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
			int eq = JoglUtil.getGLBlendEquation(nextState.getEquation());
			if (eq != pr.blendEquationRgb || eq != pr.blendEquationAlpha) {
				pr.blendEquationRgb = eq;
				pr.blendEquationAlpha = eq;
				gl.glBlendEquation(eq);
			}

			// factors
			int funcSrc = JoglUtil.getGLBlendFactor(nextState.getSourceFactor());
			int funcDst = JoglUtil.getGLBlendFactor(nextState.getDestFactor());
			if (funcSrc != pr.blendSrcRgb || funcSrc != pr.blendSrcAlpha || 
				funcDst != pr.blendDstRgb || funcDst != pr.blendDstAlpha) {
				pr.blendSrcRgb = funcSrc;
				pr.blendSrcAlpha = funcSrc;
				pr.blendDstRgb = funcDst;
				pr.blendDstAlpha = funcSrc;
				gl.glBlendFunc(funcSrc, funcDst);
			}

			// because the blend factor doesn't allow CONSTANT_X,
			// ONE_MINUS_CONSTANT_X
			// we don't have to bother checking or updating the blend color
		}
	}
}
