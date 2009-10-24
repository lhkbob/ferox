package com.ferox.effect;

import com.ferox.math.Color4f;

public class FixedFunctionShader extends OpenGlShader<FixedFunctionShader> {
	// alpha test
	private Comparison alphaTest;
	private float alphaReferenceValue;
	
	// fog
	private final Fog fog;
	private boolean enableFog;
	
	// draw styles
	private float pointSize;
	private float lineSize;
	
	private boolean pointSmoothing;
	private boolean lineSmoothing;
	private boolean polySmoothing;
	
	// lighting
	private boolean enableLighting;
	
	private boolean glSeparateSpec;
	private boolean glLocalViewer;
	private boolean glUseTwoSidedLighting;
	private final Color4f globalAmbient;
	
	private boolean glSmoothShaded;
	
	private final Light[] lights;
	private final Material material;
	
	// textures
	private final TextureEnvironment[] texEnv;
	
	// geometry bindings
	private String vertexBinding;
	private String normalBinding;
	private final String[] texCoords;
	
	public FixedFunctionShader(int numLights, int numTextures) {
		lights = new Light[numLights];
		texEnv = new TextureEnvironment[numTextures];
		texCoords = new String[numTextures];
		
		for (int i = 0; i < numLights; i++)
			lights[i] = new Light();
		for (int i = 0; i < numTextures; i++)
			texEnv[i] = new TextureEnvironment();

		material = new Material();
		fog = new Fog();
		
		globalAmbient = new Color4f(.2f, .2f, .2f, 1f);
		
		setAlphaTest(Comparison.ALWAYS, 0f);
		setFogEnabled(false);
		
		setLightingEnabled(false);
		setSeparateSpecularEnabled(false);
		setLocalViewerEnabled(false);
		setTwoSidedLightingEnabled(false);
		setSmoothShaded(true);
		
		setAntiAliasingEnabled(false);
		setWidth(1f);
		
		// leave geometry unbound
	}

	/**
	 * Return the named binding used by this Shader to look-up vertex attributes
	 * for use as vertices in any Geometry rendered by it. If this is null or
	 * does not correspond to a set of vertex attributes in the Geometry, then
	 * no vertices will be rendered.
	 * 
	 * @return The vertex attribute name used as a source of vertices
	 */
	public String getVertexBinding() {
		return vertexBinding;
	}

	/**
	 * Set the vertex attribute name used to look up the source of vertex
	 * information from any Geometry rendered by this Shader. If vertices is
	 * null, or if a Geometry does not have a set of attributes with that name,
	 * then no vertices will be rendered.
	 * 
	 * @param vertices The new vertices name
	 * @return This Shader
	 */
	public FixedFunctionShader setVertexBinding(String vertices) {
		vertexBinding = vertices;
		return this;
	}

	/**
	 * Return the named binding used by this Shader to look-up vertex attributes
	 * for use as normal vectors in any Geometry rendered by it. If this is null
	 * or does not correspond to a set of vertex attributes in the Geometry,
	 * then it will be rendered without correct normal information.
	 * 
	 * @return The vertex attribute name used as a source of normals
	 */
	public String getNormalBinding() {
		return normalBinding;
	}
	
	/**
	 * Set the vertex attribute name used to look up the source of normal
	 * information from any Geometry rendered by this Shader. If normals is
	 * null, or if a Geometry does not have a set of attributes with that name,
	 * then normals will not be specified when rendering.
	 * 
	 * @param normals The new normals name
	 * @return This Shader
	 */
	public FixedFunctionShader setNormalBinding(String normals) {
		normalBinding = normals;
		return this;
	}

