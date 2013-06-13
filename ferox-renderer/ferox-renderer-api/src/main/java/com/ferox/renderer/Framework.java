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
package com.ferox.renderer;

import com.ferox.renderer.builder.*;

import java.util.concurrent.Future;

/**
 * <p/>
 * The Framework is the main entry point to the rendering API. It controls the creation of
 * {@link Surface surfaces}, which store the final render outputs, and provides {@link
 * HardwareAccessLayer} and {@link Context} implementations that allow actual use of
 * Renderers and Resources. A Framework acts as an advanced task execution service that
 * queues up {@link Task tasks} to run on internal threads that can communicate with
 * low-level graphics drivers.
 * <p/>
 * It is not defined how many threads are used internally to execute tasks. Given the
 * state of current desktop hardware and OSes (aka Windows), it will likely use a single
 * thread. This is reasonably because there is generally only one GPU.
 * <p/>
 * Framework implementations must be thread safe so that a single Framework instance can
 * be used from multiple threads. The thread safety of Renderers, Contexts and
 * HardwareAccessLayers is not defined because their exposure is carefully controlled by
 * task invocation.
 * <p/>
 * An important part of a Framework implementation is resource management. All Frameworks
 * are required to automatically clean up internal resource data when a {@link Resource}
 * is garbage collected.
 * <p/>
 * Most low-level graphics languages have the concept of a context, where a thread needs
 * an active context to be able to communicate with the graphics hardware. The contexts
 * provided by a Framework are on a much higher level but have a similar scope of
 * usability; they only function on threads managed by the Framework that control the
 * low-level driver access.
 *
 * @author Michael Ludwig
 */
public interface Framework extends Destructible {
    /**
     * Return an array of available DisplayModes that can be used when creating fullscreen
     * surfaces with {@link #createSurface(OnscreenSurfaceOptions)}. The returned array
     * can be modified because a defensive copy is returned.
     *
     * @return All available display modes on the system
     */
    public DisplayMode[] getAvailableDisplayModes();

    /**
     * Return the DisplayMode representing the default display mode selected when the
     * surface is no longer fullscreen. This will be the original display mode selected by
     * the user before they started the application.
     *
     * @return The default DisplayMode.
     */
    public DisplayMode getDefaultDisplayMode();


    /**
     * <p/>
     * Return the current exclusive fullscreen surface. There can only be one fullscreen
     * surface at a time. While this returns a non-null value, attempts to create new
     * OnscreenSurfaces will fail. Null is returned if there is no fullscreen surface or
     * after the exclusive surface gets destroyed.
     * <p/>
     * If a non-null surface is returned, its {@link OnscreenSurface#isFullscreen()
     * isFullscreen()} method will return true.
     *
     * @return The current fullscreen surface, or null
     */
    public OnscreenSurface getFullscreenSurface();

    // FIXME get rid of the clauses saying frameworks can modify the options to create
    // a successful surface. It's all or nothing at this point, makes the implementation
    // easier and fallback code is easier because the application knows what each failure
    // was, instead of having to dig into the returned surface and check what changed

    /**
     * <p/>
     * Create a OnscreenSurface with the given options. These parameters are requests to
     * the underlying Framework, which will try its best to follow them. When the window
     * surface is returned, it will be visible and on screen.
     * <p/>
     * If any of the options have unsupported values, the Framework may change them to
     * successfully create a surface.
     * <p/>
     * If there is already a fullscreen surface and <var>options</var> would create a new
     * fullscreen surface, an exception is thrown. It is possible to have standard
     * windowed surfaces and fullscreen surface, although the windowed surfaces will be
     * hidden until the fullscreen surface is destroyed.
     * <p/>
     * Some Frameworks may not support multiple OnscreenSurfaces depending on their
     * windowing libraries.
     *
     * @param options Requested pixel format and initial configuration of the surface
     *
     * @return The created surface, or null if the Framework has been destroyed
     *
     * @throws NullPointerException     if options is null
     * @throws SurfaceCreationException if the Framework cannot create the
     *                                  OnscreenSurface
     */
    public OnscreenSurface createSurface(OnscreenSurfaceOptions options);

