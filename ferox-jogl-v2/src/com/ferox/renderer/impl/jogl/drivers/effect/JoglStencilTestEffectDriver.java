package com.ferox.renderer.impl.jogl.drivers.effect;

import javax.media.opengl.GL;
import javax.media.opengl.GLBase;

import com.ferox.effect.StencilTest;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.record.FramebufferRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PixelOpRecord;

/**
 * Effect driver that provides support for the STENCIL_TEST role by using
 * instances of StencilTest.
 * 
 * @author Michael Ludwig
 */
public class JoglStencilTestEffectDriver extends SingleEffectDriver<StencilTest, GL> {
	private static final int FULL_MASK = ~0;

	public JoglStencilTestEffectDriver(JoglContextManager factory) {
		super(null, StencilTest.class, factory);
	}
	
	@Override
	protected GL convert(GLBase gl) {
		return gl.getGL();
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, StencilTest nextState) {
		PixelOpRecord pr = record.pixelOpRecord;
		FramebufferRecord fr = record.frameRecord;

		if (nextState == null) {
			// disable stencil testing
			if (pr.enableStencilTest) {
				pr.enableStencilTest = false;
				gl.glDisable(GL.GL_STENCIL_TEST);
			}
			// we must reset the mask so stencil clearing works properly
			setWriteMask(gl, fr, FULL_MASK);
		} else {
			// enable, make sure the stencil record is correct
			if (!pr.enableStencilTest) {
				pr.enableStencilTest = true;
				gl.glEnable(GL.GL_STENCIL_TEST);
			}

			// set the stencil write mask
			setWriteMask(gl, fr, nextState.getWriteMask());

			// set the stencil function
			setStencilFunc(gl, pr, JoglUtil.getGLPixelTest(nextState.getTest()), 
						   nextState.getReferenceValue(), nextState.getTestMask());

			// set the stencil operations
			setStencilOp(gl, pr, JoglUtil.getGLStencilOp(nextState.getStencilFailOp()), 
						 JoglUtil.getGLStencilOp(nextState.getDepthFailOp()), 
						 JoglUtil.getGLStencilOp(nextState.getDepthPassOp()));
		}
	}

	private void setWriteMask(GL gl, FramebufferRecord fr, int writeMask) {
		if (fr.stencilWriteMask != writeMask || fr.stencilBackWriteMask != writeMask) {
			fr.stencilBackWriteMask = writeMask;
			fr.stencilWriteMask = writeMask;
			gl.glStencilMask(writeMask);
		}
	}

	private void setStencilOp(GL gl, PixelOpRecord pr, int sfail, int dfail, int dpass) {
		if (sfail != pr.stencilFail || sfail != pr.stencilBackFail || 
			dfail != pr.stencilPassDepthFail || dfail != pr.stencilBackPassDepthFail || 
			dpass != pr.stencilPassDepthPass || dpass != pr.stencilBackPassDepthPass) {
			// update the record
			pr.stencilFail = sfail;
			pr.stencilBackFail = sfail;
			pr.stencilPassDepthFail = dfail;
			pr.stencilBackPassDepthFail = dfail;
			pr.stencilPassDepthPass = dpass;
			pr.stencilBackPassDepthPass = dpass;

			gl.glStencilOp(sfail, dfail, dpass);
		}
	}

	private void setStencilFunc(GL gl, PixelOpRecord pr, int func, int ref, int mask) {
		if ((func != pr.stencilFunc || ref != pr.stencilRef || ref != pr.stencilValueMask) || 
			(func != pr.stencilBackFunc || ref != pr.stencilBackRef || ref != pr.stencilBackValueMask)) {
			// update the record
			pr.stencilFunc = func;
			pr.stencilBackFunc = func;
			pr.stencilRef = ref;
			pr.stencilBackRef = ref;
			pr.stencilValueMask = mask;
			pr.stencilBackValueMask = mask;

			gl.glStencilFunc(func, ref, mask);
		}
	}
}
