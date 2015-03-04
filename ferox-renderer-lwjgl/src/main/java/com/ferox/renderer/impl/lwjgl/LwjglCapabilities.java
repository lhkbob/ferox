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
import com.ferox.renderer.DisplayMode;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.*;

import java.util.*;

/**
 * An extension of RenderCapabilities that implements querying OpenGL via JOGL.
 *
 * @author Michael Ludwig
 */
public class LwjglCapabilities extends Capabilities {
    private static final int[] GUESSED_DEPTH_BITS = new int[] { 0, 16, 24, 32 };
    private static final int[] GUESSED_STENCIL_BITS = new int[] { 0, 2, 4, 8, 16 };
    private static final int[] GUESSED_MSAA_COUNTS = new int[] { 0, 2, 4, 8 };

    private boolean queried = false;

    public static LwjglCapabilities computeCapabilities(ContextAttribs attribs,
                                                        DisplayMode[] availableModes) {
        boolean forceNoPBuffer = Boolean.getBoolean(Framework.Factory.DISABLE_PBUFFER_PROPERTY);
        boolean forceNoFBO = Boolean.getBoolean(Framework.Factory.DISABLE_FBO_PROPERTY);

        // Technically on Mac 10.7+ pbuffers are deprecated but LWJGL does not support sharing contexts
        // between AWTGLCanvas and Display. That means there's no real alternative. Also, unlike JOGL
        // LWJGL seems to create valid enough contexts to get away with not forcing pbuffers to false

        LwjglCapabilities caps = new LwjglCapabilities();
        caps.isDebugEnabled = Boolean.getBoolean(Framework.Factory.DEBUG_PROPERTY);
        caps.availableModes = availableModes;
        caps.pbuffersSupported =
                !forceNoPBuffer && (Pbuffer.getCapabilities() & Pbuffer.PBUFFER_SUPPORTED) != 0;

        PixelFormat baseFormat = new PixelFormat().withBitsPerPixel(availableModes[0].getBitDepth());

        // for loop over guessed common msaa, depth, and stencil options
        List<Integer> validDepth = new ArrayList<>();
        List<Integer> validStencil = new ArrayList<>();
        List<Integer> validMSAA = new ArrayList<>();

        for (int depth : GUESSED_DEPTH_BITS) {
            if (isValid(attribs, baseFormat.withDepthBits(depth), caps, forceNoFBO)) {
                validDepth.add(depth);
            }
        }
        for (int stencil : GUESSED_STENCIL_BITS) {
            if (isValid(attribs, baseFormat.withStencilBits(stencil), caps, forceNoFBO)) {
                validStencil.add(stencil);
            }
        }
        for (int msaa : GUESSED_MSAA_COUNTS) {
            if (isValid(attribs, baseFormat.withSamples(msaa), caps, forceNoFBO)) {
                validMSAA.add(msaa);
            }
        }

        if (!caps.queried) {
            throw new FrameworkException("Unable to create valid context to query capabilities");
        }

        caps.depthBufferSizes = new int[validDepth.size()];
        for (int i = 0; i < validDepth.size(); i++) {
            caps.depthBufferSizes[i] = validDepth.get(i);
        }
        Arrays.sort(caps.depthBufferSizes);

        caps.stencilBufferSizes = new int[validStencil.size()];
        for (int i = 0; i < validStencil.size(); i++) {
            caps.stencilBufferSizes[i] = validStencil.get(i);
        }
        Arrays.sort(caps.stencilBufferSizes);

        caps.msaaSamples = new int[validMSAA.size()];
        for (int i = 0; i < validMSAA.size(); i++) {
            caps.msaaSamples[i] = validMSAA.get(i);
        }
        Arrays.sort(caps.msaaSamples);

        return caps;
    }