	/**
	 * <p>
	 * Return the named binding used by this Shader to look-up vertex attributes
	 * for use as texture coordinates associated with the unit: tc. If this is
	 * null or does not correspond to a set of vertex attributes in the
	 * Geometry, then it will be rendered without correct texture coordinate
	 * data.
	 * </p>
	 * <p>
	 * The texture unit specified by tc corresponds to a configured
	 * TextureEnvironment returned by {@link #getTexture(int)} with the same
	 * unit. tc must be at least 0, and no greater than
	 * {@link #getNumTextureUnits()} - 1.
	 * </p>
	 * 
	 * @param tc The texture unit that these coordinates are associated with
	 * @return The vertex attribute name used as a source of texture coordinates
	 *         for texture tc
	 * @throws IndexOutOfBoundsException if tc is < 0 or >= getNumTextures()
	 */
	public String getTextureCoordinateBinding(int tc) {
		return texCoords[tc];
	}

	/**
	 * <p>
	 * Set the vertex attribute name used to look up the source of texture
	 * coordinate information from any Geometry rendered by this Shader. If
	 * binding is null, or if a Geometry does not have a set of attributes with
	 * that name, then texture coordinates will not be specified for this
	 * texture unit.
	 * </p>
	 * <p>
	 * The given name binding is only associated with the TextureEnvironment of
	 * the same unit as tc. Any other available texture units for this Shader
	 * will have to be bound separately.
	 * </p>
	 * 
	 * @param tc The texture unit that the given binding applies to
	 * @param binding The new binding for texture coordinates on unit tc
	 * @return This Shader
	 * @throws IndexOutOfBoundsException if tc < 0 or >= getNumTextures()
	 */
	public FixedFunctionShader setTextureCoordinateBinding(int tc, String binding) {
		texCoords[tc] = binding;
		return this;
	}

	/**
	 * <p>
	 * Convenience function to specify all necessary Geometry vertex attribute
	 * bindings in one call. vertices and normals have the same meaning as they
	 * would in {@link #setVertexBinding(String)} and
	 * {@link #setNormalBinding(String)}.
	 * </p>
	 * <p>
	 * texCoords is an optional array of texture coordinate bindings. Each
	 * element within texCoords is a binding name corresponding to the texture
	 * unit equal to the array index. For example, the first texture coordinate
	 * binding corresponds to texture unit 0, etc. All previous bindings for
	 * texture coordinates are set to null before configuring the bindings to
	 * match texCoords.
	 * </p>
	 * 
	 * @param vertices The new vertex binding
	 * @param normals The new normal binding
	 * @param texCoords An array of texture coordinate bindings
	 * @return This Shader
	 * @throws IndexOutOfBoundsException if texCoords contains more elements
	 *             than available texture units
	 */
	public FixedFunctionShader setGeometryBindings(String vertices, String normals, String... texCoords) {
		setVertexBinding(vertices);
		setNormalBinding(normals);
		
		// clear texture coordinates
		for (int i = 0; i < this.texCoords.length; i++)
			this.texCoords[i] = null;
		if (texCoords != null) {
			for (int i = 0; i < texCoords.length; i++)
				setTextureCoordinateBinding(i, texCoords[i]);
		}
		
		return this;
	}

	/**
	 * Set both the line width and the point width to the given value. This
	 * float value must be at least 1, and is in pixels. Width values with
	 * fractional values will appear more correct when anti-aliasing is enabled
	 * for the primitive type.
	 * 
	 * @param width The new point and line width
	 * @return This Shader
	 * @throws IllegalArgumentException if width < 1
	 */
	public FixedFunctionShader setWidth(float width) {
		return setLineWidth(width).setPointWidth(width);
	}

	/**
	 * Return the pixel width that lines will be rendered in when using this
	 * Shader. If line anti-aliasing is disabled, the actual rendered width may
	 * not equal this width because it has to rasterize the line onto discrete
	 * samples.
	 * 
	 * @return The line width
	 */
	public float getLineWidth() {
		return lineSize;
	}

	/**
	 * Set the line width of rendered lines for this Shader. This
	 * width must be at least 1.
	 * 
	 * @param width The new line width
	 * @return This Shader
	 * @throws IllegalArgumentException if width < 1
	 */
	public FixedFunctionShader setLineWidth(float width) {
		if (width < 1f)
			throw new IllegalArgumentException("Width must be at least 1");
		lineSize = width;
		return this;
	}

