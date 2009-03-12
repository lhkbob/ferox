package com.ferox.state;

import com.ferox.math.Color;
import com.ferox.math.Plane;
import com.ferox.math.Transform;
import com.ferox.resource.TextureImage;

/**
 * A Texture wraps a TextureImage a provides information on how
 * it will be blended together with other textures and material colors
 * of the rendered object.  It also controls texture coordinate generation 
 * (if desired), and setting the a transform that is applied to each 
 * texture coordinate before accessing the texture.
 * 
 * If attached directly to an Appearance, only one texture will be active.
 * To allow for multiple textures, use a MultiTexture.  There can only be one
 * of MultiTexture or Texture attached to a given appearance.  When a texture is 
 * used, it is equivalent to using a MultiTexture with the texture set to unit 0.
 * 
 * Texture's also allow for advanced combinations when the EnvMode COMBINE is used.
 * Summary of EnvMode operations:
 * Cv = final rgb color Av = final alpha
 * Cs = rgb of this texture As = alpha of this tex
 * Cp = rgb of prev. color  Ap = alpha of prev. color
 * 
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
 * 
 * Operations on colors are done component wise.  Cs for Luminance is (L, L, L), As for Lum. is L
 * Cs for LA is (L, L, L), As for LA is A
 * 
 * Summary of Combine operation:
 * 
 * In the table below, opX is a function applied to either a color(per component) or an alpha value
 * as defined in CombineOperand.  srcX is the input and is designated with CombineSource
 * In the DOT3_x varieties, rN, gN, and bN represent the red, green, and blue values of opN(srcN)
 * 
 * CombineRgb 	|	Rgb Result
 * REPLACE		|	op0(src0)
 * MODULATE		|	op0(src0) * op1(src1)
 * ADD			|	op0(src0) + op1(src1)
 * ADD_SIGNED	|	op0(src0) + op1(src1) - .5
 * INTERPOLATE	| 	op0(src0) * op2(src2) + op1(src1) * (1 - op2(src2))
 * SUBTRACT		| 	op0(src0) - op1(src1)
 * DOT3_RGB		| 	4 * ((r0-.5)*(r1-.5) + (g0-.5)*(g1-.5) + (b0-.5)*(b1-.5))
 * DOT3_RGBA	|	4 * ((r0-.5)*(r1-.5) + (g0-.5)*(g1-.5) + (b0-.5)*(b1-.5))
 * 
 * CombineAlpha is computed in exactly the same way, except that there are no DOT3_x options.
 * In CombineRgb, DOT3_RGBA ignores the result of CombineAlpha.
 * The combine operations, SRC_COLOR and ONE_MINUS_SRC_COLOR are meaningless for CombineAlpha,
 * so they are mapped to SRC_ALPHA and ONE_MINUS_SRC_ALPHA when specified.
 * 
 * Defaults:
 * 
 * COMBINE_RGB = CombineRgb.MODULATE;
 * COMBINE_ALPHA = CombineAlpha.MODULATE;
 * 
 * SOURCE_RGB0 = CombineSource.PREV_TEX;
 * SOURCE_RGB1 = CombineSource.CURR_TEX;
 * SOURCE_RGB2 = CombineSource.VERTEX_COLOR;
 * SOURCE_ALPHA0 = CombineSource.PREV_TEX;
 * SOURCE_ALPHA1 = CombineSource.CURR_TEX;
 * SOURCE_ALPHA2 = CombineSource.VERTEX_COLOR;
 * 
 * OP_RGB0 = CombineOperand.COLOR;
 * OP_RGB1 = CombineOperand.COLOR;
 * OP_RGB2 = CombineOperand.ALPHA;
 * OP_ALPHA0 = CombineOperand.ALPHA;
 * OP_ALPHA1 = CombineOperand.ALPHA;
 * OP_ALPHA2 = CombineOperand.ALPHA;
 * 
 * ENV_MODE = EnvMode.MODULATE;
 * COORD_GEN = TexCoordGen.NONE;
 * 
 * @author Michael Ludwig
 *
 */
