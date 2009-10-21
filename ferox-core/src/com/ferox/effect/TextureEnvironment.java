package com.ferox.effect;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Plane;
import com.ferox.resource.TextureImage;

/**
 * <p>
 * A TextureEnvironment wraps a TextureImage a provides information on how it will be
 * blended together with other textures and material colors of the rendered
 * object. It also controls texture coordinate generation (if desired), and
 * setting the a transform that is applied to each texture coordinate before
 * accessing the texture.
 * </p>
 * <p>
 * TextureEnvironment isn't an Effect, it is to be used in conjunction with MultiTexture,
 * which describes which integer unit a TextureEnvironment is applied to.
 * </p>
 * <p>
 * TextureEnvironment's also allow for advanced combinations when the EnvironmentMode COMBINE is
 * used. The EnvModes and combine operations are modeled directly after the
 * texture environment options in OpenGL.
 * </p>
 * <p>
 * TextureEnvironment's also allow for advanced combinations when the EnvironmentMode COMBINE is
 * used.<br>
 * Summary of EnvironmentMode operations:<br>
 * Cv = final rgb color Av = final alpha<br>
 * Cs = rgb of this texture As = alpha of this tex<br>
 * Cp = rgb of prev. color Ap = alpha of prev. color<br>
 * <br>
 * 
 * <pre>
 * Format       | REPLACE   | MODULATE  | DECAL                | BLEND                | Add
 * Alpha        | Cv = Cp   | Cv = Cp   | --                   | Cv = Cp              | Cv = Cp
 *                Av = As     Av = ApAs                          Av = ApAs              Av = ApAs
 * Luminance    | Cv = Cs   | Cv = CpCs | --                   | Cv = Cp(1-Cs)+CcCs   | Cv = Cp+Cs
 *                Av = Ap     Av = Ap                            Av = Ap                Av = Ap
 * Lum. Alpha   | Cv = Cs   | Cv = CpCs | --                   | Cv = Cp(1-Cs)+CcCs   | Cv = Cp+Cs
 *                Av = As     Av = ApAs                          Av = ApAs              Av = ApAs
 * RGB          | Cv = Cs   | Cv = CpCs | Cv = Cs              | Cv = Cp(1-Cs)+CcCs   | Cv = Cp+Cs
 *                Av = Ap     Av = Ap     Av = Ap                Av = Ap                Av = Ap
 * RGBA         | Cv = Cs   | Cv = CpCs | Cv = Cp(1-As)+CsAs   | Cv = Cp(1-Cs)+CcCs   | Cv = Cp+Cs
 *                Av = As     Av = ApAs   Av = Ap                Av = ApAs              Av = ApAs
 * </pre>
 * 
 * </p>
 * <p>
 * Operations on colors are done component wise. Cs for Luminance is (L, L, L),
 * As for Lum. is L Cs for LA is (L, L, L), As for LA is A
 * </p>
 * <p>
 * Summary of CombineFunctions:<br>
 * In the table below, opX is a function applied to either a color(per
 * component) or an alpha value as defined in CombineOperand. srcX is the input
 * and is designated with CombineSource. In the DOT3_x varieties, rN, gN, and bN
 * represent the red, green, and blue values of opN(srcN).<br>
 * <br>
 * 
 * <pre>
 * CombineRgb   |   Rgb Result
 * REPLACE      |   op0(src0)
 * MODULATE     |   op0(src0) * op1(src1)
 * ADD          |   op0(src0) + op1(src1)
 * ADD_SIGNED   |   op0(src0) + op1(src1) - .5
 * INTERPOLATE  |   op0(src0) * op2(src2) + op1(src1) * (1 - op2(src2))
 * SUBTRACT     |   op0(src0) - op1(src1)
 * DOT3_RGB     |   4 * ((r0-.5)*(r1-.5) + (g0-.5)*(g1-.5) + (b0-.5)*(b1-.5))
 * DOT3_RGBA    |   4 * ((r0-.5)*(r1-.5) + (g0-.5)*(g1-.5) + (b0-.5)*(b1-.5))
 * </pre>
 * 
 * </p>
 * <p>
 * CombineAlpha is computed in exactly the same way, except that there are no
 * DOT3_x options. In CombineRgb, DOT3_RGBA ignores the result of CombineAlpha.
 * The combine operations, SRC_COLOR and ONE_MINUS_SRC_COLOR are meaningless for
 * CombineAlpha, so they are mapped to SRC_ALPHA and ONE_MINUS_SRC_ALPHA when
 * specified.
 * </p>
 * <p>
 * Defaults:<br>
 * COMBINE_RGB = CombineRgb.MODULATE; <br>
 * COMBINE_ALPHA = CombineAlpha.MODULATE;<br>
 * SOURCE_RGB0 = CombineSource.PREV_TEX; <br>
 * SOURCE_RGB1 = CombineSource.CURR_TEX;<br>
 * SOURCE_RGB2 = CombineSource.VERTEX_COLOR; <br>
 * SOURCE_ALPHA0 = CombineSource.PREV_TEX; <br>
 * SOURCE_ALPHA1 = CombineSource.CURR_TEX; <br>
 * SOURCE_ALPHA2 = CombineSource.VERTEX_COLOR;<br>
 * <br>
 * OP_RGB0 = CombineOperand.COLOR;<br>
 * OP_RGB1 = CombineOperand.COLOR; <br>
 * OP_RGB2 = CombineOperand.ALPHA;<br>
 * OP_ALPHA0 = CombineOperand.ALPHA;<br>
 * OP_ALPHA1 = CombineOperand.ALPHA;<br>
 * OP_ALPHA2 = CombineOperand.ALPHA;<br>
 * <br>
 * ENV_MODE = EnvironmentMode.MODULATE;<br>
 * COORD_GEN = TexCoordGeneration.NONE;<br>
 * </p>
 * 
 * @author Michael Ludwig
 */
