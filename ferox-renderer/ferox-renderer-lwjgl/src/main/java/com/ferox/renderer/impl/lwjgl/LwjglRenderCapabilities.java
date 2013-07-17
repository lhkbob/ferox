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

import com.ferox.renderer.*;
import org.lwjgl.opengl.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An extension of RenderCapabilities that implements querying OpenGL via JOGL.
 *
 * @author Michael Ludwig
 */
public class LwjglRenderCapabilities extends Capabilities {
    /**
     * Force the returned RenderCapabilities to report no support for fbos.
     */
    public static final int FORCE_NO_FBO = 0x1;
    /**
     * Force the returned RenderCapabilities to report no support for pbuffers.
     */
    public static final int FORCE_NO_PBUFFER = 0x2;

    private final int forceBits;

    /**
     * Query the capabilities of the current hardware. This is not a fast constructor.
     *
     * @param forceBits
     */
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

        // GL_MAJOR_VERSION and GL_MINOR_VERSION are only supported on 3+ so that seems kind of silly
        float version = formatVersion(GL11.glGetString(GL11.GL_VERSION));
        majorVersion = (int) Math.floor(version);
        minorVersion = (int) Math.floor(10 * (version - majorVersion));


        float glslVersionNum = formatVersion(GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        glslVersion = (int) Math.floor(100 * glslVersionNum);

        geometryShaderSupport = caps.GL_EXT_geometry_shader4 || (majorVersion >= 3 && minorVersion >= 3);
        pbuffersSupported =
                !isSet(FORCE_NO_PBUFFER) && (Pbuffer.getCapabilities() & Pbuffer.PBUFFER_SUPPORTED) != 0;

        fboSupported = !isSet(FORCE_NO_FBO) && (majorVersion >= 3 || caps.GL_EXT_framebuffer_object);
        if (fboSupported) {
            maxColorTargets = GL11.glGetInteger(GL30.GL_MAX_COLOR_ATTACHMENTS);
        } else {
            maxColorTargets = 1;
        }

        maxVertexAttributes = GL11.glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS);

        maxVertexShaderTextures = GL11.glGetInteger(GL20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS);
        maxFragmentShaderTextures = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
        maxCombinedTextures = GL11.glGetInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);

        fpTextures = version >= 3f || caps.GL_ARB_texture_float;
        s3tcTextures = caps.GL_EXT_texture_compression_s3tc;
        hasDepthStencilTextures = false; // FIXME
        hasIntegerTextures = false; // FIXME
        maxArrayImages = 0; //

        // FIXME how do I implement this?
        depthBufferSizes = null;
        stencilBufferSizes = null;
        msaaSamples = null;
        availableModes = null;

        // LWJGL does not support multiple windows with its static display
        supportsMultipleOnscreenSurfaces = false;

        if (caps.GL_EXT_texture_filter_anisotropic) {
            maxAnisoLevel = GL11.glGetInteger(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
        } else {
            maxAnisoLevel = 0f;
        }

        Set<Class<? extends Sampler>> supportedSamplers = new HashSet<>();
        supportedSamplers
                .addAll(Arrays.asList(Texture1D.class, Texture2D.class, Texture3D.class, TextureCubeMap.class,
                                      DepthMap2D.class));
        supportedTargets = Collections.unmodifiableSet(supportedSamplers);


        maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        maxTextureCubeMapSize = GL11.glGetInteger(GL13.GL_MAX_CUBE_MAP_TEXTURE_SIZE);
        maxTexture3DSize = GL11.glGetInteger(GL12.GL_MAX_3D_TEXTURE_SIZE);

        if (fboSupported) {
            maxRenderbufferSize = GL11.glGetInteger(GL30.GL_MAX_RENDERBUFFER_SIZE);
        } else {
            maxRenderbufferSize = maxTextureSize;
        }
    }
}
