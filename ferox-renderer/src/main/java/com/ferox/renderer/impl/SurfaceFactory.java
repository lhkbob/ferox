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

import com.ferox.renderer.*;

/**
 * SurfaceFactory is a factory that interfaces with lower-level OpenGL wrappers to create concrete
 * implementations of Surfaces and OpenGLContextAdapters. Implementations must be thread safe. They are not
 * tied to the lifecycle of a particular framework.
 *
 * @author Michael Ludwig
 */
public interface SurfaceFactory {
    /**
     * Create an AbstractTextureSurface implementation. This is used by the AbstractFramework to implement
     * {@link Framework#createSurface(TextureSurfaceOptions)}. The returned surface must be an AbstractSurface
     * because AbstractFramework requires AbstractSurfaces to function properly.
     *
     * @param framework     The framework that will own the surface
     * @param options       The TextureSurfaceOptions used to create the surface
     * @param sharedContext The context to share resources with, this will not be null
     *
     * @return A new texture surface
     *
     * @throws NullPointerException if any of the arguments are null
     */
    public AbstractTextureSurface createTextureSurface(FrameworkImpl framework, TextureSurfaceOptions options,
                                                       OpenGLContext sharedContext);

    /**
     * Create an AbstractOnscreenSurface implementation. This is used by the AbstractFramework to implement
     * {@link Framework#createSurface(OnscreenSurfaceOptions)}. The returned surface must be an
     * AbstractSurface because AbstractFramework requires AbstractSurfaces to function properly.
     *
     * @param framework     The framework that will own the surface
     * @param options       The OnscreenSurfaceOptions used to create the surface
     * @param sharedContext The context to share resources with, this will not be null
     *
     * @return A new, visible onscreen surface
     *
     * @throws NullPointerException if any of the arguments are null
     */
    public AbstractOnscreenSurface createOnscreenSurface(FrameworkImpl framework,
                                                         OnscreenSurfaceOptions options,
                                                         OpenGLContext sharedContext);

    /**
     * Create an OpenGLContext that wraps an underlying OpenGL context. The context should be "offscreen" and
     * not attached to a Surface. This can be a context owned by a pbuffer, or it can be a context attached to
     * a 1x1 hidden window. The term "offscreen" is loosely defined but should not be noticeable by a user.
     *
     * @param sharedContext An OpenGLContext to share all resources with, if this is null then no sharing is
     *                      done
     *
     * @return An offscreen context
     */
    public OpenGLContext createOffscreenContext(OpenGLContext sharedContext);

    /**
     * @return The default display mode, as required {@link Framework#getDefaultDisplayMode()}
     */
    public DisplayMode getDefaultDisplayMode();

    /**
     * @return The current hardware capabilities
     */
    public Capabilities getCapabilities();

    /**
     * Perform any native resource destruction, called by the Framework when it's lifecycle is completed.
     */
    public void destroy();
}
