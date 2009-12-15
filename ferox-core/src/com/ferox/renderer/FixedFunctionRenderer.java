package com.ferox.renderer;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.resource.Geometry;
import com.ferox.resource.TextureImage;

/**
 * <p>
 * The FixedFunctionRenderer describes a Renderer that exposes the majority of
 * useful state described by the fixed-function pipeline of pre-3.1 OpenGL
 * implementations. Although the OpenGL versions between 2.0 and 3.0 had support
 * for GLSL shaders (and even earlier using extensions), this Renderer does not
 * expose that since it's more robust to completely separate fixed and
 * programmable pipelines.
 * </p>
 * <p>
 * There are a number of semi-distinct state groupings that are exposed by the
 * FixedFunctionRenderer:
 * <ul>
 * <li>Alpha testing: another pixel test that can discard pixels based on alpha
 * value</li>
 * <li>Fog: an approximation of fog based on eye-space pixel depth</li>
 * <li>Lighting: collection of states for a number of lights, global lighting
 * parameters, and the material color of rendered Geometry</li>
 * <li>Texturing: collection of states for a number of texture units that allow
 * TextureImages to be attached</li>
 * <li>Drawing parameters: anti-aliasing of points, lines, and solid polygons,
 * and point and line pixel widths</li>
 * <li>Transforms: contains the projection, modelview, and texture coordinate
 * matrices</li>
 * </ul>
 * </p>
 * <p>
 * In addition to this, there are a three attribute binding points. They are
 * specified in a manner similar to attribute bindings in a GLSL shader, except
 * that there can only be vertices, normals and texture coordinates. The
 * vertices represent the 3D points in object space of a Geometry. The normals
 * represent the object-space normal vectors for each vertex. The texture
 * coordinates are 1-4 dimensional vectors that lookup texel values from
 * TextureImages (for 2D images, they are a mapping from the 3D shape to the 2D
 * image plane).
 * </p>
 * <p>
 * With texturing and lighting, there are a number of units available that have
 * the same configuration values, yet are independent. This allows for combining
 * multiple textures, or having shapes lit from multiple lights. There are
 * hardware limits to the number of textures and lights, which can be fetched
 * from a Framework's RenderCapabilities. For all exposed texturing state that
 * takes a texture unit, valid units are in the range [0,
 * {@link RenderCapabilities#getMaxFixedPipelineTextures()}]. For all exposed
 * lighting state that takes a light number, valid units are in the range [0,
 * {@link RenderCapabilities#getMaxActiveLights()}]. All state modifications
 * that use an invalid unit will be ignored. All other state modifications that
 * use invalid values will generally throw exceptions because the valid values
 * are known at compile time.
 * </p>
 * <p>
 * Like the state exposed by the Renderer super-type, there are default values
 * that the Renderer will use at the start of a RenderPass and each time
 * {@link #reset()} is called manually. The defaults are as follows:
 * <ul>
 * <li>The vertex binding name is set to {@link Geometry#DEFAULT_VERTICES_NAME}</li>
 * <li>The normal binding name is set to {@link Geometry#DEFAULT_NORMALS_NAME}</li>
 * <li>The 0th texture coordinate is set to
 * {@link Geometry#DEFAULT_TEXCOORD_NAME}, all other texture coordinates have a
 * null name</li>
 * <li>The alpha test uses the Comparison ALWAYS with a reference value of 1</li>
 * <li>Fogging is disabled, but its color is set to black with an exponential
 * fog of density 1</li>
 * <li>All point, line and polygon anti-aliasing is disabled</li>
 * <li>Point and line widths are set to 1</li>
 * <li>The global ambient color is (.2, .2, .2, 1)</li>
 * <li>Smooth shading is enabled, and two-sided lighting is disabled</li>
 * <li>Each light has a black ambient color. The 0th light has white diffuse and
 * specular lighting, while all others have black diffuse and specular colors</li>
 * <li>Each light has a position vector of (0, 0, 1, 0) - it's directional</li>
 * <li>Each light has a spotlight direction of (0, 0, -1) and attenuation of
 * (c,l,q) = (1,0,0)</li>
 * <li>Lighting in general, and every light is initially disabled</li>
 * <li>The material is set to have an ambient color of (.2, .2, .2, 1), diffuse
 * color of (.8, .8, .8, 1) and the specular and emmissive colors are black</i>
 * <li>The material shininess is set to 0</li>
 * <li>The modelview and projection matrix are set to the identity matrices</li>
 * <li>All TextureImages are unbound, all texture units are disabled</li>
 * <li>Every texture unit uses {@link EnvMode#MODULATE}</li>
 * <li>Every texture unit uses a texture color of (0, 0, 0, 0)</li>
 * <li>All texture units use {@link TexCoordSource#ATTRIBUTE} for each of the
 * available coordinates</li>
 * <li>For each texture unit, the object and eye planes are set to S=(1, 0, 0,
 * 0), T=(0, 1, 0, 0), R=Q=(0,0,0,0)</li>
 * <li>Each texture transform is set to the identity matrix</li>
 * <li>For each texture unit, the rgb and alpha CombineFunctions are MODULATE</li>
 * <li>All 0th sources are CURR_TEX, all 1st sources are PREV_TEX, and all 2nd
 * sources are CONST_COLOR</li>
 * <li>All alpha operands (0, 1, 2) are ALPHA, the 0th and 1st rgb operands are
 * COLOR and the 2nd rgb operand is ALPHA</li>
 * </ul>
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface FixedFunctionRenderer extends Renderer {
	/**
	 * The CombineFunction enum describes the advanced texture combining
	 * functionality that's enabled when using {@link EnvMode#COMBINE}. The
	 * final color for a combined texture is computed by evaluating one
	 * CombineFunction for the RGB values and one CombineFunction for the alpha
	 * values. </p>
	 * <p>
	 * Each CombineFunction uses up to three inputs to compute the final
	 * component values. We define the inputs as follows:
	 * <ul>
	 * <li>N = current input, one of {0, 1, }</li>
	 * <li>opN = configured operand for Nth input</li>
	 * <li>srcN = configured source for Nth input</li>
	 * </ul>
	 * then argN = opN(srcN). For the RGB function, this is actually the tuple
	 * (argNr, argNg, argNb). Also, keep in mind that component values are in
	 * the range [0, 1] and are clamped to this range after being computed.
	 */
	public static enum CombineFunction {
		/**
		 * The REPLACE function is defined as:
		 * <code>f(arg0,arg1,arg2) = arg0</code>
		 */
		REPLACE,
		/**
		 * The MODULATE function is defined as:
		 * <code>f(arg0,arg1,arg2) = arg0*arg1</code>
		 */
		MODULATE,
		/**
		 * The ADD function is defined as:
		 * <code>f(arg0,arg1,arg2) = arg0 + arg1</code>
		 */
		ADD,
		/**
		 * The ADD_SIGNED function is defined as:
		 * <code>f(arg0,arg1,arg2) = arg0 + arg1 - 0.5</code>
		 */
		ADD_SIGNED,
		/**
		 * The INTERPOLATE function is defined as:
		 * <code>f(arg0,arg1,arg2) = arg2*arg0 + (1-arg2)*arg1</code>
		 */
		INTERPOLATE,
		/**
		 * The SUBTRACT function is defined as:
		 * <code>f(arg0,arg1,arg2) = arg0 - arg1</code>
		 */
		SUBTRACT,
		/**
		 * <p>
		 * The DOT3_RGB is a special CombineFunction that's useful when
		 * performing bump-mapping. Instead of working component wise like the
		 * other functions, DOT3_RGB computes a single value using the red,
		 * green, and blue values and then stores that value in each of the RGB
		 * components. It is computed as follows:
		 * 
		 * <pre>
		 * 4 x ((r0 - 0.5) * (r1 - 0.5) +
		 *      (g0 - 0.5) * (g1 - 0.5) +
		 *      (b0 - 0.5) * (b1 - 0.5))
		 * </pre>
		 * 
		 * where arg0 = (r0, g0, b0) and arg1 = (r1, g1, b1).
		 * </p>
		 * <p>
		 * The DOT3_RGB function cannot be used with the alpha CombineFunction.
		 * The alpha value for the final texture is computed as normal using the
		 * configured function for that component.
		 * </p>
		 */
		DOT3_RGB,
		/**
		 * DOT3_RGBA is identical to DOT3_RGB except that the computed
		 * dot-product value is stored in the alpha component is well. This
		 * value overwrites any computed alpha value from the configured alpha
		 * CombineFunction (the alpha function is ignored). Like DOT3_RGB, this
		 * CombineFunction cannot be used for the alpha function.
		 */
		DOT3_RGBA
	}
	
	/**
	 * <p>
	 * Each CombineOp represents a simple function that acts on component values
	 * of some color source, specified by {@link CombineSource}.
	 * {@link CombineFunction} evaluates the configured CombineOps on the
	 * correct CombineSources before combining the computed values into a final
	 * color.
	 * </p>
	 * <p>
	 * The COMBINE EnvMode uses two CombineFunctions, one for alpha values and
	 * one for the RGB values. When specifying the CombineOps for the alpha
	 * function, it is invalid to use the operands that rely on RGB data.
	 * </p>
	 * <p>
	 * When describing the operands below, Cs refers to the RGB components and
	 * As refers to the alpha component. Operations are done component-wise.
	 * </p>
	 */
	public static enum CombineOp {
		/**
		 * This operand returns Cs unmodified. It's invalid to use for the alpha
		 * CombineFunction.
		 */
		COLOR,
		/**
		 * This operand returns As unmodified. It can be used with both the
		 * alpha and RGB CombineFunctions.
		 */
		ALPHA, 
		/**
		 * This operand returns (1 - Cs). It cannot be used with the alpha
		 * CombineFunction.
		 */
		ONE_MINUS_COLOR, 
		/**
		 * This operand returns (1 - As). It can be used with both the alpha and
		 * RGB CombineFunctions.
		 */
		ONE_MINUS_ALPHA
	}
	
	/**
	 * <p>
	 * The source of color and alpha values for an associated
	 * {@link CombineOp}. Up to three CombineSources and CombineOperands
	 * can be used by {@link CombineFunction} to provide more complex texture
	 * combination algorithms.
	 * </p>
	 * CURR_TEX = color of this texture image <br>
	 * PREV_TEX = color of the texture in the texture unit processed just before
	 * this one (if this is the first unit, then it's the same as VERTEX_COLOR). <br>
	 * CONST_COLOR = environment color of this texture <br>
	 * VERTEX_COLOR = color computed based on material color and lighting <br>
	 * TEXi = color of the texture image bound to the given unit
	 * <p>
	 * </p>
	 * <b>Note:</b> not all TEXi will be supported because hardware may not have
	 * that many texture units available. Units beyond 8 are included for
	 * advanced graphics cards (or future cards). </p>
	 */
	public static enum CombineSource {
		CURR_TEX, PREV_TEX, CONST_COLOR, VERTEX_COLOR, 
		TEX0, TEX1, TEX2, TEX3, TEX4, TEX5, TEX6, TEX7, TEX8, 
		TEX9, TEX10, TEX11, TEX12, TEX13, TEX14, TEX15, TEX16, 
		TEX17, TEX18, TEX19, TEX20, TEX21, TEX22, TEX23, TEX24, 
		TEX25, TEX26, TEX27, TEX28, TEX29, TEX30, TEX31
	}
	
	/**
	 * <p>
	 * Each EnvMode describes a different way in which TextureImages are
	 * combined with each other and with the original color of a shape (either
	 * lit or solid). For each enum value, the exact equation varies slightly
	 * based on the TextureFormat of the texture.
	 * </p>
	 * <p>
	 * For brevity, the formats can be easily grouped into RGB, RGBA, and Alpha.
	 * Luminance formats are either RGB or RGBA, where the RGB values hold the
	 * same luminance value (RGBA is used when the luminance format includes
	 * alpha data). In the descriptions below, the combine functions will refer
	 * to the following variables:
	 * <ul>
	 * <li>Cv, Av = Final RGB and alpha values</li>
	 * <li>Cs, As = RGB and alpha values for current texture</li>
	 * <li>Cp, Ap = RGB and alpha values for the previous color</li>
	 * <li>Cc, Ac = RGB and alpha values for the configured 'constant' color of
	 * the texture unit</li>
	 * </ul>
	 * Also, all mathematical operations are done component-wise on Cs and Cp.
	 * </p>
	 */
	public static enum EnvMode {
		/**
		 * The REPLACE EnvMode basically ignores the previous color and uses the
		 * texture color as is, unless the texture doesn't have the
		 * corresponding component data (ie when using Alpha or RGB).
		 * 
		 * <pre>
		 * Format | Alpha   | RGB     | RGBA
		 *        | Cv = Cp | Cv = Cs | Cv = Cs
		 *        | Av = As | Av = Ap | Av = As
		 * </pre>
		 */
		REPLACE,
		/**
		 * The DECAL EnvMode interpolates between the texture and previous
		 * colors by the texture's alpha (alpha = 1 for RGB textures), while
		 * preserving the previous alpha value. It is undefined when using alpha
		 * or luminance based textures.
		 * 
		 * <pre>
		 * Format | Alpha/Lum | RGB     | RGBA
		 *        |    --     | Cv = Cs | Cv = Cp(1-As)+CsAs
		 *        |    --     | Av = Ap | Av = Ap
		 * </pre>
		 */
		DECAL,
		/**
		 * The MODULATE EnvMode combines the texture and previous colors by
		 * multiplying the components with each other, effectively mixing them.
		 * 
		 * <pre>
		 * Format | Alpha   | RGB       | RGBA
		 *        | Cv = Cp | Cv = CpCs | Cv = CpCs
		 *        | Av = As | Av = Ap   | Av = ApAs
		 * </pre>
		 */
		MODULATE,
		/**
		 * The BLEND EnvMode interpolates between the previous color and the
		 * constant color, per-component based on the texture's component
		 * values. Alpha values, when present, are modulated instead.
		 * 
		 * <pre>
		 * Format | Alpha     | RGB                | RGBA
		 *        | Cv = Cp   | Cv = Cp(1-Cs)+CcCs | Cv = Cp(1-Cs)+CcCs
		 *        | Av = ApAs | Av = Ap            | Av = ApAs
		 * </pre>
		 */
		BLEND,
		/**
		 * <p>
		 * The COMBINE EnvMode is unlike the other EnvModes in that it exposes
		 * additional configuration points. The final color values are derived
		 * by separate rgb and alpha functions, which each take three
		 * parameters. The parameters are modeled as a source (or input) and an
		 * operand on that source.
		 * </p>
		 * <p>
		 * See {@link CombineFunction}, {@link CombineSource}, and {@link CombineOp}
		 * for more information.
		 * </p>
		 */
		COMBINE
	}
	
	/**
	 * TexCoord represents the enum of available texture coordinates used to
	 * access a texture. Each texture can be accessed by up to four coordinates:
	 * (s, t, r, q). Often however, only the first two or three are used. When
	 * all four are specified, the q coordinate acts as a homogeneous coordinate.
	 * If r is not provided for when a TextureImage requires three coordinates,
	 * undefined results occur.
	 */
	public static enum TexCoord {
		/** The first texture coordinate. */
		S, 
		/** The second texture coordinate. */
		T, 
		/** The third texture coordinate. */
		R, 
		/** The fourth texture coordinate. */
		Q
	}

	/**
	 * When mapping a TextureImage onto a rendered Geometry, texture coordinates
	 * are used to wrap the image about the shape. There are multiple options
	 * for how these coordinates are derived.
	 */
	public static enum TexCoordSource {
		/**
		 * Texture coordinates are taken from the associated vertex attribute of
		 * the Geometry.
		 */
		ATTRIBUTE,
		/**
		 * Texture coordinates are generated as follows:
		 * <ul>
		 * <li>(xe, ye, ze, we) is the vertex in eye coordinates</li>
		 * <li>(p1, p2, p3, p4) is the configured plane equation</li>
		 * <li>M^-1 is the inverse of the modelview matrix when (p1, p2, p3, p4) were set</li>
		 * <li>(p1', p2', p3', p4') = (p1, p2, p3, p4) x M^-1</li>
		 * </ul>
		 * Then the texture coordinate g is: <code>
		 * g = p1'*xe + p2'*ye + p3'*ze + p4'*we
		 * </code>
		 */
		EYE,
		/**
		 * Texture coordinates are generated as follows:
		 * <ul>
		 * <li>(xo, yo, zo, wo) is the vertex in object coordinates</li>
		 * <li>(p1, p2, p3, p4) is the configured plane equation</li>
		 * </ul>
		 * Then the texture coordinate g is: <code>
		 * g = p1*xo + p2*yo + p3*zo + p4*wo
		 * </code>
		 */
		OBJECT,
		/**
		 * Texture coordinates are generated to correctly look up a TextureImage
		 * whose pixel data is organized as a spherical reflection map.
		 */
		SPHERE,
		/**
		 * Texture coordinates are generated and set to the normal vector
		 * of each associated vertex.
		 */
		NORMAL,
		/**
		 * Texture coordinates are generated and set to a computed reflection
		 * vector at each vertex.  The reflection vector is based on the vertex
		 * normal and the current eye point.
		 */
		REFLECTION
	}

	/**
	 * Set whether or not eye-space fogging is enabled. If this is enabled, each
	 * rendered pixel's color value is blended with the configured fog color (at
	 * the time of the rendering) based on the fog equation. The fog equation
	 * can be linear, set with {@link #setFogLinear(float, float)}, or
	 * exponential, set with {@link #setFogExponential(float, boolean)}.
	 * 
	 * @param enable True if fogging is enabled
	 */
	public void setFogEnabled(boolean enable);

	/**
	 * Set the color used when fogging is enabled. The color cannot be null.
	 * 
	 * @param color The new fog color
	 * @throws NullPointerException if color is null
	 */
	public void setFogColor(Color4f color);

	/**
	 * <p>
	 * Set the fog range for linear fog to be between start and end. Anything
	 * before start is un-fogged, and anything after end is completely fog.
	 * Calling this method also configures the Renderer to subsequently use
	 * linear fog if it's enabled.
	 * </p>
	 * <p>
	 * The fog blend factor is computed as (e - c) / (e - s), where e = end
	 * distance, s = start distance and c = camera eye distance for a pixel.
	 * </p>
	 * 
	 * @param start The start range for the linear fog
	 * @param end The end range for the linear fog
	 * @throws IllegalArgumentException if start >= end
	 */
	public void setFogLinear(float start, float end);

	/**
	 * <p>
	 * Set the fog to use an exponential blending factor with the given density
	 * function. Fog will be rendered using an exponential equation until
	 * {@link #setFogLinear(float, float)} is called, which sets it to use
	 * linear fog instead.
	 * </p>
	 * <p>
	 * The fog blend factor is computed two different ways, depending on
	 * squared. If squared is true, then it is e^(-density * c), else it is
	 * e^(-(density * c)^2), where c is the camera eye distance.
	 * </p>
	 * 
	 * @param density The new fog density
	 * @param squared True if the exponent should be squared
	 * @throws IllegalArgumentException if density is less than 0
	 */
	public void setFogExponential(float density, boolean squared);

	/**
	 * Set to true to cause points to be rendered as anti-aliased circles
	 * instead of squares. When anti-aliasing is enabled, the final point width
	 * may not be exactly the requested point size. This anti-aliasing is
	 * independent of any anti-aliasing performed by the RenderSurface.
	 * 
	 * @param enable True if points should be anti-aliased
	 */
	public void setPointAntiAliasingEnabled(boolean enable);

	/**
	 * Set to true to cause lines to be rendered as anti-aliased lines. When
	 * enabled, the actual line width may not be exactly the requested line
	 * width. This anti-aliasing is independent of the RenderSurface
	 * anti-aliasing.
	 * 
	 * @param enable True if lines should be anti-aliased
	 */
	public void setLineAntiAliasingEnabled(boolean enable);

	/**
	 * Set to true if the edges of rendered polygons should be anti-aliased. It
	 * is not recommended to use this when using adjacent polygons because the
	 * edges are independently anti-aliased and will appear to pull away from
	 * each other. Polygons that have DrawStyles of LINE or POINT use the
	 * anti-aliasing configuration for points and lines.
	 * 
	 * @param enable True if solid polygons should have their edges anti-aliased
	 */
	public void setPolygonAntiAliasingEnabled(boolean enable);

	/**
	 * Configure the alpha test to be used when rendering. If the given test
	 * Comparison returns true when a pixel's alpha value is compared to
	 * refValue, then the pixel will continue to be processed. Using a
	 * Comparison of ALWAYS effectively disables the alpha test.
	 * 
	 * @param test The new alpha test Comparison to use
	 * @param refValue The reference value that pixel's alphas are compared to
	 */
	public void setAlphaTest(Comparison test, float refValue);

	/**
	 * Set the pixel width of rendered points. If the width has a fractional
	 * component, it may only appear that width when anti-aliasing is enabled.
	 * 
	 * @param width The new point width
	 * @throws IllegalArgumentException if width < 1
	 */
	public void setPointSize(float width);

	/**
	 * Set the line width of rendered lines. If the width has a fractional
	 * component, it may only appear the correct width when line anti-aliasing
	 * is enabled.
	 * 
	 * @param width The new line width
	 * @throws IllegalArgumentException if width < 1
	 */
	public void setLineSize(float width);

	/**
	 * Set whether or not lighting is calculated for rendered Geometry. When
	 * lighting is enabled, the global ambient light, and all enabled lights
	 * have their colors summed together based on how much each contributes to
	 * each vertex. In the end, the lit pixel is then fed through the texturing
	 * pipeline. The amount that each light contributes to a polygon is based on
	 * how the light's direction shines onto the polygon's face, and how the
	 * configured material colors interact with each light's colors.
	 * 
	 * @param enable True if lighting is enabled
	 */
	public void setLightingEnabled(boolean enable);

	/**
	 * Set the ambient color that's always added to pixel colors when lighting
	 * is enabled. The color cannot be null.
	 * 
	 * @param ambient The new global ambient color
	 * @throws NullPointerException if ambient is null
	 */
	public void setGlobalAmbientLight(Color4f ambient);

	/**
	 * <p>
	 * Configure two parameters of the lighting model. If smoothed is true, then
	 * the vertex lighting calculations are interpolated across each polygon to
	 * approximate per-pixel lighting. If smoothed is false, a single vertex's
	 * lighting color is used for the entire face and the geometry will appear
	 * faceted.
	 * </p>
	 * <p>
	 * If twoSided is true, then lighting will be calculated separately for
	 * front facing pixels and back facing pixels. This is a more expensive
	 * operation when enabled, and should only be enabled when it's possible to
	 * view both inside and outside of a geometry mesh.
	 * </p>
	 * 
	 * @param smoothed True if polygons are smooothly shaded
	 * @param twoSided True if front and back faces use separate light
	 *            calculations
	 */
	public void setLightingModel(boolean smoothed, boolean twoSided);

	/**
	 * Set whether or not the given light is enabled. If it's set to true then
	 * it will be used to compute lighting when the overall lighting system has
	 * been enabled. If the light index is outside of the range of valid lights,
	 * then the request should be ignored.
	 * 
	 * @param light The given light, from 0 to the maximum number of lights
	 *            available (usually 8)
	 * @param enable True if it's enabled
	 */
	public void setLightEnabled(int light, boolean enable);

	/**
	 * <p>
	 * Set the homogenous position for the given light. This position vector can
	 * be used to create two different types of light: point/spotlight and
	 * infinite directional lights. It is based on the interpretation that a
	 * directional light is a point light that's shining infinitely far away.
	 * </p>
	 * <p>
	 * When the given pos vector has a w component of 1, the light will act as a
	 * point or spotlight located at (x, y, z). When the w component is 0, then
	 * the light is infinitely far away in the direction of (x, y, z). In effect
	 * the light is a directional light shining in (-x, -y, -z).
	 * </p>
	 * <p>
	 * The given position vector is transformed by the current modelview matrix
	 * at the time that this method is called, which converts the position into
	 * 'eye' space so that all lighting calculations can be done in eye space
	 * when a Geometry is finally rendered.
	 * </p>
	 * <p>
	 * Any of value for w is not allowed. If the light index is outside of the
	 * range of valid lights, then the request should be ignored.
	 * </p>
	 * 
	 * @param light The given light, from 0 to the maximum number of lights
	 *            available (usually 8)
	 * @param pos The position vector for this light
	 * @throws NullPointerException if pos is null
	 * @throws IllegalArgumentException if pos.w is not 0 or 1
	 */
	public void setLightPosition(int light, Vector4f pos);

	/**
	 * <p>
	 * Set the ambient, diffuse and specular colors for the given light. Every
	 * enabled light's colors are multiplied with corresponding material
	 * component and is then modulated by the amount of contribution for the
	 * light. This scaled color is then added to final color. Thus a red diffuse
	 * material color and a green diffuse light color would become a black
	 * contribution since (1, 0, 0, 1) * (0, 1, 0, 1) is (0, 0, 0, 1).
	 * </p>
	 * <p>
	 * If the light index is outside of the range of valid lights, then the
	 * request should be ignored.
	 * </p>
	 * 
	 * @param light The given light to configure
	 * @param amb The new ambient color of the light
	 * @param diff The new diffuse color of the light
	 * @param spec The new specular color of the light
	 * @throws NullPointerException if amb, diff, or spec are null
	 */
	public void setLightColor(int light, Color4f amb, Color4f diff, Color4f spec);

	/**
	 * <p>
	 * Set the spotlight direction and cutoff angle for the given light.
	 * Although this can be set on any light, it only has a visible effect when
	 * the light has a position that's not infinite (e.g. it's not a direction
	 * light). When a light is set to a point or spotlight, the spotlight
	 * direction and cutoff angle control how the light shines. Like the light
	 * position, the spotlight direction is transformed by the current modelview
	 * matrix when this is called.
	 * </p>
	 * <p>
	 * The cutoff angle is the degree measure of the half-angle of a cone of
	 * light that expands outwards from the light's position in the direction of
	 * dir. Anything outside of this cone is not light. Acceptable values for
	 * the angle are in the range [0, 90] and 180. 180 is a special value and
	 * causes the light to act as a point light where the light shines in a
	 * sphere. In this case, the spotlight direction has no effect on the
	 * lighting.
	 * </p>
	 * <p>
	 * If the light index is outside the range of valid lights, then the request
	 * should be ignored.
	 * </p>
	 * 
	 * @param light The given light to configure
	 * @param dir The direction that spotlights shine
	 * @param angle The cutoff angle for the light from the spotlight
	 * @throws NullPointerException if dir is null
	 * @throws IllegalArgumentException if angle is not in [0, 90] or equal to
	 *             180
	 */
	public void setSpotlight(int light, Vector3f dir, float angle);

	/**
	 * <p>
	 * Set the light attenuation factors used for spotlights and point lights.
	 * Light attenuation is an approximation of the light's intensity fading
	 * with distance from the light source. To compute this distance requires a
	 * finite light's position, which is why attenuation is pointless for
	 * directional lights.
	 * </p>
	 * <p>
	 * When determining the contribution of a given light, it is scaled by some
	 * attenuation factor that is computed based on these configured constants
	 * and a pixel's distance to the light. If we let c, l, and q represent the
	 * constant, linear and quadratic terms and d is equal to the distance to
	 * the light, then the attenuation factor is computed as:
	 * <code>1 / (c + l * d + q * d^2)</code>
	 * </p>
	 * <p>
	 * Thus, the default values of c = 1, l = q = 0 cause the attenuation factor
	 * to always equal 1 and so the light never fades. Setting the linear and
	 * quadratic terms to non-zero values can cause a decrease in performance.
	 * Undefined results occur if the attenuation values are given that cause
	 * the divisor to equal 0.
	 * </p>
	 * <p>
	 * If the light index is outside of the range of valid lights, then the
	 * request should be ignored.
	 * </p>
	 * 
	 * @param light The given light to configure
	 * @param constant The constant attenuation term, >= 0
	 * @param linear The linear attenuation factor, >= 0
	 * @param quadratic The quadratic attenuation factor, >= 0
	 * @throws IllegalArgumentException if constant, linear or quadratic < 0
	 */
	public void setLightAttenuation(int light, float constant, float linear, float quadratic);

	/**
	 * <p>
	 * Set the material colors used when rendering a Geometry. These colors
	 * correspond to the amount of each color that's reflected by each type of
	 * light component (ambient, diffuse, and specular). Emmissive light is
	 * special because it represents the amount of light that the material is
	 * generating itself. This color is added to the final lit color without
	 * modulation by the light's colors.
	 * </p>
	 * <p>
	 * When lighting is not enabled, then the diffuse color value is used as the
	 * solid, unlit color for the rendered shape. This is because the diffuse
	 * color generally represents the 'color' of a lit object while the other
	 * color values serve as adding realism to it.
	 * </p>
	 * 
	 * @param amb The ambient color of the material
	 * @param diff The diffuse color of the material, or solid color when
	 *            lighting is disabled
	 * @param spec The specular color of the material
	 * @param emm The emmissive color of the material
	 */
	public void setMaterial(Color4f amb, Color4f diff, Color4f spec, Color4f emm);

	/**
	 * Set the material shininess to use when lighting is enabled. This
	 * shininess acts as an exponent on the specular intensity, and can be used
	 * to increase or dampen the brightness of the specular highlight. The
	 * shininess is an exponent in the range [0, 128], where a value of 0 causes
	 * the highlight to be non-existent and 128 causes an extremely bright
	 * highlight.
	 * 
	 * @param shininess The material shininess
	 * @throws IllegalArgumentException if shininess is not in [0, 128]
	 */
	public void setMaterialShininess(float shininess);

	/**
	 * <p>
	 * Set whether or not a texture is enabled. When a texture unit is disabled,
	 * it will not modify the color of a pixel based on a bound TextureImage. If
	 * a texture is enabled but has no bound TextureImage, it has the same
	 * impact on rendering as if the unit were disabled.
	 * </p>
	 * <p>
	 * If tex is outside the valid range of available texture units, this
	 * request should be ignored.
	 * </p>
	 * 
	 * @see #setTexture(int, TextureImage)
	 * @param tex The texture unit
	 * @param enable True if the texture unit should be enabled
	 */
	public void setTextureEnabled(int tex, boolean enable);

	/**
	 * <p>
	 * Set the TextureImage to be bound to the given unit. In order for a
	 * texture to affect the rendering, it must have a READY TextureImage bound
	 * and the unit must be enabled via {@link #setTextureEnabled(int, boolean)}
	 * Specifying a null image unbinds the previous TextureImage.
	 * </p>
	 * <p>
	 * If tex is outside the valid range of available texture units, this
	 * request should be ignored.
	 * </p>
	 * 
	 * @param tex The texture unit
	 * @param image The TextureImage to be bound to tex
	 */
	public void setTexture(int tex, TextureImage image);

	/**
	 * <p>
	 * Set the texture environment mode that's used for the given texture unit.
	 * The texture environment mode specifies how a given texture is combined
	 * with the lit/solid color and any other enabled texture units. See
	 * {@link EnvMode} for a detailed description of each EnvMode.
	 * </p>
	 * <p>
	 * If tex is outside the valid range of available texture units, this
	 * request should be ignored.
	 * </p>
	 * 
	 * @param tex The texture unit
	 * @param mode The EnvMode for this unit
	 * @throws NullPointerException if mode is null
	 */
	public void setTextureMode(int tex, EnvMode mode);

	/**
	 * <p>
	 * Set the texture color that's used for this unit. The texture color is the
	 * constant color used by {@link EnvMode#BLEND} and
	 * {@link CombineSource#CONST_COLOR}.
	 * </p>
	 * <p>
	 * If tex is outside the valid range of available texture units, this
	 * request should be ignored.
	 * </p>
	 * 
	 * @param tex The texture unit
	 * @param color The new constant texture color for the unit
	 * @throws NullPointerException if color is null
	 */
	public void setTextureColor(int tex, Color4f color);

	/**
	 * <p>
	 * Set the texture coordinate source for all four coordinates to gen, for
	 * the given texture unit.
	 * </p>
	 * <p>
	 * If tex is outside the valid range of available texture units, this
	 * request should be ignored.
	 * </p>
	 * 
	 * @see #setTextureCoordGeneration(int, TexCoord, TexCoordSource)
	 * @param tex The texture unit
	 * @param gen The TexCoordSource for all four coordinates
	 * @throws NullPointerException if gen is null
	 */
	public void setTextureCoordGeneration(int tex, TexCoordSource gen);

	/**
	 * <p>
	 * Set the texture coordinate source for the specified texture coordinate on
	 * the given texture unit. Each usable texture unit has an associated
	 * TextureImage. However, there must be a mapping between the image value
	 * and the rendered Geometry, which is in essence, how does the Geometry
	 * unwrap onto the image? This is accomplished by using texture coordinates
	 * that are per-vertex vectors much like vertices or normals that represent
	 * how to access the TextureImage. These texture coordinates can be
	 * auto-generated or specified as part of the Geometry. See
	 * {@link TexCoordSource} for a description of each source.
	 * </p>
	 * <p>
	 * If tex is outside the valid range of available texture units, this
	 * request should be ignored.
	 * </p>
	 * 
	 * @param tex The texture unit
	 * @param coord The coordinate that's source is to be modified
	 * @param gen The new texture coordinate source
	 * @throws NullPointerException if coord or gen are null
	 */
	public void setTextureCoordGeneration(int tex, TexCoord coord, TexCoordSource gen);

	/**
	 * <p>
	 * Set the four values used for the {@link TexCoordSource#OBJECT} generation
	 * for the given coordinate. These four values represent (p1, p2, p3, p4) as
	 * described in TexCoordSource.OBJECT for the given coordinate. Each texture
	 * coordinate has its own four planar values (for each unit, too). Also,
	 * these four values are independent of the four values stored for the eye
	 * plane.
	 * </p>
	 * <p>
	 * If tex is outside the valid range of available texture units, this
	 * request should be ignored.
	 * </p>
	 * 
	 * @param tex The texture unit
	 * @param coord The coordinate whose object plane will be set
	 * @param plane The object plane that's used for this unit and coordinate * @throws
	 *            NullPointerException if coord or plane are null
	 */
	public void setTextureObjectPlane(int tex, TexCoord coord, Vector4f plane);

	/**
	 * <p>
	 * Set the four values used for the {@link TexCoordSource#EYE} generation
	 * for the given coordinate and texture. These four values represent (p1,
	 * p2, p3, p4) as described in TexCoordSource.EYE for the coordinate. These
	 * four values are multiplied by the inverse of the current modelview matrix
	 * when this method is invoked. Like with the object plane, each coordinate
	 * for each unit has its own set of four eye plane values. These values are
	 * independent from the object plane.
	 * </p>
	 * <p>
	 * If tex is outside the valid range of available texture units, this
	 * request should be ignored.
	 * </p>
	 * 
	 * @param tex The texture unit
	 * @param coord The coordinate whose eye plane will be set
	 * @param plane The eye plane to be specified, values are before inverse
	 *            modelview multiplication
	 * @throws NullPointerException if coord or plane are null
	 */
	public void setTextureEyePlane(int tex, TexCoord coord, Vector4f plane);

	/**
	 * <p>
	 * In addition to texture coordinates on each unit, these coordinates can be
	 * transformed by a matrix before accessing the TextureImage. When
	 * performing this transformation, each texture coordinate uses reasonable
	 * default values for coordinates that aren't provided: the 3rd coordinate
	 * is mapped to 0 and the 4th coordinate is mapped to 1.
	 * </p>
	 * <p>
	 * As a convenience, if a null matrix is specified, the transform is reset
	 * to the identity matrix.
	 * </p>
	 * <p>
	 * If tex is outside the valid range of available texture units, this
	 * request should be ignored.
	 * </p>
	 * 
	 * @param tex The texture unit
	 * @param matrix The texture coordinate transform matrix
	 */
	public void setTextureTransform(int tex, Matrix4f matrix);

	/**
	 * Set the rgb and alpha {@link CombineFunction}s that will be used if the
	 * {@link EnvMode#COMBINE} is active. The two functions are used to modify
	 * the the RGB color values and the alpha component, respectively. It is
	 * illegal to specify DOT3_RGB or DOT3_RGBA in alphaFunc.</p>
	 * <p>
	 * <p>
	 * If tex is outside the valid range of available texture units, this
	 * request should be ignored.
	 * </p>
	 * 
	 * @param tex The texture unit
	 * @param rgbFunc The CombineFunction used for rgb values
	 * @param alphaFunc The CombineFunction used for the alpha component
	 * @throws NullPointerException if rgbFunc or alphaFunc are null
	 * @throws IllegalArgumentException if alphaFunc is DOT3_RGB or DOT3_RGBA
	 */
	public void setTextureCombineFunction(int tex, CombineFunction rgbFunc, CombineFunction alphaFunc);

	/**
	 * <p>
	 * Set the {@link CombineSource} and {@link CombineOp} for the texture unit
	 * and given operand. The operand corresponds to the 0th, 1st, or 2nd
	 * argument that is used with the configured RGB CombineFunction.
	 * </p>
	 * <p>
	 * If tex is outside the valid range of available texture units, this
	 * request should be ignored.
	 * </p>
	 * 
	 * @param tex The texture unit
	 * @param operand The argument to the function, must be 0, 1, or 2
	 * @param src The CombineSource for the operand
	 * @param op The CombineOp that modifies the specified CombineSource
	 * @throws NullPointerException if src or op are null
	 * @throws IllegalArgumentException if operand is not 0, 1 or 2
	 */
	public void setTextureCombineOpRgb(int tex, int operand, CombineSource src, CombineOp op);

	/**
	 * Identical to
	 * {@link #setTextureCombineOpRgb(int, int, CombineSource, CombineOp)}
	 * except that the CombineSource and CombineOp apply to the configured alpha
	 * function.
	 * 
	 * @param tex The texture unit
	 * @param operand The argument to the function, must be 0, 1, or 2
	 * @param src The CombineSource for the operand
	 * @param op The CombineOp that modifies the specified CombineSource
	 * @throws NullPointerException if src or op are null
	 * @throws IllegalArgumentException if operand is not 0, 1, or 2
	 */
	public void setTextureCombineOpAlpha(int tex, int operand, CombineSource src, CombineOp op);

	/**
	 * <p>
	 * Set the projection matrix that transforms eye-space coordinates into clip
	 * coordinates. After perspective division (division by the 4th coordinate
	 * of a vertex, generally 1), the clip coordinates are converted to
	 * normalized device coordinates. These are three-dimensional points that
	 * lie within the cube (-1, -1, -1) to (1, 1, 1).
	 * </p>
	 * <p>
	 * An important effect of this transformation is that it flips the z-axis
	 * direction; after the modelview matrix, negative z values extend in front
	 * of and away from the view, and now positive z values are farther away.
	 * </p>
	 * <p>
	 * Like with the {@link #setTextureTransform(int, Matrix4f) texture matrix},
	 * a null matrix will reset the projection matrix to the identity.
	 * </p>
	 * 
	 * @param projection The new projection matrix
	 */
	public void setProjectionMatrix(Matrix4f projection);

	/**
	 * <p>
	 * Set the combined modelview matrix that transforms object coordinates into
	 * the eye coordinate system. This matrix is important because it is not
	 * only used during rendering, but its value impacts the final stored values
	 * for light position, spotlight direction, and eye plane values for texture
	 * coordinate generation.
	 * </p>
	 * <p>
	 * After this transformation, the coordinate space is the 'viewer' located
	 * at the origin, looking down the negative z-axis. Thus, more negative
	 * z-values are farther way in appearance (assuming a perspective
	 * projection).
	 * </p>
	 * <p>
	 * Because of this, it is often useful to first set the modelview to a
	 * matrix that transforms 'world' coordinates into eye space and specify all
	 * of the lights, etc. that are stored in world coordinates in the
	 * application. Then for each Geometry to render, compute the product of the
	 * camera matrix and the shape's to-world matrix and use this as that
	 * Geometry's modelview matrix.
	 * </p>
	 * 
	 * @param modelView The new modelview matrix
	 */
	public void setModelViewMatrix(Matrix4f modelView);

	/**
	 * Set the name of the Geometry attribute that holds the vertices of the
	 * Geometry. A null name clears the currently assigned vertex name. In order
	 * for a Geometry to be rendered successfully, the following conditions must
	 * hold:
	 * <ol>
	 * <li>The vertex name binding is not null at the time of
	 * {@link #render(com.ferox.resource.Geometry)}</li>
	 * <li>The Geometry contains an attribute of the given name</li>
	 * <li>The attribute in the Geometry has an element size of 2, 3, or 4</li>
	 * </ol>
	 * In addition to these conditions, others exist as described in
	 * {@link #render(com.ferox.resource.Geometry)}
	 * 
	 * @param name The name of the attribute used for vertices
	 */
	public void setVertexBinding(String name);

	/**
	 * Set the name of the Geometry attribute that holds onto the normals for
	 * the Geometry. A null name clears the currently assigned normal name. In
	 * order for a Geometry to use normals while rendering, the following
	 * conditions must hold:
	 * <ol>
	 * <li>The normal name at the time of rendering cannot be null</li>
	 * <li>The Geometry must have an attribute with the given name</li>
	 * <li>The attribute must have an element size of 3
	 * <li>
	 * </ol>
	 * When a Geometry is rendered without normals, an undetermined normal is
	 * used for computing the lighting effects. Thus it is not necessary to
	 * specify normals when rendering with lighting disabled.
	 * 
	 * @param name The name of the attribute for normals
	 */
	public void setNormalBinding(String name);

	/**
	 * <p>
	 * Set the name of the Geometry attribute that holds onto the texture
	 * coordinates used to access the texture unit at tex. A null name clears
	 * the currently assigned texture coordinate name for the given texture
	 * unit. In order for the Geometry to use the texture coordinates for the
	 * unit while rendering, the following must hold:
	 * <ol>
	 * <li>The texture coordinate name at the time of rendering cannot be null</li>
	 * <li>The Geometry must have the an attribute with the given name</li>
	 * </ol>
	 * </p>
	 * <p>
	 * When rendering a Geometry without a set of texture coordinates, an
	 * undetermined texture coordinate is used to access the associated enabled
	 * texture unit. Because texture lookups are only performed on enabled
	 * units, it's not necessary to set a texture coordinate name for disabled
	 * texture units.
	 * </p>
	 * <p>
	 * If tex is outside the range of available texture units, then this request
	 * will be ignored.
	 * </p>
	 * 
	 * @param tex The texture unit that the name is assigned to
	 * @param name The name of the attribute for texture coordinates at the
	 *            given unit
	 */
	public void setTextureCoordinateBinding(int tex, String name);

	/**
	 * <p>
	 * Convenience function to set the vertex, normal and texture coordinate
	 * name bindings all in one function call. vertices and normals function
	 * equivalently to the name values described in
	 * {@link #setVertexBinding(String)} and {@link #setNormalBinding(String)}.
	 * </p>
	 * <p>
	 * The texCoords array holds a texture coordinate name binding for each
	 * texture unit. The 0th unit's name is stored at index 0, etc. If there are
	 * more texture units than values in texCoords, they will have a null name.
	 * If texCoords contains null elements, their corresponding attributes will
	 * have null names. If texCoords is null, then all texture coordinate names
	 * will be set to null. Any indices above the available number of texture
	 * units will be ignored.
	 * </p>
	 * 
	 * @param vertices The new vertices name
	 * @param normals The new normals attribute name
	 * @param texCoords An array of texture coordinate attribute names
	 */
	public void setBindings(String vertices, String normals, String[] texCoords);
}
