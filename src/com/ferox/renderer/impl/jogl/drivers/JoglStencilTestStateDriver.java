package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.FramebufferRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PixelOpRecord;
import com.ferox.state.StencilTest;

/** State driver that provides support for the STENCIL_TEST role
 * by using instances of StencilTest.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglStencilTestStateDriver extends SingleStateDriver<StencilTest> {
	private static final int FULL_MASK = ~0;
	
	public JoglStencilTestStateDriver(JoglSurfaceFactory factory) {
		super(null, StencilTest.class, factory);
	}
	
	@Override
	protected void restore(GL gl, JoglStateRecord record) {
		PixelOpRecord pr = record.pixelOpRecord;
		FramebufferRecord fr = record.frameRecord;
		
		if (pr.enableStencilTest) {
			pr.enableStencilTest = false;
			gl.glDisable(GL.GL_STENCIL_TEST);
		}
		
		this.setWriteMask(gl, fr, FULL_MASK);
		this.setStencilOp(gl, pr, GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);
		this.setStencilFunc(gl, pr, GL.GL_ALWAYS, 0, FULL_MASK);
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
			this.setWriteMask(gl, fr, FULL_MASK);
		} else {
			// enable, make sure the stencil record is correct
			if (!pr.enableStencilTest) {
				pr.enableStencilTest = true;
				gl.glEnable(GL.GL_STENCIL_TEST);
			}
			
			// set the stencil write mask
			this.setWriteMask(gl, fr, nextState.getWriteMask());
			
			// set the stencil function
			this.setStencilFunc(gl, pr, EnumUtil.getGLPixelTest(nextState.getTest()), nextState.getReferenceValue(), nextState.getTestMask());
			
			// set the stencil operations
			this.setStencilOp(gl, pr, EnumUtil.getGLStencilOp(nextState.getStencilFailOp()), 
									  EnumUtil.getGLStencilOp(nextState.getDepthFailOp()), 
									  EnumUtil.getGLStencilOp(nextState.getDepthPassOp()));
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
			pr.stencilFail = sfail; pr.stencilBackFail = sfail;
			pr.stencilPassDepthFail = dfail; pr.stencilBackPassDepthFail = dfail;
			pr.stencilPassDepthPass = dpass; pr.stencilBackPassDepthPass = dpass;

			gl.glStencilOp(sfail, dfail, dpass);
		}
	}
	
	private void setStencilFunc(GL gl, PixelOpRecord pr, int func, int ref, int mask) {
		if ((func != pr.stencilFunc || ref != pr.stencilRef || ref != pr.stencilValueMask) ||
				(func != pr.stencilBackFunc || ref != pr.stencilBackRef || ref != pr.stencilBackValueMask)) {
			// update the record
			pr.stencilFunc = func; pr.stencilBackFunc = func;
			pr.stencilRef = ref; pr.stencilBackRef = ref;
			pr.stencilValueMask = mask;	pr.stencilBackValueMask = mask;

			gl.glStencilFunc(func, ref, mask);
		}
	}
}
