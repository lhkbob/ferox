package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PixelOpRecord;
import com.ferox.state.AlphaTest;

/** A simple state driver that implements the ALPHA_TEST role
 * using the AlphaTest state implementation.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglAlphaTestStateDriver extends SingleStateDriver<AlphaTest> {
	public JoglAlphaTestStateDriver(JoglSurfaceFactory factory) {
		super(null, AlphaTest.class, factory);
	}
	
	@Override
	protected void restore(GL gl, JoglStateRecord record) {
		PixelOpRecord pr = record.pixelOpRecord;
		
		if (pr.enableAlphaTest) {
			pr.enableAlphaTest = false;
			gl.glDisable(GL.GL_ALPHA_TEST);
		}
		if (pr.alphaTestFunc != GL.GL_ALWAYS || pr.alphaTestRef != 0f) {
			pr.alphaTestFunc = GL.GL_ALWAYS;
			pr.alphaTestRef = 0f;
			gl.glAlphaFunc(GL.GL_ALWAYS, 0f);
		}
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
			int test = EnumUtil.getGLPixelTest(nextState.getTest());
			if (ref != pr.alphaTestRef || test != pr.alphaTestFunc) {
				pr.alphaTestFunc = test;
				pr.alphaTestRef = ref;
				gl.glAlphaFunc(test, ref);
			}
		}
	}
}
