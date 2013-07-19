/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * <p/>
 * Capabilities holds onto a set of different parameters describing more advanced features that the baseline
 * hardware may not support.
 * <p/>
 * Because Ferox was designed to be implemented with an OpenGL system, the capabilities here reflect that and
 * allow for lower-level inspection of the current hardware. Framework implementations are expected to extend
 * Capabilities to provide the correct values for each parameter. Capabilities instances returned from {@link
 * com.ferox.renderer.Framework#getCapabilities()} can be considered immutable.
 *
 * @author Michael Ludwig
 */
public abstract class Capabilities {
    // texture properties
    protected int maxTextureSize = 0; //
    protected int maxTextureCubeMapSize = 0; //
    protected int maxTexture3DSize = 0; //
    protected int maxRenderbufferSize = 0; //

    protected int maxArrayImages = 0; //

    protected boolean fpTextures = false; //
    protected boolean s3tcTextures = false; //
    protected boolean hasDepthStencilTextures = false; //
    protected boolean hasIntegerTextures = false; //
    protected double maxAnisoLevel = 0; //

    protected Set<Class<? extends Sampler>> supportedTargets = Collections.emptySet(); //

    // glsl
    protected boolean geometryShaderSupport = false; //
    protected int glslVersion;
    protected int maxVertexShaderTextures = -1; //
    protected int maxFragmentShaderTextures = -1; //
    protected int maxCombinedTextures = -1; //
    protected int maxVertexAttributes = 0; //

    protected String vendor = ""; //
    protected int majorVersion = 0;
    protected int minorVersion = 0;

    // frame properties
    protected boolean fboSupported = false; //
    protected boolean pbuffersSupported = false; //
    protected int maxColorTargets = 0; //
    protected int[] depthBufferSizes = new int[1]; //
    protected int[] stencilBufferSizes = new int[1]; //
    protected int[] msaaSamples = new int[1]; //

    protected DisplayMode[] availableModes = new DisplayMode[0]; //

    protected boolean supportsMultipleOnscreenSurfaces;

    /**
     * @return True if multiple non-fullscreen onscreen surfaces can exist at the same time with this
     *         framework
     */
    public boolean getMultipleOnscreenSurfaceSupport() {
        return supportsMultipleOnscreenSurfaces;
    }

    /**
     * Return an array of available DisplayModes that can be used when creating fullscreen surfaces with
     * {@link Framework#createSurface(OnscreenSurfaceOptions)}. A defensive array copy is returned, with no
     * particular ordering. If the returned array is empty, fullscreen surfaces are not supported.
     *
     * @return All available display modes on the system
     */
    public DisplayMode[] getAvailableDisplayModes() {
        return Arrays.copyOf(availableModes, availableModes.length);
    }

    /**
     * Return an array of the depth buffer bit sizes that are supported on this hardware. The returned array
     * is ordered from least to greatest bit size and is a defensive copy. The array will always contain the
     * value 0 for no depth buffer support.
     *
     * @return Supported depth buffer sizes
     */
    public int[] getAvailableDepthBufferSizes() {
        return Arrays.copyOf(depthBufferSizes, depthBufferSizes.length);
    }

    /**
     * Return an array of the stencil buffer bit sizes that are supported on this hardware. The returned array
     * is ordered from least to greatest bit size and is a defensive copy. The array will always at least
     * contain the value 0 for no stencil buffer support.
     *
     * @return Supported stencil buffer sizes
     */
    public int[] getAvailableStencilBufferSizes() {
        return Arrays.copyOf(stencilBufferSizes, stencilBufferSizes.length);
    }

    /**
     * Return an array of the sample counts that are supported on this hardware when using MSAA. The returned
     * array is ordered from least to greatest bit size and is a defensive copy. The array will always contain
     * the value 0 for no MSAA support.
     *
     * @return Supported MSAA sample counts
     */
    public int[] getAvailableSamples() {
        return Arrays.copyOf(msaaSamples, msaaSamples.length);
    }

    /**
     * @return The set of supported sampler interfaces on this hardware.
     */
    public Set<Class<? extends Sampler>> getSupportedTextureTargets() {
        return supportedTargets;
    }

    /**
     * @return Maximum number of images in a 1D or 2D texture array
     */
    public int getMaxTextureArrayImages() {
        return maxArrayImages;
    }

    /**
     * @return True if the DEPTH_STENCIL BaseFormat is supported
     */
    public boolean getDepthStencilTextureSupport() {
        return hasDepthStencilTextures;
    }

    /**
     * @return True if textures can use unnormalized signed and unsigned integer types
     */
    public boolean getIntegerTextureSupport() {
        return hasIntegerTextures;
    }

    /**
     * Return the maximum side dimension of a Texture1D, Texture2D, or DepthMap2D.
     *
     * @return Maximum size of a 1d or 2d texture
     */
    public int getMaxTextureSize() {
        return maxTextureSize;
    }

    /**
     * Return the maximum side dimension of a Texture3D.
     *
     * @return Maximum size of a 3d texture
     */
    public int getMaxTexture3DSize() {
        return maxTexture3DSize;
    }

    /**
     * Return the maximum side dimension of a face of a TextureCubeMap or a DepthCubeMap.
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
     * Return the maximum number of color buffers that can be rendered into simultaneously with a GLSL program
     * when using a TextureSurface.
     *
     * @return Number of color targets allowed for TextureSurfaces
     */
    public int getMaxColorBuffers() {
        return maxColorTargets;
    }

    /**
     * Get max number of textures allowed in the vertex shader stage of a GLSL program. This will return a
     * number <= 0 if GLSL shaders are not supported.
     *
     * @return Number of textures allowed in a vertex shader
     */
    public int getMaxVertexShaderTextures() {
        return maxVertexShaderTextures;
    }

    /**
     * Get the max number of textures allowed in the fragment shader of GLSL program. This will return a
     * number <= 0 if GLSL shaders are not supported.
     *
     * @return Number of textures allowed in a fragment shader
     */
    public int getMaxFragmentShaderTextures() {
        return maxFragmentShaderTextures;
    }

    /**
     * Get the max number of textures used by an entire GLSL program. This may be less than the sum of {@link
     * #getMaxVertexShaderTextures()} and {@link #getMaxFragmentShaderTextures()}.
     *
     * @return Total number of texture samplers in a GLSL program
     */
    public int getMaxCombinedTextures() {
        return maxCombinedTextures;
    }

    /**
     * Get the max supported level of anisotropic filtering for textures. If anisotropic filtering is not
     * supported, this should return a number <= 0. A value of 1 in {@link com.ferox.renderer.Texture#getAnisotropicFiltering()}
     * will be scaled by the Framework to the returned number.
     *
     * @return Maximum level of anistropic filtering
     */
    public double getMaxAnisotropicLevel() {
        return maxAnisoLevel;
    }

    /**
     * Whether or not unclamped floating point textures are supported. If false, float texture values are
     * clamped to be within 0 to 1.
     *
     * @return If full floating point textures can be stored
     */
    public boolean getUnclampedFloatTextureSupport() {
        return fpTextures;
    }

    /**
     * Whether or not the S3TC extension is present. This allows for DXT1, DXT3, and DXT5 texture compression
     * on the graphics card.
     *
     * @return If DXT texture compression is supported
     */
    public boolean getS3TextureCompression() {
        return s3tcTextures;
    }

    /**
     * Get the maximum vertex attributes allowed on each vertex rendered.
     *
     * @return Number of vertex attributes
     */
    public int getMaxVertexAttributes() {
        return maxVertexAttributes;
    }

    /**
     * Whether or not offscreen surfaces can be implemented using frame buffer objects, which is significantly
     * faster than relying on pbuffers.
     *
     * @return True if fbos can be used
     */
    public boolean getFBOSupport() {
        return fboSupported;
    }

    /**
     * Whether or not pbuffers (different than pixel buffers) are supported for offscreen surfaces. Pbuffers
     * are slower than fbos but are supported on more hardware.
     *
     * @return True if pbuffers can be used
     */
    public boolean getPBufferSupport() {
        return pbuffersSupported;
    }

    /**
     * Get the vendor returned string that describes the OpenGL drivers installed on the computer.
     *
     * @return Implementation vendor description
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * @return The OpenGL major version present on the computer
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * @return The OpenGL minor version present on the computer
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Get the GLSL shading language available on the computer. The reported version is the integer value used
     * in the #version declaration in source code. Example: for GLSL 1.4 the returned value is 140, for 3.3 it
     * is 330.
     *
     * @return Version in integer form
     */
    public int getGlslVersion() {
        return glslVersion;
    }

    /**
     * @return True if the GlslRenderer can support shaders with a geometry shader. Shaders always support
     *         vertex and fragment shaders.
     */
    public boolean hasGeometryShaderSupport() {
        return geometryShaderSupport;
    }
}
