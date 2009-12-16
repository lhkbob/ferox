package com.ferox.renderer.impl;

import java.util.Arrays;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.FixedFunctionRenderer.CombineFunction;
import com.ferox.renderer.FixedFunctionRenderer.CombineOp;
import com.ferox.renderer.FixedFunctionRenderer.CombineSource;
import com.ferox.renderer.FixedFunctionRenderer.EnvMode;
import com.ferox.renderer.FixedFunctionRenderer.TexCoord;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.resource.Geometry;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

public abstract class FixedFunctionRendererDelegate {
	protected static enum FogMode {
		LINEAR, EXP, EXP_SQUARED
	}
	
	protected static enum LightColor {
		AMBIENT, DIFFUSE, SPECULAR, EMISSIVE
	}
	
	protected static enum MatrixMode {
		MODELVIEW, PROJECTION, TEXTURE
	}
	
	private static final Matrix4f IDENTITY = new Matrix4f().setIdentity();
	
	// cached defaults
	private static final Color4f DEFAULT_MAT_A_COLOR = new Color4f(.2f, .2f, .2f, 1f);
	private static final Color4f DEFAULT_MAT_D_COLOR = new Color4f(.8f, .8f, .8f, 1f);
	
	private static final Color4f ZERO = new Color4f(0f, 0f, 0f, 0f);
	private static final Color4f BLACK = new Color4f(0f, 0f, 0f, 1f);
	private static final Color4f WHITE = new Color4f(1f, 1f, 1f, 1f);
	
	private static final Vector4f DEFAULT_LIGHT_POS = new Vector4f(0f, 0f, 1f, 0f);
	private static final Vector3f DEFAULT_SPOT_DIR = new Vector3f(0f, 0f, -1f);
	
	private static final Vector4f DEFAULT_S_PLANE = new Vector4f(1f, 0f, 0f, 0f);
	private static final Vector4f DEFAULT_T_PLANE = new Vector4f(0f, 1f, 0f, 0f);
	private static final Vector4f DEFAULT_0_PLANE = new Vector4f(0f, 0f, 0f, 0f);
	
	// alpha test
	private Comparison alphaTest = Comparison.ALWAYS;
	private float alphaRefValue = 1f;
	
	// fog
	private final Color4f fogColor = new Color4f(ZERO);
	
	private float fogStart = 0f;
	private float fogEnd = 1f;
	private float fogDensity = 1f;
	
	private FogMode fogMode = FogMode.EXP;
	private boolean fogEnabled = false;
	
	// global lighting
	private final Color4f globalAmbient = new Color4f(DEFAULT_MAT_A_COLOR);
	private boolean lightingEnabled = false;
	private boolean lightingTwoSided = false;
	private boolean lightingSmoothed = true;
	
	// lights
	private final LightState[] lights;
	
	// material
	private final Color4f matDiffuse = new Color4f(DEFAULT_MAT_D_COLOR);
	private final Color4f matAmbient = new Color4f(DEFAULT_MAT_A_COLOR);
	private final Color4f matSpecular = new Color4f(BLACK);
	private final Color4f matEmmissive = new Color4f(BLACK);
	
	private float matShininess = 0f;
	
	// primitive size/aa
	private boolean lineAAEnabled = false;
	private boolean pointAAEnabled = false;
	private boolean polyAAEnabled = false;
	
	private float lineWidth = 1f;
	private float pointWidth = 1f;
	
	// texturing
	private int activeTex = 0;
	private final TextureState[] textures;
	
	// bindings, has protected access for sub-classes
	protected String vertexBinding = Geometry.DEFAULT_VERTICES_NAME;
	protected String normalBinding = Geometry.DEFAULT_NORMALS_NAME;
	protected final String[] texBindings;
	
	private final String[] restoreTexBindings;
	
	// matrix
	private MatrixMode matrixMode = MatrixMode.MODELVIEW;
	
