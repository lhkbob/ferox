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
     * <p>
     * Return a GlslRenderer to render into the surface that is currently in
     * use. This will return null if the Context cannot support a GlslRenderer.
     * A GlslRenderer and FixedFunctionRenderer can be used at the same time,
     * although this is highly discouraged as it will hurt performance. Tasks
     * should use a one renderer per returned Context.
     * </p>
     * 
     * @return A GlslRenderer to render into this Context's active Surface
     */
    public GlslRenderer getGlslRenderer();

    /**
     * <p>
     * Return a FixedFunctionRenderer to render into the surface that is
     * currently in use. This will return null if the Context cannot support a
     * FixedFunctionRenderer. A GlslRenderer and FixedFunctionRenderer can be
     * used at the same time, although this is highly discouraged as it will
     * hurt performance. Tasks should use a one renderer per returned Context.
     * </p>
     * 
     * @return A FixedFunctionRenderer to render into this Context's active
     *         Surface
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
     * Regardless, this method should be called regardless of type of surface
     * because Tasks cannot know before hand what flushing will do.
     * </p>
     * <p>
     * When rendering in multiple passes, a flush only needs to be performed in
     * the last pass. Alternatively, {@link Framework#flush(Surface, String)}
     * can be used as a convenience.
     * </p>
     */
    public void flush();
}