public class TextureEnvironment {
	/** Describes how texels are combined with other textures and colors. */
	public static enum EnvironmentMode {
		REPLACE, DECAL, MODULATE, BLEND, COMBINE
	}

	/**
	 * How a texture coordinates are generated automatically (or if the
	 * specified coordinates are used instead).
	 */
	public static enum TexCoordGeneration {
		/**
		 * NONE uses none, or the actual texture coordinates of the rendered
		 * object.
		 */
		NONE,
		/**
		 * OBJECT uses the texture coord generation planes in object space to
		 * generate coords.
		 */
		OBJECT,
		/**
		 * EYE uses those planes similarly to object space, but after everything
		 * has moved to eye space.
		 */
		EYE,
		/**
		 * SPHERE sets up coordinates suitable to access a sphere mapped texture
		 * image. It's only supported s and t coordinates.
		 */
		SPHERE,
		/**
		 * REFLECTION generates reflection vector coordinate at a given location
		 * (vertex normal + viewer location, etc). It's intended for all 3
		 * coordinates for cube maps
		 */
		REFLECTION,
		/**
		 * NORMAL generates vector coordinates based off of the surface normal.
		 * It's intended for all 3 coordinates for cube maps
		 */
		NORMAL
	}

	/**
	 * Specifies the function used to compute a texture's final color
	 * contribution when using the COMBINE EnvironmentMode. See above for
	 * descriptions of each function. DOT3_RGB and DOT3_RGBA can only be used
	 * for the rgb combine function and aren't allowed for the alpha function.
	 */
	public static enum CombineFunction {
		REPLACE, ADD, MODULATE, INTERPOLATE, ADD_SIGNED, SUBTRACT, DOT3_RGB, DOT3_RGBA
	}

	/**
	 * The source for a given component (0, 1, or 2) for a given combine
	 * operation.<br>
	 * CURR_TEX = color of this texture image <br>
	 * PREV_TEX = color of the texture in the texture unit processed just before
	 * this one (if this is the first unit, then it's the same as VERTEX_COLOR). <br>
	 * BLEND_COLOR = environment color of this texture <br>
	 * VERTEX_COLOR = color computed based on material color and lighting <br>
	 * TEXi = color of the texture image bound to the given unit
	 * <p>
	 * (<b>Note:</b> not all TEXi will be supported because hardware may not
	 * have that many texture units available. Units beyond 8 are included for
	 * advanced graphics cards (or future cards)).
	 * </p>
	 */
	public static enum CombineSource {
		CURR_TEX, PREV_TEX, BLEND_COLOR, VERTEX_COLOR, 
		TEX0, TEX1, TEX2, TEX3, TEX4, TEX5, TEX6, TEX7, TEX8, 
		TEX9, TEX10, TEX11, TEX12, TEX13, TEX14, TEX15, TEX16, 
		TEX17, TEX18, TEX19, TEX20, TEX21, TEX22, TEX23, TEX24, 
		TEX25, TEX26, TEX27, TEX28, TEX29, TEX30, TEX31
	}