public class Texture implements State {
	/** Describes how texels are combined with other textures and colors. */
	public static enum EnvMode {
		REPLACE, DECAL, MODULATE, BLEND, COMBINE
	}
	
	/** How a given texture coordinate is found.  NONE uses none, or the
	 * actual texture coordinates of the rendered object.
	 * OBJECT uses the texture coord generation planes in object space to generate coords.
	 * EYE uses those planes similarly to object space, but after everything has moved to eye space.
	 * SPHERE sets up coordinates suitable to access a sphere mapped texture image.
	 * REFLECTION generates reflection vector coordinate at a given location (vertex normal + viewer location, etc)
	 * NORMAL generates vector coordinates based off of the surface normal.
	 * 
	 * OBJECT, EYE can work with any type of texture, and any of STR coordinates.
	 * SPHERE is intended for ST coordinates with a 2D texture.
	 * REFLECTION and NORMAL work best with STR coordinates for a cube map. */
	public static enum TexCoordGen {
		NONE, OBJECT, EYE, SPHERE, REFLECTION, NORMAL
	}
	
	/** The combine operation applied to the texture's alpha sources (after being
	 * affected by the operands). */
	public static enum CombineAlpha {
		REPLACE, ADD, MODULATE, INTERPOLATE, ADD_SIGNED, SUBTRACT
	}
	
	/** As CombineAlpha, but for RGB values.  Adds DOT3_RGB and DOT3_RGBA that can be used
	 * to simulate bump-mapping.  DOT3_RGBA ignores the alpha value generated by CombineAlpha. */
	public static enum CombineRgb {
		REPLACE, ADD, MODULATE, INTERPOLATE, ADD_SIGNED, SUBTRACT, DOT3_RGB, DOT3_RGBA
	}
	
	/** The source for a given component (0, 1, or 2) for a given combine operation. 
	 * CURR_TEX = color of this texture image
	 * PREV_TEX = color of the texture in the texture unit processed just before this one (if this is the 
	 * 		first unit, then it's the same as VERTEX_COLOR). 
	 * BLEND_COLOR = environment color of this texture
	 * VERTEX_COLOR = color computed based on material color and lighting
	 * TEXi = color of the texture image bound to the given unit (note not all TEXi will be supported because
	 * 		hardware may not have that many texture units available.  Units beyond 8 are included for advanced
	 * 		graphics cards (or future cards)). */
	public static enum CombineSource {
		CURR_TEX, PREV_TEX, BLEND_COLOR, VERTEX_COLOR,
		TEX0, TEX1, TEX2, TEX3, TEX4, TEX5, TEX6, TEX7, TEX8, TEX9, TEX10, TEX11, TEX12,
		TEX13, TEX14, TEX15, TEX16, TEX17, TEX18, TEX19, TEX20, TEX21, TEX22, TEX23, TEX24, TEX25,
		TEX26, TEX27, TEX28, TEX29, TEX30, TEX31
	}
	
	/** How to access a given source component (0, 1, or 2). 
	 * COLOR and ONE_MINUS_COLOR are meaningless when used for alpha operands,
	 * so they are switched to ALPHA and ONE_MINUS_ALPHA when specified. */
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
	
	private Object renderData;
	
	/** Create a texture with default parameters and the given texture image. */
	public Texture(TextureImage data) {
		this();
		this.setTexture(data);
	}
	
	/** Create a texture with default parameters, and the given image and environment mode. */
	public Texture(TextureImage data, EnvMode mode) {
		this();
		this.setTexture(data);
		this.setTextureEnvMode(mode);
	}
	