	/**
	 * <p>
	 * Return the pixel width that points will be rendered in when using this
	 * PrimitiveRenderStyle. If point anti-aliasing is disabled, the actual
	 * rendered width may not always equal this width because it has to
	 * rasterize the line onto discrete samples.
	 * </p>
	 * <p>
	 * When anti-aliasing is enabled, the rendered point will appear as a circle
	 * of this width. Without anti-aliasing, the rendered point is a square.
	 * </p>
	 * 
	 * @return The point width
	 */
	public float getPointWidth() {
		return pointSize;
	}

	/**
	 * Set the point width of rendered points for this Shader. This width must
	 * be at least 1.
	 * 
	 * @param width The new point width
	 * @return This PrimitiveRenderStyle
	 * @throws IllegalArgumentException if width < 1
	 */
	public FixedFunctionShader setPointWidth(float width) {
		if (width < 1f)
			throw new IllegalArgumentException("Width must be at least 1");
		pointSize = width;
		return this;
	}

	/**
	 * Set whether or not primitive anti-aliasing is enabled for points, lines,
	 * and polygons. Note that this anti-aliasing is different than fullscreen
	 * anti-aliasing that's applied via a RenderSurface.
	 * 
	 * @param enabled True if primitives should be smoothed
	 * @return This Shader
	 */
	public FixedFunctionShader setAntiAliasingEnabled(boolean enabled) {
		return setAntiAliasingEnabled(enabled, enabled, enabled);
	}

	/**
	 * Return whether or not polygon edges are anti-aliased. For complex models,
	 * this should generally disabled since the edges of adjacent triangles will
	 * appear to pull away from each other.
	 * 
	 * @return True if polygon anti-aliasing is enabled
	 */
	public boolean getPolygonAntiAliasingEnabled() {
		return polySmoothing;
	}
	
	/**
	 * Return whether or not points are anti-aliased. When points are anti-aliased,
	 * they appear as circles instead of squares.  If point sprites are enabled, point
	 * anti-aliasing is ignored.
	 * 
	 * @return True if point anti-aliasing is enabled
	 */
	public boolean getPointAntiAliasingEnabled() {
		return pointSmoothing;
	}
	
	/**
	 * Return whether or not lines are anti-aliased. 
	 * 
	 * @return True if line anti-aliasing is enabled
	 */
	public boolean getLineAntiAliasingEnabled() {
		return lineSmoothing;
	}

	/**
	 * Set whether or not polygons, lines and points are anti-aliased. This
	 * performs the same function as {@link #setAntiAliasingEnabled(boolean)},
	 * except it allows for greater granularity by primitive type.
	 * 
	 * @param polys Enabled boolean for polygons
	 * @param lines Enabled boolean for lines
	 * @param points Enabled boolean for points
	 * @return This Shader
	 */
	public FixedFunctionShader setAntiAliasingEnabled(boolean polys, boolean lines, 
													   boolean points) {
		polySmoothing = polys;
		lineSmoothing = lines;
		pointSmoothing = points;
		
		return this;
	}

	/**
	 * Convenience function to set this FixedFunctionShader's Material's diffuse
	 * and specular colors and its shininess. All values are constrained as per
	 * the rules of {@link Material#setDiffuse(Color4f)},
	 * {@link Material#setSpecular(Color4f)}, and
	 * {@link Material#setShininess(shininess)}.
	 * 
	 * @param diffuse The diffuse color to use
	 * @param specular The specular color to use
	 * @param shininess The new shininess
	 * @return This Shader
	 * @throws IllegalArgumentException if shininess < 0 or > 128
	 */
	public FixedFunctionShader setMaterial(Color4f diffuse, Color4f specular, float shininess) {
		material.setDiffuse(diffuse).setSpecular(specular).setShininess(shininess);
		return this;
	}

	/**
	 * Return the Material instance that stores the coloring parameters for this
	 * Shader. Any changes made to the Material will be reflected in subsequent
	 * renderings with this Shader. If lighting is not enabled, the Material's
	 * diffuse color should be used as the flat color for any rendered Geometry.
	 * 
	 * @return This Shader's Material
	 */
	public Material getMaterial() {
		return material;
	}