	/**
	 * How to access a given source component (0, 1, or 2). COLOR and
	 * ONE_MINUS_COLOR are meaningless when used for alpha operands, so they are
	 * switched to ALPHA and ONE_MINUS_ALPHA when specified.
	 */
	public static enum CombineOperand {
		COLOR, ALPHA, ONE_MINUS_COLOR, ONE_MINUS_ALPHA
	}

	private static final CombineFunction DEFAULT_COMBINE_FUNC = CombineFunction.MODULATE;
	private static final CombineSource DEFAULT_SOURCE_RGB0 = CombineSource.PREV_TEX;
	private static final CombineSource DEFAULT_SOURCE_RGB1 = CombineSource.CURR_TEX;
	private static final CombineSource DEFAULT_SOURCE_RGB2 = CombineSource.VERTEX_COLOR;
	private static final CombineSource DEFAULT_SOURCE_ALPHA0 = CombineSource.PREV_TEX;
	private static final CombineSource DEFAULT_SOURCE_ALPHA1 = CombineSource.CURR_TEX;
	private static final CombineSource DEFAULT_SOURCE_ALPHA2 = CombineSource.VERTEX_COLOR;
	private static final CombineOperand DEFAULT_OP_RGB0 = CombineOperand.COLOR;
	private static final CombineOperand DEFAULT_OP_RGB1 = CombineOperand.COLOR;
	private static final CombineOperand DEFAULT_OP_RGB2 = CombineOperand.ALPHA;
	private static final CombineOperand DEFAULT_OP_ALPHA0 = CombineOperand.ALPHA;
	private static final CombineOperand DEFAULT_OP_ALPHA1 = CombineOperand.ALPHA;
	private static final CombineOperand DEFAULT_OP_ALPHA2 = CombineOperand.ALPHA;
	private static final EnvironmentMode DEFAULT_ENV_MODE = EnvironmentMode.MODULATE;
	private static final TexCoordGeneration DEFAULT_COORD_GEN = TexCoordGeneration.NONE;

	private boolean enabled;
	private TextureImage data;

	private CombineFunction combineRGBFunc;
	private CombineFunction combineAlphaFunc;

	private CombineSource sourceRGB0;
	private CombineSource sourceRGB1;
	private CombineSource sourceRGB2;

	private CombineSource sourceAlpha0;
	private CombineSource sourceAlpha1;
	private CombineSource sourceAlpha2;

	private CombineOperand operandRGB0;
	private CombineOperand operandRGB1;
	private CombineOperand operandRGB2;

	private CombineOperand operandAlpha0;
	private CombineOperand operandAlpha1;
	private CombineOperand operandAlpha2;

	private EnvironmentMode texEnvMode;
	private final Color4f texEnvColor;

	private Plane planeR;
	private Plane planeS;
	private Plane planeT;

	private TexCoordGeneration rCoordGen;
	private TexCoordGeneration sCoordGen;
	private TexCoordGeneration tCoordGen;

	private Matrix4f texCoordTransform;

	/**
	 * Create a TextureEnvironment with the default parameters and no bound
	 * TextureImage.  It is disabled to start out with.
	 */
	public TextureEnvironment() {
		setTexCoordGenerationSTR(null);
		setTextureEnvMode(null);

		setCombineAlphaFunction(null);
		setCombineRgbFunction(null);
		setSourceAlpha(null, null, null);
		setSourceRgb(null, null, null);

		setOperandAlpha(null, null, null);
		setOperandRgb(null, null, null);

		setPlaneR(null);
		setPlaneS(null);
		setPlaneT(null);
		
		setEnabled(false);

		data = null;
		texCoordTransform = null;
		texEnvColor = new Color4f();
	}

