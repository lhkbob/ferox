package com.ferox.effect;

import com.ferox.math.Color4f;

public class FixedFunctionShader extends OpenGlShader<FixedFunctionShader> {
	// alpha test
	private Comparison alphaTest;
	private float alphaReferenceValue;
	
	// fog
	private final Fog fog;
	
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
		setLightingEnabled(false);
		setSeparateSpecularEnabled(false);
		setLocalViewerEnabled(false);
		setTwoSidedLightingEnabled(false);
		setSmoothShaded(true);
		
		// leave geometry unbound
	}
	
	public String getVertexBinding() {
		return vertexBinding;
	}
	
	public FixedFunctionShader setVertexBinding(String vertices) {
		vertexBinding = vertices;
		return this;
	}
	
	public String getNormalBinding() {
		return normalBinding;
	}
	
	public FixedFunctionShader setNormalBinding(String normals) {
		normalBinding = normals;
		return this;
	}
	
	public String getTextureCoordinateBinding(int tc) {
		return texCoords[tc];
	}
	
	public FixedFunctionShader setTextureCoordinateBinding(int tc, String binding) {
		texCoords[tc] = binding;
		return this;
	}
	
	public FixedFunctionShader setGeometryBindings(String vertices, String normals, String... texCoords) {
		setVertexBinding(vertices);
		setNormalBinding(normals);
		if (texCoords != null) {
			for (int i = 0; i < texCoords.length; i++)
				setTextureCoordinateBinding(i, texCoords[i]);
		}
		
		return this;
	}
	
	public FixedFunctionShader setMaterial(Color4f diffuse, Color4f specular, float shininess) {
		material.setDiffuse(diffuse).setSpecular(specular).setShininess(shininess);
		return this;
	}
	
	public Material getMaterial() {
		return material;
	}
	
	public boolean isSmoothShaded() {
		return glSmoothShaded;
	}
	
	public FixedFunctionShader setSmoothShaded(boolean smooth) {
		glSmoothShaded = smooth;
		return this;
	}
	
	public int getNumLights() {
		return lights.length;
	}
	
	public Light getLight(int l) {
		return lights[l];
	}
	
	public boolean isLightEnabled(int l) {
		return lights[l].isEnabled();
	}
	
	public int getNumTextureUnits() {
		return texEnv.length;
	}
	
	public TextureEnvironment getTexture(int t) {
		return texEnv[t];
	}
	
	public boolean isTextureEnabled(int t) {
		return texEnv[t].isEnabled() && texEnv[t].getImage() != null;
	}
	
	public Fog getFog() {
		return fog;
	}
	
	public Comparison getAlphaTest() {
		return alphaTest;
	}
	
	public float getAlphaReferenceValue() {
		return alphaReferenceValue;
	}
	
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

	public FixedFunctionShader setAmbientColor(Color4f globalAmb) {
		if (globalAmb == null)
			globalAmbient.set(.2f, .2f, .2f, 1f);
		else
			globalAmbient.set(globalAmb);
		return this;
	}
}
