package com.ferox.effect;

import com.ferox.effect.Effect.Type;

/**
 * <p>
 * BlendMode controls the blending of source and destination pixels when
 * rendering into a color buffer. Blending is usually a costly operation, so
 * BlendModes should only be added to appearances that need blending. Pixels
 * with alpha values not 1 will not appear that way unless a blend mode is also
 * attached to an appearance. (they can still be used with alpha testing,
 * however).
 * </p>
 * 
 * @author Michael Ludwig
 */
@Type(EffectType.BLEND)
public class BlendMode extends AbstractEffect {
	public static enum BlendEquation {
		ADD, SUBTRACT, REVERSE_SUBTRACT, MIN, MAX
	}

	public static enum BlendFactor {
		ZERO, ONE, SRC_COLOR, ONE_MINUS_SRC_COLOR, 
		SRC_ALPHA, ONE_MINUS_SRC_ALPHA, SRC_ALPHA_SATURATE
	}

	private static final BlendEquation DEFAULT_BLEND_EQ = BlendEquation.ADD;
	private static final BlendFactor DEFAULT_SRC_FACTOR = BlendFactor.SRC_ALPHA;
	private static final BlendFactor DEFAULT_DST_FACTOR = BlendFactor.ONE_MINUS_SRC_ALPHA;
	
	private BlendEquation blendFunc;
	private BlendFactor srcBlendFactor;
	private BlendFactor dstBlendFactor;

	/**
	 * Creates a blend mode using ADD, with source factor = SRC_ALPHA, dest
	 * factor = ONE_MINUS_SRC_ALPHA.
	 */
	public BlendMode() {
		setDestFactor(null);
		setSourceFactor(null);
		setEquation(null);
	}

	/**
	 * Get the equation used to blend source and dest pixels together.
	 * 
	 * @return Current BlendEquation
	 */
	public BlendEquation getEquation() {
		return blendFunc;
	}

	/**
	 * Set the equation to use, if null it's set to default
	 * 
	 * @param blendEq New blend equation
	 */
	public void setEquation(BlendEquation blendEq) {
		if (blendEq == null)
			blendEq = DEFAULT_BLEND_EQ;
		blendFunc = blendEq;
	}

	/**
	 * Get the source factor used when blending.
	 * 
	 * @return Current BlendFactor for incoming pixels
	 */
	public BlendFactor getSourceFactor() {
		return srcBlendFactor;
	}

	/**
	 * Set the source factor to use when blending, if null it's set to default.
	 * 
	 * @param srcBlendFactor New BlendFactor for incoming pixels
	 */
	public void setSourceFactor(BlendFactor srcBlendFactor) {
		if (srcBlendFactor == null)
			srcBlendFactor = DEFAULT_SRC_FACTOR;
		this.srcBlendFactor = srcBlendFactor;
	}

	/**
	 * Get the dest factor to use when blending.
	 * 
	 * @return Current BlendFactor for pixels already rendered
	 */
	public BlendFactor getDestFactor() {
		return dstBlendFactor;
	}

	/**
	 * Set the dest factor to use when blending, if null it's set to default.
	 * 
	 * @param dstBlendFactor New BlendFactor for already rendered pixels
	 */
	public void setDestFactor(BlendFactor dstBlendFactor) {
		if (dstBlendFactor == null)
			dstBlendFactor = DEFAULT_DST_FACTOR;
		this.dstBlendFactor = dstBlendFactor;
	}

	@Override
	public String toString() {
		return "(BlendMode blendFunc: " + blendFunc + " src: " + srcBlendFactor 
			 + " dst:" + dstBlendFactor + ")";
	}
}