	/**
	 * Return whether or not this TextureEnvironment is enabled. If this returns
	 * true, then the environment is used to modify rendered fragments to
	 * include contributions from the associated TextureImage.
	 * 
	 * @return True if this TextureEnvironment is enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set whether or not this TextureEnvironment is enabled. If it's enabled,
	 * then the associated TextureImage will be combined with rendered fragments
	 * so that the final image is 'textured'. If this environment has no
	 * associated TextureImage, then the environment's enabled state is ignored
	 * and there will be no effect.
	 * 
	 * @param enabled True if this TextureEnvironment is enabled
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setEnabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	/**
	 * Get the plane that is used to compute the R texture coordinate when R
	 * texture coord generation is OBJECT or EYE.
	 * 
	 * @return Plane for OBJECT/EYE coordinate generation
	 */
	public Plane getPlaneR() {
		return planeR;
	}

	/**
	 * Get the plane that is used to compute the S texture coordinate when S
	 * texture coord generation is OBJECT or EYE.
	 * 
	 * @return Plane for OBJECT/EYE coordinate generation
	 */
	public Plane getPlaneS() {
		return planeS;
	}

	/**
	 * Get the plane that is used to compute the T texture coordinate when T
	 * texture coord generation is OBJECT or EYE.
	 * 
	 * @return Plane for OBJECT/EYE coordinate generation
	 */
	public Plane getPlaneT() {
		return planeT;
	}

	/**
	 * Set the plane that is used to compute the R texture coordinate when R
	 * texture coord generation is OBJECT or EYE.
	 * 
	 * @param plane Plane for OBJECT/EYE coordinate generation, null = (0, 0, 1,
	 *            0)
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setPlaneR(Plane plane) {
		if (plane == null)
			plane = new Plane(0f, 0f, 1f, 0f);
		planeR = plane;
		return this;
	}

	/**
	 * Set the plane that is used to compute the S texture coordinate when S
	 * texture coord generation is OBJECT or EYE.
	 * 
	 * @param plane Plane for OBJECT/EYE coordinate generation, null = (1, 0, 0,
	 *            0)
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setPlaneS(Plane plane) {
		if (plane == null)
			plane = new Plane(1f, 0f, 0f, 0f);
		planeS = plane;
		return this;
	}

	/**
	 * Set the plane that is used to compute the T texture coordinate when T
	 * texture coord generation is OBJECT or EYE.
	 * 
	 * @param plane Plane for OBJECT/EYE coordinate generation, null = (0, 1, 0,
	 *            0)
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setPlaneT(Plane plane) {
		if (plane == null)
			plane = new Plane(0f, 1f, 0f, 0f);
		planeT = plane;
		return this;
	}

	/**
	 * Set the transform that is applied to each texture coordinate before
	 * accessing the texture. This value may be null (if it is, Framework's
	 * should treat it as an identity transform).
	 * 
	 * @param trans Matrix transform applied to all texture coordinates before
	 *            image access
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setCoordinateTransform(Matrix4f trans) {
		texCoordTransform = trans;
		return this;
	}

	/**
	 * Get the texture coordinate transform. This may be null, and if it is, it
	 * should be treated as the identity.
	 * 
	 * @return Current texture coordinate transform
	 */
	public Matrix4f getTextureTransform() {
		return texCoordTransform;
	}

	/**
	 * Get the TextureImage data used by this texture. If null, then this
	 * TextureEnvironment will behave as if it's disabled (even if isEnabled()
	 * returns true).
	 * 
	 * @return TextureImage used as image data for pixel colors
	 */
	public TextureImage getImage() {
		return data;
	}

	/**
	 * Set the texture image data used for this texture.
	 * 
	 * @param data TextureImage to use, can be null
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setImage(TextureImage data) {
		this.data = data;
		return this;
	}

	/**
	 * Get the equation used to combine rgb values when the texture environment
	 * mode is COMBINE.
	 * 
	 * @return CombineRgb equation in use
	 */
	public CombineFunction getCombineRgbFunction() {
		return combineRGBFunc;
	}

	/**
	 * Set the equation used to combine rgb values when the texture environment
	 * mode is COMBINE.
	 * 
	 * @param combineRgb CombineRgb to use, null uses default
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setCombineRgbFunction(CombineFunction combineRgb) {
		if (combineRgb == null)
			combineRgb = DEFAULT_COMBINE_FUNC;
		combineRGBFunc = combineRgb;
		return this;
	}

	/**
	 * Get the equation used to combine alpha values when the texture
	 * environment mode is COMBINE.
	 * 
	 * @return CombineAlpha equation in use
	 */
	public CombineFunction getCombineAlphaFunction() {
		return combineAlphaFunc;
	}

