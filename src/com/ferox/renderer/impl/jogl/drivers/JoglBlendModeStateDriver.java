package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglContext;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
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
	protected void apply(GL gl, JoglContext context, BlendMode nextState) {
		PixelOpRecord pr = context.getStateRecord().pixelOpRecord;
		
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
