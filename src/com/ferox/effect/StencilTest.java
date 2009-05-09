package com.ferox.effect;

import com.ferox.effect.EffectType.Type;

/**
 * <p>
 * A stencil test allows the discarding of pixels based on testing against the
 * "stencil" buffer. This can be used for a number of fancy effects, such as
 * rendering only to an oddly shaped region, do shadow volumes and things like
 * that.
 * </p>
 * <p>
 * <b>NOTE:</b> not all render surfaces will allocate a stencil buffer unless
 * requested. Without a stencil buffer, stencil tests will do nothing, so if
 * using these, make sure to enable stencil buffers when getting surfaces.
 * </p>
 * 
 * @author Michael Ludwig
 */
@EffectType(Type.STENCIL)
public class StencilTest extends AbstractEffect {
	/** Operation to perform on the stencil buffer under certain conditions. */
	public static enum StencilOp {
		KEEP, ZERO, REPLACE, INCREMENT, DECREMENT, INVERT, INCREMENT_WRAP,
		DECREMENT_WRAP
	}

	private static final PixelTest DEFAULT_PIXELTEST = PixelTest.ALWAYS;
	private static final StencilOp DEFAULT_STENCIL_OP = StencilOp.KEEP;
	private static final int DEFAULT_REF_VALUE = 0;
	private static final int DEFAULT_MASK = ~0;

	private PixelTest stencilFunc;

	private StencilOp stencilFail;
	private StencilOp depthFail;
	private StencilOp depthPass;

	private int reference;
	private int funcMask;
	private int writeMask;

	/** Create a stencil test with default values. */
	public StencilTest() {
		setStencilFailOp(null);
		setDepthFailOp(null);
		setDepthPassOp(null);

		setTest(null);

		setReferenceValue(DEFAULT_REF_VALUE);
		setTestMask(DEFAULT_MASK);
		setWriteMask(DEFAULT_MASK);
	}

	/**
	 * Get the pixel test used when comparing stencil values.
	 * 
	 * @return PixelTest for stencil comparisons
	 */
	public PixelTest getTest() {
		return stencilFunc;
	}

	/**
	 * Set the pixel test to use for stencil tests.
	 * 
	 * @param stencilFunc PixelTest used for stencil testing, null = ALWAYS
	 */
	public void setTest(PixelTest stencilFunc) {
		if (stencilFunc == null)
			stencilFunc = DEFAULT_PIXELTEST;
		this.stencilFunc = stencilFunc;
	}

	/**
	 * Get the stencil operation performed when a stencil test fails when
	 * rendering pixels.
	 * 
	 * @return StencilOp used when the stencil test fails
	 */
	public StencilOp getStencilFailOp() {
		return stencilFail;
	}

	/**
	 * Set the stencil operation performed on the stencil buffer when a stencil
	 * test fails.
	 * 
	 * @param stencilFail StencilOp to use for failed stencil tests, null = KEEP
	 */
	public void setStencilFailOp(StencilOp stencilFail) {
		if (stencilFail == null)
			this.stencilFail = DEFAULT_STENCIL_OP;
		this.stencilFail = stencilFail;
	}

	/**
	 * Get the stencil operation performed when a depth test fails.
	 * 
	 * @return StencilOp used when the depth test fails
	 */
	public StencilOp getDepthFailOp() {
		return depthFail;
	}

	/**
	 * Set the stencil operation performed when a depth test fails.
	 * 
	 * @param depthFail StencilOp used for failed depth tests, null = KEEP
	 */
	public void setDepthFailOp(StencilOp depthFail) {
		if (depthFail == null)
			depthFail = DEFAULT_STENCIL_OP;
		this.depthFail = depthFail;
	}

	/**
	 * Get the stencil operation performed when the depth test passes.
	 * 
	 * @return StencilOp used if the depth test passes
	 */
	public StencilOp getDepthPassOp() {
		return depthPass;
	}

	/**
	 * Set the stencil operation performed when a depth test succeeds.
	 * 
	 * @param depthPass StencilOp for when depth testing passes, null = KEEP
	 */
	public void setDepthPassOp(StencilOp depthPass) {
		if (depthPass == null)
			depthPass = DEFAULT_STENCIL_OP;
		this.depthPass = depthPass;
	}

	/**
	 * Get the reference value used in stencil tests. Default value is 0.
	 * 
	 * @return Reference value used in stencil tests
	 */
	public int getReferenceValue() {
		return reference;
	}

	/**
	 * Set the reference value used in stencil tests.
	 * 
	 * @param reference Reference value used in stencil tests
	 */
	public void setReferenceValue(int reference) {
		this.reference = reference;
	}

	/**
	 * Get the stencil mask applied to both the stencil buffer value and the
	 * reference value before applying the test function. Default is all one
	 * bits.
	 * 
	 * @return Bit mask applied to reference and stencil value
	 */
	public int getTestMask() {
		return funcMask;
	}

	/**
	 * Set the stencil mask applied to both the buffer and reference before
	 * doing the test function. Because a stencil buffer may not have 32 bits,
	 * the s least significant bits of funcMask are used when masking (s is the
	 * number of bits in the stencil buffer).
	 * 
	 * @param funcMask Bit mask applied to reference and stencil value
	 */
	public void setTestMask(int funcMask) {
		this.funcMask = funcMask;
	}

	/**
	 * Get the mask applied to writes to the stencil buffer. Default is all one
	 * bits.
	 * 
	 * @return Mask applied to values as they're written
	 */
	public int getWriteMask() {
		return writeMask;
	}

	/**
	 * Set the mask applied to writes to the stencil buffer. Just like the
	 * stencil func mask, it uses the s least significant bits. For a given bit,
	 * if it's a 1, then that bit is written, if it's a 0, then no bit is
	 * written (leaving whatever was at that bit unchanged).
	 * 
	 * @param writeMask Mask applied to written stencil values
	 */
	public void setWriteMask(int writeMask) {
		this.writeMask = writeMask;
	}

	@Override
	public String toString() {
		return "(StencilTest test: " + stencilFunc + " stencilFail: "
			+ stencilFail + " depthFail: " + depthFail + " depthPass: "
			+ depthPass + " reference: " + reference + " testMask: " + funcMask
			+ " writeMask: " + writeMask + ")";
	}
}
