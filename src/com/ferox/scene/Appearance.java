package com.ferox.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openmali.vecmath.Vector3f;

import com.ferox.effect.AlphaTest;
import com.ferox.effect.BlendMode;
import com.ferox.effect.ColorMask;
import com.ferox.effect.DepthTest;
import com.ferox.effect.Effect;
import com.ferox.effect.GlobalLighting;
import com.ferox.effect.GlslShader;
import com.ferox.effect.LineStyle;
import com.ferox.effect.Material;
import com.ferox.effect.MultiTexture;
import com.ferox.effect.PointStyle;
import com.ferox.effect.PolygonStyle;
import com.ferox.effect.StencilTest;
import com.ferox.effect.Texture;
import com.ferox.effect.BlendMode.BlendEquation;
import com.ferox.effect.BlendMode.BlendFactor;
import com.ferox.effect.Effect.PixelTest;
import com.ferox.effect.PointStyle.PointSpriteOrigin;
import com.ferox.effect.PolygonStyle.DrawStyle;
import com.ferox.effect.PolygonStyle.Winding;
import com.ferox.effect.StencilTest.StencilOp;
import com.ferox.math.Color;

/**
 * Appearance describes a set of Effects that can be applied to rendered
 * geometries when using Shapes to build up a scene.
 * 
 * @author Michael Ludwig
 */
public class Appearance {
	// lists of non-null effects below
	private final List<Effect> effects;
	private final List<Effect> lockedEffects;

	// effect instances that are supported by an Appearance
	private AlphaTest alphaTest;
	private BlendMode blendMode;
	private ColorMask colorMask;
	private DepthTest depthTest;
	private GlobalLighting globalLighting;
	private GlslShader shader;
	private LineStyle lineStyle;
	private Material material;
	private MultiTexture textures;
	private PointStyle pointStyle;
	private PolygonStyle polyStyle;
	private StencilTest stencilTest;

	private boolean enableFog;

	/**
	 * Create an Appearance that is equivalent to the default or null
	 * Appearance. It has no Effects assigned or modified.
	 */
	public Appearance() {
		effects = new ArrayList<Effect>();
		lockedEffects = Collections.unmodifiableList(effects);
	}

	/**
	 * @return True if FogNodes should affect the rendering of the geometry
	 */
	public boolean getFogEnabled() {
		return enableFog;
	}