	/** Create a texture with default plane and combine modes, and the given image, environment mode, environment color and texture coord. generation. */
	public Texture(TextureImage data, EnvMode mode, Color envColor, TexCoordGen texCoord) {
		this();
		this.setTexture(data);
		this.setTextureEnvMode(mode);
		this.setTextureEnvColor(envColor);
		this.setTexCoordGenSTR(texCoord);
	}
	
	/** Create a blank texture, with default values, no texture image and no texture transform. */
	public Texture() {
		this.setTexCoordGenSTR(null);
		this.setTextureEnvColor(null);
		this.setTextureEnvMode(null);
		
		this.setCombineAlphaEquation(null);
		this.setCombineRgbEquation(null);
		this.setSourceAlpha(null, null, null);
		this.setSourceRgb(null, null, null);
		
		this.setOperandAlpha(null, null, null);
		this.setOperandRgb(null, null, null);
		
		this.setTexCoordGenPlaneR(null);
		this.setTexCoordGenPlaneS(null);
		this.setTexCoordGenPlaneT(null);
		
		this.data = null;
		this.texTrans = null;
	}
	
	/** Get the plane that is used to compute the R texture coordinate
	 * when R texture coord generation is OBJECT or EYE. */
	public Plane getTexCoordGenPlaneR() {
		return this.planeR;
	}
	
	/** Get the plane that is used to compute the S texture coordinate
	 * when S texture coord generation is OBJECT or EYE. */
	public Plane getTexCoordGenPlaneS() {
		return this.planeS;
	}
	
	/** Get the plane that is used to compute the T texture coordinate
	 * when T texture coord generation is OBJECT or EYE. */
	public Plane getTexCoordGenPlaneT() {
		return this.planeT;
	}
	
	/** Set the plane that is used to compute the R texture coordinate
	 * when R texture coord generation is OBJECT or EYE. 
	 * If null, sets the plane to be (0, 0, 1, 0). */
	public void setTexCoordGenPlaneR(Plane plane) {
		if (plane == null)
			plane = new Plane(0f, 0f, 1f, 0f);
		this.planeR = plane;
	}
	
	/** Set the plane that is used to compute the S texture coordinate
	 * when S texture coord generation is OBJECT or EYE. 
	 * If null, sets the plane to be (1, 0, 0, 0). */
	public void setTexCoordGenPlaneS(Plane plane) {
		if (plane == null)
			plane = new Plane(1f, 0f, 0f, 0f);
		this.planeS = plane;
	}
	
	/** Set the plane that is used to compute the T texture coordinate
	 * when T texture coord generation is OBJECT or EYE. 
	 * If null, sets the plane to be (0, 1, 0, 0). */
	public void setTexCoordGenPlaneT(Plane plane) {
		if (plane == null)
			plane = new Plane(0f, 1f, 0f, 0f);
		this.planeT = plane;
	}
	
	/** Set the texture transform that is applied to each
	 * texture coordinate before accessing the texture.
	 * This value may be null (if it is, Renderer's should treat
	 * it as an identity transform). */
	public void setTextureTransform(Transform trans) {
		this.texTrans = trans;
	}
	
	/** Get the texture coordinate transform.  May be null. */
	public Transform getTextureTransform() {
		return this.texTrans;
	}
	
	/** Get the texture image data used by this texture.  If null,
	 * then this texture has no effect on the rendering. */
	public TextureImage getTexture() {
		return this.data;
	}

	/** Set the texture image data used for this texture. */
	public void setTexture(TextureImage data) {
		this.data = data;
	}

	/** Get the equation used to combine rgb values when the texture
	 * environment mode is COMBINE. */
	public CombineRgb getCombineRgbEquation() {
		return this.combineRGBFunc;
	}

	/** Set the equation used to combine rgb values when the texture
	 * environment mode is COMBINE. If null, uses the default. */
	public void setCombineRgbEquation(CombineRgb combineRgb) {
		if (combineRgb == null)
			combineRgb = DEFAULT_COMBINE_RGB;
		this.combineRGBFunc = combineRgb;
	}

