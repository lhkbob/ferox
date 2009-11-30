package com.ferox.renderer;

/**
 * <p>
 * RenderCapabilities holds onto a set of different parameters describing more
 * advanced features that the baseline hardware may not support.
 * </p>
 * <p>
 * Because Ferox was designed to be implemented with an OpenGL system, the
 * capabilities here reflect that and allow for lower-level inspection of the
 * current hardware.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class RenderCapabilities {
	// texture properties
	private final int maxVertexShaderTextures;
	private final int maxFragmentShaderTextures;
	private final int maxFixedPipelineTextures;
	private final int maxCombinedTextures;
	private final float maxAnisoLevel;

	// texture + renderbuffer dimensions
	private final int maxTextureSize;
	private final int maxTextureRectSize;
	private final int maxTextureCubeMapSize;
	private final int maxTexture3DSize;
	private final int maxRenderbufferSize;

	// type of supported textures
	private final boolean fpTextures;
	private final boolean npotTextures;
	private final boolean rectTextures;
	private final boolean s3tcTextures;

	// geometry properties
	private final int maxVertexAttributes;
	private final int maxTextureCoordinates;
	private final int maxRecommendedIndices;
	private final int maxRecommendedVertices;
	private final boolean vboSupported;

	// misc
	private final int maxActiveLights;
	private final boolean glslSupported; // FIXME: add glsl language version?
	private final boolean pointSpriteSupport; // FIXME: to be removed

	// frame properties
	private final boolean fboSupported;
	private final boolean pbuffersSupported;
	private final int maxColorTargets;

	// version
	private final String vendor;
	private final float version;
	
	// FIXME: add booleans for ffp and glsl renderer support

	/**
	 * Create a render capabilities object with the given properties. There is
	 * no validation checking, it is assumed that renderer's will be responsible
	 * and fill each value with a meaningful number, etc.
	 */
	public RenderCapabilities(int maxVertexShaderTextures, int maxFragmentShaderTextures, 
							  int maxFixedPipelineTextures, int maxCombinedTextures, 
							  float maxAnisoLevel, int maxTextureSize, int maxTextureRectSize,
							  int maxTextureCubeMapSize, int maxTexture3DSize, int maxRenderbufferSize,
							  boolean fpTextures, boolean npotTextures, boolean rectTextures, 
							  boolean s3tcTextures, int maxVertexAttributes, int maxTextureCoordinates, 
							  int maxRecommendedIndices, int maxRecommendedVertices, boolean vboSupported, 
							  int maxActiveLights, boolean pointSpriteSupport, boolean glslSupported, 
							  boolean fboSupported, boolean pbuffersSupported, int maxColorTargets, 
							  String vendor, float version) {
		this.maxVertexShaderTextures = maxVertexShaderTextures;
		this.maxFragmentShaderTextures = maxFragmentShaderTextures;
		this.maxFixedPipelineTextures = maxFixedPipelineTextures;
		this.maxCombinedTextures = maxCombinedTextures;
		this.maxAnisoLevel = maxAnisoLevel;

		this.maxTextureSize = maxTextureSize;
		this.maxTextureCubeMapSize = maxTextureCubeMapSize;
		this.maxTextureRectSize = maxTextureRectSize;
		this.maxTexture3DSize = maxTexture3DSize;
		this.maxRenderbufferSize = maxRenderbufferSize;

		this.fpTextures = fpTextures;
		this.npotTextures = npotTextures;
		this.rectTextures = rectTextures;
		this.s3tcTextures = s3tcTextures;

		this.maxVertexAttributes = maxVertexAttributes;
		this.maxTextureCoordinates = maxTextureCoordinates;
		this.maxRecommendedIndices = maxRecommendedIndices;
		this.maxRecommendedVertices = maxRecommendedVertices;
		this.vboSupported = vboSupported;

		this.maxActiveLights = maxActiveLights;
		this.glslSupported = glslSupported;
		this.pointSpriteSupport = pointSpriteSupport;

		this.fboSupported = fboSupported;
		this.pbuffersSupported = pbuffersSupported;
		this.maxColorTargets = maxColorTargets;

		this.vendor = vendor;
		this.version = version;
	}

	/**
	 * Return whether or not points can actually be rendered as point sprites on
	 * the current hardware.
	 * 
	 * @return True if points can be rendered as point sprites
	 */
	public boolean getPointSpriteSupport() {
		return pointSpriteSupport;
	}

	/**
	 * Return the maximum side length of a Texture1D or Texture2D.
	 * 
	 * @return Maximum size of a 1d or 2d texture
	 */
	public int getMaxTextureSize() {
		return maxTextureSize;
	}

	/**
	 * Return the maximum side length of a TextureRectangle.
	 * 
	 * @return Maximum size of a rectangular texture
	 */
	public int getMaxTextureRectangleSize() {
		return maxTextureRectSize;
	}

	/**
	 * Return the maximum side length of a Texture3D.
	 * 
	 * @return Maximum size of a 3d texture
	 */
	public int getMaxTexture3DSize() {
		return maxTexture3DSize;
	}

	/**
	 * Return the maximum side length of a face of a TextureCubeMap.
	 * 
	 * @return Maximum size of a cube map
	 */
	public int getMaxTextureCubeMapSize() {
		return maxTextureCubeMapSize;
	}

	/**
	 * Return the maximum side length of a texture used with a TextureSurface.
	 * 
	 * @return Maximum dimension of a TextureSurface
	 */
	public int getMaxRenderbufferSize() {
		return maxRenderbufferSize;
	}

	/**
	 * Return the maximum number of color buffers that can be rendered into
	 * simultaneously with a GLSL program.
	 * 
	 * @return Number of color targets allowed for TextureSurfaces
	 */
	public int getMaxColorTargets() {
		return maxColorTargets;
	}

	/**
	 * Get max number of textures allowed in the vertex shader stage of a glsl
	 * program. Should be set to 0 if glsl programs aren't allowed, otherwise >=
	 * 0.
	 * 
	 * @return Number of textures allowed in a vertex shader
	 */
	public int getMaxVertexShaderTextures() {
		return maxVertexShaderTextures;
	}

	/**
	 * Get the max number of textures allowed in the fragment shader of glsl
	 * program. Should be set to 0 if glsl programs aren't allowed, otherwise >=
	 * 0.
	 * 
	 * @return Number of textures allowed in a fragment shader
	 */
	public int getMaxFragmentShaderTextures() {
		return maxFragmentShaderTextures;
	}

	/**
	 * Get the max number of textures usable when fixed function RenderQueue is
	 * enabled. Textures beyond this will be ignored when no glsl program is
	 * bound.
	 * 
	 * @return Total number of textures usable in fixed-function
	 */
	public int getMaxFixedPipelineTextures() {
		return maxFixedPipelineTextures;
	}

	/**
	 * Get the max number of textures allowed in an entire shader program. Each
	 * reference to a texture increases the total for a given program.
	 * 
	 * @return Total number of texture samplers in a shader
	 */
	public int getMaxCombinedTextures() {
		return maxCombinedTextures;
	}

	/**
	 * Get the max supported level of anisotropic filtering for textures. Should
	 * be >= 0.
	 * 
	 * @return Maximum level of anistropic filtering
	 */
	public float getMaxAnisotropicLevel() {
		return maxAnisoLevel;
	}

	/**
	 * Whether or not unclamped floating point textures are supported. If false,
	 * float texture values are clamped to be within 0 to 1.
	 * 
	 * @return If full floating point textures can be stored
	 */
	public boolean getUnclampedFloatTextureSupport() {
		return fpTextures;
	}

	/**
	 * Whether or not non-power of two dimensions are supported for textures.
	 * 
	 * @return If NPOT texturing is available for 1d, 2d, 3d and cube map
	 *         textures
	 */
	public boolean getNpotTextureSupport() {
		return npotTextures;
	}

	/**
	 * Whether or not rectangular textures are supported (if false, textures
	 * must be squares). Rectangular textures and square textures use different
	 * texture coordinates for access.
	 * 
	 * @return If the rectangular texture target is allowed
	 */
	public boolean getRectangularTextureSupport() {
		return rectTextures;
	}

	/**
	 * Whether or not the S3TC extension is present. This allows for DXT1, DXT3,
	 * and DXT5 texture compression on the graphics card.
	 * 
	 * @return If DXT texture compression is supported
	 */
	public boolean getS3TextureCompression() {
		return s3tcTextures;
	}

	/**
	 * Get the maximum vertex attributes allowed on each vertex rendered. Should
	 * be >= 0.
	 * 
	 * @return Number of vertex attributes
	 */
	public int getMaxVertexAttributes() {
		return maxVertexAttributes;
	}

	/**
	 * Get the maximum number of texture coordinates for each vertex. This may
	 * be different then the allowed number of textures in fixed RenderQueue or
	 * graphics card.
	 * 
	 * @return Number of texture coordinates
	 */
	public int getMaxTextureCoordinates() {
		return maxTextureCoordinates;
	}

	/**
	 * Get the max recommended indices in rendered geometry. Numbers greater
	 * than this are supported, but they may suffer performance issues (not as
	 * optimized).
	 * 
	 * @return Recommended index size for vbo geometry
	 */
	public int getMaxRecommendedIndices() {
		return maxRecommendedIndices;
	}

	/**
	 * Get the max recommended vertices in rendered geometry. Numbers greater
	 * than this are supported, but they may suffer from less
	 * optimization/caching.
	 * 
	 * @return Recommended vertex size for vbo geometry
	 */
	public int getMaxRecommendedVertices() {
		return maxRecommendedVertices;
	}

	/**
	 * Whether or not vertex buffers are supported.
	 * 
	 * @return True if the VBO compile option will work
	 */
	public boolean getVertexBufferSupport() {
		return vboSupported;
	}

	/**
	 * Get the maximum number of lights that can affect a rendered object at one
	 * time.
	 * 
	 * @return Total number of simultaneous lights
	 */
	public int getMaxActiveLights() {
		return maxActiveLights;
	}

	/**
	 * Whether or not GLSL shaders are supported.
	 * 
	 * @return True if shaders can be used
	 */
	public boolean getGlslSupport() {
		return glslSupported;
	}

	/**
	 * Whether or not offscreen surfaces can be implemented using frame buffer
	 * objects, significantly faster (especially when true context sharing is
	 * used).
	 * 
	 * @return True if fbos can be used
	 */
	public boolean getFboSupport() {
		return fboSupported;
	}

	/**
	 * Whether or not pbuffers (different than pixel buffers) are supported for
	 * offscreen surfaces. Pbuffers are slower than fbos but are sometimes
	 * necessary when no other context is available.
	 * 
	 * @return True if pbuffers can be used
	 */
	public boolean getPbufferSupport() {
		return pbuffersSupported;
	}

	/**
	 * Get the vendor returned string that describes the opengl drivers on
	 * installed on the computer.
	 * 
	 * @return Implementation vendor description
	 */
	public String getVendor() {
		return vendor;
	}

	/**
	 * Get the opengl version present on the computer.
	 * 
	 * @return Version to one decimal point
	 */
	public float getVersion() {
		return version;
	}
}
