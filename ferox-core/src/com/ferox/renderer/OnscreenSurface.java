package com.ferox.renderer;

import com.ferox.input.MouseKeyEventSource;

/**
 * <p>
 * A renderable Surface that has a visible element, either because it's a window or
 * it's fullscreen. Multiple OnscreenSurfaces can exist at a given time.
 * 
 * @author Michael Ludwig
 */
public interface OnscreenSurface extends Surface, MouseKeyEventSource {
    /**
     * @return The options used to create this Surface. Any changes to the
     *         returned options will not be reflected by the Surface or future
     *         calls to this method.
     */
    public OnscreenSurfaceOptions getOptions();
    
    /**
     * @see #setFullscreen(boolean)
     * @return True if this surface is exclusively fullscreen.
     */
    public boolean isFullscreen();

    /**
     * <p>
     * Set whether or not this OnscreenSurface is a fullscreen window. A
     * fullscreen window automatically appears undecorated and fills the primary
     * monitor's screen. It will exclude other windows from appearing above it.
     * When being made fullscreen, an OnscreenSurface will use the last assigned
     * DisplayMode, or the requested DisplayMode at creation time. If fullscreen
     * parameters were not provided when the surface was created, the current
     * display mode of the user's computer will be used instead.
     * </p>
     * <p>
     * Only one OnscreenSurface can be made fullscreen at a given time. Attempts
     * to make a second surface fullscreen will result in an exception. A
     * destroyed surface will automatically restore the original display mode.
     * </p>
     * 
     * @param fullscreen True if this window should go into exclusive fullscreen
     *            mode
     * @throws IllegalStateException if there is an existing fullscreen window
     */
    public void setFullscreen(boolean fullscreen);

    /**
     * Set the DisplayMode resolution to use when this OnscreenSurface is in
     * fullscreen. When the surface is not in fullscreen mode, the current
     * DisplayMode is ignored and will use the original resolution before the
     * surface was created (the assigned DisplayMode will be used, however, when
     * made fullscreen again). If <tt>mode</tt> is null, the original
     * DisplayMode is restored.
     * 
     * @param mode The new DisplayMode to use
     * @throws UnsupportedOperationException if mode is not an available display
     *             mode
     */
    public void setDisplayMode(DisplayMode mode);

    /**
     * Return the current DisplayMode in use. If the surface is not in
     * fullscreen mode, this returns a DisplayMode representing the original
     * resolution, or default display mode, of the user's monitor. In this
     * situation, this reflects the current DisplayMode and not the DisplayMode
     * that will be used when the surface is made fullscreen again (which will
     * use the last assigned mode).
     * 
     * @return The current DisplayMode
     */
    public DisplayMode getDisplayMode();

    /**
     * Return an array of available DisplayModes for this surface. The returned
     * array can be modified, as it is a defensive copy. Although when an
     * OnscreenSurface is first created, it will choose a best-fit DisplayMode
     * to use based on the requested options, calls to
     * {@link #setDisplayMode(DisplayMode)} require that it use a DisplayMode
     * that is supported. This method returns all available DisplayModes that
     * can be passed to {@link #setDisplayMode(DisplayMode)} without failing,
     * any other resolution will throw an exception when assigned.
     * 
     * @return An array of available DisplayModes
     */
    public DisplayMode[] getAvailableDisplayModes();
    
    /**
     * Return true if the surface should have its update rate limited to the
     * refresh rate of the monitor.
     * 
     * @see #setVSyncEnabled(boolean)
     * @return True if rendering is limited to refresh rate
     */
    public boolean isVSyncEnabled();

    /**
     * Set whether or not vsync should be enabled. If it is enabled, the refresh
     * limit is limited to that of the monitor's. This is generally around 60
     * Hz. This is not a guaranteed operation, it should be considered a request
     * to the underlying system.
     * 
     * @param enable Whether or not vsyncing is used
     */
    public void setVSyncEnabled(boolean enable);

    /**
     * Get the title string that appears in the window frame if isUndecorated()
     * returns false. It may also be used in a dock, system tray or other OS UI
     * element to represent the window.
     * 
     * @return The title displayed for this window element by the OS
     */
    public String getTitle();

    /**
     * Set the title string to be used by the window. If title == null, the
     * empty string is used.
     * 
     * @param title The new title for this surface, null == ""
     */
    public void setTitle(String title);

    /**
     * <p>
     * Return the actual object corresponding to the window displayed by the
     * windowing system. This is dependent on the Framework that was used to
     * create the surface, but could be something such as java.awt.Frame or
     * javax.swing.JFrame.
     * </p>
     * <p>
     * Return null if there is no actual object that is returnable for the
     * window impl.
     * </p>
     * <p>
     * Renderers must document the return class of this method for
     * WindowSurfaces and FullscreenSurfaces.
     * </p>
     * 
     * @return An implementation dependent object representing the displayed
     *         window
     * @deprecated Because it's unrecommended to depend on this method.
     */
    @Deprecated
    public Object getWindowImpl();

    /**
     * Return true if this surface is visible. This will return false if the
     * surface was destroyed, closed, minimized, or hidden (e.g. the user can no
     * longer see the surface). It does not mean that the surface is no longer
     * usable, since it may not have been destroyed. The ability of a surface to
     * report when it's been hidden (e.g. minimized) is OS dependent.
     * 
     * @return Whether or not the window is visible
     */
    public boolean isVisible();

    /**
     * Return true if the visible window doesn't have a frame, title bar, and
     * other features that are part of the operating system's windowing system
     * (such as minimize/close buttons). When a window is fullscreen, this
     * should return true or false based on the window's appearance were it to
     * be made 'windowed' again.
     * 
     * @return True if the displayed window has no framing UI elements displayed
     */
    public boolean isUndecorated();

    /**
     * Return true if the window is resizable by user action. The surface can
     * still be resized by calling {@link #setWindowSize(int, int)} programatically.
     * 
     * @return True if the displayed window is resizable by the user
     */
    public boolean isResizable();

    /**
     * Get the x coordinate, in screen space, of the top left corner of the
     * window.
     * 
     * @return X coordinate of the window in the OS's desktop
     */
    public int getX();

    /**
     * Get the y coordinate, in screen space, of the top left corner of the
     * window.
     * 
     * @return Y coordinate of the window in the OS's desktop
     */
    public int getY();

    /**
     * <p>
     * Set the dimensions of the window to the given width and height. It will
     * extend these values, in pixels, down and to the right.
     * </p>
     * <p>
     * This will affect the values returned by getWidth() and getHeight().
     * However, they may not be the same. These dimensions represent the size of
     * the window (possibly including a frame), while getWidth/Height() return
     * the size of the actual drawable area of the surface.
     * </p>
     * <p>
     * If this window is currently fullscreen, the changes to size will not take
     * affect until the window is no longer fullscreen.
     * </p>
     * 
     * @param width Width of the window, may not represent the drawable width
     * @param height Height of the window, may not represent the drawable height
     * @throws IllegalArgumentException if width or height are less than or
     *             equal to 0
     */
    public void setWindowSize(int width, int height);

    /**
     * Set the location of the window to the given screen points. 0 represents
     * the left edge of the monitor for x and the top edge for y.
     * 
     * @param x The new x coordinate for the window
     * @param y The new y coordinate for the window
     */
    public void setLocation(int x, int y);
}