	/** Get the equation used to combine alpha values when the texture
	 * environment mode is COMBINE. */
	public CombineAlpha getCombineAlphaEquation() {
		return this.combineAlphaFunc;
	}

	/** Set the equation used to combine alpha values when the texture
	 * environment mode is COMBINE. If null, uses the default. */
	public void setCombineAlphaEquation(CombineAlpha combineAlphaFunc) {
		if (combineAlphaFunc == null)
			combineAlphaFunc = DEFAULT_COMBINE_ALPHA;
		this.combineAlphaFunc = combineAlphaFunc;
	}
	
	/** Get the source of the first input for rgb combining. */
	public CombineSource getSourceRgb0() {
		return this.sourceRGB0;
	}

	/** Set the source of the first input for rgb combining.
	 * If null, uses the default. */
	public void setSourceRgb0(CombineSource sourceRgb0) {
		if (sourceRgb0 == null)
			sourceRgb0 = DEFAULT_SOURCE_RGB0;
		this.sourceRGB0 = sourceRgb0;
	}

	/** Get the source of the second input for rgb combining. */
	public CombineSource getSourceRgb1() {
		return this.sourceRGB1;
	}

	/** Set the source of the second input for rgb combining.
	 * If null, uses the default. */
	public void setSourceRgb1(CombineSource sourceRgb1) {
		if (sourceRgb1 == null)
			sourceRgb1 = DEFAULT_SOURCE_RGB1;
		this.sourceRGB1 = sourceRgb1;
	}

	/** Get the source of the third input for rgb combining. */
	public CombineSource getSourceRgb2() {
		return this.sourceRGB2;
	}

	/** Set the source of the third input for rgb combining.
	 * If null, uses the default. */
	public void setSourceRgb2(CombineSource sourceRgb2) {
		if (sourceRgb2 == null)
			sourceRgb2 = DEFAULT_SOURCE_RGB2;
		this.sourceRGB2 = sourceRgb2;
	}

	/** Get the source of the first input for alpha combining. */
	public CombineSource getSourceAlpha0() {
		return this.sourceAlpha0;
	}

	/** Get the source of the first input for alpha combining.
	 * If null, uses the default. */
	public void setSourceAlpha0(CombineSource sourceAlpha0) {
		if (sourceAlpha0 == null)
			sourceAlpha0 = DEFAULT_SOURCE_ALPHA0;
		this.sourceAlpha0 = sourceAlpha0;
	}

	/** Get the source of the second input for alpha combining. */
	public CombineSource getSourceAlpha1() {
		return this.sourceAlpha1;
	}

	/** Get the source of the second input for alpha combining.
	 * If null, uses the default. */
	public void setSourceAlpha1(CombineSource sourceAlpha1) {
		if (sourceAlpha1 == null)
			sourceAlpha1 = DEFAULT_SOURCE_ALPHA1;
		this.sourceAlpha1 = sourceAlpha1;
	}

	/** Get the source of the third input for alpha combining. */
	public CombineSource getSourceAlpha2() {
		return this.sourceAlpha2;
	}

	/** Get the source of the third input for alpha combining.
	 * If null, uses the default. */
	public void setSourceAlpha2(CombineSource sourceAlpha2) {
		if (sourceAlpha2 == null)
			sourceAlpha2 = DEFAULT_SOURCE_ALPHA2;
		this.sourceAlpha2 = sourceAlpha2;
	}
	
	/** Get the operand applied to the rgb color of the first source. */
	public CombineOperand getOperandRgb0() {
		return this.operandRGB0;
	}

	/** Set the operand applied to the rgb color of the first source.
	 * If null, uses the default. */
	public void setOperandRgb0(CombineOperand operandRgb0) {
		if (operandRgb0 == null)
			operandRgb0 = DEFAULT_OP_RGB0;
		this.operandRGB0 = operandRgb0;
	}

