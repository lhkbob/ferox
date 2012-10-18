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
 * A Context provides access to {@link Renderer} implementations to render into
 * a single Surface. It also controls when the rendered contents of the Surface
 * are flushed to their final destination (e.g. visible window or texture). When
 * executing Tasks, a HardwareAccessLayer is provided; the HardwareAccessLayer
 * provides Contexts by calling its setActiveSurface() methods. Within a Task,
 * only a single context can be active at one time. Multiple contexts can be
 * used in one Task, although old contexts will be invalidated when
 * setActiveSurface() is called again.
 * 
 * @see HardwareAccessLayer
 * @author Michael Ludwig
 */
public interface Context {
    /**
     * @return True if {@link #getGlslRenderer()} will return a non-null
     *         renderer (ignoring which renderer was selected first)
     */
    public boolean hasGlslRenderer();

    /**
     * @return True if {@link #getFixedFunctionRenderer()} will return a
     *         non-null renderer (ignoring which renderer was selected first)
     */
    public boolean hasFixedFunctionRenderer();

    /**
     * <p>
     * Return a GlslRenderer to render into the surface that is currently in
     * use. This will return null if the Context cannot support a GlslRenderer.
     * Tasks can only use one renderer per returned Context. The Surface that
     * provided this context must be re-activated to reset the context to get at
     * a different renderer if needed.
     * <p>
     * A GlslRenderer and FixedFunctionRenderer cannot be used at the same time.
     * If this is called after a call to {@link #getFixedFunctionRenderer()} on
     * the same context, an exception is thrown.
     * 
     * @return A GlslRenderer to render into this Context's active Surface
     * @throws IllegalStateException if a FixedFunctionRenderer has already been
     *             returned
     */
    public GlslRenderer getGlslRenderer();

    /**
     * <p>
     * Return a FixedFunctionRenderer to render into the surface that is
     * currently in use. This will return null if the Context cannot support a
     * FixedFunctionRenderer. Tasks can only use a one renderer per returned
     * Context. The Surface that provided this context must be re-activated to
     * reset the context to get at a different renderer if needed.
     * <p>
     * A FixedFunctionRenderer and GlslRenderer cannot be used at the same time.
     * If this is called after a call to {@link #getGlslRenderer()} on the same
     * context, an exception is thrown.
     * 
     * @return A FixedFunctionRenderer to render into this Context's active
     *         Surface
     * @throws IllegalStateException if a GlslRenderer has already been returned
     */
    public FixedFunctionRenderer getFixedFunctionRenderer();

    /**
     * <p>
     * Flush all rendered content to this Context's surface. When using an
     * OnscreenSurface, this will generally flip a buffer to make the rendered
     * context visible (i.e. double-buffering). With TextureSurfaces, this may
     * copy the content from an offscreen buffer into the texture.
     * Alternatively, TextureSurfaces using fbo's will not perform any time
     * consuming operations because the textures are rendered into directly.
     * This method should be called regardless of type of surface because Tasks
     * cannot know before hand what flushing will do.
     * <p>
     * If rendering is done across in multiple tasks, a flush only needs to be
     * performed in the last pass. Alternatively,
     * {@link Framework#flush(Surface)} can be used as a convenience.
     */
    public void flush();

    /**
     * Get the Surface that was the argument to
     * {@link HardwareAccessLayer#setActiveSurface(Surface)} or
     * {@link HardwareAccessLayer#setActiveSurface(TextureSurface, int)} that
     * initially produced this Context.
     * 
     * @return The Surface for this context
     */
    public Surface getSurface();

    /**
     * Get the active surface layer that this Context applies to. This is only
     * meaningful if the surface is a TextureSurface and was activated using
     * {@link HardwareAccessLayer#setActiveSurface(TextureSurface, int)}.
     * 
     * @return The active layer of this context's surface
     */
    public int getSurfaceLayer();
}
