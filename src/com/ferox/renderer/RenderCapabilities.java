package com.ferox.renderer;

/**
 * RenderCapabilities holds onto a set of different
 * parameters describing more advanced features that the
 * baseline hardware may not support.
 * 
 * @author Michael Ludwig
 *
 */
public class RenderCapabilities {
	// texture properties
	private int maxVertexShaderTextures;
	private int maxFragmentShaderTextures;
	private int maxFixedPipelineTextures;
	private int maxCombinedTextures;
	private float maxAnisoLevel;
	
	// texture + renderbuffer dimensions
	private int maxTextureSize;
	private int maxTextureRectSize;
	private int maxTextureCubeMapSize;
	private int maxTexture3DSize;
	private int maxRenderbufferSize;
	
	// type of supported textures
	private boolean fpTextures;
	private boolean npotTextures;
	private boolean rectTextures;
	private boolean s3tcTextures;
	
	// geometry properties
	private int maxVertexAttributes;
	private int maxTextureCoordinates;
	private int maxRecommendedIndices;
	private int maxRecommendedVertices;
	private boolean vboSupported;
	
	// misc
	private int maxActiveLights;
	private boolean glslSupported;
	private boolean pointSpriteSupport;
	
	// frame properties
	private boolean fboSupported;
	private boolean pbuffersSupported;
	private int maxColorTargets;
	
	// version
	private String vendor;
	private float version;
	
