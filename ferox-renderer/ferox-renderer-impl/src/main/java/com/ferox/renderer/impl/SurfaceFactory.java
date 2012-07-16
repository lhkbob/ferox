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
public interface SurfaceFactory {
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
    public AbstractTextureSurface createTextureSurface(AbstractFramework framework, TextureSurfaceOptions options, 
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
    public AbstractOnscreenSurface createOnscreenSurface(AbstractFramework framework, OnscreenSurfaceOptions options, 
                                                         OpenGLContext sharedContext);

    /**
     * Create an OpenGLContext that wraps an underlying OpenGL context.
     * The context should be "offscreen" and not attached to a Surface. This can
     * be a context owned by a pbuffer, or it can be a context attached to a 1x1
     * hidden window. The term "offscreen" is loosely defined but should not be
     * noticeable by a user.
     * 
     * @param sharedContext An OpenGLContext to share all resources with,
     *            if this is null then no sharing is done
     * @return An offscreen context
     */
    public OpenGLContext createOffscreenContext(OpenGLContext sharedContext);

    /**
     * @return The default display mode, as required
     *         {@link Framework#getDefaultDisplayMode()}
     */
    public DisplayMode getDefaultDisplayMode();
    
    /**
     * @return Available display modes, as required by
     *         {@link Framework#getAvailableDisplayModes()}
     */
    public DisplayMode[] getAvailableDisplayModes();
}
