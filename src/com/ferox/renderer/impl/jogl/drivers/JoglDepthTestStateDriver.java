package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.FramebufferRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
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
	protected void restore(GL gl, JoglStateRecord record) {
		PixelOpRecord pr = record.pixelOpRecord;
		FramebufferRecord fr = record.frameRecord;
		
		if (pr.enableDepthTest) {
			pr.enableDepthTest = false;
			gl.glDisable(GL.GL_DEPTH_TEST);
		}
		
		if (pr.depthFunc != GL.GL_LESS) {
			pr.depthFunc = GL.GL_LESS;
			gl.glDepthFunc(GL.GL_LESS);
		}
		
		if (!fr.depthWriteMask) {
			fr.depthWriteMask = true;
			gl.glDepthMask(true);
		}
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, DepthTest nextState) {
		PixelOpRecord pr = record.pixelOpRecord;
		FramebufferRecord fr = record.frameRecord;
		
		int test = EnumUtil.getGLPixelTest(nextState.getTest());
		if (test == GL.GL_ALWAYS) {
			// just disable the depth test
			if (pr.enableDepthTest) {
				pr.enableDepthTest = false;
				gl.glDisable(GL.GL_DEPTH_TEST);
			}
		} else {
			// func
			if (pr.depthFunc != test) {
				pr.depthFunc = test;
				gl.glDepthFunc(test);
			}
			
			// enable
			if (!pr.enableDepthTest) {
				pr.enableDepthTest = true;
				gl.glEnable(GL.GL_DEPTH_TEST);
			}
		}
		
		// writing
		if (nextState.isWriteEnabled() != fr.depthWriteMask) {
			fr.depthWriteMask = nextState.isWriteEnabled();
			gl.glDepthMask(fr.depthWriteMask);
		}
	}
}
