package com.ferox.core.system;

public class SystemCapabilities {
	private int maxVertexShaderTextureUnits;
	private int maxFragmentShaderTextureUnits;
	private int maxFFPTextureUnits;
	private int maxCombinedTextureUnits;
	private int maxLights;
	private int maxVertexAttributes;
	private int maxTextureCoordinates;
	private int maxRecommendedIndices;
	private int maxRecommendedVertices;
	private float maxAnisoLevel;
	
	private String glVersion;
	private float versionNumber;
	
	private boolean fboSupported;
	private boolean glslSupported;
	private boolean vboSupported;
	private boolean multiTexSupported;
	private boolean separateSpecularLightingSupported;
	private boolean cubeMapSupport;
	private boolean threeDSupport;
	private boolean pboSupported;
	
	private boolean floatingPointTextures;
	private boolean npotTextures;
	private boolean rectTextures;
	private boolean s3tcTextures;
	
	public SystemCapabilities(int maxVertexShaderTextureUnits,
			int maxFragmentShaderTextureUnits, int maxCombinedTextureUnits,
			int maxFFPTextureUnits,
			int maxLights, int maxVertexAttributes,
			int maxTextureCoordinates, int maxVertices, int maxIndices, float maxAnisoLevel,
			String glVersion, float versionNumber,
			boolean fboSupported, boolean glslSupported, boolean vboSupported,
			boolean pboSupported, boolean multiTexSupported,
			boolean separateSpecularLightingSupported, boolean cubeMapSupport,
			boolean threeDSupport,
			boolean fpTextures, boolean npotTextures, boolean rectTextures,
			boolean s3tcTextures) {
		this.maxVertexShaderTextureUnits = maxVertexShaderTextureUnits;
		this.maxFragmentShaderTextureUnits = maxFragmentShaderTextureUnits;
		this.maxFFPTextureUnits = maxFFPTextureUnits;
		this.maxCombinedTextureUnits = maxCombinedTextureUnits;
		this.maxLights = maxLights;
		this.maxVertexAttributes = maxVertexAttributes;
		this.maxTextureCoordinates = maxTextureCoordinates;
		this.maxRecommendedIndices = maxIndices;
		this.maxRecommendedVertices = maxVertices;
		this.pboSupported = pboSupported;
		this.glVersion = glVersion;
		this.versionNumber = versionNumber;
		this.fboSupported = fboSupported;
		this.glslSupported = glslSupported;
		this.vboSupported = vboSupported;
		this.multiTexSupported = multiTexSupported;
		this.separateSpecularLightingSupported = separateSpecularLightingSupported;
		this.cubeMapSupport = cubeMapSupport;
		this.threeDSupport = threeDSupport;
		this.floatingPointTextures = fpTextures;
		this.npotTextures = npotTextures;
		this.rectTextures = rectTextures;
		this.s3tcTextures = s3tcTextures;
	}
	
	public boolean arePixelBuffersSupported() {
		return this.pboSupported;
	}
	
	public float getMaxAnisotropicFilterLevel() {
		return this.maxAnisoLevel;
	}
	
	public int getMaxRecommendedIndices() {
		return this.maxRecommendedIndices;
	}

	public int getMaxRecommendedVertices() {
		return this.maxRecommendedVertices;
	}

	public boolean areUnclampedFloatTexturesSupported() {
		return this.floatingPointTextures;
	}

	public boolean areNpotTexturesSupported() {
		return this.npotTextures;
	}

	public boolean areRectangularTexturesSupported() {
		return this.rectTextures;
	}
	
	public boolean isS3TCSupported() {
		return this.s3tcTextures;
	}

	public float getVersionNumber() {
		return this.versionNumber;
	}

	public boolean isMultiTexturingSupported() {
		return this.multiTexSupported;
	}
	
	public String getGLVersion() {
		return this.glVersion;
	}
	
	public boolean isFBOSupported() {
		return this.fboSupported;
	}

	public int getMaxVertexShaderTextureUnits() {
		return this.maxVertexShaderTextureUnits;
	}
	
	public int getMaxFragmentShaderTextureUnits() {
		return this.maxFragmentShaderTextureUnits;
	}
	
	public int getMaxFixedFunctionTextureUnits() {
		return this.maxFFPTextureUnits;
	}
	
	public int getMaxCombinedTextureUnits() {
		return this.maxCombinedTextureUnits;
	}
	
	public int getMaxLights() {
		return this.maxLights;
	}
	
	public int getMaxVertexAttributes() {
		return this.maxVertexAttributes;
	}
	
	public int getMaxTextureCoordinates() {
		return this.maxTextureCoordinates;
	}
	
	public boolean areGLSLShadersSupported() {
		return this.glslSupported;
	}
	
	public boolean areVertexBuffersSupported() {
		return this.vboSupported;
	}
	
	public boolean isSeparateSpecularLightingSupported() {
		return this.separateSpecularLightingSupported;
	}
	
	public boolean isCubeMapTexturingSupported() {
		return this.cubeMapSupport;
	}
	
	public boolean is3DTexturingSupported() {
		return this.threeDSupport;
	}
}