	/**
	 * Set the equation used to combine alpha values when the texture
	 * environment mode is COMBINE.
	 * 
	 * @param combineAlphaFunc CombineAlpha to use, null uses default
	 * @throws IllegalArgumentException if combineAlphaFunc is equal to DOT3_RGB
	 *             or DOT3_RGBA
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setCombineAlphaFunction(CombineFunction combineAlpha) {
		if (combineAlpha == CombineFunction.DOT3_RGB || combineAlpha == CombineFunction.DOT3_RGBA)
			throw new IllegalArgumentException("Combine function for alpha cannot use DOT3 functions");
		if (combineAlpha == null)
			combineAlpha = DEFAULT_COMBINE_FUNC;
		this.combineAlphaFunc = combineAlpha;
		
		return this;
	}

	/**
	 * Get the source of the first input for rgb combining.
	 * 
	 * @return CombineSource for Rgb0 in COMBINE EnvironmentMode
	 */
	public CombineSource getSourceRgb0() {
		return sourceRGB0;
	}

	/**
	 * Set the source of the first input for rgb combining.
	 * 
	 * @param sourceRgb0 CombineSource for Rgb0, null uses default
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setSourceRgb0(CombineSource sourceRgb0) {
		if (sourceRgb0 == null)
			sourceRgb0 = DEFAULT_SOURCE_RGB0;
		sourceRGB0 = sourceRgb0;
		return this;
	}

	/**
	 * Get the source of the second input for rgb combining.
	 * 
	 * @return CombineSource for Rgb1 in COMBINE EnvironmentMode
	 */
	public CombineSource getSourceRgb1() {
		return sourceRGB1;
	}

	/**
	 * Set the source of the second input for rgb combining.
	 * 
	 * @param sourceRgb1 CombineSource for Rgb1, null uses default
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setSourceRgb1(CombineSource sourceRgb1) {
		if (sourceRgb1 == null)
			sourceRgb1 = DEFAULT_SOURCE_RGB1;
		sourceRGB1 = sourceRgb1;
		return this;
	}

	/**
	 * Get the source of the third input for rgb combining.
	 * 
	 * @return CombineSource for Rgb2 in COMBINE EnvironmentMode
	 */
	public CombineSource getSourceRgb2() {
		return sourceRGB2;
	}

	/**
	 * Set the source of the third input for rgb combining.
	 * 
	 * @param sourceRgb2 CombineSource for Rgb2, null uses default
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setSourceRgb2(CombineSource sourceRgb2) {
		if (sourceRgb2 == null)
			sourceRgb2 = DEFAULT_SOURCE_RGB2;
		sourceRGB2 = sourceRgb2;
		return this;
	}

	/**
	 * Get the source of the first input for alpha combining.
	 * 
	 * @return CombineSource for Alpha0 in COMBINE EnvironmentMode
	 */
	public CombineSource getSourceAlpha0() {
		return sourceAlpha0;
	}

	/**
	 * Get the source of the first input for alpha combining.
	 * 
	 * @param sourceAlpha0 CombineSource for Alpha0, null uses default
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setSourceAlpha0(CombineSource sourceAlpha0) {
		if (sourceAlpha0 == null)
			sourceAlpha0 = DEFAULT_SOURCE_ALPHA0;
		this.sourceAlpha0 = sourceAlpha0;
		return this;
	}

	/**
	 * Get the source of the second input for alpha combining.
	 * 
	 * @return CombineSource for Alpha1 in COMBINE EnvironmentMode
	 */
	public CombineSource getSourceAlpha1() {
		return sourceAlpha1;
	}

	/**
	 * Get the source of the second input for alpha combining.
	 * 
	 * @param sourceAlpha1 CombineSource for Alpha1, null uses default
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setSourceAlpha1(CombineSource sourceAlpha1) {
		if (sourceAlpha1 == null)
			sourceAlpha1 = DEFAULT_SOURCE_ALPHA1;
		this.sourceAlpha1 = sourceAlpha1;
		return this;
	}

	/**
	 * Get the source of the third input for alpha combining.
	 * 
	 * @return CombineSource for Alpha2 in COMBINE EnvironmentMode
	 */
	public CombineSource getSourceAlpha2() {
		return sourceAlpha2;
	}

