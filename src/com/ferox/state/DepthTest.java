package com.ferox.state;



/** 
 * DepthTest controls the various depth testing performed on
 * a pixel's screen space depth value.  It passes the test if the
 * new pixel's depth passes the test function when compared to the
 * previous pixel's depth (possibly an old pixel if the depth buffer wasn't
 * cleared).
 * 
 * If an Appearance has no DepthTest and the default appearance of a renderer
 * doesn't either, then there should be no depth testing.
 * 
 * This class also provides the ability to disable writing to the
 * depth buffer for the influenced pixels.
 * 
 * The functionality of the depth test is dependent on the existence 
 * of a depth buffer in a surface being rendered to (much like the stencil test).
 * 
 * @author Michael Ludwig
 *
 */
public class DepthTest implements State {
	private static final PixelTest DEFAULT_PIXELTEST = PixelTest.LEQUAL;
	
	private PixelTest depthTest;
	private boolean enableWrite;
	
	private Object renderData;
	
	/** Creates a depth test representing the default behavior: write enabled, default test. */
	public DepthTest() {
		this.setWriteEnabled(true);
		this.setTest(null);
	}

	/** Get the depth test to use. */
	public PixelTest getTest() {
		return this.depthTest;
	}

	/** Set the depth test to use, if null uses default. */
	public void setTest(PixelTest test){
		if (test == null)
			test = DEFAULT_PIXELTEST;
		this.depthTest = test;
	}

	/** Whether or not pixels influenced by this depth test will write to
	 * the depth buffer when rendered. */
	public boolean isWriteEnabled() {
		return this.enableWrite;
	}
 
	/** Set whether or not depth writing is enabled.  See isWriteEnabled(). */
	public void setWriteEnabled(boolean writeDepth) {
		this.enableWrite = writeDepth;
	}

	@Override
	public Role getRole() {
		return Role.DEPTH_TEST;
	}
	
	@Override
	public Object getStateData() {
		return this.renderData;
	}
	
	@Override
	public void setStateData(Object data) {
		this.renderData = data;
	}
}
