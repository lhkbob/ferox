package com.ferox.effect;

import com.ferox.effect.EffectType.Type;

/**
 * <p>
 * DepthTest controls the various depth testing performed on a pixel's screen
 * space depth value. It passes the test if the new pixel's depth passes the
 * test function when compared to the previous pixel's depth (possibly an old
 * pixel if the depth buffer wasn't cleared).
 * </p>
 * <p>
 * If an Appearance has no DepthTest and the default appearance of a renderer
 * doesn't either, then there should be no depth testing.
 * </p>
 * <p>
 * This class also provides the ability to disable writing to the depth buffer
 * for the influenced pixels.
 * </p>
 * <p>
 * The functionality of the depth test is dependent on the existence of a depth
 * buffer in a surface being rendered to (much like the stencil test).
 * </p>
 * 
 * @author Michael Ludwig
 */
@EffectType({Type.DEPTH_TEST, Type.DEPTH_WRITE})
public class DepthTest extends AbstractEffect {
	private static final PixelTest DEFAULT_PIXELTEST = PixelTest.LEQUAL;

	private PixelTest depthTest;
	private boolean enableWrite;

	/**
	 * Creates a depth test representing the default behavior: write enabled,
	 * default test.
	 */
	public DepthTest() {
		setWriteEnabled(true);
		setTest(null);
	}

	/**
	 * Get the depth test to use.
	 * 
	 * @return PixelTest used for depth testing
	 */
	public PixelTest getTest() {
		return depthTest;
	}

	/**
	 * Set the depth test to use, if null uses default.
	 * 
	 * @param test New PixelTest to use
	 */
	public void setTest(PixelTest test) {
		if (test == null)
			test = DEFAULT_PIXELTEST;
		depthTest = test;
	}

	/**
	 * Whether or not pixels influenced by this depth test will write to the
	 * depth buffer when rendered.
	 * 
	 * @return True if depth values are written
	 */
	public boolean isWriteEnabled() {
		return enableWrite;
	}

	/**
	 * Set whether or not depth writing is enabled.
	 * 
	 * @see #isWriteEnabled()
	 * @param writeDepth True if depth values should be written
	 */
	public void setWriteEnabled(boolean writeDepth) {
		enableWrite = writeDepth;
	}

	@Override
	public String toString() {
		return "(DepthTest test: " + depthTest + " write: " + enableWrite + ")";
	}
}
