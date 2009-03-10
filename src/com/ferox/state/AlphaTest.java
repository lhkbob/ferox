package com.ferox.state;



/**
 * If attached to an appearance, pixels will only be rendered (for that appearance)
 * if a pixel's alpha value satisfies the pixel test when compared to the
 * alpha reference value.
 * 
 * If not attached, renderers should not use alpha testing unless the default
 * appearance has it overridden.
 * 
 * @author Michael Ludwig
 *
 */
public class AlphaTest implements State {
	private static final PixelTest DEFAULT_PIXELTEST = PixelTest.GEQUAL;
	private static final float DEFAULT_REF_VALUE = 1f;
	
	private PixelTest alphaTest;
	private float refValue;
	
	private Object renderData;
	
	/** Creates an alpha test with a test of GEQUAL and reference value of 1. */
	public AlphaTest() {
		this.setReferenceValue(DEFAULT_REF_VALUE);
		this.setTest(null);
	}

	/** Get the pixel test in use. */
	public PixelTest getTest() {
		return this.alphaTest;
	}

	/** Set the pixel test, if null it's set to default. */
	public void setTest(PixelTest alphaTest) {
		if (alphaTest == null)
			alphaTest = DEFAULT_PIXELTEST;
		this.alphaTest = alphaTest;
	}

	/** Get the reference alpha value, used when testing pixels based on getTest(). */
	public float getReferenceValue() {
		return this.refValue;
	}

	/** Set the reference alpha value, it is clamped to be between 0 and 1. */
	public void setReferenceValue(float refValue) {
		this.refValue = Math.max(0, Math.min(1f, refValue));
	}

	@Override
	public Role getRole() {
		return Role.ALPHA_TEST;
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
