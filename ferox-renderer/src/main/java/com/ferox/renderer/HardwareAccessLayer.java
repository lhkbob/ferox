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

/**
 * The HardwareAccessLayer is the direct interface Tasks use to access graphics hardware. Each Task that is
 * run by a Framework is provided a HardwareAccessLayer to then activate surfaces and use the returned
 * contexts to access the approprate renderers. The HardwareAccessLayer is tied to the thread executing the
 * Task so it (and the related {@link Context}) must not be held onto outside of the Task's execution period.
 * <p/>
 * Surfaces can be rendered into by first activating them with setActiveSurface(), and then using one of the
 * Renderer's from the Context associated with the active surface. In order for a user to see the actual
 * rendered content, the surface must be {@link Context#flush() flushed}.
 *
 * @author Michael Ludwig
 * @see Context
 */
public interface HardwareAccessLayer {
    /**
     * <p/>
     * Set the active Surface for rendering when using this hardware access layer. A Surface must be active in
     * order to use any Renderers (which are provided by the returned Context). This may be called multiple
     * times by the same Task, although only one Surface will be active for the task at a given time.
     * <p/>
     * The returned Context is only valid while the specified surface is active. If this is called again with
     * any surface (including this one), the old Context must be discarded and the new Context should be
     * used.
     * <p/>
     * Calling this method with a null surface is allowed. A null surface will return a null context. Using a
     * null surface effectively restores the hardware access layer to its initial state when the Task began
     * executing.
     * <p/>
     * If the provided Surface is a TextureSurface, the surface will be activated using its last render target
     * configuration. Its render target configuration was either determined by the options specified when it
     * was created, or by a call to one of the setActiveSurface() variants that take render targets.
     * <p/>
     * If the surface has its {@link Surface#destroy()} method called before it is activated, a null Context
     * is returned. A Surface cannot be destroyed until it has been deactivated, in which case the destruction
     * action will wait until it's released. The only exception to this is if the Thread running the Task
     * calls destroy. In this case, the Surface is deactivated automatically, destroyed, and its context must
     * be discarded.
     *
     * @param surface The Surface to activate, or null to deactivate the current surface
     *
     * @return A Context to use for the activation lifetime of the given Surface, or null if surface was null
     * was or destroyed
     *
     * @throws IllegalArgumentException if the surface was created by another Framework
     */
    public Context setActiveSurface(Surface surface);

    /**
     * Activate the given TextureSurface and update its color target configuration to render into the single
     * color target. This is equivalent to calling {@code setActiveSurface(surface, singleColorBuffer,
     *null)}.
     *
     * @param surface           The texture surface to activate
     * @param singleColorBuffer The new color render target
     *
     * @return A context to use, or null if the surface was destroyed
     *
     * @throws IllegalArgumentException if the surface was created by another Framework, or if the dimensions
     *                                  of the surface and target don't match
     * @see #setActiveSurface(TextureSurface, com.ferox.renderer.Sampler.RenderTarget[],
     * com.ferox.renderer.Sampler.RenderTarget)
     */
    public Context setActiveSurface(TextureSurface surface, Sampler.RenderTarget singleColorBuffer);

    /**
     * Activate the given TextureSurface and update its color target configuration to render into the single
     * color target. Update the depth target to {@code depthBuffer}. This is equivalent to calling:
     * <pre>
     *     RenderTarget targets = new RenderTarget[1];
     *     targets[0] = singleColorBuffer;
     *     setActiveSurface(surface, targets, depthBuffer):
     * </pre>
     *
     * @param surface           The texture surface to activate
     * @param singleColorBuffer The new color render target
     * @param depthBuffer       The new depth/stencil target
     *
     * @return A context to use, or null if the surface was destroyed
     *
     * @throws IllegalArgumentException if the surface was created by another Framework, if the dimensions of
     *                                  the surface and target don't match, or if the surface has a depth
     *                                  render buffer but the depth target is non-null
     * @see #setActiveSurface(TextureSurface, com.ferox.renderer.Sampler.RenderTarget[],
     * com.ferox.renderer.Sampler.RenderTarget)
     */
    public Context setActiveSurface(TextureSurface surface, Sampler.RenderTarget singleColorBuffer,
                                    Sampler.RenderTarget depthBuffer);