	/**
	 * Get the source of the third input for alpha combining.
	 * 
	 * @param sourceAlpha2 CombineSource for Alpha2, null uses default
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setSourceAlpha2(CombineSource sourceAlpha2) {
		if (sourceAlpha2 == null)
			sourceAlpha2 = DEFAULT_SOURCE_ALPHA2;
		this.sourceAlpha2 = sourceAlpha2;
		return this;
	}

	/**
	 * Get the operand applied to the rgb color of the first source.
	 * 
	 * @return CombineOperand used on Rgb0 in COMBINE EnvironmentMode
	 */
	public CombineOperand getOperandRgb0() {
		return operandRGB0;
	}

	/**
	 * Set the operand applied to the rgb color of the first source.
	 * 
	 * @param operandRgb0 CombineOperand applied to Rgb0
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setOperandRgb0(CombineOperand operandRgb0) {
		if (operandRgb0 == null)
			operandRgb0 = DEFAULT_OP_RGB0;
		operandRGB0 = operandRgb0;
		return this;
	}

	/**
	 * Get the operand applied to the rgb color of the second source.
	 * 
	 * @return CombineOperand used on Rgb1 in COMBINE EnvironmentMode
	 */
	public CombineOperand getOperandRgb1() {
		return operandRGB1;
	}

	/**
	 * Set the operand applied to the rgb color of the second source.
	 * 
	 * @param operandRgb1 CombineOperand applied to Rgb1
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setOperandRgb1(CombineOperand operandRgb1) {
		if (operandRgb1 == null)
			operandRgb1 = DEFAULT_OP_RGB1;
		operandRGB1 = operandRgb1;
		return this;
	}

	/**
	 * Get the operand applied to the rgb color of the third source.
	 * 
	 * @return CombineOperand used on Rgb2 in COMBINE EnvironmentMode
	 */
	public CombineOperand getOperandRgb2() {
		return operandRGB2;
	}

	/**
	 * Set the operand applied to the rgb color of the third source.
	 * 
	 * @param operandRgb2 CombineOperand applied to Rgb2
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setOperandRgb2(CombineOperand operandRgb2) {
		if (operandRgb2 == null)
			operandRgb2 = DEFAULT_OP_RGB2;
		operandRGB2 = operandRgb2;
		return this;
	}

	/**
	 * Get the operand applied to the alpha value of the first source.
	 * 
	 * @return CombineOperand used on Alpha0 in COMBINE EnvironmentMode
	 */
	public CombineOperand getOperandAlpha0() {
		return operandAlpha0;
	}

	/**
	 * Set the operand applied to the alpha value of the first source. If
	 * operandAlpha1 is equal to ONE_MINUS_COLOR it is set to ONE_MINUS_ALPHA,
	 * if it is equal to COLOR then it is set to ALPHA.
	 * 
	 * @param operandAlpha0 CombineOperand applied to Alpha0
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setOperandAlpha0(CombineOperand operandAlpha0) {
		if (operandAlpha0 == null)
			operandAlpha0 = DEFAULT_OP_ALPHA0;

		if (operandAlpha0 == CombineOperand.ONE_MINUS_COLOR)
			operandAlpha0 = CombineOperand.ONE_MINUS_ALPHA;
		else if (operandAlpha0 == CombineOperand.COLOR)
			operandAlpha0 = CombineOperand.ALPHA;

		this.operandAlpha0 = operandAlpha0;
		return this;
	}

	/**
	 * Get the operand applied to the alpha value of the second source.
	 * 
	 * @return CombineOperand used on Alpha1 in COMBINE EnvironmentMode
	 */
	public CombineOperand getOperandAlpha1() {
		return operandAlpha1;
	}

