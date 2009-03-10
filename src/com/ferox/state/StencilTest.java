package com.ferox.state;



/** A stencil test allows the discarding of pixels based on
 * testing against the "stencil" buffer.  This can be used for
 * a number of fancy effects, such as rendering only to an oddly shaped
 * region, do shadow volumes and things like that.
 * 
 * NOTE: not all render surfaces will allocate a stencil buffer unless
 * requested.  Without a stencil buffer, stencil tests will do nothing,
 * so if using these, make sure to enable stencil buffers when getting surfaces.
 * 
 * @author Michael Ludwig
 *
 */
public class StencilTest implements State {
	/** Operation to perform on the stencil buffer under certain conditions. */
	public static enum StencilOp {
		KEEP, ZERO, REPLACE, INCREMENT, DECREMENT, INVERT, INCREMENT_WRAP, DECREMENT_WRAP
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
	
	private Object renderData;
	
	/** Create a stencil test with default values. */
	public StencilTest() {
		this.setStencilFailOp(null);
		this.setDepthFailOp(null);
		this.setDepthPassOp(null);
		
		this.setTest(null);
		
		this.setReferenceValue(DEFAULT_REF_VALUE);
		this.setTestMask(DEFAULT_MASK);
		this.setWriteMask(DEFAULT_MASK);
	}
	
	/** Get the pixel test used when comparing stencil values. */
	public PixelTest getTest() {
		return this.stencilFunc;
	}
	
	/** Set the pixel test to use for stencil tests, if null uses default. */
	public void setTest(PixelTest stencilFunc) {
		if (stencilFunc == null)
			stencilFunc = DEFAULT_PIXELTEST;
		this.stencilFunc = stencilFunc;
	}

	/** Get the stencil operation performed when a stencil test fails when rendering pixels. */
	public StencilOp getStencilFailOp() {
		return this.stencilFail;
	}

	/** Set the stencil operation performed on the stencil buffer when a stencil test fails. 
	 * If null, sets to default. */
	public void setStencilFailOp(StencilOp stencilFail) {
		if (stencilFail == null)
			this.stencilFail = DEFAULT_STENCIL_OP;
		this.stencilFail = stencilFail;
	}

	/** Get the stencil operation performed when a depth test fails. */
	public StencilOp getDepthFailOp() {
		return this.depthFail;
	}

	/** Set the stencil operation performed when a depth test fails.
	 * If null, sets to default. */
	public void setDepthFailOp(StencilOp depthFail) {
		if (depthFail == null)
			depthFail = DEFAULT_STENCIL_OP;
		this.depthFail = depthFail;
	}

	/** Get the stencil operation performed when the depth test passes. */
	public StencilOp getDepthPassOp() {
		return this.depthPass;
	}

	/** Set the stencil operation performed when a depth test succeeds.
	 * If null, sets to default. */
	public void setDepthPassOp(StencilOp depthPass) {
		if (depthPass == null)
			depthPass = DEFAULT_STENCIL_OP;
		this.depthPass = depthPass;
	}

	/** Get the reference value used in stencil tests. */
	public int getReferenceValue() {
		return this.reference;
	}

	/** Set the reference value used in stencil tests.
	 * Default value is 0. */
	public void setReferenceValue(int reference) {
		this.reference = reference;
	}

	/** Get the stencil mask applied to both the stencil buffer value
	 * and the reference value before applying the test function. */
	public int getTestMask() {
		return this.funcMask;
	}

	/** Set the stencil mask applied to both the buffer and reference
	 * before doing the test function.  Because a stencil buffer may not
	 * have 32 bits, the s least significant bits of funcMask are used when
	 * masking (s is the number of bits in the stencil buffer). 
	 * 
	 * Default is all one bits. */
	public void setTestMask(int funcMask) {
		this.funcMask = funcMask;
	}

	/** Get the mask applied to writes to the stencil buffer. */
	public int getWriteMask() {
		return this.writeMask;
	}

	/** Set the mask applied to writes to the stencil buffer.
	 * Just like the stencil func mask, it uses the s least significant bits.
	 * For a given bit, if it's a 1, then that bit is written, if it's a 0,
	 * then no bit is written (leaving whatever was at that bit unchanged).
	 * 
	 * Default is all one bits. */
	public void setWriteMask(int writeMask) {
		this.writeMask = writeMask;
	}

	@Override
	public Role getRole() {
		return Role.STENCIL_TEST;
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