	/**
	 * Return whether or not per-vertex lighting is interpolated across polygons
	 * to create smoother looking shading. If this is false, then the first
	 * vertex's normal of a polygon is used for the lighting computations and
	 * creates a faceted appearance.
	 * 
	 * @return True if lighting is interpolated or smoothed
	 */
	public boolean isSmoothShaded() {
		return glSmoothShaded;
	}

	/**
	 * Set whether or not lighting should be computed per-vertex and
	 * interpolated, or per-face. Per-face lighting is approximated by choosing
	 * one normal from the vertices making a polygon and using that for the
	 * entire face.
	 * 
	 * @param smooth New smoothing parameter
	 * @return This Shader
	 */
	public FixedFunctionShader setSmoothShaded(boolean smooth) {
		glSmoothShaded = smooth;
		return this;
	}

	/**
	 * Return the number of Lights that can be enabled at a single time,
	 * generally this will be a low number around 8.
	 * 
	 * @return The maximum number of active lights for this Shader
	 */
	public int getNumLights() {
		return lights.length;
	}

	/**
	 * Return the Light that is associated with unit l. Each FixedFunctionShader
	 * has a number of lights equal to {@link #getNumLights()}. Each of these
	 * lights have an associated unit that starts at index 0 and goes to
	 * getNumLights() - 1.
	 * 
	 * @param l The light unit to look-up
	 * @return The Light associated with the unit l, this will not be null
	 * @throws IndexOutOfBoundsException if l < 0 or >= getNumLights()
	 */
	public Light getLight(int l) {
		return lights[l];
	}

	/**
	 * Return whether or not the Light at unit l is enabled or disabled. This is
	 * equivalent to getLight(l).isEnabled(). A Light's enabled state is
	 * controlled by {@link Light#setEnabled(boolean)}.
	 * 
	 * @param l The light unit to look-up
	 * @return True if the lth Light is enabled
	 */
	public boolean isLightEnabled(int l) {
		return lights[l].isEnabled();
	}

	/**
	 * Return the maximum number of textures that can be enabled simultaneously.
	 * This number is likely to be very small, ranging from 1 to 16 depending on
	 * the quality of the system.
	 * 
	 * @return The number of available textures
	 */
	public int getNumTextures() {
		return texEnv.length;
	}

	/**
	 * <p>
	 * Return the TextureEnvironment associated with the unit t. A
	 * FixedFunctionShader has a number of TextureEnvironments equal to
	 * {@link #getNumTextures()}. Each of these has a unit number associated
	 * with it, indexed from 0. This same unit number is used to pair a
	 * TextureEnvironment with a texture coordinate binding.
	 * </p>
	 * <p>
	 * A TextureEnvironment can be used without a texture coordinates if it's
	 * configured to use automatic texture coordinate generation. If a texture
	 * uses automatic texture coordinates, any coordinates from a bound Geometry
	 * will be overridden by the computed ones (this could be on a per-component
	 * basis and not necessarily on the entire coordinate).
	 * <p>
	 * 
	 * @param t The texture unit to look-up
	 * @return The TextureEnvironment associated with t
	 * @throws IndexOutOfBoundsException if t < 0 or t >= getNumTextures()
	 */
	public TextureEnvironment getTexture(int t) {
		return texEnv[t];
	}
	
	/**
	 * Return whether or not the TextureEnvironment at unit t is enabled or disabled. This is
	 * equivalent to getTexture(t).isEnabled(). A TextureEnvironments's enabled state is
	 * controlled by {@link TextureEnvironment#setEnabled(boolean)}.
	 * 
	 * @param t The texture unit to look-up
	 * @return True if the tth TextureEnvironment is enabled
	 */
	public boolean isTextureEnabled(int t) {
		return texEnv[t].isEnabled();
	}

