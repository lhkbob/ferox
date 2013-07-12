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
import com.ferox.renderer.DisplayMode.PixelFormat;
import com.ferox.renderer.impl.*;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Pbuffer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * LwjglSurfaceFactory is a SurfaceFactory implementation for the JOGL OpenGL wrapper. It
 * uses {@link LwjglAWTSurface}, {@link LwjglFboTextureSurface}, {@link
 * LwjglPbufferTextureSurface} for its surface implementations. It uses the {@link
 * LWJGLFixedFunctionRenderer} and {@link LwjglGlslRenderer} for its renderer
 * implementations.
 *
 * @author Michael Ludwig
 */
public class LwjglSurfaceFactory extends SurfaceFactory {
    private static final int TARGET_REFRESH_RATE = 60;

    private final int capBits;

    private final DisplayMode defaultMode;
    private final DisplayMode[] availableModes;

    private final Map<DisplayMode, org.lwjgl.opengl.DisplayMode> convertMap;

    /**
     * Create a new LwjglSurfaceFactory that will use the given profile and capability
     * bits. The bit mask uses the bit flags defined in {@link LwjglRenderCapabilities}.
     *
     * @param profile The GLProfile
     * @param capBits The forced capabilities
     *
     * @throws NullPointerException if profile is null
     */
    public LwjglSurfaceFactory(int capBits) {
        this.capBits = capBits;

        convertMap = new HashMap<DisplayMode, org.lwjgl.opengl.DisplayMode>();

        try {
            org.lwjgl.opengl.DisplayMode[] modes = Display.getAvailableDisplayModes();
            for (org.lwjgl.opengl.DisplayMode lwjglMode : modes) {
                if (!lwjglMode.isFullscreenCapable()) {
                    continue;
                }

                DisplayMode feroxMode = convert(lwjglMode);
                if (convertMap.containsKey(feroxMode)) {
                    // compare refresh rates and pick the one closest to target
                    if (Math.abs(TARGET_REFRESH_RATE - lwjglMode.getFrequency()) <
                        Math.abs(TARGET_REFRESH_RATE -
                                 convertMap.get(feroxMode).getFrequency())) {
                        convertMap.put(feroxMode, lwjglMode);
                    }
                } else {
                    // no refresh rate overlap
                    convertMap.put(feroxMode, lwjglMode);
                }
            }
        } catch (LWJGLException e) {
            throw new FrameworkException(
                    "Unable to query available DisplayModes through LWJGL", e);
        }

        availableModes = convertMap.keySet().toArray(new DisplayMode[convertMap.size()]);
        defaultMode = convert(Display.getDesktopDisplayMode());
    }

    /**
     * Return an LWJGL DisplayMode that exactly matches the given DisplayMode, or null if
     * there was no exact match.
     *
     * @param mode The mode to "convert"
     *
     * @return The AWT DisplayMode matching mode, or null
     */
    public org.lwjgl.opengl.DisplayMode getLWJGLDisplayMode(DisplayMode mode) {
        return convertMap.get(mode);
    }

    public org.lwjgl.opengl.PixelFormat choosePixelFormat(
            OnscreenSurfaceOptions request) {
        PixelFormat pf;
        if (request.getFullscreenMode() != null) {
            pf = request.getFullscreenMode().getPixelFormat();
        } else {
            pf = getDefaultDisplayMode().getPixelFormat();
        }

        org.lwjgl.opengl.PixelFormat caps = new org.lwjgl.opengl.PixelFormat();

        switch (pf) {
        case RGB_16BIT:
            caps = caps.withBitsPerPixel(16).withAlphaBits(0);
            break;
        case RGB_24BIT:
        case UNKNOWN:
            caps = caps.withBitsPerPixel(24).withAlphaBits(0);
            break;
        case RGBA_32BIT:
            caps = caps.withBitsPerPixel(24).withAlphaBits(8);
            break;
        }

        switch (request.getDepthFormat()) {
        case DEPTH_16BIT:
            caps = caps.withDepthBits(16);
            break;
        case DEPTH_24BIT:
        case UNKNOWN:
            caps = caps.withDepthBits(24);
            break;
        case DEPTH_32BIT:
            caps = caps.withDepthBits(32);
            break;
        case NONE:
            caps = caps.withDepthBits(0);
            break;
        }

        switch (request.getStencilFormat()) {
        case STENCIL_16BIT:
            caps = caps.withStencilBits(16);
            break;
        case STENCIL_8BIT:
            caps = caps.withStencilBits(8);
            break;
        case STENCIL_4BIT:
            caps = caps.withStencilBits(4);
            break;
        case STENCIL_1BIT:
            caps = caps.withStencilBits(1);
            break;
        case NONE:
        case UNKNOWN:
            caps = caps.withStencilBits(0);
            break;
        }

        switch (request.getMultiSampling()) {
        case EIGHT_X:
            caps = caps.withSamples(8);
            break;
        case FOUR_X:
            caps = caps.withSamples(4);
            break;
        case TWO_X:
            caps = caps.withSamples(2);
            break;
        case NONE:
        case UNKNOWN:
            caps = caps.withSamples(0);
            break;
        }

        return caps;
    }

