package com.ferox.effect;

/**
 * <p>
 * BlendMode encapsulates the state necessary for blending rendered primitives
 * into the current RenderSurface's color buffer. If blending is enabled (see
 * {@link OpenGlShader#isBlendingEnabled()}, then a rendered pixel's final color
 * is determined by the appropriate BlendMode's BlendFunction and BlendFactors
 * for source and destination pixels.
 * </p>
 * <p>
 * The final component value (for each component) is determined by: Note: For
 * BlendFunction MIN and MAX, the BlendFactors have no effect, the final color
 * is determined by min(sC, dC) or max(sC, dC) respectively.
 * <ol>
 * <li>bF = BlendFunction to use</li>
 * <li>bS = BlendFactor that determines amount of the incoming/source color used
 * </li>
 * <li>bD = BlendFactor that determines amount of the previous/destination color
 * used</li>
 * <li>sC = Incoming/source color value for the current component</li>
 * <li>dC = Previous/destination color value for the current componet</li>
 * </ol>
 * 
 * <pre>
 * finalColor = bF(bS(sC, sD) * sC, bD(sC, sD) * bD)
 * </pre>
 * 
 * <pre>
 * Example: bF = ADD, bS = SRC_ALPHA bD = ONE_MINUS_SRC_ALPHA
 * finalColor = sA * sC + (1 - sA) * dC
 * 
 * Example: bF = REVERSE_SUBTRACT, bS = ONE_MINUS_COLOR bD = CONSTANT_COLOR
 * finalColor = cC * bD - (1 - sC) * sC
 * cC is the color component value for the current constant blend color.
 * </pre>
 * 
 * </p>
 * <p>
 * Each BlendFactor determines a 'weight' for each component of a color. For a
 * given input, these weights need not be equal for each component. See
 * {@link BlendFactor} for more details on each factor.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class BlendMode {
	/**
	 * BlendFunction represents the function that determines
	 * the final color from the source and destination colors,
	 * and will possibly modify these colors by the appropriate
	 * BlendFactors.
	 */
	public static enum BlendFunction {
		/** fC = sC * bS + dC + bD */
		ADD, 
		/** fC = sC * bS - dC * bD */
		SUBTRACT, 
		/** fC = dC * bD - sC * bS */
		REVERSE_SUBTRACT, 
		/** fC = min(sC, dC) */
		MIN,
		/** fC = max(sC, dC) */
		MAX
	}

	/**
	 * <p>
	 * BlendFactor determines a set of four values that weight the contribution
	 * of either the source or destination color. The inputs to a BlendFactor
	 * are the source color (Rs, Gs, Bs, As) and the destination color (Rd, Gd,
	 * Bd, Ad); often only 1 is actually used for a given BlendFactor. The color
	 * (Rc, Gc, Bc, Ac) is the configured blend color for the Shader.
	 * </p>
	 * <p>
	 * In the above examples for BlendFunction and BlendMode, the notation bD or
	 * bS refers to one of the four values computed by a BlendFactor. The value
	 * chosen corresponds to the appropriate color component in question.
	 * </p>
	 */
	public static enum BlendFactor {
		/** factor = (0, 0, 0, 0) */
		ZERO, 
		/** factor = (1, 1, 1, 1) */
		ONE, 
		/** factor = (Rs, Gs, Bs, As) */
		SRC_COLOR, 
		/** factor = (1 - Rs, 1 - Gs, 1 - Bs, 1 - As) */
		ONE_MINUS_SRC_COLOR, 
		/** factor = (As, As, As, As) */
		SRC_ALPHA,
		/** factor = (1 - As, 1 - As, 1 - As, 1 - As) */
		ONE_MINUS_SRC_ALPHA, 
		/** 
		 * f = min(As, 1 - Ad)
		 * factor = (f, f, f, 1)
		 * Can only be used as a source BlendFactor.
		 */
		SRC_ALPHA_SATURATE,
		/** factor = (Rd, Gd, Bd, Ad) */
		DST_COLOR, 
		/** factor = (1 - Rd, 1 - Gd, 1 - Bd, 1 - Ad) */
		ONE_MINUS_DST_COLOR,
		/** factor = (Ad, Ad, Ad, Ad) */
		DST_ALPHA,
		/** factor = (1 - Ad, 1 - Ad, 1 - Ad, 1 - Ad) */
		ONE_MINUS_DST_ALPHA, 
		/** factor = (Rc, Gc, Bc, Ac) */
		CONSTANT_COLOR, 
		/** factor = (Ac, Ac, Ac, Ac) */
		CONSTANT_ALPHA,
		/** factor = (1 - Rc, 1 - Gc, 1 - Bc, 1 - Ac) */
		ONE_MINUS_CONSTANT_COLOR, 
		/** factor = (1 - Ac, 1 - Ac, 1 - Ac, 1 - Ac) */
		ONE_MINUS_CONSTANT_ALPHA
	}

	private BlendFunction blendFunction;
	private BlendFactor blendSrc;
	private BlendFactor blendDst;

	/**
	 * Create a new BlendMode that initially uses the BlendFunction Add and a
	 * source BlendFactor of SRC_ALPHA and a destination BlendFactor of
	 * ONE_MINUS_SRC_ALPHA.
	 */
	public BlendMode() {
		setBlendMode(BlendFunction.ADD, BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
	}

	/**
	 * Return the BlendFunction used to combine source and destination colors.
	 * 
	 * @return The BlendFunction used for this BlendMode
	 */
	public BlendFunction getBlendFunction() {
		return blendFunction;
	}

	/**
	 * Return the BlendFactor that modifies the source color components if the
	 * BlendFunction is not MIN or MAX.  The source color is the color that's
	 * about to be placed into the color buffer at a given pixel.
	 * 
	 * @return The BlendFactor for the source color
	 */
	public BlendFactor getSourceBlendFactor() {
		return blendSrc;
	}

	/**
	 * Return the BlendFactor that modifies the destination color components if
	 * the BlendFunction is not MIN or MAX. The destination color is the color
	 * that is currently within the color buffer at a given pixel.
	 * 
	 * @return The BlendFactor the destination color
	 */
	public BlendFactor getDestinationBlendFactor() {
		return blendDst;
	}

	/**
	 * Set the BlendFunction to use for this BlendMode. If eq is null, then ADD
	 * is assigned instead.
	 * 
	 * @param eq The new BlendFunction
	 * @return This BlendMode
	 */
	public BlendMode setBlendFunction(BlendFunction eq) {
		blendFunction = (eq != null ? eq : BlendFunction.ADD);
		
		return this;
	}

	/**
	 * Set the source BlendFactor for this BlendMode. If src is null, then
	 * SRC_ALPHA is assigned instead.
	 * 
	 * @param src The new source BlendFactor
	 * @return This BlendMode
	 */
	public BlendMode setSourceBlendFactor(BlendFactor src) {
		blendSrc = (src != null ? src : BlendFactor.SRC_ALPHA);
		
		return this;
	}

	/**
	 * Set the destination BlendFactor for this BlendMode. If dst is null, then
	 * ONE_MINUS_SRC_ALPHA is assigned instead. dst cannot be equal to
	 * SRC_ALPHA_SATURATE.
	 * 
	 * @param dst The new destination BlendFactor
	 * @return This BlendMode
	 * @throws IllegalArgumentException if dst is equal to SRC_ALPHA_SATURATE
	 */
	public BlendMode setDestinationBlendFactor(BlendFactor dst) {
		if (dst == BlendFactor.SRC_ALPHA_SATURATE)
			throw new IllegalArgumentException("SRC_ALPHA_SATURATE is only allowed for source BlendFactors");
		
		blendSrc = (dst != null ? dst : BlendFactor.ONE_MINUS_SRC_ALPHA);
		
		return this;
	}

	/**
	 * Convenience method to set the BlendEquation and the source and
	 * destination BlendFactors all at once. If eq is null, ADD is used. If
	 * source is null, SRC_ALPHA is used. If dest is null, ONE_MINUS_SRC_ALPHA
	 * is used. dest cannot be SRC_ALPHA_SATURATE.
	 * 
	 * @param eq The new BlendFunction
	 * @param source The new source BlendFactor
	 * @param dest The new destination BlendFactor
	 * @return This BlendMode
	 * @throws IllegalArgumentException if dest is equal to SRC_ALPHA_SATURATE
	 */
	public BlendMode setBlendMode(BlendFunction eq, BlendFactor source, BlendFactor dest) {
		return setBlendFunction(eq).
			   setSourceBlendFactor(source).
			   setDestinationBlendFactor(dest);
	}
}