	/**
	 * Set the operand applied to the alpha value of the second source. If
	 * operandAlpha1 is equal to ONE_MINUS_COLOR it is set to ONE_MINUS_ALPHA,
	 * if it is equal to COLOR then it is set to ALPHA.
	 * 
	 * @param operandAlpha1 CombineOperand applied to Alpha1
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setOperandAlpha1(CombineOperand operandAlpha1) {
		if (operandAlpha1 == null)
			operandAlpha1 = DEFAULT_OP_ALPHA1;

		if (operandAlpha1 == CombineOperand.ONE_MINUS_COLOR)
			operandAlpha1 = CombineOperand.ONE_MINUS_ALPHA;
		else if (operandAlpha1 == CombineOperand.COLOR)
			operandAlpha1 = CombineOperand.ALPHA;

		this.operandAlpha1 = operandAlpha1;
		return this;
	}

	/**
	 * Get the operand applied to the alpha value of the third source.
	 * 
	 * @return CombineOperand used on Alpha2 in COMBINE EnvironmentMode
	 */
	public CombineOperand getOperandAlpha2() {
		return operandAlpha2;
	}

	/**
	 * Set the operand applied to the alpha value of the third source. If
	 * operandAlpha1 is equal to ONE_MINUS_COLOR it is set to ONE_MINUS_ALPHA,
	 * if it is equal to COLOR then it is set to ALPHA.
	 * 
	 * @param operandAlpha2 CombineOperand applied to Alpha2
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setOperandAlpha2(CombineOperand operandAlpha2) {
		if (operandAlpha2 == null)
			operandAlpha2 = DEFAULT_OP_ALPHA2;

		if (operandAlpha2 == CombineOperand.ONE_MINUS_COLOR)
			operandAlpha2 = CombineOperand.ONE_MINUS_ALPHA;
		else if (operandAlpha2 == CombineOperand.COLOR)
			operandAlpha2 = CombineOperand.ALPHA;

		this.operandAlpha2 = operandAlpha2;
		return this;
	}

	/**
	 * Convenience method to set alpha operands for the three combine variables.
	 * 
	 * @param a0 CombineOperand for setOperandAlpha0()
	 * @param a1 CombineOperand for setOperandAlpha1()
	 * @param a2 CombineOperand for setOperandAlpha2()
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setOperandAlpha(CombineOperand a0, CombineOperand a1, CombineOperand a2) {
		return setOperandAlpha0(a0).setOperandAlpha1(a1).setOperandAlpha2(a2);
	}

	/**
	 * Convenience method to set the rgb color operands for the three combine
	 * variables.
	 * 
	 * @param r0 CombineOperand for setOperandRgb0()
	 * @param r1 CombineOperand for setOperandRgb1()
	 * @param r2 CombineOperand for setOperandRgb2()
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setOperandRgb(CombineOperand r0, CombineOperand r1, CombineOperand r2) {
		return setOperandRgb0(r0).setOperandRgb1(r1).setOperandRgb2(r2);
	}

	/**
	 * Convenience method to set the alpha source for the three combine
	 * variables.
	 * 
	 * @param a0 CombineSource for setSourceAlpha0()
	 * @param a1 CombineSource for setSourceAlpha1()
	 * @param a2 CombineSource for setSourceAlpha2()
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setSourceAlpha(CombineSource a0, CombineSource a1, CombineSource a2) {
		return setSourceAlpha0(a0).setSourceAlpha1(a1).setSourceAlpha2(a2);
	}

	/**
	 * Convenience method to set the rgb color source for the three combine
	 * variables.
	 * 
	 * @param r0 CombineSource for setSourceRgb0()
	 * @param r1 CombineSource for setSourceRgb1()
	 * @param r2 CombineSource for setSourceRgb2()
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setSourceRgb(CombineSource r0, CombineSource r1, CombineSource r2) {
		return setSourceRgb0(r0).setSourceRgb1(r1).setSourceRgb2(r2);
	}

	/**
	 * Get the texture environment mode. This determines how the texture is combined
	 * with other intermediate fragment colors.
	 * 
	 * @return The EnvironmentMode used to apply TextureImages when rendering
	 */
	public EnvironmentMode getEnvironmentMode() {
		return texEnvMode;
	}