    private static DisplayMode convert(org.lwjgl.opengl.DisplayMode lwjglMode) {
        PixelFormat pixFormat;
        switch (lwjglMode.getBitsPerPixel()) {
        case 16:
            pixFormat = PixelFormat.RGB_16BIT;
            break;
        case 24:
            pixFormat = PixelFormat.RGB_24BIT;
            break;
        case 32:
            pixFormat = PixelFormat.RGBA_32BIT;
            break;
        default:
            pixFormat = PixelFormat.UNKNOWN;
            break;
        }

        return new DisplayMode(lwjglMode.getWidth(), lwjglMode.getHeight(), pixFormat);
    }

    @Override
    public AbstractTextureSurface createTextureSurface(FrameworkImpl framework,
                                                       TextureSurfaceOptions options,
                                                       OpenGLContext sharedContext) {
        if (framework.getCapabilities().getFboSupport()) {
            return new LwjglFboTextureSurface(framework, this, options);
        } else if (framework.getCapabilities().getPbufferSupport()) {
            return new LwjglPbufferTextureSurface(framework, this, options,
                                                  (LwjglContext) sharedContext,
                                                  new LwjglRendererProvider());
        } else {
            throw new SurfaceCreationException(
                    "No render-to-texture support on current hardware");
        }
    }

    @Override
    public AbstractOnscreenSurface createOnscreenSurface(FrameworkImpl framework,
                                                         OnscreenSurfaceOptions options,
                                                         OpenGLContext sharedContext) {
        LwjglStaticDisplaySurface surface = new LwjglStaticDisplaySurface(framework, this,
                                                                          options,
                                                                          (LwjglContext) sharedContext,
                                                                          new LwjglRendererProvider());
        surface.initialize();
        return surface;
    }

    @Override
    public OpenGLContext createOffscreenContext(OpenGLContext sharedContext) {
        if ((capBits & LwjglRenderCapabilities.FORCE_NO_PBUFFER) == 0 &&
            (Pbuffer.getCapabilities() | Pbuffer.PBUFFER_SUPPORTED) != 0) {
            return PbufferShadowContext.create(this, (LwjglContext) sharedContext,
                                               new LwjglRendererProvider());
        } else {
            throw new FrameworkException(
                    "No Pbuffer support, and LWJGL framework cannot do onscreen shadow contexts");
        }
    }

    /**
     * @return The capabilities bits this factory was created with, to be passed into the
     *         constructor of all related {@link LwjglRenderCapabilities}
     */
    public int getCapabilityForceBits() {
        return capBits;
    }

    @Override
    public DisplayMode getDefaultDisplayMode() {
        return defaultMode;
    }

    @Override
    public DisplayMode[] getAvailableDisplayModes() {
        return Arrays.copyOf(availableModes, availableModes.length);
    }

    private class LwjglRendererProvider implements RendererProvider {
        private FixedFunctionRenderer ffp;
        private GlslRenderer glsl;

        private LwjglRendererDelegate sharedDelegate;

        @Override
        public FixedFunctionRenderer getFixedFunctionRenderer(Capabilities caps) {
            if (ffp == null) {
                if (caps.hasFixedFunctionRenderer()) {
                    if (sharedDelegate == null) {
                        sharedDelegate = new LwjglRendererDelegate();
                    }
                    ffp = new LwjglFixedFunctionRenderer(sharedDelegate);
                }
            }

            return ffp;
        }

        @Override
        public GlslRenderer getGlslRenderer(Capabilities caps) {
            if (glsl == null) {
                if (caps.hasGlslRenderer()) {
                    if (sharedDelegate == null) {
                        sharedDelegate = new LwjglRendererDelegate();
                    }
                    glsl = new LwjglGlslRenderer(sharedDelegate);
                }
            }

            return glsl;
        }
    }
}