	public FixedFunctionRendererDelegate(int numLights, int numTextures) {
		if (numLights < 8)
			throw new IllegalArgumentException("numLights is below required hardware minimum of 8: " + numLights);
		if (numTextures < 0)
			throw new IllegalArgumentException("numTextures below required minimum of 0");
		
		lights = new LightState[numLights];
		for (int i = 0; i < lights.length; i++)
			lights[i] = new LightState();
		// modify 0th light's colors
		lights[0].specular.set(WHITE);
		lights[0].diffuse.set(WHITE);
		
		textures = new TextureState[numTextures];
		for (int i = 0; i < textures.length; i++)
			textures[i] = new TextureState();
		
		texBindings = new String[numTextures];
		texBindings[0] = Geometry.DEFAULT_TEXCOORD_NAME;
		restoreTexBindings = new String[] {Geometry.DEFAULT_TEXCOORD_NAME};
	}

	/**
	 * Perform identical operations to {@link Renderer#render(Geometry)}, except
	 * within the fixed-function context.
	 */
	public abstract int render(Geometry geom);
	
	public void reset() {
		setModelViewMatrix(null);
		setProjectionMatrix(null);
		
		setAlphaTest(Comparison.ALWAYS, 1f);
		
		setFogColor(ZERO);
		setFogExponential(1f, false);
		setFogEnabled(false);
		
		setGlobalAmbientLight(DEFAULT_MAT_A_COLOR);
		setLightingEnabled(false);
		setLightingModel(true, false);
		
		setMaterial(DEFAULT_MAT_A_COLOR, DEFAULT_MAT_D_COLOR, BLACK, BLACK);
		setMaterialShininess(0f);
		
		setLineAntiAliasingEnabled(false);
		setPointAntiAliasingEnabled(false);
		setPolygonAntiAliasingEnabled(false);
		
		setLineSize(1f);
		setPointSize(1f);
		
		// reset all lights
		for (int i = 0; i < lights.length; i++) {
			setLightEnabled(i, false);
			setLightPosition(i, DEFAULT_LIGHT_POS);
			
			if (i == 0)
				setLightColor(i, BLACK, WHITE, WHITE);
			else
				setLightColor(i, BLACK, WHITE, WHITE);
			
			setSpotlight(i, DEFAULT_SPOT_DIR, 180f);
			setLightAttenuation(i, 1f, 0f, 0f);
		}
		
		// reset all textures
		for (int i = 0; i < textures.length; i++) {
			setTextureTransform(i, null);
			
			setTextureEnabled(i, false);
			setTexture(i, null);
			
			setTextureColor(i, ZERO);
			setTextureMode(i, EnvMode.MODULATE);
			
			setTextureCombineFunction(i, CombineFunction.MODULATE, CombineFunction.MODULATE);
			setTextureCombineOpAlpha(i, 0, CombineSource.CURR_TEX, CombineOp.ALPHA);
			setTextureCombineOpAlpha(i, 1, CombineSource.PREV_TEX, CombineOp.ALPHA);
			setTextureCombineOpAlpha(i, 2, CombineSource.CONST_COLOR, CombineOp.ALPHA);
			setTextureCombineOpRgb(i, 0, CombineSource.CURR_TEX, CombineOp.COLOR);
			setTextureCombineOpRgb(i, 1, CombineSource.PREV_TEX, CombineOp.COLOR);
			setTextureCombineOpRgb(i, 2, CombineSource.CONST_COLOR, CombineOp.ALPHA);

			setTextureCoordGeneration(i, TexCoordSource.ATTRIBUTE);
			
			setTextureEyePlane(i, TexCoord.S, DEFAULT_S_PLANE);
			setTextureEyePlane(i, TexCoord.T, DEFAULT_T_PLANE);
			setTextureEyePlane(i, TexCoord.R, DEFAULT_0_PLANE);
			setTextureEyePlane(i, TexCoord.Q, DEFAULT_0_PLANE);
			setTextureObjectPlane(i, TexCoord.S, DEFAULT_S_PLANE);
			setTextureObjectPlane(i, TexCoord.T, DEFAULT_T_PLANE);
			setTextureObjectPlane(i, TexCoord.R, DEFAULT_0_PLANE);
			setTextureObjectPlane(i, TexCoord.Q, DEFAULT_0_PLANE);
		}
		
		// reset binding
		setBindings(Geometry.DEFAULT_VERTICES_NAME, Geometry.DEFAULT_NORMALS_NAME, restoreTexBindings);
	}
	