    private static boolean isValid(ContextAttribs attribs, PixelFormat format, LwjglCapabilities caps,
                                   boolean forceNoFBOs) {
        if (caps.pbuffersSupported) {
            try {
                Pbuffer pbuffer = new Pbuffer(1, 1, format, null, null, attribs);
                pbuffer.makeCurrent();
                if (!caps.queried) {
                    caps.queryWithContext(forceNoFBOs);
                }
                pbuffer.releaseContext();
                pbuffer.destroy();
                return true;
            } catch (LWJGLException e) {
                return false;
            }
        } else {
            try {
                Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(1, 1));
                Display.create(format, attribs);
                Display.makeCurrent();
                if (!caps.queried) {
                    caps.queryWithContext(forceNoFBOs);
                }
                Display.releaseContext();
                Display.destroy();
                return true;
            } catch (LWJGLException e) {
                return false;
            }
        }
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

    private void queryWithContext(boolean forceNoFBO) {
        ContextCapabilities caps = GLContext.getCapabilities();

        vendor = GL11.glGetString(GL11.GL_VENDOR) + "-" +
                 GL11.glGetString(GL11.GL_RENDERER);

        // GL_MAJOR_VERSION and GL_MINOR_VERSION are only supported on 3+ so that seems kind of silly
        float version = formatVersion(GL11.glGetString(GL11.GL_VERSION));
        majorVersion = (int) Math.floor(version);
        minorVersion = (int) Math.floor(10 * (version - majorVersion));

        float glslVersionNum = formatVersion(GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        glslVersion = (int) Math.floor(100 * glslVersionNum);

        geometryShaderSupport = caps.GL_EXT_geometry_shader4 || (majorVersion >= 3 && minorVersion >= 2);

        fboSupported = !forceNoFBO && (majorVersion >= 3 || caps.GL_EXT_framebuffer_object);
        if (fboSupported) {
            maxColorTargets = GL11.glGetInteger(GL30.GL_MAX_COLOR_ATTACHMENTS);
        } else {
            maxColorTargets = 1;
        }

        maxVertexAttributes = GL11.glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS);

        maxFragmentSamplers = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
        maxVertexSamplers = GL11.glGetInteger(GL20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS);

        if (geometryShaderSupport) {
            maxGeometrySamplers = GL11.glGetInteger(GL32.GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS);
        } else {
            maxGeometrySamplers = 0;
        }

        maxTextureUnits = GL11.glGetInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);

        fpTextures = majorVersion >= 3 || caps.GL_ARB_texture_float;
        s3tcTextures = caps.GL_EXT_texture_compression_s3tc;
        hasDepthStencilTextures = majorVersion >= 3 || caps.GL_EXT_packed_depth_stencil;
        hasIntegerTextures = majorVersion >= 3 || caps.GL_EXT_texture_integer;

        if (majorVersion >= 3) {
            maxArrayImages = GL11.glGetInteger(GL30.GL_MAX_ARRAY_TEXTURE_LAYERS);
        } else if (caps.GL_EXT_texture_array) {
            maxArrayImages = GL11.glGetInteger(EXTTextureArray.GL_MAX_ARRAY_TEXTURE_LAYERS_EXT);
        } else {
            maxArrayImages = 0;
        }

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
        if (majorVersion >= 3) {
            supportedSamplers.add(DepthCubeMap.class);
            supportedSamplers.add(Texture2DArray.class);
            supportedSamplers.add(Texture1DArray.class);
        } else if (caps.GL_EXT_texture_array) {
            supportedSamplers.add(Texture2DArray.class);
            supportedSamplers.add(Texture1DArray.class);
        }
        // depth cube maps showed up in 3.0, but there doesn't seem to be an extension

        supportedTargets = Collections.unmodifiableSet(supportedSamplers);


        maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        maxTextureCubeMapSize = GL11.glGetInteger(GL13.GL_MAX_CUBE_MAP_TEXTURE_SIZE);
        maxTexture3DSize = GL11.glGetInteger(GL12.GL_MAX_3D_TEXTURE_SIZE);

        if (fboSupported) {
            maxRenderbufferSize = GL11.glGetInteger(GL30.GL_MAX_RENDERBUFFER_SIZE);
        } else {
            maxRenderbufferSize = maxTextureSize;
        }

        queried = true;
    }
}