    /**
     * <p/>
     * Create a TextureSurface that can be used to render into textures. The Framework
     * will create new textures that can be retrieved by calling the returned surface's
     * {@link TextureSurface#getColorBuffer(int)} and {@link TextureSurface#getDepthBuffer()}.
     * The size and texture format of the {@link Texture textures} used for the
     * TextureSurface are determined by the provided <var>options</var>.
     * <p/>
     * If <var>options</var> is unsupported, the Framework is permitted to choose options
     * that allow it to create a valid TextureSurface. This includes changing the format
     * or dimensions to fit within hardware limits.
     *
     * @param options The requested options for configuring the created surface
     *
     * @return The created surface, or null if the Framework has been destroyed
     *
     * @throws NullPointerException     if options is null
     * @throws SurfaceCreationException if the TextureSurface can't be created
     */
    public TextureSurface createSurface(TextureSurfaceOptions options);

    /**
     * <p/>
     * Queue the given Task to be run as soon as possible by internal threads managed by
     * the Framework. The Framework must support receiving tasks from multiple threads
     * safely. Ordering of queued tasks across multiple threads depends on the scheduling
     * of threads. Tasks queued from the same thread will be invoked in the order
     * received.
     * <p/>
     * If the Framework is destroyed before a Task has started, its returned Future will
     * be canceled. Calls to this method are ignored if the Framework is already
     * destroyed.
     *
     * @param <T>  The return type of the Task
     * @param task The task to run with a valid context
     *
     * @return A future linked to the given task
     *
     * @throws NullPointerException if the task is null
     */
    public <T> Future<T> queue(Task<T> task);

    /**
     * <p/>
     * Convenience method to queue a Task that will dispose the given resource by calling
     * {@link Context#dispose(Resource)}. It is not recommended to use this method if
     * resources are being updated and disposed of with custom Tasks.
     * <p/>
     * This method does nothing if the Framework has been destroyed. Since the Framework's
     * destruction disposes any resource data anyway, this is not a problem. This will
     * unblock and return if the Framework is destroyed while waiting for the dispose to
     * complete.
     *
     * @param resource The resource to dispose
     *
     * @throws NullPointerException if resource is null
     */
    public void destroy(Resource resource);

    /**
     * <p/>
     * Convenience method to queue a Task that will flush the provided surface. If the
     * flush is not being performed by already queued tasks, this is needed to ensure that
     * any rendering is made visible to the surface. If the surface is a TextureSurface
     * with multiple render targets, only its default active layer is flushed. If finer
     * control is needed, a custom task will need to be queued instead.
     * <p/>
     * An exception is thrown if the surface is not owned by the Framework. If the
     * provided surface has been destroyed, this method will do nothing. It is not best
     * practice to queue or use surfaces that have been destroyed, but this behavior is
     * safe in order to play nicely with onscreen surfaces that can be closed by the user
     * at any time. If the Framework is destroyed, this will do nothing and return
     * immediately. This will also return as a soon as a Framework is destroyed if this
     * was actively blocking.
     *
     * @param surface The surface to flush
     *
     * @throws NullPointerException     if surface is null
     * @throws IllegalArgumentException if the surface was not created by this Framework
     */
    public Future<Void> flush(Surface surface);

    /**
     * <p/>
     * Block the calling thread until all tasks queued prior to this method call have
     * completed. Tasks queued from other threads after this is invoked will not be
     * processed until this method returns.
     * <p/>
     * If the Framework is destroyed, this will return immediately. This will also return
     * as soon as a Framework is destroyed if this was actively blocking.
     */
    public void sync();

    public VertexBufferBuilder newVertexBuffer();

    public ElementBufferBuilder newElementBuffer();

    public ShaderBuilder newShader();

    public Texture1DBuilder newTexture1D();

    public Texture2DBuilder newTexture2D();

    public TextureCubeMapBuilder newTextureCubeMap();

    public Texture3DBuilder newTexture3D();

    public Texture1DArrayBuilder newTexture1DArray();

    public Texture2DArrayBuilder newTexture2DArray();

    public DepthMap2DBuilder newDepthMap2D();

    public DepthCubeMapBuilder newDepthCubeMap();

    /**
     * Get the capabilities of this Framework. This is allowed to return null after the
     * Framework is destroyed although Frameworks might not behave this way.
     *
     * @return The RenderCapabilities for this Framework
     */
    public RenderCapabilities getCapabilities();
}