	/**
	 * @param enabled Whether or not fog should affect the rendered geometry
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setFogEnabled(boolean enabled) {
		enableFog = enabled;
		return this;
	}

	/**
	 * Convenience method to create a new AlphaTest with the given parameters
	 * and assign to this Appearance.
	 * 
	 * @param alpha The PixelTest to use
	 * @param refValue The reference alpha value to use
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setAlphaTest(PixelTest alpha, float refValue) {
		AlphaTest test = new AlphaTest();
		test.setTest(alpha);
		test.setReferenceValue(refValue);

		return setAlphaTest(test);
	}

	/**
	 * Convenience method to create a new BlendMode with the given parameters
	 * and assign to this Appearance.
	 * 
	 * @param blendEq The BlendEquation to use
	 * @param src The BlendFactor that affects the incoming source color
	 * @param dst The BlendFactor affecting the destination color
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setBlendMode(BlendEquation blendEq, BlendFactor src,
		BlendFactor dst) {

		BlendMode blend = new BlendMode();
		blend.setEquation(blendEq);
		blend.setSourceFactor(src);
		blend.setDestFactor(dst);

		return setBlendMode(blend);
	}
	
	/**
	 * Convenience method to create a new ColorMask with the given
	 * parameters and assign it to this Appearance.
	 * 
	 * @param red The red mask boolean
	 * @param green The green mask boolean
	 * @param blue The blue mask boolean
	 * @param alpha The alpha mask boolean
	 * @return The Appearance for chaining purposes
	 */
	public Appearance setColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		ColorMask cm = new ColorMask(red, green, blue, alpha);
		return setColorMask(cm);
	}

	/**
	 * Convenience method to create a new DepthTest with the given parameters
	 * and assign to this Appearance.
	 * 
	 * @param depthTest The PixelTest to use
	 * @param enableWriting True if depth values should be written
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setDepthTest(PixelTest depthTest, boolean enableWriting) {
		DepthTest test = new DepthTest();
		test.setTest(depthTest);
		test.setWriteEnabled(enableWriting);

		return setDepthTest(test);
	}

	/**
	 * Convenience method to create a new GlobalLighting with the given
	 * parameters and assign to this Appearance.
	 * 
	 * @param ambient The global ambient color
	 * @param separateSpec Separate specular color calculation policy
	 * @param localView True if the specular should be computed from a local
	 *            view position vs. infinite
	 * @param twoSided True if lighting should be computed separately for each
	 *            side of a polygon
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setGlobalLighting(Color ambient, boolean separateSpec,
		boolean localView, boolean twoSided) {

		GlobalLighting lighting = new GlobalLighting();
		lighting.setGlobalAmbient(ambient);
		lighting.setLocalViewer(localView);
		lighting.setSeparateSpecular(separateSpec);
		lighting.setTwoSidedLighting(twoSided);

		return setGlobalLighting(lighting);
	}

	/**
	 * Convenience method to create a new LineStyle with the given parameters
	 * and assign to this Appearance (everything else remains the default).
	 * 
	 * @param width The width of the line
	 * @param enableSmooth True if the line should be anti-aliased
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setLineStyle(float width, boolean enableSmooth) {
		LineStyle style = new LineStyle();
		style.setLineWidth(width);
		style.setSmoothingEnabled(enableSmooth);

		return setLineStyle(style);
	}

	/**
	 * Convenience method to create a new AlphaTest with the given parameters
	 * and assign to this Appearance. Like the other setLineStyle() method, it
	 * will set the width and smoothing. In addition it will enable stipplying
	 * and set the given stipple pattern and stipple factor.
	 * 
	 * @param width The width of the line
	 * @param enableSmooth True if the line should be anti-aliased
	 * @param pattern The stipple pattern to use
	 * @param factor The stipple repeat factor
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setLineStyle(float width, boolean enableSmooth,
		short pattern, int factor) {

		LineStyle style = new LineStyle();
		style.setLineWidth(width);
		style.setSmoothingEnabled(enableSmooth);
		style.setStipplingEnabled(true);
		style.setStipplePattern(pattern);
		style.setStippleFactor(factor);

		return setLineStyle(style);
	}

	/**
	 * Identical to setMaterial(diffuse, specular, new Color(.2f, .2f, .2f,
	 * 1f)).
	 * 
	 * @param diffuse The diffuse color for the new material
	 * @param specular The specular color for the new material
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setMaterial(Color diffuse, Color specular) {
		return setMaterial(diffuse, specular, new Color(.2f, .2f, .2f, 1f));
	}

	/**
	 * Identical to setMaterial(diffuse, specular, ambient, 5f, true).
	 * 
	 * @param diffuse The diffuse color for the new material
	 * @param specular The specular color for the new material
	 * @param ambient The ambient color for the new material
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setMaterial(Color diffuse, Color specular, Color ambient) {
		return setMaterial(diffuse, specular, ambient, 5f, true);
	}

	/**
	 * Convenience method to create a new Material with the given parameters and
	 * assign to this Appearance.
	 * 
	 * @param diffuse The diffuse color for the new material
	 * @param specular The specular color for the new material
	 * @param ambient The ambient color for the new material
	 * @param shininess The shininess of the new material
	 * @param smoothed True if lit polygons have smooth or faceted lighting
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setMaterial(Color diffuse, Color specular, Color ambient,
		float shininess, boolean smoothed) {

		Material m = new Material(diffuse, specular, ambient, shininess);
		m.setSmoothShaded(smoothed);

		return setMaterial(m);
	}

	/**
	 * <p>
	 * Convenience method to create a new MultiTexture. The textures var-arg is
	 * passed directly into the MultiTexture constructor. The created
	 * MultiTexture is then assigned to this Appearance.
	 * </p>
	 * <p>
	 * If textures is null or empty, the previous MultiTexture binding is
	 * broken.
	 * </p>
	 * 
	 * @param textures The list of textures to use for this Appearance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setTextures(Texture... textures) {
		if (textures == null || textures.length == 0)
			return setMultiTexture(null);
		else {
			MultiTexture tex = new MultiTexture(textures);
			return setMultiTexture(tex);
		}
	}

	/**
	 * Convenience method to create a new PointStyle with the given width and
	 * smoothing policy (everything else remains the default), and apply it to
	 * this Appearance.
	 * 
	 * @param width The width of the points
	 * @param smoothing True if the points should be anti-aliased
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setPointStyle(float width, boolean smoothing) {
		return setPointStyle(width, smoothing, 1f, null);
	}

	/**
	 * Convenience method to create a new PointStyle with the given width
	 * smoothing policy, minimum attenuated width and a distance attenuation
	 * vector (everything else remains the default), and apply it to this
	 * Appearance.
	 * 
	 * @param width The width of the points
	 * @param smoothing True if the points should be anti-aliased
	 * @param min The minimum width of an attenuated or faded point
	 * @param attenuation The distance attenuation vector to use
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setPointStyle(float width, boolean smoothing, float min,
		Vector3f attenuation) {

		PointStyle style = new PointStyle();
		style.setPointSize(width);
		style.setMinMaxPointSize(min, Float.MAX_VALUE);
		style.setDistanceAttenuation(attenuation);

		return setPointStyle(style);
	}

	/**
	 * Convenience method to create a new PointStyle with the given width and
	 * point sprite origin. The created PointStyle has point sprites enabled and
	 * will use the 0th texture unit for coordinate generation. The PointStyle
	 * is then applied to this Appearance
	 * 
	 * @param width The width of rendered point sprites
	 * @param pointSpriteMode The origin for point sprite texture coordinate
	 *            generation
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setPointStyle(float width,
		PointSpriteOrigin pointSpriteMode) {

		PointStyle style = new PointStyle();
		style.setPointSize(width);
		style.setPointSpriteEnabled(true);
		style.setPointSpriteOrigin(pointSpriteMode);
		style.setPointSpriteTextureUnit(0);

		return setPointStyle(style);
	}

	/**
	 * Identical to setPolygonStyle(fron, back, Winding.COUNTER_CLOCKWISE).
	 * 
	 * @param front The draw style for front facing polygons
	 * @param back The draw style for back facing polygons
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setPolygonStyle(DrawStyle front, DrawStyle back) {
		return setPolygonStyle(front, back, Winding.COUNTER_CLOCKWISE);
	}

	/**
	 * Identical to setPolygonStyle(fron, back, winding, false, 0f).
	 * 
	 * @param front The draw style for front facing polygons
	 * @param back The draw style for back facing polygons
	 * @param winding The winding of polygons to determine front/back facing
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setPolygonStyle(DrawStyle front, DrawStyle back,
		Winding winding) {
		return setPolygonStyle(front, back, winding, false, 0f);
	}

	/**
	 * Convenience method to create a new PolygonStyle with the given properties
	 * and assign it to this Appearance.
	 * 
	 * @param front The draw style for front facing polygons
	 * @param back The draw style for back facing polygons
	 * @param winding The winding of polygons to determine front/back facing
	 * @param smoothing True if polygon edges should be anti-aliased
	 * @param offset The depth offset applied to rendered polygons
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setPolygonStyle(DrawStyle front, DrawStyle back,
		Winding winding, boolean smoothing, float offset) {

		PolygonStyle style = new PolygonStyle(front, back);
		style.setWinding(winding);
		style.setDepthOffset(offset);
		style.setSmoothingEnabled(smoothing);

		setPolygonStyle(style);

		return this;
	}

	/**
	 * Identical to setStencilTest(test, reference, stencilFail, depthFail,
	 * depthPass, ~0, ~0).
	 * 
	 * @param test The stencil test to use
	 * @param reference The stencil value used to determine stencil test failure
	 * @param stencilFail The operation that occurs on stencil failure
	 * @param depthFail The operation that occurs on depth test failure
	 * @param depthPass The operation that occurs on depth test success
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setStencilTest(PixelTest test, int reference,
		StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass) {
		return setStencilTest(test, reference, stencilFail, depthFail,
			depthPass, ~0, ~0);
	}

	/**
	 * Convenience method to create a new StencilTest with the given properties
	 * and assign it to this Appearance.
	 * 
	 * @param stencilTest The stencil test to use
	 * @param reference The stencil value used to determine stencil test failure
	 * @param stencilFail The operation that occurs on stencil failure
	 * @param depthFail The operation that occurs on depth test failure
	 * @param depthPass The operation that occurs on depth test success
	 * @param testMask The bit mask applied to reference and the stencil buffer
	 *            during testing
	 * @param writeMask The bit mask enabling/disabling bits for writing into
	 *            the buffer
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setStencilTest(PixelTest stencilTest, int reference,
		StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass,
		int testMask, int writeMask) {

		StencilTest test = new StencilTest();
		test.setDepthFailOp(depthFail);
		test.setDepthPassOp(depthPass);
		test.setStencilFailOp(stencilFail);
		test.setTest(stencilTest);
		test.setReferenceValue(reference);
		test.setTestMask(testMask);
		test.setWriteMask(writeMask);

		setStencilTest(test);

		return this;
	}

	/**
	 * Set the AlphaTest instance to use, null breaks any previous binding.
	 * 
	 * @param alpha The AlphaTest instance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setAlphaTest(AlphaTest alpha) {
		if (alphaTest != null)
			effects.remove(alphaTest);
		alphaTest = alpha;
		if (alphaTest != null)
			effects.add(alphaTest);

		return this;
	}

	/**
	 * @return The AlphaTest attached to this Appearance, may be null
	 */
	public AlphaTest getAlphaTest() {
		return alphaTest;
	}

	/**
	 * Set the BlendMode instance to use, null breaks any previous binding.
	 * 
	 * @param blend The BlendMode instance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setBlendMode(BlendMode blend) {
		if (blendMode != null)
			effects.remove(blendMode);
		blendMode = blend;
		if (blendMode != null)
			effects.add(blendMode);

		return this;
	}

	/**
	 * @return The BlendMode attached to this Appearance, may be null
	 */
	public BlendMode getBlendMode() {
		return blendMode;
	}
	
	/** Set the ColorMask instance to use, null breaks any previous binding.
	 * 
	 * @param mask The ColorMask instance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setColorMask(ColorMask mask) {
		if (colorMask != null)
			effects.remove(colorMask);
		colorMask = mask;
		if (colorMask != null)
			effects.add(mask);
		
		return this;
	}
	
	/**
	 * @return The ColorMask attached to this Appearance, may be null
	 */
	public ColorMask getColorMask() {
		return colorMask;
	}

	/**
	 * Set the DepthTest instance to use, null breaks any previous binding.
	 * 
	 * @param depth The DepthTest instance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setDepthTest(DepthTest depth) {
		if (depthTest != null)
			effects.remove(depthTest);
		depthTest = depth;
		if (depthTest != null)
			effects.add(depthTest);

		return this;
	}

	/**
	 * @return The DepthTest attached to this Appearance, may be null
	 */
	public DepthTest getDepthTest() {
		return depthTest;
	}

	/**
	 * Set the GlobalLighting instance to use, null breaks any previous binding.
	 * If global is not null, then nodes using this Appearance will be affected
	 * by intersecting LightNodes in the scene tree.
	 * 
	 * @param global The GlobalLighting instance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setGlobalLighting(GlobalLighting global) {
		if (globalLighting != null)
			effects.remove(globalLighting);
		globalLighting = global;
		if (globalLighting != null)
			effects.add(globalLighting);

		return this;
	}

	/**
	 * @return The GlobalLighting attached to this Appearance, may be null. If
	 *         it is null, then nodes rendered with this appearance will not be
	 *         affected by lighting
	 */
	public GlobalLighting getGlobalLighting() {
		return globalLighting;
	}

	/**
	 * Set the GlslShader instance to use, null breaks any previous binding.
	 * 
	 * @param glsl The GlslShader instance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setGlslShader(GlslShader glsl) {
		if (shader != null)
			effects.remove(shader);
		shader = glsl;
		if (shader != null)
			effects.add(shader);

		return this;
	}

	/**
	 * @return The GlslShader attached to this Appearance, may be null
	 */
	public GlslShader getGlslShader() {
		return shader;
	}

	/**
	 * Set the LineStyle instance to use, null breaks any previous binding.
	 * 
	 * @param line The LineStyle instance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setLineStyle(LineStyle line) {
		if (lineStyle != null)
			effects.remove(lineStyle);
		lineStyle = line;
		if (lineStyle != null)
			effects.add(lineStyle);

		return this;
	}

	/**
	 * @return The LineStyle attached to this Appearance, may be null
	 */
	public LineStyle getLineStyle() {
		return lineStyle;
	}

	/**
	 * Set the Material instance to use, null breaks any previous binding.
	 * 
	 * @param mat The Material instance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setMaterial(Material mat) {
		if (material != null)
			effects.remove(material);
		material = mat;
		if (material != null)
			effects.add(material);

		return this;
	}

	/**
	 * @return The Material attached to this Appearance, may be null
	 */
	public Material getMaterial() {
		return material;
	}

	/**
	 * Set the MultiTexture instance to use, null breaks any previous binding.
	 * 
	 * @param tex The MultiTexture instance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setMultiTexture(MultiTexture tex) {
		if (textures != null)
			effects.remove(textures);
		textures = tex;
		if (textures != null)
			effects.add(textures);

		return this;
	}

	/**
	 * @return The MultiTexture attached to this Appearance, may be null
	 */
	public MultiTexture getMultiTexture() {
		return textures;
	}

	/**
	 * Set the PointStyle instance to use, null breaks any previous binding.
	 * 
	 * @param point The PointStyle instance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setPointStyle(PointStyle point) {
		if (pointStyle != null)
			effects.remove(pointStyle);
		pointStyle = point;
		if (pointStyle != null)
			effects.add(pointStyle);

		return this;
	}

	/**
	 * @return The PointTyle attached to this Appearance, may be null
	 */
	public PointStyle getPointStyle() {
		return pointStyle;
	}

	/**
	 * Set the PolygonStyle instance to use, null breaks any previous binding.
	 * 
	 * @param poly The PolygonStyle instance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setPolygonStyle(PolygonStyle poly) {
		if (polyStyle != null)
			effects.remove(polyStyle);
		polyStyle = poly;
		if (polyStyle != null)
			effects.add(polyStyle);

		return this;
	}

	/**
	 * @return The PolygonStyle attached to this Appearance, may be null
	 */
	public PolygonStyle getPolygonStyle() {
		return polyStyle;
	}

	/**
	 * Set the StencilTest instance to use, null breaks any previous binding.
	 * 
	 * @param stencil The StencilTest instance
	 * @return This Appearance for chaining purposes
	 */
	public Appearance setStencilTest(StencilTest stencil) {
		if (stencilTest != null)
			effects.remove(stencilTest);
		stencilTest = stencil;
		if (stencilTest != null)
			effects.add(stencilTest);

		return this;
	}

	/**
	 * @return The StencilTest attached to this Appearance, may be null
	 */
	public StencilTest getStencilTest() {
		return stencilTest;
	}

	/**
	 * Get all effects on this appearance. Returns an unmodifiable list backed
	 * by this appearance. Therefore any changes to this appearance will be
	 * present in the list, too.
	 * 
	 * @return All actively used Effects for this Appearance
	 */
	protected List<Effect> getEffects() {
		return lockedEffects;
	}
}
