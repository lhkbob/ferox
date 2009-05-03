package com.ferox.renderer;

/**
 * <p>
 * A render surface that has a visible element, either because of a window or
 * it's fullscreen.
 * </p>
 * <p>
 * This provides common functionalities available to all onscreen windows.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface OnscreenSurface extends RenderSurface {
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
	 * returns false. It may also be used in a dock, system tray or other OS ui
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
	 * windowing system. This is dependent on the Renderer that was used to
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
	 * longer see the surface. It does not mean that the surface is no longer
	 * usable, since it may not have been destroyed.
	 * 
	 * @return Whether or not the window is visible
	 */
	public boolean isVisible();
}