	/** Get the operand applied to the rgb color of the second source. */
	public CombineOperand getOperandRgb1() {
		return this.operandRGB1;
	}

	/** Set the operand applied to the rgb color of the second source.
	 * If null, uses the default. */
	public void setOperandRgb1(CombineOperand operandRgb1) {
		if (operandRgb1 == null)
			operandRgb1 = DEFAULT_OP_RGB1;
		this.operandRGB1 = operandRgb1;
	}

	/** Get the operand applied to the rgb color of the third source. */
	public CombineOperand getOperandRgb2() {
		return this.operandRGB2;
	}

	/** Set the operand applied to the rgb color of the third source.
	 * If null, uses the default. */
	public void setOperandRgb2(CombineOperand operandRgb2) {
		if (operandRgb2 == null)
			operandRgb2 = DEFAULT_OP_RGB2;
		this.operandRGB2 = operandRgb2;
	}

	/** Get the operand applied to the alpha value of the first source. */
	public CombineOperand getOperandAlpha0() {
		return this.operandAlpha0;
	}

	/** Set the operand applied to the alpha value of the first source.
	 * If null, uses the default. */
	public void setOperandAlpha0(CombineOperand operandAlpha0) {
		if (operandAlpha0 == null)
			operandAlpha0 = DEFAULT_OP_ALPHA0;
		
		if (operandAlpha0 == CombineOperand.ONE_MINUS_COLOR)
			operandAlpha0 = CombineOperand.ONE_MINUS_ALPHA;
		else if (operandAlpha0 == CombineOperand.COLOR)
			operandAlpha0 = CombineOperand.ALPHA;
		
		this.operandAlpha0 = operandAlpha0;
	}

	/** Get the operand applied to the alpha value of the second source. */
	public CombineOperand getOperandAlpha1() {
		return this.operandAlpha1;
	}

	/** Set the operand applied to the alpha value of the second source.
	 * If null, uses the default. */
	public void setOperandAlpha1(CombineOperand operandAlpha1) {
		if (operandAlpha1 == null)
			operandAlpha1 = DEFAULT_OP_ALPHA1;

		if (operandAlpha1 == CombineOperand.ONE_MINUS_COLOR)
			operandAlpha1 = CombineOperand.ONE_MINUS_ALPHA;
		else if (operandAlpha1 == CombineOperand.COLOR)
			operandAlpha1 = CombineOperand.ALPHA;
		
		this.operandAlpha1 = operandAlpha1;
	}

	/** Get the operand applied to the alpha value of the third source. */
	public CombineOperand getOperandAlpha2() {
		return this.operandAlpha2;
	}

	/** Set the operand applied to the alpha value of the third source.
	 * If null, uses the default. */
	public void setOperandAlpha2(CombineOperand operandAlpha2) {
		if (operandAlpha2 == null)
			operandAlpha2 = DEFAULT_OP_ALPHA2;

		if (operandAlpha2 == CombineOperand.ONE_MINUS_COLOR)
			operandAlpha2 = CombineOperand.ONE_MINUS_ALPHA;
		else if (operandAlpha2 == CombineOperand.COLOR)
			operandAlpha2 = CombineOperand.ALPHA;
		
		this.operandAlpha2 = operandAlpha2;
	}
	
	/** Convenience method to set alpha operands for the three combine variables. */
	public void setOperandAlpha(CombineOperand a0, CombineOperand a1, CombineOperand a2) {
		this.setOperandAlpha0(a0);
		this.setOperandAlpha1(a1);
		this.setOperandAlpha2(a2);
	}
	
	/** Convenience method to set the rgb color operands for the three combine variables. */
	public void setOperandRgb(CombineOperand r0, CombineOperand r1, CombineOperand r2) {
		this.setOperandRgb0(r0);
		this.setOperandRgb1(r1);
		this.setOperandRgb2(r2);
	}
	
