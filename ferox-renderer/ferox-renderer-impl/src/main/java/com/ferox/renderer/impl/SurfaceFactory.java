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
package com.ferox.renderer.impl;

import com.ferox.renderer.DisplayMode;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.TextureSurfaceOptions;

/**
 * SurfaceFactory is a factory that interfaces with lower-level OpenGL wrappers
 * to create concrete implementations of Surfaces and OpenGLContextAdapters.
 * Implementations must be thread safe. They are not tied to the lifecycle of a
 * particular framework.
 * 
 * @author Michael Ludwig
 */
public abstract class SurfaceFactory {
    /**
     * Create an AbstractTextureSurface implementation. This is used by the
     * AbstractFramework to implement
     * {@link Framework#createSurface(TextureSurfaceOptions)}. The returned
     * surface must be an AbstractSurface because AbstractFramework requires
     * AbstractSurfaces to function properly.
     * 
     * @param framework The framework that will own the surface
     * @param options The TextureSurfaceOptions used to create the surface
     * @param sharedContext The context to share resources with, this will not
     *            be null
     * @return A new texture surface
     * @throws NullPointerException if any of the arguments are null
     */
    public abstract AbstractTextureSurface createTextureSurface(AbstractFramework framework,
                                                                TextureSurfaceOptions options,
                                                                OpenGLContext sharedContext);

    /**
     * Create an AbstractOnscreenSurface implementation. This is used by the
     * AbstractFramework to implement
     * {@link Framework#createSurface(OnscreenSurfaceOptions)}. The returned
     * surface must be an AbstractSurface because AbstractFramework requires
     * AbstractSurfaces to function properly.
     * 
     * @param framework The framework that will own the surface
     * @param options The OnscreenSurfaceOptions used to create the surface
     * @param sharedContext The context to share resources with, this will not
     *            be null
     * @return A new, visible onscreen surface
     * @throws NullPointerException if any of the arguments are null
     */
    public abstract AbstractOnscreenSurface createOnscreenSurface(AbstractFramework framework,
                                                                  OnscreenSurfaceOptions options,
                                                                  OpenGLContext sharedContext);

    /**
     * Create an OpenGLContext that wraps an underlying OpenGL context. The
     * context should be "offscreen" and not attached to a Surface. This can be
     * a context owned by a pbuffer, or it can be a context attached to a 1x1
     * hidden window. The term "offscreen" is loosely defined but should not be
     * noticeable by a user.
     * 
     * @param sharedContext An OpenGLContext to share all resources with, if
     *            this is null then no sharing is done
     * @return An offscreen context
     */
    public abstract OpenGLContext createOffscreenContext(OpenGLContext sharedContext);

    /**
     * @return The default display mode, as required
     *         {@link Framework#getDefaultDisplayMode()}
     */
    public abstract DisplayMode getDefaultDisplayMode();

    /**
     * @return Available display modes, as required by
     *         {@link Framework#getAvailableDisplayModes()}
     */
    public abstract DisplayMode[] getAvailableDisplayModes();

    /**
     * Perform any native resource destruction, called by the Framework when
     * it's lifecycle is completed.
     */
    public void destroy() {}

    /**
     * Select the closest matching of the supported DisplayModes given the
     * requested.
     * 
     * @param requested The requested display mode
     * @return Best matching mode
     * @throws NullPointerException if requested is null
     */
    public DisplayMode chooseCompatibleDisplayMode(DisplayMode requested) {
        DisplayMode[] availableModes = getAvailableDisplayModes();

        // we assume there is at least 1 (would be the default)
        DisplayMode best = availableModes[0];
        int reqArea = requested.getWidth() * requested.getHeight();
        int bestArea = best.getWidth() * best.getHeight();
        for (int i = 1; i < availableModes.length; i++) {
            int area = availableModes[i].getWidth() * availableModes[i].getHeight();
            boolean update = false;
            if (Math.abs(area - reqArea) < Math.abs(bestArea - reqArea)) {
                // available[i] has a better  match with screen resolution
                update = true;
            } else if (Math.abs(area - reqArea) == Math.abs(bestArea - reqArea)) {
                // resolution tie, so evaluate the pixel format
                if (availableModes[i].getPixelFormat() == requested.getPixelFormat()) {
                    // exact match on format, go with available[i]
                    update = true;
                } else {
                    // go with the highest bit depth pixel format
                    if (availableModes[i].getPixelFormat().getBitDepth() > best.getPixelFormat()
                                                                               .getBitDepth()) {
                        update = true;
                    }
                }
            }

            if (update) {
                best = availableModes[i];
                bestArea = area;
            }
        }

        return best;
    }
}
