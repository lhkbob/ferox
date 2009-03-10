package com.ferox.state;



/** BlendMode controls the blending of source and destination pixels when
 * rendering into a color buffer.  Blending is usually a costly operation,
 * so BlendModes should only be added to appearances that need blending.
 * Pixels with alpha values not 1 will not appear so unless a blend mode
 * is also attached to an appearance. (they can still be used with alpha testing, however).
 * 
 * Renderers should not enable blend mode if a blend mode isn't attached
 * (unless present on the default state).
 * 
 * @author Michael Ludwig
 *
 */
public class BlendMode implements State {
	private static final BlendEquation DEFAULT_BLEND_EQ = BlendEquation.ADD;
	private static final BlendFactor DEFAULT_SRC_FACTOR = BlendFactor.SRC_ALPHA;
	private static final BlendFactor DEFAULT_DST_FACTOR = BlendFactor.ONE_MINUS_SRC_ALPHA;
	
	public static enum BlendEquation {
		ADD, SUBTRACT, REVERSE_SUBTRACT, MIN, MAX
	}
	
	public static enum BlendFactor {
		ZERO, ONE, SRC_COLOR, ONE_MINUS_SRC_COLOR, SRC_ALPHA, ONE_MINUS_SRC_ALPHA, SRC_ALPHA_SATURATE
	}
	
	private BlendEquation blendFunc;
	private BlendFactor srcBlendFactor;
	private BlendFactor dstBlendFactor;
	
	private Object renderData;
	
	/** Creates a blend mode using ADD, with source factor = SRC_ALPHA, dest factor = ONE_MINUS_SRC_ALPHA. */
	public BlendMode() {
		this.setDestFactor(null);
		this.setSourceFactor(null);
		this.setEquation(null);
	}
	
	/** Get the equation used to blend source and dest pixels together. */
	public BlendEquation getEquation() {
		return this.blendFunc;
	}

	/** Set the equation to use, if null it's set to default */
	public void setEquation(BlendEquation blendEq) {
		if (blendEq == null)
			blendEq = DEFAULT_BLEND_EQ;
		this.blendFunc = blendEq;
	}

	/** Get the source factor used when blending. */
	public BlendFactor getSourceFactor() {
		return this.srcBlendFactor;
	}

	/** Set the source factor to use when blending, if null it's set to default. */
	public void setSourceFactor(BlendFactor srcBlendFactor) throws StateException {
		if (srcBlendFactor == null)
			srcBlendFactor = DEFAULT_SRC_FACTOR;
		this.srcBlendFactor = srcBlendFactor;
	}

	/** Get the dest factor to use when blending. */
	public BlendFactor getDestFactor() {
		return this.dstBlendFactor;
	}

	/** Set the dest factor to use when blending, if null it's set to default. */
	public void setDestFactor(BlendFactor dstBlendFactor) throws StateException {
		if (dstBlendFactor == null)
			dstBlendFactor = DEFAULT_DST_FACTOR;
		this.dstBlendFactor = dstBlendFactor;
	}

	@Override
	public Role getRole() {
		return Role.BLEND_MODE;
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
