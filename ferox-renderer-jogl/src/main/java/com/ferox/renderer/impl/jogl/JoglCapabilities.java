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
package com.ferox.renderer.impl.jogl;

import com.ferox.renderer.*;

import javax.media.opengl.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An extension of RenderCapabilities that implements querying OpenGL via JOGL.
 *
 * @author Michael Ludwig
 */
public class JoglCapabilities extends Capabilities {
    public static JoglCapabilities computeCapabilities(GLProfile profile, DisplayMode[] availableModes,
                                                       boolean forceNoPBuffer, boolean forceNoFBO) {
        GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);

        JoglCapabilities caps = new JoglCapabilities();
        caps.availableModes = availableModes;
        caps.pbuffersSupported = !forceNoPBuffer && factory.canCreateGLPbuffer(factory.getDefaultDevice());

        // for loop over guessed common msaa, depth, and stencil options
        Set<Integer> validDepth = new HashSet<>();
        Set<Integer> validStencil = new HashSet<>();
        Set<Integer> validMSAA = new HashSet<>();

        for (GLCapabilitiesImmutable avail : factory.getAvailableCapabilities(factory.getDefaultDevice())) {
            validDepth.add(avail.getDepthBits());
            validStencil.add(avail.getStencilBits());
            if (avail.getSampleBuffers()) {
                validMSAA.add(avail.getNumSamples());
            } else {
                validMSAA.add(0);
            }
        }

        try {
            GLDrawable offscreen = factory
                    .createOffscreenDrawable(factory.getDefaultDevice(), new GLCapabilities(profile),
                                             new DefaultGLCapabilitiesChooser(), 1, 1);
            GLContext ctx = offscreen.createContext(null);
            ctx.makeCurrent();
            caps.queryWithContext(ctx.getGL(), forceNoFBO);
            ctx.release();
            ctx.destroy();
        } catch (GLException e) {
            throw new FrameworkException("Unable to create valid context to query capabilities", e);
        }

        int i = 0;
        caps.depthBufferSizes = new int[validDepth.size()];
        for (Integer v : validDepth) {
            caps.depthBufferSizes[i++] = v;
        }
        Arrays.sort(caps.depthBufferSizes);

        i = 0;
        caps.stencilBufferSizes = new int[validStencil.size()];
        for (Integer v : validStencil) {
            caps.stencilBufferSizes[i++] = v;
        }
        Arrays.sort(caps.stencilBufferSizes);

        i = 0;
        caps.msaaSamples = new int[validMSAA.size()];
        for (Integer v : validMSAA) {
            caps.msaaSamples[i++] = v;
        }
        Arrays.sort(caps.msaaSamples);

        return caps;
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

    private void queryWithContext(GL gl, boolean forceNoFBO) {
        vendor = gl.glGetString(GL.GL_VENDOR) + "-" +
                 gl.glGetString(GL.GL_RENDERER);

        // GL_MAJOR_VERSION and GL_MINOR_VERSION are only supported on 3+ so that seems kind of silly
        float version = formatVersion(gl.glGetString(GL.GL_VERSION));
        majorVersion = (int) Math.floor(version);
        minorVersion = (int) Math.floor(10 * (version - majorVersion));

        float glslVersionNum = formatVersion(gl.glGetString(GL2.GL_SHADING_LANGUAGE_VERSION));
        glslVersion = (int) Math.floor(100 * glslVersionNum);

        // FIXME do extensions start with GL_ or go straight into EXT_ or ARB_?
        geometryShaderSupport = gl.isExtensionAvailable("GL_EXT_geometry_shader4") ||
                                (majorVersion >= 3 && minorVersion >= 3);

        int[] query = new int[1];
        fboSupported =
                !forceNoFBO && (majorVersion >= 3 || gl.isExtensionAvailable("GL_EXT_framebuffer_object"));
        if (fboSupported) {
            gl.glGetIntegerv(GL3.GL_MAX_COLOR_ATTACHMENTS, query, 0);
            maxColorTargets = query[0];
        } else {
            maxColorTargets = 1;
        }

        gl.glGetIntegerv(GL2.GL_MAX_VERTEX_ATTRIBS, query, 0);
        maxVertexAttributes = query[0];

        gl.glGetIntegerv(GL2.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, query, 0);
        maxVertexShaderTextures = query[0];
        gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_IMAGE_UNITS, query, 0);
        maxFragmentShaderTextures = query[0];
        gl.glGetIntegerv(GL2.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, query, 0);
        maxCombinedTextures = query[0];

        fpTextures = majorVersion >= 3 || gl.isExtensionAvailable("GL_ARB_texture_float");
        s3tcTextures = gl.isExtensionAvailable("GL_EXT_texture_compression_s3tc");
        hasDepthStencilTextures = majorVersion >= 3 || gl.isExtensionAvailable("GL_EXT_packed_depth_stencil");
        hasIntegerTextures = majorVersion >= 3 || gl.isExtensionAvailable("GL_EXT_texture_integer");

        if (majorVersion >= 3 || gl.isExtensionAvailable("GL_EXT_texture_array")) {
            gl.glGetIntegerv(GL3.GL_MAX_ARRAY_TEXTURE_LAYERS, query, 0);
            maxArrayImages = query[0];
        } else {
            maxArrayImages = 0;
        }

        // LWJGL does not support multiple windows with its static display
        supportsMultipleOnscreenSurfaces = false;

        if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic")) {
            gl.glGetIntegerv(GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, query, 0);
            maxAnisoLevel = query[0];
        } else {
            maxAnisoLevel = 0;
        }

        Set<Class<? extends Sampler>> supportedSamplers = new HashSet<>();
        supportedSamplers
                .addAll(Arrays.asList(Texture1D.class, Texture2D.class, Texture3D.class, TextureCubeMap.class,
                                      DepthMap2D.class));
        if (majorVersion >= 3) {
            supportedSamplers.add(DepthCubeMap.class);
            supportedSamplers.add(Texture2DArray.class);
            supportedSamplers.add(Texture1DArray.class);
        } else if (maxArrayImages > 0) {
            supportedSamplers.add(Texture2DArray.class);
            supportedSamplers.add(Texture1DArray.class);
        }
        // depth cube maps showed up in 3.0, but there doesn't seem to be an extension

        supportedTargets = Collections.unmodifiableSet(supportedSamplers);

        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, query, 0);
        maxTextureSize = query[0];
        gl.glGetIntegerv(GL.GL_MAX_CUBE_MAP_TEXTURE_SIZE, query, 0);
        maxTextureCubeMapSize = query[0];
        gl.glGetIntegerv(GL2.GL_MAX_3D_TEXTURE_SIZE, query, 0);
        maxTexture3DSize = query[0];

        if (fboSupported) {
            gl.glGetIntegerv(GL3.GL_MAX_RENDERBUFFER_SIZE, query, 0);
            maxRenderbufferSize = query[0];
        } else {
            maxRenderbufferSize = maxTextureSize;
        }
    }
}
