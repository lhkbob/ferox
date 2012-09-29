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
}
