package com.ferox.renderer;


/**
 * <p>
 * Represents a two dimensional surface that stores a collection of logical
 * color buffers that can be rendered into using a {@link Renderer}. These
 * surfaces could be offscreen surfaces, in that they have no visible monitor
 * region associated with their rendered pixels, or they could be an onscreen
 * window or exclusively fullscreen.
 * <p>
 * The exact representation is dependent on the renderer's implementations,
 * however some possibilities include surfaces that use framebuffer objects to
 * render directly into a set of textures.
 * 
 * @author Michael Ludwig
 */
public interface Surface {
    /**
     * <p>
     * Return whether or not the surface has been destroyed.
     * <p>
     * Destruction could be because of an explicit call to {@link #destroy()} or
     * {@link Framework#destroy()}, or because implementations provide a way for
     * them to be implicitly destroyed (such as an OnscreenSurface being closed
     * by the user).
     * 
     * @return True if this surface is no longer usable by its creating
     *         Framework.
     */
    public boolean isDestroyed();

    /**
     * Destroy this Surface, cleaning up internal resources connecting it to the
     * low-level graphics layer and hiding any onscreen elements, such as a
     * window. Destroying an already destroyed surface does nothing.
     */
    public void destroy();

    /**
     * Return the width of the actual drawable area of the surface (doesn't
     * include any border or frame). This is allowed to return any value after
     * the surface has been destroyed.
     * 
     * @return The width of the drawable area
     */
    public int getWidth();

    /**
     * Return the height of the actual drawable area of the surface (doesn't
     * include any border or frame). This is allowed to return any value after
     * the surface has been destroyed.
     * 
     * @return The height of the drawable area
     */
    public int getHeight();

    /**
     * Get the Framework that created this surface.
     * 
     * @return The Framework that created this surface, will not be null
     */
    public Framework getFramework();
}
