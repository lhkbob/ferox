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
import com.ferox.renderer.impl.*;
import com.jogamp.newt.*;

import javax.media.nativewindow.util.SurfaceSize;
import javax.media.opengl.GLProfile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JoglSurfaceFactory is a SurfaceFactory implementation for the JOGL OpenGL wrapper. It uses {@link
 * JoglNEWTSurface}, {@link JoglFboTextureSurface}, {@link JoglPbufferTextureSurface} for its surface
 * implementations. It uses the {@link JoglFixedFunctionRenderer} and {@link JoglGlslRenderer} for its
 * renderer implementations.
 *
 * @author Michael Ludwig
 */
public class JoglSurfaceFactory implements SurfaceFactory {
    private static final int TARGET_REFRESH_RATE = 60;

    private final GLProfile profile;
    private final JoglCapabilities caps;

    private final DisplayMode defaultMode;
    private final Map<DisplayMode, MonitorMode> convertMap;

    // the Display and Screen used by all windows created by this factory
    private final Display display;
    private final Screen screen;
    private final MonitorDevice device;

    /**
     * Create a new JoglSurfaceFactory.
     */
    public JoglSurfaceFactory() {
        if (GLProfile.isAvailable(GLProfile.GL4)) {
            profile = GLProfile.get(GLProfile.GL4);
        } else if (GLProfile.isAvailable(GLProfile.GL3)) {
            profile = GLProfile.get(GLProfile.GL3);
        } else {
            profile = GLProfile.get(GLProfile.GL2);
        }

        display = NewtFactory.createDisplay(null);
        display.addReference();

        screen = NewtFactory.createScreen(display, 0);
        screen.addReference();
        device = screen.getMonitorDevices().get(0);

        convertMap = new HashMap<>();

        List<MonitorMode> modes = device.getSupportedModes();
        for (MonitorMode joglMode : modes) {
            DisplayMode feroxMode = convert(joglMode);
            if (convertMap.containsKey(feroxMode)) {
                // compare refresh rates and pick the one closest to target
                if (Math.abs(TARGET_REFRESH_RATE - joglMode.getRefreshRate()) <
                    Math.abs(TARGET_REFRESH_RATE - convertMap.get(feroxMode).getRefreshRate())) {
                    convertMap.put(feroxMode, joglMode);
                }
            } else {
                // no refresh rate overlap
                convertMap.put(feroxMode, joglMode);
            }
        }

        defaultMode = convert(device.getOriginalMode());
        caps = JoglCapabilities.computeCapabilities(profile, convertMap.keySet().toArray(
                new DisplayMode[convertMap.size()]));
    }

    @Override
    public void destroy() {
        screen.removeReference();
        display.removeReference();
    }

    public MonitorDevice getMonitor() {
        return device;
    }

    public Screen getScreen() {
        return screen;
    }

    /**
     * @return The GLProfile selected by this factory
     */
    public GLProfile getGLProfile() {
        return profile;
    }

    /**
     * Return an JOGL ScreenMode that exactly matches the given DisplayMode, or null if there was no exact
     * match.
     *
     * @param mode The mode to "convert"
     *
     * @return The JOGL DisplayMode matching mode, or null
     */
    public MonitorMode getScreenMode(DisplayMode mode) {
        return convertMap.get(mode);
    }

    private static DisplayMode convert(MonitorMode mode) {
        SurfaceSize realMode = mode.getSurfaceSize();

        return new DisplayMode(realMode.getResolution().getWidth(), realMode.getResolution().getHeight(),
                               realMode.getBitsPerPixel(), (int) mode.getRefreshRate());
    }

    @Override
    public AbstractTextureSurface createTextureSurface(FrameworkImpl framework, TextureSurfaceOptions options,
                                                       OpenGLContext sharedContext) {
        if (framework.getCapabilities().getFBOSupport()) {
            return new JoglFboTextureSurface(framework, options);
        } else if (framework.getCapabilities().getPBufferSupport()) {
            return new JoglPbufferTextureSurface(profile, framework, options, (JoglContext) sharedContext);
        } else {
            throw new SurfaceCreationException("No render-to-texture support on current hardware");
        }
    }

    @Override
    public AbstractOnscreenSurface createOnscreenSurface(FrameworkImpl framework,
                                                         OnscreenSurfaceOptions options,
                                                         OpenGLContext sharedContext) {
        JoglNEWTSurface s = new JoglNEWTSurface(framework, this, options, (JoglContext) sharedContext);
        s.initialize();
        return s;
    }

    @Override
    public OpenGLContext createOffscreenContext(OpenGLContext sharedContext) {
        if (caps.getPBufferSupport()) {
            return PbufferShadowContext.create(this, (JoglContext) sharedContext);
        } else {
            return OnscreenShadowContext.create(this, (JoglContext) sharedContext);
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
}
