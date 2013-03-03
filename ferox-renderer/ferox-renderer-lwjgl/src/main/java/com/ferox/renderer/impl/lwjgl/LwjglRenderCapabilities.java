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
package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.GlslShader.Version;
import com.ferox.resource.Texture.Target;
import org.lwjgl.opengl.*;

import java.util.EnumSet;

/**
 * An extension of RenderCapabilities that implements querying OpenGL via JOGL.
 *
 * @author Michael Ludwig
 */
public class LwjglRenderCapabilities extends RenderCapabilities {
    /**
     * Force the returned RenderCapabilities to report no support for fbos.
     */
    public static final int FORCE_NO_FBO = 0x1;
    /**
     * Force the returned RenderCapabilities to report no support for pbuffers.
     */
    public static final int FORCE_NO_PBUFFER = 0x2;
    /**
     * Force the returned RenderCapabilities to report no support for programmable
     * shaders.
     */
    public static final int FORCE_NO_GLSL = 0x4;

    private final int forceBits;

    public LwjglRenderCapabilities(int forceBits) {
        this.forceBits = forceBits;
        query();
    }

    private boolean isSet(int bit) {
        return (bit & forceBits) == bit;
    }

    private static float formatVersion(String glv) {
        glv = glv.trim();
        char[] c = glv.toCharArray();
        boolean dotFound = false;
        char h;
        String v = "";
        for (int i = 0; i < c.length; i++) {
            h = c[i];
            if (!Character.isDigit(h)) {
                if (dotFound || h != '.') {
                    break;
                }
                dotFound = true;
            }
            v += h;
        }

        return Float.parseFloat(v);
    }

    private void query() {
        ContextCapabilities caps = GLContext.getCapabilities();

        vendor = GL11.glGetString(GL11.GL_VENDOR) + "-" +
                 GL11.glGetString(GL11.GL_RENDERER);
        version = formatVersion(GL11.glGetString(GL11.GL_VERSION));

        if (version >= 2f & !isSet(FORCE_NO_GLSL)) {
            float glslVersionNum = formatVersion(
                    GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
            if (glslVersionNum >= 1f && glslVersionNum < 1.3f) {
                glslVersion = Version.V1_20;
            } else if (glslVersionNum < 1.4f) {
                glslVersion = Version.V1_30;
            } else if (glslVersionNum < 1.5f) {
                glslVersion = Version.V1_40;
            } else if (glslVersionNum < 3.3f) {
                glslVersion = Version.V1_50;
            } else if (glslVersionNum < 4.0f) {
                glslVersion = Version.V3_30;
            } else {
                glslVersion = Version.V4_00;
            }

            supportedShaders = EnumSet.of(ShaderType.VERTEX, ShaderType.FRAGMENT);
            if (caps.GL_EXT_geometry_shader4 || version >= 3f) {
                supportedShaders.add(ShaderType.GEOMETRY);
            }
        } else {
            supportedShaders = EnumSet.noneOf(ShaderType.class);
            glslVersion = null;
        }

        hasFfpRenderer = true; // there is always support, it might just be emulated by a shader
        hasGlslRenderer = glslVersion != null;
        pbuffersSupported = !isSet(FORCE_NO_PBUFFER) &&
                            (Pbuffer.getCapabilities() | Pbuffer.PBUFFER_SUPPORTED) != 0;

        fboSupported =
                !isSet(FORCE_NO_FBO) && (version >= 3f || caps.GL_EXT_framebuffer_object);
        if (fboSupported) {
            maxColorTargets = GL11.glGetInteger(GL30.GL_MAX_COLOR_ATTACHMENTS);
        } else {
            maxColorTargets = 0;
        }

        hasSeparateBlend = version >= 2f || caps.GL_EXT_blend_equation_separate;
        hasSeparateStencil = version >= 2f || caps.GL_EXT_stencil_two_side;
        blendSupported = version >= 1.4f;

        maxActiveLights = GL11.glGetInteger(GL11.GL_MAX_LIGHTS);

        vboSupported = version >= 1.5f || caps.GL_ARB_vertex_buffer_object;

        if (hasGlslRenderer) {
            maxVertexAttributes = GL11.glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS);
        } else {
            maxVertexAttributes = 0;
        }

        boolean multiTexture = version >= 1.3f || caps.GL_ARB_multitexture;
        if (multiTexture) {
            maxFixedPipelineTextures = GL11.glGetInteger(GL13.GL_MAX_TEXTURE_UNITS);
        } else {
            maxFixedPipelineTextures = 1;
        }

        if (hasGlslRenderer) {
            maxVertexShaderTextures = GL11
                    .glGetInteger(GL20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS);
            maxFragmentShaderTextures = GL11
                    .glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
            maxCombinedTextures = GL11
                    .glGetInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
            maxTextureCoordinates = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_COORDS);
        } else {
            maxVertexShaderTextures = 0;
            maxFragmentShaderTextures = 0;
            maxCombinedTextures = 0;
            maxTextureCoordinates = maxFixedPipelineTextures;
        }

        npotTextures = version >= 2f || caps.GL_ARB_texture_non_power_of_two;
        fpTextures = version >= 3f || caps.GL_ARB_texture_float;
        s3tcTextures = caps.GL_EXT_texture_compression_s3tc;

        hasDepthTextures =
                version >= 1.4f || (caps.GL_ARB_depth_texture && caps.GL_ARB_shadow);
        hasEnvCombine = version >= 1.3f ||
                        (caps.GL_ARB_texture_env_combine && caps.GL_ARB_texture_env_dot3);
        hasMirrorRepeat = version >= 1.4f || caps.GL_ARB_texture_mirrored_repeat;
        hasClampEdge = version >= 1.2f;

        if (caps.GL_EXT_texture_filter_anisotropic) {
            maxAnisoLevel = GL11.glGetInteger(
                    EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
        } else {
            maxAnisoLevel = 0f;
        }

        boolean hasCubeMaps = version >= 1.3f || caps.GL_ARB_texture_cube_map;
        boolean has3dTextures = version >= 1.2f || caps.GL_EXT_texture_3d;
        supportedTargets = EnumSet.of(Target.T_1D, Target.T_2D);
        if (hasCubeMaps) {
            supportedTargets.add(Target.T_CUBEMAP);
        }
        if (has3dTextures) {
            supportedTargets.add(Target.T_3D);
        }

        maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        if (hasCubeMaps) {
            maxTextureCubeMapSize = GL11.glGetInteger(GL13.GL_MAX_CUBE_MAP_TEXTURE_SIZE);
        }
        if (has3dTextures) {
            maxTexture3DSize = GL11.glGetInteger(GL12.GL_MAX_3D_TEXTURE_SIZE);
        }

        if (fboSupported) {
            maxRenderbufferSize = GL11.glGetInteger(GL30.GL_MAX_RENDERBUFFER_SIZE);
        } else {
            maxRenderbufferSize = maxTextureSize;
        }
    }
}