	/** Create a render capabilities object with the given properties.  There is no
	 * validation checking, it is assumed that renderer's will be responsible and fill
	 * each value with a meaningful number, etc. */
	public RenderCapabilities(int maxVertexShaderTextures,
			int maxFragmentShaderTextures, int maxFixedPipelineTextures,
			int maxCombinedTextures, float maxAnisoLevel, 
			int maxTextureSize, int maxTextureRectSize, int maxTextureCubeMapSize, 
			int maxTexture3DSize, int maxRenderbufferSize,
			boolean fpTextures,	boolean npotTextures, boolean rectTextures, boolean s3tcTextures,
			int maxVertexAttributes, int maxTextureCoordinates,
			int maxRecommendedIndices, int maxRecommendedVertices,
			boolean vboSupported, int maxActiveLights, boolean pointSpriteSupport,
			boolean glslSupported, boolean fboSupported, boolean pbuffersSupported, int maxColorTargets,
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
	
	/** Return whether or not points can actually be rendered as
	 * point sprites on the current hardware. */
	public boolean getPointSpriteSupport() {
		return this.pointSpriteSupport;
	}
	
	/** Return the maximum side length of a Texture1D or
	 * Texture2D. */
	public int getMaxTextureSize() {
		return this.maxTextureSize;
	}
	
	/** Return the maximum side length of a TextureRectangle. */
	public int getMaxTextureRectangleSize() {
		return this.maxTextureRectSize;
	}
	
	/** Return the maximum side length of a Texture3D. */
	public int getMaxTexture3DSize() {
		return this.maxTexture3DSize;
	}
	
	/** Return the maximum side length of a face of a TextureCubeMap. */
	public int getMaxTextureCubeMapSize() {
		return this.maxTextureCubeMapSize;
	}
	
	/** Return the maximum side length of a texture used with a TextureSurface. */
	public int getMaxRenderbufferSize() {
		return this.maxRenderbufferSize;
	}
	
	/** Return the maximum number of color buffers that can
	 * be rendered into simultaneously with a GLSL program. */
	public int getMaxColorTargets() {
		return this.maxColorTargets;
	}
	
	/** Get max number of textures allowed in the vertex shader stage of a glsl program.
	 * Should be set to 0 if glsl programs aren't allowed, otherwise >= 0. */
	public int getMaxVertexShaderTextures() {
		return this.maxVertexShaderTextures;
	}
	
	/** Get the max number of textures allowed in the fragment shader of glsl program. 
	 * Should be set to 0 if glsl programs aren't allowed, otherwise >= 0. */
	public int getMaxFragmentShaderTextures() {
		return this.maxFragmentShaderTextures;
	}
	
	/** Get the max number of textures usable when fixed function RenderQueue is enabled.
	 * Textures beyond this will be ignored when no glsl program is bound. */
	public int getMaxFixedPipelineTextures() {
		return this.maxFixedPipelineTextures;
	}
	
	/** Get the max number of textures allowed in an entire shader program.  Each reference
	 * to a texture increases the total for a given program. */
	public int getMaxCombinedTextures() {
		return this.maxCombinedTextures;
	}
	
	/** Get the max supported level of anisotropic filtering for textures. 
	 * Should be >= 0. */
	public float getMaxAnisotropicLevel() {
		return this.maxAnisoLevel;
	}
	
	/** Whether or not unclamped floating point textures are supported.  If false,
	 * float texture values are clamped to be within 0 to 1. */
	public boolean getUnclampedFloatTextureSupport() {
		return this.fpTextures;
	}
	
	/** Whether or not non-power of two dimensions are supported for textures. */
	public boolean getNpotTextureSupport() {
		return this.npotTextures;
	}
	
	/** Whether or not rectangular textures are supported (if false, textures must be squares).
	 * Rectangular textures and square textures use different texture coordinates for access. */
	public boolean getRectangularTextureSupport() {
		return this.rectTextures;
	}
	
	/** Whether or not the S3TC extension is present.  This allows for DXT1, DXT3, and DXT5 texture
	 * compression on the graphics card. */
	public boolean getS3TextureCompression() {
		return this.s3tcTextures;
	}
	
	/** Get the maximum vertex attributes allowed on each vertex rendered. 
	 * Should be >= 0. */
	public int getMaxVertexAttributes() {
		return this.maxVertexAttributes;
	}
	
	/** Get the maximum number of texture coordinates for each vertex.  This
	 * may be different then the allowed number of textures in fixed RenderQueue or
	 * graphics card. */
	public int getMaxTextureCoordinates() {
		return this.maxTextureCoordinates;
	}
	
	/** Get the max recommended indices in rendered geometry.  Numbers greater than this
	 * are supported, but they may suffer performance issues (not as optimized). */
	public int getMaxRecommendedIndices() {
		return this.maxRecommendedIndices;
	}
	
	/** Get the max recommended vertices in rendered geometry.  Numbers greater
	 * than this are supported, but they may suffer from less optimization/caching. */
	public int getMaxRecommendedVertices() {
		return this.maxRecommendedVertices;
	}
	
	/** Whether or not vertex buffers are supported.  If not supported, VertexBufferObjects
	 * and VertexBufferGeometries will result in errors. */
	public boolean getVertexBufferSupport() {
		return this.vboSupported;
	}
	
	/** Get the maximum number of lights that can affect a rendered object at one time. */
	public int getMaxActiveLights() {
		return this.maxActiveLights;
	}
	
	/** Whether or not GLSL shaders are supported. */
	public boolean getGlslSupport() {
		return this.glslSupported;
	}
	
	/** Whether or not offscreen surfaces can be implemented using frame buffer objects,
	 * significantly faster (especially when true context sharing is used). */
	public boolean getFboSupport() {
		return this.fboSupported;
	}
	
	/** Whether or not pbuffers (different than pixel buffers) are supported for
	 * offscreen surfaces.  Pbuffers are slower than fbos but are sometimes necessary
	 * when no other context is available. */
	public boolean getPbufferSupport() {
		return this.pbuffersSupported;
	}
	
	/** Get the vendor returned string that describes the opengl drivers on 
	 * installed on the computer. */
	public String getVendor() {
		return this.vendor;
	}
	
	/** Get the opengl version present on the computer. */
	public float getVersion() {
		return this.version;
	}
}
