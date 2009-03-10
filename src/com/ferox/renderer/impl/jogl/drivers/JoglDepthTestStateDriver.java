package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglContext;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.FramebufferRecord;
import com.ferox.renderer.impl.jogl.record.PixelOpRecord;
import com.ferox.state.DepthTest;

/** A state driver for the DEPTH_TEST role that only supports DepthTest
 * instances.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglDepthTestStateDriver extends SingleStateDriver<DepthTest> {
	public JoglDepthTestStateDriver(JoglSurfaceFactory factory) {
		super(new DepthTest(), DepthTest.class, factory);
	}

	@Override
	protected void apply(GL gl, JoglContext context, DepthTest nextState) {
		PixelOpRecord pr = context.getStateRecord().pixelOpRecord;
		FramebufferRecord fr = context.getStateRecord().frameRecord;
		
		// func
		int test = EnumUtil.getGLPixelTest(nextState.getTest());
		if (pr.depthFunc != test) {
			pr.depthFunc = test;
			gl.glDepthFunc(test);
		}
		// writing
		if (nextState.isWriteEnabled() != fr.depthWriteMask) {
			fr.depthWriteMask = nextState.isWriteEnabled();
			gl.glDepthMask(fr.depthWriteMask);
		}
		// enable
		if (!pr.enableDepthTest) {
			pr.enableDepthTest = true;
			gl.glEnable(GL.GL_DEPTH_TEST);
		}
	}
}
