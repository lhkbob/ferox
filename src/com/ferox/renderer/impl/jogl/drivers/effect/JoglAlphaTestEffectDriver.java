package com.ferox.renderer.impl.jogl.drivers.effect;

import javax.media.opengl.GL;

import com.ferox.effect.AlphaTest;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PixelOpRecord;

/**
 * A simple state driver that implements the ALPHA_TEST role using the AlphaTest
 * state implementation.
 * 
 * @author Michael Ludwig
 */
public class JoglAlphaTestEffectDriver extends SingleEffectDriver<AlphaTest> {
	public JoglAlphaTestEffectDriver(JoglContextManager factory) {
		super(null, AlphaTest.class, factory);
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, AlphaTest nextState) {
		PixelOpRecord pr = record.pixelOpRecord;

		if (nextState == null) {
			// we need to turn it off
			if (pr.enableAlphaTest) {
				pr.enableAlphaTest = false;
				gl.glDisable(GL.GL_ALPHA_TEST);
			}
		} else {
			// enable, and update values
			if (!pr.enableAlphaTest) {
				pr.enableAlphaTest = true;
				gl.glEnable(GL.GL_ALPHA_TEST);
			}

			float ref = nextState.getReferenceValue();
			int test = JoglUtil.getGLPixelTest(nextState.getTest());
			if (ref != pr.alphaTestRef || test != pr.alphaTestFunc) {
				pr.alphaTestFunc = test;
				pr.alphaTestRef = ref;
				gl.glAlphaFunc(test, ref);
			}
		}
	}
}