	/**
	 * Set the texture environment mode. If texEnvMode is COMBINE the actual
	 * fragment colors are determined by the current configuration of
	 * CombineFunctions, CombineSources and CombienOperands of this
	 * TextureEnvironment.
	 * 
	 * @param texEnvMode EnvironmentMode for applying textures, null uses
	 *            MODULATE
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setTextureEnvMode(EnvironmentMode texEnvMode) {
		if (texEnvMode == null)
			texEnvMode = DEFAULT_ENV_MODE;
		this.texEnvMode = texEnvMode;
		return this;
	}

	/**
	 * Get the color to use for blending when the environment mode is BLEND, or
	 * when using BLEND_COLOR for a CombineSource.
	 * 
	 * @return Color4f instance used when BLEND is the EnvironmentMode
	 */
	public Color4f getBlendColor() {
		return texEnvColor;
	}

	/**
	 * Copy texEnvColor into the color to use for blending when env mode is
	 * BLEND, or using a BLEND_COLOR CombineSource.
	 * 
	 * @param texEnvColor Color4f for use with BLEND mode
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setBlendColor(Color4f texEnvColor) {
		if (texEnvColor == null)
			texEnvColor = new Color4f();
		this.texEnvColor.set(texEnvColor);
		return this;
	}

	/**
	 * Get the texture coordinate generation policy for the r tex coord (third).
	 * 
	 * @return TexCoordGeneration for the r coordinate
	 */
	public TexCoordGeneration getTexCoordGenerationR() {
		return rCoordGen;
	}

	/**
	 * Set the texture coordinate generation policy for the r tex coord (third).
	 * If null uses the default. A value of NONE causes texture coordinates to
	 * come from the rendered geometry. SPHERE mapping isn't supported on the
	 * 3rd coordinate, so if SPHERE is given, NONE is used instead.
	 * 
	 * @param coordGen TexCoordGeneration to use
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setTexCoordGenerationR(TexCoordGeneration coordGen) {
		if (coordGen == null || coordGen == TexCoordGeneration.SPHERE)
			coordGen = DEFAULT_COORD_GEN;
		rCoordGen = coordGen;
		return this;
	}

	/**
	 * Get the texture coordinate generation policy for the s tex coord (first).
	 * 
	 * @return TexCoordGeneration for the s coordinate
	 */
	public TexCoordGeneration getTexCoordGenerationS() {
		return sCoordGen;
	}

	/**
	 * Set the texture coordinate generation policy for the s tex coord (first).
	 * If null uses the default. A value of NONE causes texture coordinates to
	 * come from the rendered geometry.
	 * 
	 * @param coordGen TexCoordGeneration to use
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setTexCoordGenerationS(TexCoordGeneration coordGen) {
		if (coordGen == null)
			coordGen = DEFAULT_COORD_GEN;
		sCoordGen = coordGen;
		return this;
	}

	/**
	 * Get the texture coordinate generation policy for the t tex coord
	 * (second).
	 * 
	 * @return TexCoordGeneration for the t coordinate
	 */
	public TexCoordGeneration getTexCoordGenerationT() {
		return tCoordGen;
	}

	/**
	 * Set the texture coordinate generation policy for the t tex coord
	 * (second). If null uses the default. A value of NONE causes texture
	 * coordinates to come from the rendered geometry.
	 * 
	 * @param coordGen TexCoordGeneration to use
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setTexCoordGenerationT(TexCoordGeneration coordGen) {
		if (coordGen == null)
			coordGen = DEFAULT_COORD_GEN;
		tCoordGen = coordGen;
		return this;
	}
	
	/**
	 * Convenience method to set all texture coordinate generations to the given
	 * type.
	 * 
	 * @param coordGen TexCoordGeneration for s, t, and r coordinates
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setTexCoordGenerationSTR(TexCoordGeneration coordGen) {
		return setTexCoordGenerationSTR(coordGen, coordGen, coordGen);
	}

	/**
	 * Convenience method to set the three coordinate generation policies for
	 * each coordinate.
	 * 
	 * @param s TexCoordGeneration for the s coordinate
	 * @param t TexCoordGeneration for the t coordinate
	 * @param r TexCoordGeneration for the r coordinate
	 * @return This TextureEnvironment
	 */
	public TextureEnvironment setTexCoordGenerationSTR(TexCoordGeneration s, TexCoordGeneration t, TexCoordGeneration r) {
		return setTexCoordGenerationS(s).setTexCoordGenerationT(t).setTexCoordGenerationR(r);
	}
}
