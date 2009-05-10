package com.ferox.renderer.impl.jogl.drivers.effect;

import javax.media.opengl.GL;

import com.ferox.effect.DepthTest;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.record.FramebufferRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PixelOpRecord;

/**
 * A state driver for the DEPTH_TEST role that only supports DepthTest
 * instances.
 * 
 * @author Michael Ludwig
 * 
 */
public class JoglDepthTestEffectDriver extends SingleEffectDriver<DepthTest> {
	public JoglDepthTestEffectDriver(JoglContextManager factory) {
		super(new DepthTest(), DepthTest.class, factory);
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, DepthTest nextState) {
		PixelOpRecord pr = record.pixelOpRecord;
		FramebufferRecord fr = record.frameRecord;

		int test = JoglUtil.getGLPixelTest(nextState.getTest());
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
