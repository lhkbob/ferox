package com.ferox.effect;

import com.ferox.math.Color;
import com.ferox.math.Plane;
import com.ferox.math.Transform;
import com.ferox.resource.texture.TextureImage;

/**
 * <p>
 * A Texture wraps a TextureImage a provides information on how it will be
 * blended together with other textures and material colors of the rendered
 * object. It also controls texture coordinate generation (if desired), and
 * setting the a transform that is applied to each texture coordinate before
 * accessing the texture.
 * </p>
 * <p>
 * If using multiple textures in an EffectSet, it is recommended to use
 * MultiTexture. When a Texture is used, it is equivalent to a MultiTexture with
 * the Texture at unit 0.
 * </p>
 * <p>
 * Texture's also allow for advanced combinations when the EnvMode COMBINE is
 * used. The EnvModes and combine operations are modeled directly after the
 * texture environment options in OpenGL.
 * </p>
 * <p>
 * Texture's also allow for advanced combinations when the EnvMode COMBINE is
 * used.<br>
 * Summary of EnvMode operations:<br>
 * Cv = final rgb color Av = final alpha<br>
 * Cs = rgb of this texture As = alpha of this tex<br>
 * Cp = rgb of prev. color Ap = alpha of prev. color<br>
 * <br>
 * <code>
 * Format		| REPLACE 	| MODULATE	| DECAL					| BLEND					| Add
 * Alpha		| Cv = Cp	| Cv = Cp	| --					| Cv = Cp				| Cv = Cp
 * 				  Av = As	  Av = ApAs 						  Av = ApAs			 	  Av = ApAs
 * Luminance	| Cv = Cs	| Cv = CpCs	| --					| Cv = Cp(1-Cs)+CcCs	| Cv = Cp+Cs
 * 				  Av = Ap 	  Av = Ap							  Av = Ap				  Av = Ap
 * Lum. Alpha	| Cv = Cs	| Cv = CpCs	| --					| Cv = Cp(1-Cs)+CcCs	| Cv = Cp+Cs
 * 				  Av = As	  Av = ApAs							  Av = ApAs				  Av = ApAs
 * RGB			| Cv = Cs	| Cv = CpCs	| Cv = Cs				| Cv = Cp(1-Cs)+CcCs	| Cv = Cp+Cs
 * 				  Av = Ap	  Av = Ap	  Av = Ap				  Av = Ap				  Av = Ap
 * RGBA			| Cv = Cs	| Cv = CpCs	| Cv = Cp(1-As)+CsAs	| Cv = Cp(1-Cs)+CcCs	| Cv = Cp+Cs
 * 				  Av = As	  Av = ApAs	  Av = Ap				  Av = ApAs				  Av = ApAs
 * <code>
 * </p>
 * <p>
 * Operations on colors are done component wise. Cs for Luminance is (L, L, L),
 * As for Lum. is L Cs for LA is (L, L, L), As for LA is A
 * </p>
 * <p>
 * Summary of Combine operation:<br>
 * In the table below, opX is a function applied to either a color(per
 * component) or an alpha value as defined in CombineOperand. srcX is the input
 * and is designated with CombineSource In the DOT3_x varieties, rN, gN, and bN
 * represent the red, green, and blue values of opN(srcN).<br>
 * <br>
 * <code>
 * CombineRgb 	|	Rgb Result
 * REPLACE		|	op0(src0)
 * MODULATE		|	op0(src0) * op1(src1)
 * ADD			|	op0(src0) + op1(src1)
 * ADD_SIGNED	|	op0(src0) + op1(src1) - .5
 * INTERPOLATE	| 	op0(src0) * op2(src2) + op1(src1) * (1 - op2(src2))
 * SUBTRACT		| 	op0(src0) - op1(src1)
 * DOT3_RGB		| 	4 * ((r0-.5)*(r1-.5) + (g0-.5)*(g1-.5) + (b0-.5)*(b1-.5))
 * DOT3_RGBA	|	4 * ((r0-.5)*(r1-.5) + (g0-.5)*(g1-.5) + (b0-.5)*(b1-.5))
 * </code>
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
 * ENV_MODE = EnvMode.MODULATE;<br>
 * COORD_GEN = TexCoordGen.NONE;<br>
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Texture extends AbstractEffect {
	/** Describes how texels are combined with other textures and colors. */
	public static enum EnvMode {
		REPLACE, DECAL, MODULATE, BLEND, COMBINE
	}

	/**
	 * How a texture coordinates are generated automatically (or if the
	 * specified coordinates are used instead).
	 */
	public static enum TexCoordGen {
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
	 * The combine operation applied to the texture's alpha sources (after being
	 * affected by the operands).
	 */
	public static enum CombineAlpha {
		REPLACE, ADD, MODULATE, INTERPOLATE, ADD_SIGNED, SUBTRACT
	}

	/**
	 * As CombineAlpha, but for RGB values. Adds DOT3_RGB and DOT3_RGBA that can
	 * be used to simulate bump-mapping. DOT3_RGBA ignores the alpha value
	 * generated by CombineAlpha.
	 */
	public static enum CombineRgb {
		REPLACE, ADD, MODULATE, INTERPOLATE, ADD_SIGNED, SUBTRACT, DOT3_RGB,
		DOT3_RGBA
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
		CURR_TEX, PREV_TEX, BLEND_COLOR, VERTEX_COLOR, TEX0, TEX1, TEX2, TEX3,
		TEX4, TEX5, TEX6, TEX7, TEX8, TEX9, TEX10, TEX11, TEX12, TEX13, TEX14,
		TEX15, TEX16, TEX17, TEX18, TEX19, TEX20, TEX21, TEX22, TEX23, TEX24,
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

	private static final CombineRgb DEFAULT_COMBINE_RGB = CombineRgb.MODULATE;
	private static final CombineAlpha DEFAULT_COMBINE_ALPHA = CombineAlpha.MODULATE;
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
	private static final EnvMode DEFAULT_ENV_MODE = EnvMode.MODULATE;
	private static final TexCoordGen DEFAULT_COORD_GEN = TexCoordGen.NONE;

	private TextureImage data;

	private CombineRgb combineRGBFunc;
	private CombineAlpha combineAlphaFunc;

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

	private EnvMode texEnvMode;
	private Color texEnvColor;

	private Plane planeR;
	private Plane planeS;
	private Plane planeT;

	private TexCoordGen rCoordGen;
	private TexCoordGen sCoordGen;
	private TexCoordGen tCoordGen;

	private Transform texTrans;

	/**
	 * Create a texture with default parameters and the given texture image.
	 * 
	 * @param data TextureImage used when rendering
	 */
	public Texture(TextureImage data) {
		this();
		setTexture(data);
	}

	/**
	 * Create a texture with default parameters, and the given image and
	 * environment mode.
	 * 
	 * @param data TextureImage used when rendering
	 * @param mode EnvMode used to compute final pixel color
	 */
	public Texture(TextureImage data, EnvMode mode) {
		this();
		setTexture(data);
		setTextureEnvMode(mode);
	}

	/**
	 * Create a texture with default plane and combine modes, and the given
	 * image, environment mode, environment color and texture coord. generation.
	 * 
	 * @param data TextureImage used when rendering
	 * @param mode EnvMode used to compute final pixel color
	 * @param envColor Color used for certain EnvModes
	 * @param texCoord Texture coordinate generation mode for STR coordinates
	 */
	public Texture(TextureImage data, EnvMode mode, Color envColor,
			TexCoordGen texCoord) {
		this();
		setTexture(data);
		setTextureEnvMode(mode);
		setTextureEnvColor(envColor);
		this.setTexCoordGenSTR(texCoord);
	}

	/**
	 * Create a blank texture, with default values, no texture image and no
	 * texture transform.
	 */
	public Texture() {
		this.setTexCoordGenSTR(null);
		setTextureEnvColor(null);
		setTextureEnvMode(null);

		setCombineAlphaEquation(null);
		setCombineRgbEquation(null);
		setSourceAlpha(null, null, null);
		setSourceRgb(null, null, null);

		setOperandAlpha(null, null, null);
		setOperandRgb(null, null, null);

		setTexCoordGenPlaneR(null);
		setTexCoordGenPlaneS(null);
		setTexCoordGenPlaneT(null);

		data = null;
		texTrans = null;
	}

	/**
	 * Get the plane that is used to compute the R texture coordinate when R
	 * texture coord generation is OBJECT or EYE.
	 * 
	 * @return Plane for OBJECT/EYE coordinate generation
	 */
	public Plane getTexCoordGenPlaneR() {
		return planeR;
	}

	/**
	 * Get the plane that is used to compute the S texture coordinate when S
	 * texture coord generation is OBJECT or EYE.
	 * 
	 * @return Plane for OBJECT/EYE coordinate generation
	 */
	public Plane getTexCoordGenPlaneS() {
		return planeS;
	}

	/**
	 * Get the plane that is used to compute the T texture coordinate when T
	 * texture coord generation is OBJECT or EYE.
	 * 
	 * @return Plane for OBJECT/EYE coordinate generation
	 */
	public Plane getTexCoordGenPlaneT() {
		return planeT;
	}

	/**
	 * Set the plane that is used to compute the R texture coordinate when R
	 * texture coord generation is OBJECT or EYE.
	 * 
	 * @param plane Plane for OBJECT/EYE coordinate generation, null = (0, 0, 1,
	 *            0)
	 */
	public void setTexCoordGenPlaneR(Plane plane) {
		if (plane == null)
			plane = new Plane(0f, 0f, 1f, 0f);
		planeR = plane;
	}

	/**
	 * Set the plane that is used to compute the S texture coordinate when S
	 * texture coord generation is OBJECT or EYE.
	 * 
	 * @param plane Plane for OBJECT/EYE coordinate generation, null = (1, 0, 0,
	 *            0)
	 */
	public void setTexCoordGenPlaneS(Plane plane) {
		if (plane == null)
			plane = new Plane(1f, 0f, 0f, 0f);
		planeS = plane;
	}

	/**
	 * Set the plane that is used to compute the T texture coordinate when T
	 * texture coord generation is OBJECT or EYE.
	 * 
	 * @param plane Plane for OBJECT/EYE coordinate generation, null = (0, 1, 0,
	 *            0)
	 */
	public void setTexCoordGenPlaneT(Plane plane) {
		if (plane == null)
			plane = new Plane(0f, 1f, 0f, 0f);
		planeT = plane;
	}

	/**
	 * Set the texture transform that is applied to each texture coordinate
	 * before accessing the texture. This value may be null (if it is,
	 * Renderer's should treat it as an identity transform).
	 * 
	 * @param trans Transform applied to all texture coordinates before image
	 *            access
	 */
	public void setTextureTransform(Transform trans) {
		texTrans = trans;
	}

	/**
	 * Get the texture coordinate transform. May be null.
	 * 
	 * @return Current texture coordinate transform
	 */
	public Transform getTextureTransform() {
		return texTrans;
	}

	/**
	 * Get the texture image data used by this texture. If null, then this
	 * texture has no effect on the rendering.
	 * 
	 * @return TextureImage used as image data for pixel colors
	 */
	public TextureImage getTexture() {
		return data;
	}

	/**
	 * Set the texture image data used for this texture.
	 * 
	 * @param TextureImage to use, can be null
	 */
	public void setTexture(TextureImage data) {
		this.data = data;
	}

	/**
	 * Get the equation used to combine rgb values when the texture environment
	 * mode is COMBINE.
	 * 
	 * @return CombineRgb equation in use
	 */
	public CombineRgb getCombineRgbEquation() {
		return combineRGBFunc;
	}

	/**
	 * Set the equation used to combine rgb values when the texture environment
	 * mode is COMBINE.
	 * 
	 * @param combineRgb CombineRgb to use, null uses default
	 */
	public void setCombineRgbEquation(CombineRgb combineRgb) {
		if (combineRgb == null)
			combineRgb = DEFAULT_COMBINE_RGB;
		combineRGBFunc = combineRgb;
	}

	/**
	 * Get the equation used to combine alpha values when the texture
	 * environment mode is COMBINE.
	 * 
	 * @return CombineAlpha equation in use
	 */
	public CombineAlpha getCombineAlphaEquation() {
		return combineAlphaFunc;
	}

	/**
	 * Set the equation used to combine alpha values when the texture
	 * environment mode is COMBINE.
	 * 
	 * @param combineAlpha CombineAlpha to use, null uses default
	 */
	public void setCombineAlphaEquation(CombineAlpha combineAlphaFunc) {
		if (combineAlphaFunc == null)
			combineAlphaFunc = DEFAULT_COMBINE_ALPHA;
		this.combineAlphaFunc = combineAlphaFunc;
	}

	/**
	 * Get the source of the first input for rgb combining.
	 * 
	 * @return CombineSource for Rgb0 in COMBINE EnvMode
	 */
	public CombineSource getSourceRgb0() {
		return sourceRGB0;
	}

	/**
	 * Set the source of the first input for rgb combining.
	 * 
	 * @param sourceRgb0 CombineSource for Rgb0, null uses default
	 */
	public void setSourceRgb0(CombineSource sourceRgb0) {
		if (sourceRgb0 == null)
			sourceRgb0 = DEFAULT_SOURCE_RGB0;
		sourceRGB0 = sourceRgb0;
	}

	/**
	 * Get the source of the second input for rgb combining.
	 * 
	 * @return CombineSource for Rgb1 in COMBINE EnvMode
	 */
	public CombineSource getSourceRgb1() {
		return sourceRGB1;
	}

	/**
	 * Set the source of the second input for rgb combining.
	 * 
	 * @param sourceRgb1 CombineSource for Rgb1, null uses default
	 */
	public void setSourceRgb1(CombineSource sourceRgb1) {
		if (sourceRgb1 == null)
			sourceRgb1 = DEFAULT_SOURCE_RGB1;
		sourceRGB1 = sourceRgb1;
	}

	/**
	 * Get the source of the third input for rgb combining.
	 * 
	 * @return CombineSource for Rgb2 in COMBINE EnvMode
	 */
	public CombineSource getSourceRgb2() {
		return sourceRGB2;
	}

	/**
	 * Set the source of the third input for rgb combining.
	 * 
	 * @param sourceRgb2 CombineSource for Rgb2, null uses default
	 */
	public void setSourceRgb2(CombineSource sourceRgb2) {
		if (sourceRgb2 == null)
			sourceRgb2 = DEFAULT_SOURCE_RGB2;
		sourceRGB2 = sourceRgb2;
	}

	/**
	 * Get the source of the first input for alpha combining.
	 * 
	 * @return CombineSource for Alpha0 in COMBINE EnvMode
	 */
	public CombineSource getSourceAlpha0() {
		return sourceAlpha0;
	}

	/**
	 * Get the source of the first input for alpha combining.
	 * 
	 * @param sourceAlpha0 CombineSource for Alpha0, null uses default
	 */
	public void setSourceAlpha0(CombineSource sourceAlpha0) {
		if (sourceAlpha0 == null)
			sourceAlpha0 = DEFAULT_SOURCE_ALPHA0;
		this.sourceAlpha0 = sourceAlpha0;
	}

	/**
	 * Get the source of the second input for alpha combining.
	 * 
	 * @return CombineSource for Alpha1 in COMBINE EnvMode
	 */
	public CombineSource getSourceAlpha1() {
		return sourceAlpha1;
	}

	/**
	 * Get the source of the second input for alpha combining.
	 * 
	 * @param sourceAlpha1 CombineSource for Alpha1, null uses default
	 */
	public void setSourceAlpha1(CombineSource sourceAlpha1) {
		if (sourceAlpha1 == null)
			sourceAlpha1 = DEFAULT_SOURCE_ALPHA1;
		this.sourceAlpha1 = sourceAlpha1;
	}

	/**
	 * Get the source of the third input for alpha combining.
	 * 
	 * @return CombineSource for Alpha2 in COMBINE EnvMode
	 */
	public CombineSource getSourceAlpha2() {
		return sourceAlpha2;
	}

	/**
	 * Get the source of the third input for alpha combining.
	 * 
	 * @param sourceAlpha2 CombineSource for Alpha2, null uses default
	 */
	public void setSourceAlpha2(CombineSource sourceAlpha2) {
		if (sourceAlpha2 == null)
			sourceAlpha2 = DEFAULT_SOURCE_ALPHA2;
		this.sourceAlpha2 = sourceAlpha2;
	}

	/**
	 * Get the operand applied to the rgb color of the first source.
	 * 
	 * @return CombineOperand used on Rgb0 in COMBINE EnvMode
	 */
	public CombineOperand getOperandRgb0() {
		return operandRGB0;
	}

	/**
	 * Set the operand applied to the rgb color of the first source.
	 * 
	 * @param operandRgb0 CombineOperand applied to Rgb0
	 */
	public void setOperandRgb0(CombineOperand operandRgb0) {
		if (operandRgb0 == null)
			operandRgb0 = DEFAULT_OP_RGB0;
		operandRGB0 = operandRgb0;
	}

	/**
	 * Get the operand applied to the rgb color of the second source.
	 * 
	 * @return CombineOperand used on Rgb1 in COMBINE EnvMode
	 */
	public CombineOperand getOperandRgb1() {
		return operandRGB1;
	}

	/**
	 * Set the operand applied to the rgb color of the second source.
	 * 
	 * @param operandRgb1 CombineOperand applied to Rgb1
	 */
	public void setOperandRgb1(CombineOperand operandRgb1) {
		if (operandRgb1 == null)
			operandRgb1 = DEFAULT_OP_RGB1;
		operandRGB1 = operandRgb1;
	}

	/**
	 * Get the operand applied to the rgb color of the third source.
	 * 
	 * @return CombineOperand used on Rgb2 in COMBINE EnvMode
	 */
	public CombineOperand getOperandRgb2() {
		return operandRGB2;
	}

	/**
	 * Set the operand applied to the rgb color of the third source.
	 * 
	 * @param operandRgb2 CombineOperand applied to Rgb2
	 */
	public void setOperandRgb2(CombineOperand operandRgb2) {
		if (operandRgb2 == null)
			operandRgb2 = DEFAULT_OP_RGB2;
		operandRGB2 = operandRgb2;
	}

	/**
	 * Get the operand applied to the alpha value of the first source.
	 * 
	 * @return CombineOperand used on Alpha0 in COMBINE EnvMode
	 */
	public CombineOperand getOperandAlpha0() {
		return operandAlpha0;
	}

	/**
	 * Set the operand applied to the alpha value of the first source.
	 * 
	 * @param operandAlpha0 CombineOperand applied to Alpha0
	 */
	public void setOperandAlpha0(CombineOperand operandAlpha0) {
		if (operandAlpha0 == null)
			operandAlpha0 = DEFAULT_OP_ALPHA0;

		if (operandAlpha0 == CombineOperand.ONE_MINUS_COLOR)
			operandAlpha0 = CombineOperand.ONE_MINUS_ALPHA;
		else if (operandAlpha0 == CombineOperand.COLOR)
			operandAlpha0 = CombineOperand.ALPHA;

		this.operandAlpha0 = operandAlpha0;
	}

	/**
	 * Get the operand applied to the alpha value of the second source.
	 * 
	 * @return CombineOperand used on Alpha1 in COMBINE EnvMode
	 */
	public CombineOperand getOperandAlpha1() {
		return operandAlpha1;
	}

	/**
	 * Set the operand applied to the alpha value of the second source.
	 * 
	 * @param operandAlpha1 CombineOperand applied to Alpha1
	 */
	public void setOperandAlpha1(CombineOperand operandAlpha1) {
		if (operandAlpha1 == null)
			operandAlpha1 = DEFAULT_OP_ALPHA1;

		if (operandAlpha1 == CombineOperand.ONE_MINUS_COLOR)
			operandAlpha1 = CombineOperand.ONE_MINUS_ALPHA;
		else if (operandAlpha1 == CombineOperand.COLOR)
			operandAlpha1 = CombineOperand.ALPHA;

		this.operandAlpha1 = operandAlpha1;
	}

	/**
	 * Get the operand applied to the alpha value of the third source.
	 * 
	 * @return CombineOperand used on Alpha2 in COMBINE EnvMode
	 */
	public CombineOperand getOperandAlpha2() {
		return operandAlpha2;
	}

	/**
	 * Set the operand applied to the alpha value of the third source.
	 * 
	 * @param operandAlpha2 CombineOperand applied to Alpha2
	 */
	public void setOperandAlpha2(CombineOperand operandAlpha2) {
		if (operandAlpha2 == null)
			operandAlpha2 = DEFAULT_OP_ALPHA2;

		if (operandAlpha2 == CombineOperand.ONE_MINUS_COLOR)
			operandAlpha2 = CombineOperand.ONE_MINUS_ALPHA;
		else if (operandAlpha2 == CombineOperand.COLOR)
			operandAlpha2 = CombineOperand.ALPHA;

		this.operandAlpha2 = operandAlpha2;
	}

	/**
	 * Convenience method to set alpha operands for the three combine variables.
	 * 
	 * @param a0 CombineOperand for setOperandAlpha0()
	 * @param a1 CombineOperand for setOperandAlpha1()
	 * @param a2 CombineOperand for setOperandAlpha2()
	 */
	public void setOperandAlpha(CombineOperand a0, CombineOperand a1,
			CombineOperand a2) {
		setOperandAlpha0(a0);
		setOperandAlpha1(a1);
		setOperandAlpha2(a2);
	}

	/**
	 * Convenience method to set the rgb color operands for the three combine
	 * variables.
	 * 
	 * @param r0 CombineOperand for setOperandRgb0()
	 * @param r1 CombineOperand for setOperandRgb1()
	 * @param r2 CombineOperand for setOperandRgb2()
	 */
	public void setOperandRgb(CombineOperand r0, CombineOperand r1,
			CombineOperand r2) {
		setOperandRgb0(r0);
		setOperandRgb1(r1);
		setOperandRgb2(r2);
	}

	/**
	 * Convenience method to set the alpha source for the three combine
	 * variables.
	 * 
	 * @param a0 CombineSource for setSourceAlpha0()
	 * @param a1 CombineSource for setSourceAlpha1()
	 * @param a2 CombineSource for setSourceAlpha2()
	 */
	public void setSourceAlpha(CombineSource a0, CombineSource a1,
			CombineSource a2) {
		setSourceAlpha0(a0);
		setSourceAlpha1(a1);
		setSourceAlpha2(a2);
	}

	/**
	 * Convenience method to set the rgb color source for the three combine
	 * variables.
	 * 
	 * @param r0 CombineSource for setSourceRgb0()
	 * @param r1 CombineSource for setSourceRgb1()
	 * @param r2 CombineSource for setSourceRgb2()
	 */
	public void setSourceRgb(CombineSource r0, CombineSource r1,
			CombineSource r2) {
		setSourceRgb0(r0);
		setSourceRgb1(r1);
		setSourceRgb2(r2);
	}

	/**
	 * Get the texture environment mode. Determines how the texture is blended
	 * with other intermediate fragment colors.
	 * 
	 * @return The EnvMode used to apply texture images when rendering
	 */
	public EnvMode getTextureEnvMode() {
		return texEnvMode;
	}

	/**
	 * Set the texture environment mode.
	 * 
	 * @param texEnvMode EnvMode for applying textures, null uses MODULATE
	 */
	public void setTextureEnvMode(EnvMode texEnvMode) {
		if (texEnvMode == null)
			texEnvMode = DEFAULT_ENV_MODE;
		this.texEnvMode = texEnvMode;
	}

	/**
	 * Get the color to use for blending when the environment mode is BLEND, or
	 * when using BLEND_COLOR for a CombineSource.
	 * 
	 * @return Color instance used when BLEND is the EnvMode
	 */
	public Color getTextureEnvColor() {
		return texEnvColor;
	}

	/**
	 * Set the color to use for blending when env mode is BLEND, or using a
	 * BLEND_COLOR CombineSource.
	 * 
	 * @param texEnvColor Color for use with BLEND mode
	 */
	public void setTextureEnvColor(Color texEnvColor) {
		if (texEnvColor == null)
			texEnvColor = new Color();
		this.texEnvColor = texEnvColor;
	}

	/**
	 * Get the texture coordinate generation policy for the r tex coord (third).
	 * 
	 * @return TexCoordGen for the r coordinate
	 */
	public TexCoordGen getTexCoordGenR() {
		return rCoordGen;
	}

	/**
	 * Set the texture coordinate generation policy for the r tex coord (third).
	 * If null uses the default. A value of NONE causes texture coordinates to
	 * come from the rendered geometry. SPHERE mapping isn't supported on the
	 * 3rd coordinate, so if SPHERE is given, NONE is used instead.
	 * 
	 * @param coordGen TexCoordGen to use
	 */
	public void setTexCoordGenR(TexCoordGen coordGen) {
		if (coordGen == null || coordGen == TexCoordGen.SPHERE)
			coordGen = DEFAULT_COORD_GEN;
		rCoordGen = coordGen;
	}

	/**
	 * Get the texture coordinate generation policy for the s tex coord (first).
	 * 
	 * @return TexCoordGen for the s coordinate
	 */
	public TexCoordGen getTexCoordGenS() {
		return sCoordGen;
	}

	/**
	 * Set the texture coordinate generation policy for the s tex coord (first).
	 * If null uses the default. A value of NONE causes texture coordinates to
	 * come from the rendered geometry.
	 * 
	 * @param coordGen TexCoordGen to use
	 */
	public void setTexCoordGenS(TexCoordGen coordGen) {
		if (coordGen == null)
			coordGen = DEFAULT_COORD_GEN;
		sCoordGen = coordGen;
	}

	/**
	 * Get the texture coordinate generation policy for the t tex coord
	 * (second).
	 * 
	 * @return TexCoordGen for the t coordinate
	 */
	public TexCoordGen getTexCoordGenT() {
		return tCoordGen;
	}

	/**
	 * Set the texture coordinate generation policy for the t tex coord
	 * (second). If null uses the default. A value of NONE causes texture
	 * coordinates to come from the rendered geometry.
	 * 
	 * @param coordGen TexCoordGen to use
	 */
	public void setTexCoordGenT(TexCoordGen coordGen) {
		if (coordGen == null)
			coordGen = DEFAULT_COORD_GEN;
		tCoordGen = coordGen;
	}

	/**
	 * Convenience method to set all texture coordinate generations to the given
	 * type.
	 * 
	 * @param coordGen TexCoordGen for s, t, and r coordinates
	 */
	public void setTexCoordGenSTR(TexCoordGen coordGen) {
		this.setTexCoordGenSTR(coordGen, coordGen, coordGen);
	}

	/**
	 * Convenience method to set the three coordinate generation policies for
	 * each coordinate.
	 * 
	 * @param s TexCoordGen for the s coordinate
	 * @param t TexCoordGen for the t coordinate
	 * @param r TexCoordGen for the r coordinate
	 */
	public void setTexCoordGenSTR(TexCoordGen s, TexCoordGen t, TexCoordGen r) {
		setTexCoordGenS(s);
		setTexCoordGenT(t);
		setTexCoordGenR(r);
	}

	@Override
	public String toString() {
		return "(" + super.toString() + " envMode: " + texEnvMode + ")";
	}
}
