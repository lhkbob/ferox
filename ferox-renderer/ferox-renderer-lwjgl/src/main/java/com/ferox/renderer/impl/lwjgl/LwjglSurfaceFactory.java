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
import com.ferox.renderer.impl.*;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Pbuffer;

import java.util.HashMap;
import java.util.Map;

/**
 * LwjglSurfaceFactory is a SurfaceFactory implementation for the JOGL OpenGL wrapper. It uses {@link
 * LwjglAWTSurface}, {@link LwjglFboTextureSurface}, {@link LwjglPbufferTextureSurface} for its surface
 * implementations. It uses the {@link LWJGLFixedFunctionRenderer} and {@link LwjglGlslRenderer} for its
 * renderer implementations.
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
     * Create a new LwjglSurfaceFactory that will use the given profile and capability bits. The bit mask uses
     * the bit flags defined in {@link LwjglRenderCapabilities}.
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
                        Math.abs(TARGET_REFRESH_RATE - convertMap.get(feroxMode).getFrequency())) {
                        convertMap.put(feroxMode, lwjglMode);
                    }
                } else {
                    // no refresh rate overlap
                    convertMap.put(feroxMode, lwjglMode);
                }
            }
        } catch (LWJGLException e) {
            throw new FrameworkException("Unable to query available DisplayModes through LWJGL", e);
        }

        availableModes = convertMap.keySet().toArray(new DisplayMode[convertMap.size()]);
        defaultMode = convert(Display.getDesktopDisplayMode());
    }

    /**
     * Return an LWJGL DisplayMode that exactly matches the given DisplayMode, or null if there was no exact
     * match.
     *
     * @param mode The mode to "convert"
     *
     * @return The AWT DisplayMode matching mode, or null
     */
    public org.lwjgl.opengl.DisplayMode getLWJGLDisplayMode(DisplayMode mode) {
        return convertMap.get(mode);
    }

    public org.lwjgl.opengl.PixelFormat choosePixelFormat(OnscreenSurfaceOptions request) {
        int pf;
        if (request.getFullscreenMode() != null) {
            pf = request.getFullscreenMode().getBitDepth();
        } else {
            pf = getDefaultDisplayMode().getBitDepth();
        }

        boolean depthValid = false;
        for (int depth : getCapabilities().getAvailableDepthBufferSizes()) {
            if (depth == request.getDepthBufferBits()) {
                depthValid = true;
                break;
            }
        }
        if (!depthValid) {
            throw new SurfaceCreationException(
                    "Invalid depth buffer bit count: " + request.getDepthBufferBits());
        }

        boolean stencilValid = false;
        for (int stencil : getCapabilities().getAvailableStencilBufferSizes()) {
            if (stencil == request.getStencilBufferBits()) {
                stencilValid = true;
                break;
            }
        }
        if (!stencilValid) {
            throw new SurfaceCreationException(
                    "Invalid stencil buffer bit count: " + request.getStencilBufferBits());
        }

        boolean samplesValid = false;
        for (int sample : getCapabilities().getAvailableSamples()) {
            if (sample == request.getSampleCount()) {
                samplesValid = true;
                break;
            }
        }
        if (!samplesValid) {
            throw new SurfaceCreationException("Invalid sample count: " + request.getSampleCount());
        }

        org.lwjgl.opengl.PixelFormat caps = new org.lwjgl.opengl.PixelFormat();
        return caps.withBitsPerPixel(pf).withDepthBits(request.getDepthBufferBits())
                   .withStencilBits(request.getStencilBufferBits()).withSamples(request.getSampleCount());
    }

    private static DisplayMode convert(org.lwjgl.opengl.DisplayMode lwjglMode) {
        return new DisplayMode(lwjglMode.getWidth(), lwjglMode.getHeight(), lwjglMode.getBitsPerPixel(),
                               lwjglMode.getFrequency());
    }

    @Override
    public AbstractTextureSurface createTextureSurface(FrameworkImpl framework, TextureSurfaceOptions options,
                                                       OpenGLContext sharedContext) {
        if (framework.getCapabilities().getFboSupport()) {
            return new LwjglFboTextureSurface(framework, this, options);
        } else if (framework.getCapabilities().getPbufferSupport()) {
            return new LwjglPbufferTextureSurface(framework, this, options, (LwjglContext) sharedContext,
                                                  new LwjglRendererProvider());
        } else {
            throw new SurfaceCreationException("No render-to-texture support on current hardware");
        }
    }

    @Override
    public AbstractOnscreenSurface createOnscreenSurface(FrameworkImpl framework,
                                                         OnscreenSurfaceOptions options,
                                                         OpenGLContext sharedContext) {
        LwjglStaticDisplaySurface surface = new LwjglStaticDisplaySurface(framework, this, options,
                                                                          (LwjglContext) sharedContext,
                                                                          new LwjglRendererProvider());
        surface.initialize();
        return surface;
    }

    @Override
    public OpenGLContext createOffscreenContext(OpenGLContext sharedContext) {
        if ((capBits & LwjglRenderCapabilities.FORCE_NO_PBUFFER) == 0 &&
            (Pbuffer.getCapabilities() | Pbuffer.PBUFFER_SUPPORTED) != 0) {
            return PbufferShadowContext
                    .create(this, (LwjglContext) sharedContext, new LwjglRendererProvider());
        } else {
            throw new FrameworkException(
                    "No Pbuffer support, and LWJGL framework cannot do onscreen shadow contexts");
        }
    }

    @Override
    public DisplayMode getDefaultDisplayMode() {
        return defaultMode;
    }
}