	/** Convenience method to set the alpha source for the three combine variables. */
	public void setSourceAlpha(CombineSource a0, CombineSource a1, CombineSource a2) {
		this.setSourceAlpha0(a0);
		this.setSourceAlpha1(a1);
		this.setSourceAlpha2(a2);
	}
	
	/** Convenience method to set the rgb color source for the three combine variables. */
	public void setSourceRgb(CombineSource r0, CombineSource r1, CombineSource r2) {
		this.setSourceRgb0(r0);
		this.setSourceRgb1(r1);
		this.setSourceRgb2(r2);
	}

	/** Get the texture environment mode.  Determines how the texture is blended with other
	 * intermediate fragment colors. */
	public EnvMode getTextureEnvMode() {
		return this.texEnvMode;
	}

	/** Set the texture environment mode, if null uses the default. */
	public void setTextureEnvMode(EnvMode texEnvMode) {
		if (texEnvMode == null)
			texEnvMode = DEFAULT_ENV_MODE;
		this.texEnvMode = texEnvMode;
	}

	/** Get the color to use for blending when the environment mode is BLEND. */
	public Color getTextureEnvColor() {
		return this.texEnvColor;
	}

	/** Set the color to use for blending when env mode is BLEND.
	 * If null, creates a new default color (black). */
	public void setTextureEnvColor(Color texEnvColor) {
		if (texEnvColor == null)
			texEnvColor = new Color();
		this.texEnvColor = texEnvColor;
	}
	
	/** Get the texture coordinate generation policy for the r tex coord (third). */
	public TexCoordGen getTexCoordGenR() {
		return this.rCoordGen;
	}

	/** Set the texture coordinate generation policy for the r tex coord (third).
	 * If null uses the default.  A value of NONE causes texture coordinates to come
	 * from the rendered geometry. 
	 * 
	 * SPHERE mapping isn't supported on the 3rd coordinate, so if SPHERE is given,
	 * NONE is used instead. */
	public void setTexCoordGenR(TexCoordGen coordGen) {
		if (coordGen == null || coordGen == TexCoordGen.SPHERE)
			coordGen = DEFAULT_COORD_GEN;
		this.rCoordGen = coordGen;
	}

	/** Get the texture coordinate generation policy for the s tex coord (first). */
	public TexCoordGen getTexCoordGenS() {
		return this.sCoordGen;
	}

	/** Set the texture coordinate generation policy for the s tex coord (first).
	 * If null uses the default.  A value of NONE causes texture coordinates to come
	 * from the rendered geometry. */
	public void setTexCoordGenS(TexCoordGen coordGen) {
		if (coordGen == null)
			coordGen = DEFAULT_COORD_GEN;
		this.sCoordGen = coordGen;
	}

	/** Get the texture coordinate generation policy for the t tex coord (second). */
	public TexCoordGen getTexCoordGenT() {
		return this.tCoordGen;
	}

	/** Set the texture coordinate generation policy for the t tex coord (second).
	 * If null uses the default.  A value of NONE causes texture coordinates to come
	 * from the rendered geometry. */
	public void setTexCoordGenT(TexCoordGen coordGen) {
		if (coordGen == null)
			coordGen = DEFAULT_COORD_GEN;
		this.tCoordGen = coordGen;
	}
	
	/** Convenience method to set all texture coordinate generations to the given type. */
	public void setTexCoordGenSTR(TexCoordGen coordGen) {
		this.setTexCoordGenSTR(coordGen, coordGen, coordGen);
	}
	
	/** Convenience method to set the three coordinate generation policies for each
	 * coordinate. */
	public void setTexCoordGenSTR(TexCoordGen s, TexCoordGen t, TexCoordGen r) {
		this.setTexCoordGenS(s);
		this.setTexCoordGenT(t);
		this.setTexCoordGenR(r);
	}

	@Override
	public Role getRole() {
		return Role.TEXTURE;
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
