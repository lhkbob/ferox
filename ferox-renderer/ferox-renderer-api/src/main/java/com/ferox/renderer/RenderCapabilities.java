package com.ferox.renderer;

import java.util.EnumSet;

import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.GlslShader.Version;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;

/**
 * <p>
 * RenderCapabilities holds onto a set of different parameters describing more
 * advanced features that the baseline hardware may not support.
 * <p>
 * Because Ferox was designed to be implemented with an OpenGL system, the
 * capabilities here reflect that and allow for lower-level inspection of the
 * current hardware. Framework implementations are expected to extend
 * RenderCapabilities to provide the correct values for each parameter.
 * 
 * @author Michael Ludwig
 */
public class RenderCapabilities {
    // texture properties
    protected int maxVertexShaderTextures = 0; //
    protected int maxFragmentShaderTextures = 0; //
    protected int maxFixedPipelineTextures = 0; //
    protected int maxCombinedTextures = 0; //
    
    protected float maxAnisoLevel = 0f; //
    
    protected boolean hasDepthTextures = false; //
    protected boolean hasEnvCombine = false; //
    protected boolean hasMirrorRepeat = false; //
    protected boolean hasClampEdge = false; //

    // texture + renderbuffer dimensions
    protected int maxTextureSize = 0; //
    protected int maxTextureCubeMapSize = 0; //
    protected int maxTexture3DSize = 0; //
    protected int maxRenderbufferSize = 0; //

    // type of supported textures
    protected boolean fpTextures = false; //
    protected boolean npotTextures = false; //
    protected boolean s3tcTextures = false; //
    
    protected EnumSet<Target> supportedTargets = EnumSet.noneOf(Target.class); //

    // geometry properties
    protected int maxVertexAttributes = 0; //
    protected int maxTextureCoordinates = 0; //
    protected boolean vboSupported = false; // 

    // misc
    protected int maxActiveLights = 0; //
    protected boolean blendSupported = false; //
    protected boolean hasSeparateBlend = false; //
    protected boolean hasSeparateStencil = false; //
    
    protected boolean hasFfpRenderer = false; //
    
    // glsl
    protected boolean hasGlslRenderer = false; //
    protected EnumSet<ShaderType> supportedShaders = EnumSet.noneOf(ShaderType.class);
    protected Version glslVersion;
    
    protected String vendor = ""; // 
    protected float version = 0f; // 
    
    // frame properties
    protected boolean fboSupported = false; //
    protected boolean pbuffersSupported = false; //
    protected int maxColorTargets = 0; //
    
    /**
     * @return True if the blending operation exposed in {@link Renderer} is
     *         supported.
     */
    public boolean isBlendingSupported() {
        return blendSupported;
    }
    
    /**
     * @return The set of supported texture Targets on this hardware.
     */
    public EnumSet<Target> getSupportedTextureTargets() {
        return supportedTargets;
    }

    /**
     * @return True if the DEPTH TextureFormat is supported
     */
    public boolean getDepthTextureSupport() {
        return hasDepthTextures;
    }
    
    /**
     * @return True if the COMBINE EnvMode is supported by FixedFunctionRenderers
     */
    public boolean getCombineEnvModeSupport() {
        return hasEnvCombine;
    }
    
    /**
     * @return True if the MIRROR WrapMode is supported
     */
    public boolean getMirrorWrapModeSupport() {
        return hasMirrorRepeat;
    }

    /**
     * @return True if the CLAMP WrapMode can use the GL_CLAMP_TO_EDGE extension,
     *         which improves appearance, or false when it must fallback to
     *         GL_CLAMP
     */
    public boolean getClampToEdgeSupport() {
        return hasClampEdge;
    }
    
    /**
     * @return True if blending can be correctly separated across front and back
     *         facing polygons.
     */
    public boolean getSeparateBlendSupport() {
        return hasSeparateBlend;
    }

    /**
     * @return True if stencil operations can be correctly separated across
     *         front and back facing polygons.
     */
    public boolean getSeparateStencilSupport() {
        return hasSeparateStencil;
    }
    
    /**
     * Return the maximum side dimension of a Texture with targets of T_1D or
     * T_2D.
     * 
     * @return Maximum size of a 1d or 2d texture
     */
    public int getMaxTextureSize() {
        return maxTextureSize;
    }

    /**
     * Return the maximum side dimension of a Texture with the target of T_3D.
     * 
     * @return Maximum size of a 3d texture
     */
    public int getMaxTexture3DSize() {
        return maxTexture3DSize;
    }

    /**
     * Return the maximum side dimension of a face of a Texture with the target
     * of T_CUBEMAP.
     * 
     * @return Maximum size of a cube map
     */
    public int getMaxTextureCubeMapSize() {
        return maxTextureCubeMapSize;
    }

