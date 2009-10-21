package com.ferox.effect;

/**
 * <p>
 * StencilTest contains the state necessary to describe the so-called stencil
 * test, and how the stencil buffer of a RenderSurface is updated. When stencil
 * testing is enabled, a pixel can be discarded based on the stencil test and
 * the stencil buffer can be updated at three possible places in a pixel's
 * lifetime.
 * </p>
 * <p>
 * The stencil test and buffer allow for complicated pixel masks, and more
 * complex rendering techniques such as mirror reflections and shadow volumes.
 * At its heart, the stencil buffer is just a collection of unsigned integers,
 * of a very limited size (usually between 1, 2 and 8 bits). The stencil test
 * just compares the stencil buffer's value against a reference to determine
 * success.
 * </p>
 * <p>
 * A stencil buffer is filled with values either by specifying a clear value for
 * a RenderSurface, or by performing per-pixel updates to it based on three
 * points in a pixel's lifetime:
 * <ol>
 * <li>When the stencil test fails</li>
 * <li>When the depth test fails</li>
 * <li>When the depth test passes</li>
 * </ol>
 * Each of these events can only happen once for a given pixel, since if the
 * stencil test fails it will not proceed to the depth test and the other 2
 * update points rely on the same test.
 * </p>
 * <p>
 * Each of the above events have an associated StencilUpdateOperation that will
 * be applied to the current stencil buffer pixel when the event occurs.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class StencilTest {
	/**
	 * StencilUpdateOperation represents an update action that can occur as
	 * described above. When performing an action, there are three values that
	 * come into play for a given fragment update: the current stencil buffer's
	 * value for the fragment, the reference value, and the maximum allowed
	 * stencil value. The maximum value is dependent on the number of bits used
	 * in a RenderSurface's stencil buffer.
	 */
	public static enum StencilUpdateOperation {
		/** Keeps the stencil's value the same */
		KEEP, 
		/** Sets the stencil's value to 0 */
		ZERO, 
		/** Replaces the stencil's value with the reference value */
		REPLACE, 
		/** Add one to the stencil's current value, clamping it to the max */
		INCREMENT,
		/** Subtract one from the stencil's current value, clamping it to 0 */
		DECREMENT, 
		/** Bitwise invert the stencil's value */
		INVERT, 
		/** Add one to the stencil's value, wrapping around to 0 */
		INCREMENT_WRAP,
		/** Subtract one from the stencil's value, wrapping around to the max value */
		DECREMENT_WRAP
	}

	private StencilUpdateOperation stencilFail;
	private StencilUpdateOperation depthFail;
	private StencilUpdateOperation depthPass;

	private Comparison test;
	private int reference;
	private int mask;

	/**
	 * Create a StencilTest that uses the KEEP operation on all three update
	 * points, has a test of ALWAYS, a reference value of 0, and mask filled
	 * with all 1s.
	 */
	public StencilTest() {
		setStencilFailOperation(StencilUpdateOperation.KEEP);
		setDepthFailOperation(StencilUpdateOperation.KEEP);
		setDepthPassOperation(StencilUpdateOperation.KEEP);

		setTest(Comparison.ALWAYS);
		setReference(0);
		setFunctionMask(~0);
	}

	/**
	 * Return the StencilUpdateOperation that is applied when the stencil test
	 * fails.
	 * 
	 * @return The stencil fail operation
	 */
	public StencilUpdateOperation getStencilFailOperation() {
		return stencilFail;
	}

	/**
	 * Set the StencilUpdateOpeartion that is applied when the stencil test
	 * fails. If fail is null, then it uses the KEEP operation.
	 * 
	 * @param fail The new stencil fail operation
	 * @return This StencilTest
	 */
	public StencilTest setStencilFailOperation(StencilUpdateOperation fail) {
		stencilFail = (fail != null ? fail : StencilUpdateOperation.KEEP);
		return this;
	}

	/**
	 * Return the StencilUpdateOperation that is applied when the depth test
	 * fails.
	 * 
	 * @return The depth fail operation
	 */
	public StencilUpdateOperation getDepthFailOperation() {
		return depthFail;
	}

	/**
	 * Set the StencilUpdateOpeartion that is applied when the depth test fails.
	 * If fail is null, then it uses the KEEP operation.
	 * 
	 * @param fail The new depth fail operation
	 * @return This StencilTest
	 */
	public StencilTest setDepthFailOperation(StencilUpdateOperation fail) {
		depthFail = (fail != null ? fail : StencilUpdateOperation.KEEP);
		return this;
	}

	/**
	 * Return the StencilUpdateOperation that is applied when the depth test
	 * succeeds.
	 * 
	 * @return The depth pass operation
	 */
	public StencilUpdateOperation getDepthPassOperation() {
		return depthPass;
	}

	/**
	 * Set the StencilUpdateOpeartion that is applied when the depth test
	 * succeeds. If pass is null, then it uses the KEEP operation.
	 * 
	 * @param pass The new depth pass operation
	 * @return This StencilTest
	 */
	public StencilTest setDepthPassOperation(StencilUpdateOperation pass) {
		depthPass = (pass != null ? pass : StencilUpdateOperation.KEEP);
		return this;
	}

	/**
	 * Return the comparison used when performing the actual stencil test. The
	 * test performed is
	 * 
	 * <pre>
	 * (value &amp; mask) OP (reference &amp; mask)
	 * </pre>
	 * 
	 * where value is the current stencil value in the buffer, reference is the
	 * configured reference value, and mask is the configured function mask. It
	 * is important to realize the function mask is not the write mask for the
	 * stencil buffer.
	 * 
	 * @return The stencil test
	 */
	public Comparison getTest() {
		return test;
	}

	/**
	 * Set the Comparison used for performing the actual stencil test. See
	 * {@link #getTest()} for details about the how the test is performed. If
	 * test is null, then ALWAYS is used.
	 * 
	 * @param test The new Comparison for the stencil test
	 * @return This StencilTest
	 */
	public StencilTest setTest(Comparison test) {
		this.test = (test != null ? test : Comparison.ALWAYS);
		return this;
	}

	/**
	 * Return the reference value used for comparison in the stencil test.
	 * During comparison, this value is actually AND'd with the configured
	 * function mask.
	 * 
	 * @return The reference test value
	 */
	public int getReference() {
		return reference;
	}

	/**
	 * Set the reference value used during the stencil test. For each test, the
	 * given reference value is AND'd with the current mask to get the final
	 * comparison value. Bits outside of a RenderSurface's stencil buffer's
	 * precision are ignored. When used, this value is interpreted as an
	 * unsigned int using the bits set in ref.
	 * 
	 * @param ref The new reference value
	 * @return This StencilTest
	 */
	public StencilTest setReference(int ref) {
		reference = ref;
		return this;
	}

	/**
	 * Return the function mask that is applied to both the current stencil
	 * buffer value and the configured reference value during the stencil test.
	 * The mask is applied using the bitwise AND operation.
	 * 
	 * @return The function mask
	 */
	public int getFunctionMask() {
		return mask;
	}

	/**
	 * Set the function mask to use when stencil tests are enabled. Bits outside
	 * of a RenderSurface's stencil buffer's precision are ignored.  mask is
	 * interpreted as a bit mask, and not an actual number.
	 * 
	 * @param mask The new bit mask
	 * @return This StencilTest
	 */
	public StencilTest setFunctionMask(int mask) {
		this.mask = mask;
		return this;
	}
}
