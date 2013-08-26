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
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;

import javax.media.nativewindow.NativeWindowFactory;
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
    private static final int[] GUESSED_DEPTH_BITS = new int[] { 0, 16, 24, 32 };
    private static final int[] GUESSED_STENCIL_BITS = new int[] { 0, 2, 4, 8, 16 };
    private static final int[] GUESSED_MSAA_COUNTS = new int[] { 0, 2, 4, 8 };

    private boolean queried = false;

    public static JoglCapabilities computeCapabilities(GLProfile profile, DisplayMode[] availableModes) {
        boolean forceNoPBuffer = Boolean.getBoolean(Framework.Factory.DISABLE_PBUFFER_PROPERTY);
        boolean forceNoFBO = Boolean.getBoolean(Framework.Factory.DISABLE_FBO_PROPERTY);

        if (NativeWindowFactory.getNativeWindowType(true).equals(NativeWindowFactory.TYPE_MACOSX) &&
            profile.isGL3()) {
            // newer versions of Mac OS X have deprecated pbuffers, making them unreliable
            forceNoPBuffer = true;
        }

        GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);

        JoglCapabilities caps = new JoglCapabilities();
        caps.availableModes = availableModes;

        caps.pbuffersSupported = !forceNoPBuffer && factory.canCreateGLPbuffer(factory.getDefaultDevice());

        // for loop over guessed common msaa, depth, and stencil options
        Set<Integer> validDepth = new HashSet<>();
        Set<Integer> validStencil = new HashSet<>();
        Set<Integer> validMSAA = new HashSet<>();

        for (int depth : GUESSED_DEPTH_BITS) {
            GLCapabilities format = new GLCapabilities(profile);
            format.setDepthBits(depth);
            if (isValid(format, caps, factory, forceNoFBO)) {
                validDepth.add(depth);
            }
        }
        for (int stencil : GUESSED_STENCIL_BITS) {
            GLCapabilities format = new GLCapabilities(profile);
            format.setStencilBits(stencil);
            if (isValid(format, caps, factory, forceNoFBO)) {
                validStencil.add(stencil);
            }
        }
        for (int msaa : GUESSED_MSAA_COUNTS) {
            GLCapabilities format = new GLCapabilities(profile);
            if (msaa == 0) {
                format.setSampleBuffers(false);
            } else {
                format.setSampleBuffers(true);
                format.setNumSamples(msaa);
            }
            if (isValid(format, caps, factory, forceNoFBO)) {
                validMSAA.add(msaa);
            }
        }

        if (!caps.queried) {
            throw new FrameworkException("Unable to create valid context to query capabilities");
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

    private static boolean isValid(GLCapabilities format, JoglCapabilities caps, GLDrawableFactory factory,
                                   boolean forceNoFBOs) {
        boolean canUseFBO = factory.canCreateFBO(factory.getDefaultDevice(), format.getGLProfile());
        if (caps.pbuffersSupported || canUseFBO) {
            try {
                // if pbuffers are available use them for proper offscreen, since it looks like the FBO
                // fallback creates a 1x1 window, but is faster than the explicit creation we do below
                format.setFBO(!caps.pbuffersSupported);
                format.setPBuffer(caps.pbuffersSupported);
                format.setOnscreen(false);

                GLOffscreenAutoDrawable offscreen = factory
                        .createOffscreenAutoDrawable(factory.getDefaultDevice(), format,
                                                     new DefaultGLCapabilitiesChooser(), 1, 1, null);
                offscreen.getContext().makeCurrent();
                if (!caps.queried) {
                    caps.queryWithContext(offscreen.getContext().getGL(), forceNoFBOs);
                }
                offscreen.getContext().release();
                offscreen.destroy();

                return true;
            } catch (GLException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            try {
                Window w = NewtFactory.createWindow(format);
                w.setSize(1, 1);
                w.setUndecorated(true);
                w.setVisible(true);

                GLDrawable drawable = factory.createGLDrawable(w);
                GLContext ctx = drawable.createContext(null);
                drawable.setRealized(true);

                ctx.makeCurrent();
                if (!caps.queried) {
                    caps.queryWithContext(ctx.getGL(), forceNoFBOs);
                }
                ctx.release();
                ctx.destroy();
                w.destroy();

                return true;
            } catch (GLException e) {
                e.printStackTrace();
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

    private void queryWithContext(GL gl, boolean forceNoFBO) {
        vendor = gl.glGetString(GL.GL_VENDOR) + "-" +
                 gl.glGetString(GL.GL_RENDERER);

        // GL_MAJOR_VERSION and GL_MINOR_VERSION are only supported on 3+ so that seems kind of silly
        float version = formatVersion(gl.glGetString(GL.GL_VERSION));
        majorVersion = (int) Math.floor(version);
        minorVersion = (int) Math.floor(10 * (version - majorVersion));

        float glslVersionNum = formatVersion(gl.glGetString(GL2.GL_SHADING_LANGUAGE_VERSION));
        glslVersion = (int) Math.floor(100 * glslVersionNum);

        geometryShaderSupport = gl.isExtensionAvailable("GL_EXT_geometry_shader4") ||
                                (majorVersion >= 3 && minorVersion >= 2);

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

        gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_IMAGE_UNITS, query, 0);
        maxFragmentSamplers = query[0];
        gl.glGetIntegerv(GL2.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, query, 0);
        maxVertexSamplers = query[0];

        if (geometryShaderSupport) {
            gl.glGetIntegerv(GL3.GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS, query, 0);
            maxGeometrySamplers = query[0];
        } else {
            maxGeometrySamplers = 0;
        }

        gl.glGetIntegerv(GL2.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, query, 0);
        maxTextureUnits = query[0];

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
            gl.glGetIntegerv(GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, query, 0);
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

        queried = true;
    }
}
