package com.ferox.renderer;

/**
 * A Context provides access to {@link Renderer} implementations to render into
 * a single Surface. It also controls when the rendered contents of the Surface
 * are flushed to their final destination (e.g. visible window or texture). When
 * executing Tasks, a HardwareAccessLayer is provided; the HardwareAccessLayer
 * provides Contexts by calling its setActiveSurface() methods. A single Context
 * can be active at any one time in a Task. Multiple contexts can be used in one
 * Task, although old contexts will be invalidated when setActiveSurface() is
 * called again.
 * 
 * @see HardwareAccessLayer
 * @author Michael Ludwig
 */
public interface Context {
    /**
     * @return True if {@link #getGlslRenderer()} will return a non-null
     *         renderer (ignoring which renderer was activated first)
     */
    public boolean hasGlslRenderer();

    /**
     * @return True if {@link #getFixedFunctionRenderer()} will return a
     *         non-null renderer (ignoring which renderer was activated first)
     */
    public boolean hasFixedFunctionRenderer();
    
    /**
     * <p>
     * Return a GlslRenderer to render into the surface that is currently in
     * use. This will return null if the Context cannot support a GlslRenderer.
     * Tasks can only use a one renderer per returned Context.
     * </p>
     * <p>
     * A GlslRenderer and FixedFunctionRenderer cannot be used at the same time.
     * If this is called after a call to {@link #getFixedFunctionRenderer()} on
     * the same context, an exception is thrown.
     * </p>
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
     * Context.
     * </p>
     * <p>
     * A FixedFunctionRenderer and GlslRenderer cannot be used at the same time.
     * If this is called after a call to {@link #getGlslRenderer()} on the same
     * context, an exception is thrown.
     * </p>
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
     * </p>
     * <p>
     * When rendering in multiple passes, a flush only needs to be performed in
     * the last pass. Alternatively, {@link Framework#flush(Surface, String)}
     * can be used as a convenience.
     * </p>
     */
    public void flush();
}
