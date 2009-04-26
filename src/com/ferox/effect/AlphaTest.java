package com.ferox.effect;

/**
 * <p>
 * If attached to an appearance, pixels will only be rendered (for that
 * appearance) if a pixel's alpha value satisfies the pixel test when compared
 * to the alpha reference value.
 * </p>
 * 
 * <p>
 * If not attached, renderers should not use alpha testing unless the default
 * appearance has it overridden.
 * </p>
 * 
 * @author Michael Ludwig
 * 
 */
public class AlphaTest extends AbstractEffect {
	private static final PixelTest DEFAULT_PIXELTEST = PixelTest.GEQUAL;
	private static final float DEFAULT_REF_VALUE = 1f;

	private PixelTest alphaTest;
	private float refValue;

	/** Creates an alpha test with a test of GEQUAL and reference value of 1. */
	public AlphaTest() {
		setReferenceValue(DEFAULT_REF_VALUE);
		setTest(null);
	}

	/**
	 * Get the pixel test in use.
	 * 
	 * @return
	 */
	public PixelTest getTest() {
		return alphaTest;
	}

	/**
	 * Set the pixel test, if null it's set to default.
	 * 
	 * @param alphaTest The pixel test to use
	 */
	public void setTest(PixelTest alphaTest) {
		if (alphaTest == null)
			alphaTest = DEFAULT_PIXELTEST;
		this.alphaTest = alphaTest;
	}

	/**
	 * Get the reference alpha value, used when testing pixels based on
	 * getTest().
	 * 
	 * @return The test value, in [0, 1]
	 */
	public float getReferenceValue() {
		return refValue;
	}

	/**
	 * Set the reference alpha value, it is clamped to be between 0 and 1.
	 * 
	 * @param refValue Test value, clamped to be in [0, 1]
	 */
	public void setReferenceValue(float refValue) {
		this.refValue = Math.max(0, Math.min(1f, refValue));
	}

	@Override
	public String toString() {
		return "(AlphaTest test: " + alphaTest + " reference: " + refValue
				+ ")";
	}
}
