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
import org.lwjgl.LWJGLUtil;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;

import java.util.HashMap;
import java.util.Map;

/**
 * LwjglSurfaceFactory is a SurfaceFactory implementation for the JOGL OpenGL wrapper. It uses {@link
 * LwjglStaticDisplaySurface}, {@link LwjglFboTextureSurface}, {@link LwjglPbufferTextureSurface} for its
 * surface implementations.
 *
 * @author Michael Ludwig
 */
public class LwjglSurfaceFactory implements SurfaceFactory {
    private static final int TARGET_REFRESH_RATE = 60;

    private final LwjglCapabilities caps;
    private final DisplayMode defaultMode;
    private final Map<DisplayMode, org.lwjgl.opengl.DisplayMode> convertMap;

    private final ContextAttribs attribs;

    /**
     * Create a new LwjglSurfaceFactory.
     */
    public LwjglSurfaceFactory() {
        if (LWJGLUtil.getPlatform() == LWJGLUtil.PLATFORM_MACOSX &&
            LWJGLUtil.isMacOSXEqualsOrBetterThan(10, 7)) {
            // for mac we need to explicitly select this context profile to get 3+
            attribs = new ContextAttribs(3, 2).withProfileCore(true);
//                        attribs = null;
        } else {
            // for everyone else, it should just automatically select the highest opengl version
            // FIXME I think we should have better profile support because the presence of the compatibility
            // profile means that certain shader versions are supported or not, even if we're intending
            // on using only a more advanced API
            attribs = null;
        }

        convertMap = new HashMap<>();
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
        defaultMode = convert(Display.getDesktopDisplayMode());

        caps = LwjglCapabilities.computeCapabilities(attribs, convertMap.keySet().toArray(
                new DisplayMode[convertMap.size()]));
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

    /**
     * @return Get the selected ContextAttribs that must be specified when creating any context or surface
     *         used by this factory.
     */
    public ContextAttribs getContextAttribs() {
        return attribs;
    }

    private static DisplayMode convert(org.lwjgl.opengl.DisplayMode lwjglMode) {
        return new DisplayMode(lwjglMode.getWidth(), lwjglMode.getHeight(), lwjglMode.getBitsPerPixel(),
                               lwjglMode.getFrequency());
    }

    @Override
    public AbstractTextureSurface createTextureSurface(FrameworkImpl framework, TextureSurfaceOptions options,
                                                       OpenGLContext sharedContext) {
        if (framework.getCapabilities().getFBOSupport()) {
            return new LwjglFboTextureSurface(framework, options);
        } else if (framework.getCapabilities().getPBufferSupport()) {
            return new LwjglPbufferTextureSurface(framework, options, (LwjglContext) sharedContext);
        } else {
            throw new SurfaceCreationException("No render-to-texture support on current hardware");
        }
    }

    @Override
    public AbstractOnscreenSurface createOnscreenSurface(FrameworkImpl framework,
                                                         OnscreenSurfaceOptions options,
                                                         OpenGLContext sharedContext) {
        LwjglStaticDisplaySurface surface = new LwjglStaticDisplaySurface(framework, this, options,
                                                                          (LwjglContext) sharedContext);
        surface.initialize();
        return surface;
    }

    @Override
    public OpenGLContext createOffscreenContext(OpenGLContext sharedContext) {
        if (caps.getPBufferSupport()) {
            return PbufferShadowContext.create(this, (LwjglContext) sharedContext);
        } else {
            throw new FrameworkException(
                    "No Pbuffer support, and LWJGL framework cannot do onscreen shadow contexts");
        }
    }

    @Override
    public DisplayMode getDefaultDisplayMode() {
        return defaultMode;
    }

    @Override
    public Capabilities getCapabilities() {
        return caps;
    }

    @Override
    public void destroy() {
        // do nothing, LWJGL has no other resources to clean up
    }
}
