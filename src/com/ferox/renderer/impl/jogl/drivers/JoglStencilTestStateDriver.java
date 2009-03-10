package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglContext;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.FramebufferRecord;
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
	protected void apply(GL gl, JoglContext context, StencilTest nextState) {
		PixelOpRecord pr = context.getStateRecord().pixelOpRecord;
		FramebufferRecord fr = context.getStateRecord().frameRecord;
		
		if (nextState == null) {
			// disable stencil testing
			if (pr.enableStencilTest) {
				pr.enableStencilTest = false;
				gl.glDisable(GL.GL_STENCIL_TEST);
			}
			// we must reset the mask so stencil clearing works properly
			if (fr.stencilWriteMask != FULL_MASK || fr.stencilBackWriteMask != FULL_MASK) {
				fr.stencilBackWriteMask = FULL_MASK;
				fr.stencilWriteMask = FULL_MASK;
				gl.glStencilMask(~0);
			}
		} else {
			// enable, make sure the stencil record is correct
			if (!pr.enableStencilTest) {
				pr.enableStencilTest = true;
				gl.glEnable(GL.GL_STENCIL_TEST);
			}
			
			// set the stencil function
			int func = EnumUtil.getGLPixelTest(nextState.getTest());
			int ref = nextState.getReferenceValue();
			int mask = nextState.getTestMask();
			if ((func != pr.stencilFunc || ref != pr.stencilRef || ref != pr.stencilValueMask) ||
				(func != pr.stencilBackFunc || ref != pr.stencilBackRef || ref != pr.stencilBackValueMask)) {
				// update the record
				pr.stencilFunc = func; pr.stencilBackFunc = func;
				pr.stencilRef = ref; pr.stencilBackRef = ref;
				pr.stencilValueMask = mask;	pr.stencilBackValueMask = mask;
				
				gl.glStencilFunc(func, ref, mask);
			}
			
			// set the stencil operations
			int sfail = EnumUtil.getGLStencilOp(nextState.getStencilFailOp());
			int dfail = EnumUtil.getGLStencilOp(nextState.getDepthFailOp());
			int dpass = EnumUtil.getGLStencilOp(nextState.getDepthPassOp());
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
	}
}