	public void setModelViewMatrix(Matrix4f matrix) {
		if (matrix == null)
			matrix = IDENTITY;
		
		setMatrixMode(MatrixMode.MODELVIEW);
		glSetMatrix(matrix);
	}
	
	/**
	 * Invoke OpenGL calls to set the matrix mode
	 */
	protected abstract void glMatrixMode(MatrixMode mode);
	
	/**
	 * Invoke OpenGL calls to set the matrix for the current mode
	 */
	protected abstract void glSetMatrix(Matrix4f matrix);
	
	private void setMatrixMode(MatrixMode mode) {
		if (matrixMode != mode) {
			matrixMode = mode;
			glMatrixMode(mode);
		}
	}
	
	public void setProjectionMatrix(Matrix4f matrix) {
		if (matrix == null)
			matrix = IDENTITY;
		
		setMatrixMode(MatrixMode.PROJECTION);
		glSetMatrix(matrix);
	}
	
	public void setAlphaTest(Comparison test, float refValue) {
		if (test == null)
			throw new NullPointerException("Null comparison");
		if (alphaTest != test || alphaRefValue != refValue) {
			alphaTest = test;
			alphaRefValue = refValue;
			glAlphaTest(test, refValue);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set the alpha test
	 */
	protected abstract void glAlphaTest(Comparison test, float ref);

	public void setFogColor(Color4f color) {
		if (color == null)
			throw new NullPointerException("Null fog color");
		if (!fogColor.equals(color)) {
			fogColor.set(color);
			glFogColor(color);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set the fog color
	 */
	protected abstract void glFogColor(Color4f color);

	public void setFogEnabled(boolean enable) {
		if (fogEnabled != enable) {
			fogEnabled = enable;
			glEnableFog(enable);
		}
	}
	
	/**
	 * Invoke OpenGL calls to enable fog
	 */
	protected abstract void glEnableFog(boolean enable);

	public void setFogExponential(float density, boolean squared) {
		if (density < 0f)
			throw new IllegalArgumentException("Density must be >= 0, not: " + density);
		
		if (fogDensity != density) {
			fogDensity = density;
			glFogDensity(density);
		}

		if (squared && fogMode != FogMode.EXP_SQUARED) {
			fogMode = FogMode.EXP_SQUARED;
			glFogMode(FogMode.EXP_SQUARED);
		} else if (fogMode != FogMode.EXP) {
			fogMode = FogMode.EXP;
			glFogMode(FogMode.EXP);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set the fog density
	 */
	protected abstract void glFogDensity(float density);

	public void setFogLinear(float start, float end) {
		if (end <= start)
			throw new IllegalArgumentException("Illegal start/end range: " + start + ", " + end);
		
		if (fogStart != start || fogEnd != end) {
			fogStart = start;
			fogEnd = end;
			glFogRange(start, end);
		}
		
		if (fogMode != FogMode.LINEAR) {
			fogMode = FogMode.LINEAR;
			glFogMode(FogMode.LINEAR);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set the linear fog range
	 */
	protected abstract void glFogRange(float start, float end);
	
	/**
	 * Invoke OpenGL calls to set the fog equation
	 */
	protected abstract void glFogMode(FogMode fog);

	public void setGlobalAmbientLight(Color4f ambient) {
		if (ambient == null)
			throw new NullPointerException("Null global ambient color");
		if (!globalAmbient.equals(ambient)) {
			globalAmbient.set(ambient);
			glGlobalLighting(ambient);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set the global lighting color
	 */
	protected abstract void glGlobalLighting(Color4f ambient);
	
	public void setLightColor(int light, Color4f amb, Color4f diff, Color4f spec) {
		if (light < 0 || light >= lights.length)
			return; // ignore it
		if (amb == null || diff == null || spec == null)
			throw new NullPointerException("Colors cannot be null");
		
		LightState l = lights[light];
		if (!l.ambient.equals(amb)) {
			l.ambient.set(amb);
			glLightColor(light, LightColor.AMBIENT, amb);
		}
		if (!l.diffuse.equals(diff)) {
			l.diffuse.set(diff);
			glLightColor(light, LightColor.DIFFUSE, diff);
		}
		if (!l.specular.equals(spec)) {
			l.specular.set(spec);
			glLightColor(light, LightColor.SPECULAR, spec);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set the light color for the given light
	 */
	protected abstract void glLightColor(int light, LightColor lc, Color4f color);

	public void setLightEnabled(int light, boolean enable) {
		if (light < 0 || light >= lights.length)
			return; // ignore it
		LightState l = lights[light];
		if (l.enabled != enable) {
			l.enabled = enable;
			glEnableLight(light, enable);
		}
	}
	
	/**
	 * Invoke OpenGL calls to enable a specific light
	 */
	protected abstract void glEnableLight(int light, boolean enable);

	public void setLightPosition(int light, Vector4f pos) {
		if (light < 0 || light >= lights.length)
			return; // ignore it
		if (pos == null)
			throw new NullPointerException("Light position can't be null");
		if (pos.w != 0f && pos.w != 1f)
			throw new NullPointerException("pos.w must be 0 or 1, not: " + pos.w);
		
		// always set the light position since pos will be transformed by
		// the current matrix
		glLightPosition(light, pos);
	}
	
	/**
	 * Invoke OpenGL calls to set a light's position vector
	 */
	protected abstract void glLightPosition(int light, Vector4f pos);
	
	public void setSpotlight(int light, Vector3f dir, float angle) {
		if (light < 0 || light >= lights.length)
			return; // ignore it
		if (dir == null)
			throw new NullPointerException("Spotlight direction can't be null");
		if ((angle < 0f || angle > 90f) && angle != 180f)
			throw new IllegalArgumentException("Spotlight angle must be in [0, 90] or be 180, not: " + angle);
		
		LightState l = lights[light];
		if (l.spotAngle != angle) {
			l.spotAngle = angle;
			glLightAngle(light, angle);
		}
		
		// always set the spotlight direction since it will be transformed
		// by the current matrix
		glLightDirection(light, dir);
	}
	
	/**
	 * Invoke OpenGL calls to set a light's spotlight direction
	 */
	protected abstract void glLightDirection(int light, Vector3f dir);
	
	/**
	 * Invoke OpenGL calls to set a light's spotlight angle
	 */
	protected abstract void glLightAngle(int light, float angle);

	public void setLightAttenuation(int light, float constant, float linear, float quadratic) {
		if (light < 0 || light >= lights.length)
			return; // ignore it
		if (constant < 0f)
			throw new IllegalArgumentException("Constant factor must be positive: " + constant);
		if (linear < 0f)
			throw new IllegalArgumentException("Linear factor must be positive: " + linear);
		if (quadratic < 0f)
			throw new IllegalArgumentException("Quadratic factor must be positive: " + quadratic);
		
		LightState l = lights[light];
		if (l.constAtt != constant || l.linAtt != linear || l.quadAtt != quadratic) {
			l.constAtt = constant;
			l.linAtt = linear;
			l.quadAtt = quadratic;
			glLightAttenuation(light, constant, linear, quadratic);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set a light's attenuation factors
	 */
	protected abstract void glLightAttenuation(int light, float constant, float linear, float quadratic);
	
	public void setLightingEnabled(boolean enable) {
		if (lightingEnabled != enable) {
			lightingEnabled = enable;
			glEnableLighting(enable);
		}
	}
	
	/**
	 * Invoke OpenGL calls to enable lighting
	 */
	protected abstract void glEnableLighting(boolean enable);
	
	public void setLightingModel(boolean smoothed, boolean twoSided) {
		if (lightingSmoothed != smoothed) {
			lightingSmoothed = smoothed;
			glEnableSmoothShading(smoothed);
		}
		
		if (lightingTwoSided != twoSided) {
			lightingTwoSided = twoSided;
			glEnableTwoSidedLighting(twoSided);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set smooth shading
	 */
	protected abstract void glEnableSmoothShading(boolean enable);
	
	/**
	 * Invoke OpenGL calls to set two-sided lighting
	 */
	protected abstract void glEnableTwoSidedLighting(boolean enable);
	
	public void setLineAntiAliasingEnabled(boolean enable) {
		if (lineAAEnabled != enable) {
			lineAAEnabled = enable;
			glEnableLineAntiAliasing(enable);
		}
	}
	
	/**
	 * Invoke OpenGL calls to enable line aa
	 */
	protected abstract void glEnableLineAntiAliasing(boolean enable);

	public void setLineSize(float width) {
		if (width < 1f)
			throw new IllegalArgumentException("Line width must be at least 1, not: " + width);
		if (lineWidth != width) {
			lineWidth = width;
			glLineWidth(width);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set line width
	 */
	protected abstract void glLineWidth(float width);

	public void setMaterial(Color4f amb, Color4f diff, Color4f spec, Color4f emm) {
		if (amb == null || diff == null || spec == null || emm == null)
			throw new NullPointerException("Material colors can't be null: " + amb + ", " + diff + ", " + spec + ", " + emm);
		if (!matAmbient.equals(amb)) {
			matAmbient.set(amb);
			glMaterialColor(LightColor.AMBIENT, amb);
		}
		
		if (!matDiffuse.equals(diff)) {
			matDiffuse.set(diff);
			glMaterialColor(LightColor.DIFFUSE, diff);
		}
		
		if (!matSpecular.equals(spec)) {
			matSpecular.set(spec);
			glMaterialColor(LightColor.SPECULAR, spec);
		}
		
		if (!matEmmissive.equals(emm)) {
			matEmmissive.set(emm);
			glMaterialColor(LightColor.EMISSIVE, emm);
		}
	}

	/**
	 * Invoke OpenGL calls to set the material color for the LightColor
	 */
	protected abstract void glMaterialColor(LightColor component, Color4f color);
	
	public void setMaterialShininess(float shininess) {
		if (shininess < 0f || shininess > 128f)
			throw new IllegalArgumentException("Shininess must be in [0, 128], not: " + shininess);
		if (matShininess != shininess) {
			matShininess = shininess;
			glMaterialShininess(shininess);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set the material shininess
	 */
	protected abstract void glMaterialShininess(float shininess);

	public void setPointAntiAliasingEnabled(boolean enable) {
		if (pointAAEnabled != enable) {
			pointAAEnabled = enable;
			glEnablePointAntiAliasing(enable);
		}
	}
	
	/**
	 * Invoke OpenGL calls to enable point aa
	 */
	protected abstract void glEnablePointAntiAliasing(boolean enable);

	public void setPointSize(float width) {
		if (width < 1f)
			throw new IllegalArgumentException("Point width must be at least 1, not: " + width);
		if (pointWidth != width) {
			pointWidth = width;
			glPointWidth(width);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set point width
	 */
	protected abstract void glPointWidth(float width);
	
	public void setPolygonAntiAliasingEnabled(boolean enable) {
		if (polyAAEnabled != enable) {
			polyAAEnabled = enable;
			glEnablePolyAntiAliasing(enable);
		}
	}
	
	/**
	 * Invoke OpenGL calls to enable polygon aa
	 */
	protected abstract void glEnablePolyAntiAliasing(boolean enable);
	
	public void setTexture(int tex, TextureImage image) {
		if (tex < 0 || tex >= textures.length)
			return; // ignore it
		
		TextureState t = textures[tex];
		if (t.image != image) {
			// update the low-level enabled state
			setTextureUnit(tex);

			if (image == null) {
				// always disable target on unbind (but don't clear our 'enabled' state)
				glEnableTexture(t.image.getTarget(), false);
				glBindTexture(t.image.getTarget(), null);
			} else if (t.image == null) {
				// enable based on current 'enabled' boolean
				glEnableTexture(image.getTarget(), t.enabled);
				glBindTexture(image.getTarget(), image);
			} else if (t.image.getTarget() != image.getTarget()) {
				// target types differ, unbind/disable old one and make new target enabled
				glEnableTexture(t.image.getTarget(), false);
				glBindTexture(t.image.getTarget(), null);
				
				// now enable and bind new image
				glEnableTexture(image.getTarget(), t.enabled);
				glBindTexture(image.getTarget(), image);
			} else {
				// targets are the same, so just bind the image
				// enabled status should be correct already
				glBindTexture(image.getTarget(), image);
			}
			
			// store new bound image
			t.image = image;
		}
	}
	
	/**
	 * Invoke OpenGL calls to bind a TextureImage to the active texture
	 */
	protected abstract void glBindTexture(TextureTarget target, TextureImage img);
	
	public void setTextureEnabled(int tex, boolean enable) {
		if (tex < 0 || tex >= textures.length)
			return; // ignore it
		
		TextureState t = textures[tex];
		if (t.enabled != enable) {
			t.enabled = enable;
			
			if (t.image != null) {
				// we only enable it when we have a bound texture,
				// setTextre() takes care of it when setting an image for the first time
				setTextureUnit(tex);
				glEnableTexture(t.image.getTarget(), enable);
			}
		}
	}
	
	/**
	 * Invoke OpenGL to enable the active texture unit
	 */
	protected abstract void glEnableTexture(TextureTarget target, boolean enable);

	public void setTextureColor(int tex, Color4f color) {
		if (tex < 0 || tex >= textures.length)
			return; // ignore it
		if (color == null)
			throw new NullPointerException("Texture color can't be null");
		
		TextureState t = textures[tex];
		if (!t.color.equals(color)) {
			t.color.set(color);
			setTextureUnit(tex);
			glTextureColor(color);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set the texture color for the active texture
	 */
	protected abstract void glTextureColor(Color4f color);

	public void setTextureCombineFunction(int tex, CombineFunction rgbFunc, CombineFunction alphaFunc) {
		if (tex < 0 || tex >= textures.length)
			return; // ignore it
		if (rgbFunc == null || alphaFunc == null)
			throw new NullPointerException("CombineFunctions can't be null");
		if (alphaFunc == CombineFunction.DOT3_RGB || alphaFunc == CombineFunction.DOT3_RGBA)
			throw new IllegalArgumentException("Alpha CombineFunction can't be DOT3_RGB or DOT3_RGBA");
		
		TextureState t = textures[tex];
		if (t.rgbFunc != rgbFunc) {
			t.rgbFunc = rgbFunc;
			setTextureUnit(tex);
			glCombineFunction(rgbFunc, true);
		}
		
		if (t.alphaFunc != alphaFunc) {
			t.alphaFunc = alphaFunc;
			setTextureUnit(tex);
			glCombineFunction(alphaFunc, false);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set a combine function, either rgb or alpha
	 */
	protected abstract void glCombineFunction(CombineFunction func, boolean rgb);
	
	public void setTextureCombineOpAlpha(int tex, int operand, CombineSource src, CombineOp op) {
		if (tex < 0 || tex >= textures.length)
			return; // ignore it
		if (src == null || op == null)
			throw new NullPointerException("CombineSource and CombineOp can't be null");
		if (operand < 0 || operand > 2)
			throw new IllegalArgumentException("Operand must be 0, 1, or 2");
		if (op == CombineOp.COLOR || op == CombineOp.ONE_MINUS_COLOR)
			throw new IllegalArgumentException("Illegal CombineOp for alpha: " + op);
		
		TextureState t = textures[tex];
		if (t.srcAlpha[operand] != src) {
			t.srcAlpha[operand] = src;
			setTextureUnit(tex);
			glCombineSrc(operand, src, false);
		}
		
		if (t.opAlpha[operand] != op) {
			t.opAlpha[operand] = op;
			setTextureUnit(tex);
			glCombineOp(operand, op, false);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set the combine source
	 */
	protected abstract void glCombineSrc(int operand, CombineSource src, boolean rgb);
	
	/**
	 * Invoke OpenGL calls to set the combine op
	 */
	protected abstract void glCombineOp(int operand, CombineOp op, boolean rgb);
	
	public void setTextureCombineOpRgb(int tex, int operand, CombineSource src, CombineOp op) {
		if (tex < 0 || tex >= textures.length)
			return; // ignore it
		if (src == null || op == null)
			throw new NullPointerException("CombineSource and CombineOp can't be null");
		if (operand < 0 || operand > 2)
			throw new IllegalArgumentException("Operand must be 0, 1, or 2");
		
		TextureState t = textures[tex];
		if (t.srcRgb[operand] != src) {
			t.srcRgb[operand] = src;
			setTextureUnit(tex);
			glCombineSrc(operand, src, true);
		}
		
		if (t.opRgb[operand] != op) {
			t.opRgb[operand] = op;
			setTextureUnit(tex);
			glCombineOp(operand, op, true);
		}
	}

	public void setTextureCoordGeneration(int tex, TexCoordSource gen) {
		setTextureCoordGeneration(tex, TexCoord.S, gen);
		setTextureCoordGeneration(tex, TexCoord.T, gen);
		setTextureCoordGeneration(tex, TexCoord.R, gen);
		setTextureCoordGeneration(tex, TexCoord.Q, gen);
	}

	public void setTextureCoordGeneration(int tex, TexCoord coord, TexCoordSource gen) {
		if (tex < 0 || tex >= textures.length)
			return; // ignore it
		if (coord == null)
			throw new NullPointerException("TexCoord can't be null");
		if (gen == null)
			throw new NullPointerException("TexCoordSource can't be null");
		
		TextureState t = textures[tex];
		switch(coord) {
		case S:
			if (t.tcS != gen) {
				setTextureUnit(tex);
				if (t.tcS == TexCoordSource.ATTRIBUTE)
					glEnableTexGen(coord, true);
				else if (gen == TexCoordSource.ATTRIBUTE)
					glEnableTexGen(coord, false);
				
				t.tcS = gen;
				glTexGen(coord, gen);
			}
			break;
		case R:
			if (t.tcR != gen) {
				setTextureUnit(tex);
				if (t.tcR == TexCoordSource.ATTRIBUTE)
					glEnableTexGen(coord, true);
				else if (gen == TexCoordSource.ATTRIBUTE)
					glEnableTexGen(coord, false);
				
				t.tcR = gen;
				glTexGen(coord, gen);
			}
			break;
		case T:
			if (t.tcT != gen) {
				setTextureUnit(tex);
				if (t.tcT == TexCoordSource.ATTRIBUTE)
					glEnableTexGen(coord, true);
				else if (gen == TexCoordSource.ATTRIBUTE)
					glEnableTexGen(coord, false);
				
				t.tcT = gen;
				glTexGen(coord, gen);
			}
			break;
		case Q:
			if (t.tcQ != gen) {
				setTextureUnit(tex);
				if (t.tcQ == TexCoordSource.ATTRIBUTE)
					glEnableTexGen(coord, true);
				else if (gen == TexCoordSource.ATTRIBUTE)
					glEnableTexGen(coord, false);
				
				t.tcQ = gen;
				glTexGen(coord, gen);
			}
			break;
		}
	}
	
	/**
	 * Invoke OpenGL to set the coordinate generation for the active texture
	 */
	protected abstract void glTexGen(TexCoord coord, TexCoordSource gen);
	
	/**
	 * Invoke OpenGL operations to enable/disable coord generation
	 */
	protected abstract void glEnableTexGen(TexCoord coord, boolean enable);

	public void setTextureEyePlane(int tex, TexCoord coord, Vector4f plane) {
		if (tex < 0 || tex >= textures.length)
			return; // ignore it
		if (plane == null)
			throw new NullPointerException("Eye plane cannot be null");
		if (coord == null)
			throw new NullPointerException("TexCoord cannot be null");
		
		// always send plane
		setTextureUnit(tex);
		glTexEyePlane(coord, plane);
		
	}
	
	/**
	 * Invoke OpenGL to set the eye plane for the given coordinate on the active texture
	 */
	protected abstract void glTexEyePlane(TexCoord coord, Vector4f plane);

	public void setTextureMode(int tex, EnvMode mode) {
		if (tex < 0 || tex >= textures.length)
			return; // ignore it
		if (mode == null)
			throw new NullPointerException("Must specify a non-null EnvMode");
		
		TextureState t = textures[tex];
		if (t.envMode != mode) {
			t.envMode = mode;
			setTextureUnit(tex);
			glTexEnvMode(mode);
		}
	}
	
	/**
	 * Invoke OpenGL to set the env mode for the active texture
	 */
	protected abstract void glTexEnvMode(EnvMode mode);

	public void setTextureObjectPlane(int tex, TexCoord coord, Vector4f plane) {
		if (tex < 0 || tex >= textures.length)
			return; // ignore it
		if (plane == null)
			throw new NullPointerException("Object plane cannot be null");
		if (coord == null)
			throw new NullPointerException("TexCoord cannot be null");
		
		TextureState t = textures[tex];
		switch(coord) {
		case S:
			if (!t.objPlaneS.equals(plane)) {
				t.objPlaneS.set(plane);
				setTextureUnit(tex);
				glTexObjPlane(coord, plane);
			}
			break;
		case T:
			if (!t.objPlaneT.equals(plane)) {
				t.objPlaneT.set(plane);
				setTextureUnit(tex);
				glTexObjPlane(coord, plane);
			}
			break;
		case R:
			if (!t.objPlaneR.equals(plane)) {
				t.objPlaneR.set(plane);
				setTextureUnit(tex);
				glTexObjPlane(coord, plane);
			}
			break;
		case Q:
			if (!t.objPlaneQ.equals(plane)) {
				t.objPlaneQ.set(plane);
				setTextureUnit(tex);
				glTexObjPlane(coord, plane);
			}
			break;
		}
	}
	
	/**
	 * Invoke OpenGL to set the object plane for the active texture
	 */
	protected abstract void glTexObjPlane(TexCoord coord, Vector4f plane);
	
	public void setTextureTransform(int tex, Matrix4f matrix) {
		if (tex < 0 || tex >= textures.length)
			return; // ignore it
		if (matrix == null)
			matrix = IDENTITY;
		
		setTextureUnit(tex);
		setMatrixMode(MatrixMode.TEXTURE);
		glSetMatrix(matrix);
	}
	
	private void setTextureUnit(int unit) {
		if (unit != activeTex) {
			activeTex = unit;
			glActiveTexture(unit);
		}
	}
	
	/**
	 * Invoke OpenGL calls to set the active texture unit
	 */
	protected abstract void glActiveTexture(int unit);
	
	public void setNormalBinding(String name) {
		normalBinding = name;
	}
	
	public void setVertexBinding(String name) {
		vertexBinding = name;
	}
	
	public void setTextureCoordinateBinding(int tex, String name) {
		if (tex < 0 || tex > texBindings.length)
			return; // ignore it
		
		texBindings[tex] = name;
	}
	
	public void setBindings(String vertices, String normals, String[] texCoords) {
		vertexBinding = vertices;
		normalBinding = normals;
		
		int len = Math.min(texBindings.length, texCoords.length);
		Arrays.fill(texBindings, null); // unbind everything
		for (int i = 0; i < len; i++) {
			texBindings[i] = texCoords[i];
		}
	}
}