	/**
	 * Return the Fog instance that controls the description of eye-space fog
	 * approximation with this Shader. The Fog will only be applied to
	 * renderings if it's also enabled via {@link #setFogEnabled(boolean)}.
	 * 
	 * @return The Fog state for this Shader
	 */
	public Fog getFog() {
		return fog;
	}

	/**
	 * Return whether or not fogging should be enabled. If this returns true,
	 * then rendered Geometries will be modified by the Fog returned by getFog()
	 * as described in Fog's documentation.
	 * 
	 * @return True if fogging is enabled
	 */
	public boolean isFogEnabled() {
		return enableFog;
	}

	/**
	 * Set whether or not fogging should be enabled. See {@link #isFogEnabled()}
	 * and {@link #getFog()}.
	 * 
	 * @param enable True if fogging should be applied when rendering
	 * @return This Shader
	 */
	public FixedFunctionShader setFogEnabled(boolean enable) {
		enableFog = enable;
		return this;
	}

	/**
	 * Return the Comparison to be used when performing the alpha test to
	 * determine if a pixel should be rendered or discarded. The alpha test is
	 * applied before all other tests (such as the stencil or depth tests) and
	 * before blending. If an incoming pixel's alpha value fails the comparison
	 * against {@link #getAlphaReferenceValue()}, then the pixel is discarded.
	 * 
	 * @return The Comparison used for alpha testing
	 */
	public Comparison getAlphaTest() {
		return alphaTest;
	}

	/**
	 * Return the alpha value that's compared against when performing the alpha
	 * test. This value will be between 0 and 1.
	 * 
	 * @return The reference alpha value for the alpha test
	 */
	public float getAlphaReferenceValue() {
		return alphaReferenceValue;
	}

	/**
	 * <p>
	 * Set the alpha test parameters for this FixedFunctionShader. test
	 * represents the comparison to be performed (as returned by
	 * {@link #getAlphaTest()}. alphaValue is the reference value that a pixel's
	 * alpha value is compared against. This value must be in [0, 1]. If test is
	 * null, it uses ALWAYS.
	 * </p>
	 * <p>
	 * Note that alpha testing is effectively disabled if the test is set to
	 * ALWAYS, since a pixel will always pass and proceed to the other passes.
	 * </p>
	 * 
	 * @param test The new Comparison to use for alpha testing
	 * @param alphaValue The new alpha reference value
	 * @return This Shader
	 * @throws IllegalArgumentException if alphaValue < 0 or > 1
	 */
	public FixedFunctionShader setAlphaTest(Comparison test, float alphaValue) {
		if (alphaValue < 0f || alphaValue > 1f)
			throw new IllegalArgumentException("Alpha reference value out of range [0, 1]: " + alphaValue);
		
		alphaTest = (test != null ? test : Comparison.ALWAYS);
		alphaReferenceValue = alphaValue;
		return this;
	}
	
	public boolean isLightingEnabled() {
		return enableLighting;
	}
	
	public FixedFunctionShader setLightingEnabled(boolean enabled) {
		enableLighting = enabled;
		return this;
	}
	
	public boolean isTwoSidedLightingEnabled() {
		return glUseTwoSidedLighting;
	}
	
	public FixedFunctionShader setTwoSidedLightingEnabled(boolean use) {
		glUseTwoSidedLighting = use;
		return this;
	}

	public boolean isSeparateSpecularEnabled() {
		return glSeparateSpec;
	}

	public FixedFunctionShader setSeparateSpecularEnabled(boolean separateSpec) {
		glSeparateSpec = separateSpec;
		return this;
	}

	public boolean isLocalViewerEnabled() {
		return glLocalViewer;
	}

	public FixedFunctionShader setLocalViewerEnabled(boolean localViewer) {
		glLocalViewer = localViewer;
		return this;
	}

	public Color4f getGlobalAmbientColor() {
		return globalAmbient;
	}

	public FixedFunctionShader setGlobalAmbientColor(Color4f globalAmb) {
		if (globalAmb == null)
			globalAmbient.set(.2f, .2f, .2f, 1f);
		else
			globalAmbient.set(globalAmb);
		return this;
	}
}