    /**
     * Return the maximum dimension of any Texture used with a TextureSurface.
     * 
     * @return Maximum dimension of a TextureSurface
     */
    public int getMaxTextureSurfaceSize() {
        return maxRenderbufferSize;
    }

    /**
     * Return the maximum number of color buffers that can be rendered into
     * simultaneously with a GLSL program when using a TextureSurface. An
     * OnscreenSurface always has only one color buffer.
     * 
     * @return Number of color targets allowed for TextureSurfaces
     */
    public int getMaxColorBuffers() {
        return maxColorTargets;
    }

    /**
     * Get max number of textures allowed in the vertex shader stage of a GLSL
     * program. This will return a number <= 0 if GLSL shaders are not
     * supported.
     * 
     * @return Number of textures allowed in a vertex shader
     */
    public int getMaxVertexShaderTextures() {
        return maxVertexShaderTextures;
    }

    /**
     * Get the max number of textures allowed in the fragment shader of GLSL
     * program. This will return a number <= 0 if GLSL shaders are not
     * supported.
     * 
     * @return Number of textures allowed in a fragment shader
     */
    public int getMaxFragmentShaderTextures() {
        return maxFragmentShaderTextures;
    }

    /**
     * Get the max number of textures usable by a {@link FixedFunctionRenderer}.
     * Textures beyond this will be ignored when using a fixed function
     * renderer, GLSL renderers may support more available textures.
     * 
     * @return Total number of textures usable in fixed-function
     */
    public int getMaxFixedPipelineTextures() {
        return maxFixedPipelineTextures;
    }

    /**
     * Get the max number of textures used by an entire GLSL program. This may
     * be less than the sum of {@link #getMaxVertexShaderTextures()} and
     * {@link #getMaxFragmentShaderTextures()}.
     * 
     * @return Total number of texture samplers in a GLSL program
     */
    public int getMaxCombinedTextures() {
        return maxCombinedTextures;
    }

    /**
     * Get the max supported level of anisotropic filtering for textures. If
     * anisotropic filtering is not supported, this should return a number <= 0.
     * A value of 1 in {@link Texture#getAnisotropicFilterLevel()} will be
     * scaled by the Framework to the returned number.
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
     * be different then the maximum number of textures.
     * 
     * @return Number of texture coordinates
     */
    public int getMaxTextureCoordinates() {
        return maxTextureCoordinates;
    }

    /**
     * Whether or not vertex buffers are supported.
     * 
     * @return True if the RESIDENT_x compile options will be supported
     */
    public boolean getVertexBufferSupport() {
        return vboSupported;
    }

    /**
     * Get the maximum number of lights that can affect a rendered object at one
     * time when using a {@link FixedFunctionRenderer}.
     * 
     * @return Total number of simultaneous lights
     */
    public int getMaxActiveLights() {
        return maxActiveLights;
    }

    /**
     * Whether or not this Framework can provide Renderers that implement
     * {@link GlslRenderer}.
     * 
     * @return True if shaders can be used
     */
    public boolean hasGlslRenderer() {
        return hasGlslRenderer;
    }

    /**
     * Whether or not this Framework can provide Renderers that implement
     * {@link FixedFunctionRenderer}.
     * 
     * @return True if fixed-function pipeline can be used
     */
    public boolean hasFixedFunctionRenderer() {
        return hasFfpRenderer;
    }

    /**
     * Whether or not offscreen surfaces can be implemented using frame buffer
     * objects, which is significantly faster than relying on pbuffers.
     * 
     * @return True if fbos can be used
     */
    public boolean getFboSupport() {
        return fboSupported;
    }

    /**
     * Whether or not pbuffers (different than pixel buffers) are supported for
     * offscreen surfaces. Pbuffers are slower than fbos but are supported on
     * more hardware.
     * 
     * @return True if pbuffers can be used
     */
    public boolean getPbufferSupport() {
        return pbuffersSupported;
    }

    /**
     * Get the vendor returned string that describes the OpenGL drivers
     * installed on the computer.
     * 
     * @return Implementation vendor description
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Get the OpenGL version present on the computer.
     * 
     * @return Version to one decimal point
     */
    public float getVersion() {
        return version;
    }

    /**
     * Get the GLSL shading language available on the computer. If
     * {@link #hasGlslRenderer()} returns false, this value is null.
     * 
     * @return Version to one decimal point
     */
    public Version getGlslVersion() {
        return glslVersion;
    }

    /**
     * Get the supported types of GLSL shaders available on the computer.
     * 
     * @return Set of available shaders
     */
    public EnumSet<ShaderType> getSupportedShaderTypes() {
        return supportedShaders;
    }
}