    /**
     * Activate the given TextureSurface and update its color target configuration to render into multiple
     * color targets. If the length of {@code colorBuffers} is less than the number of supported targets, the
     * remaining targets are cleared. An exception is thrown if more color targets are provided than are
     * supported. If there are no color targets, then rendering will proceed without a color buffer. This can
     * be useful if only the depth information is required in a particular pass.
     * <p/>
     * OpenGL defines potentially many color buffers that can be rendered into simultaneously. Shaders must be
     * configured to map from their declared output variables to the color buffers. Each color buffer is
     * indexed starting at 0. The index of each render target in the {@code colorBuffers} array is the same
     * color buffer index that must line up with the intended shader's configuration or your rendering results
     * will not be correct.
     * <p/>
     * The depth/stencil target will be bound to {@code depthBuffer}. If the texture surface was created to
     * have a depth renderbuffer, {@code depthBuffer} must be null. If {@code depthBuffer} is null and there
     * is no renderbuffer then rendering will be performed without the depth test. The same goes for the
     * stencil test.
     * <p/>
     * The dimensions of the texture surface and textures providing the render targets must be equal.
     *
     * @param surface      The texture surface to activate
     * @param colorBuffers The new color render targets
     * @param depthBuffer  The new depth/stencil target
     *
     * @return A context to use, or null if the surface was destroyed
     *
     * @throws IllegalArgumentException if the surface was created by another Framework, if the dimensions of
     *                                  the surface and target don't match, if the surface has a depth render
     *                                  buffer but the depth target is non-null, or if too many color targets
     *                                  are provided
     * @see #setActiveSurface(Surface)
     */
    public Context setActiveSurface(TextureSurface surface, Sampler.RenderTarget[] colorBuffers,
                                    Sampler.RenderTarget depthBuffer);

    /**
     * Get the current Context, which is the same context that was returned by the last call to
     * setActiveSurface(). If there is no current context, null is returned.
     *
     * @return The current context
     */
    public Context getCurrentContext();

    /**
     * Refresh the dynamic state of the resource, performing the same actions as {@link
     * com.ferox.renderer.Resource#refresh()} except the refresh is performed immediately on the calling
     * thread (i.e. the internal framework thread). The resource is guaranteed refreshed after this returns
     * and avoids the cost of allocating a task or future object.
     * <p/>
     * This is the preferred method for refreshing resources when performance is critical.
     * <p/>
     * Refreshing a resource may affect the state of any renderer that is in use. This can potentially
     * invalidate any buffer or texture configuration. It is best to only perform refreshes before or after
     * using a renderer, but never intermingled.
     *
     * @param resource The resource to update
     *
     * @throws NullPointerException     if the resource is null
     * @throws IllegalArgumentException if the resource was created by another Framework
     */
    public void refresh(Resource resource);

    /**
     * Manually destroy the resource. This performs the same actions as {@link
     * com.ferox.renderer.Resource#destroy()} except the destruction of the internal GPU resources is
     * performed immediately before the method returns. This does nothing if the resource has already been
     * destroyed by any means.
     * <p/>
     * This is the preferred method for destroying resources when performance and exact control over timing is
     * necessary. Like {@link #refresh(Resource)} it avoids the cost of allocating and queuing a task.
     * Destroying a resource may affect the state of any renderer that it in use. This can potentially
     * invalidate any buffer or texture configuration. It is best to only perform destructive actions before
     * after using the renderr, but never intermingled..
     *
     * @param resource The resource to destroy
     *
     * @throws NullPointerException     if the resource is null
     * @throws IllegalArgumentException if the resource was created by another Framework
     */
    public void destroy(Resource resource);
}
