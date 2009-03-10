package com.ferox.renderer;



/** A render surface that has a visible element,
 * either because of a window or it's fullscreen.
 * 
 * This provides common functionalities available
 * to all onscreen windows.
 * 
 * @author Michael Ludwig
 *
 */
public interface OnscreenSurface extends RenderSurface {
	/** Return true if the surface should have its update
	 * rate limited to the refresh rate of the monitor. */
	public boolean isVSyncEnabled();
	
	/** Set whether or not vsync should be enabled.  If it
	 * is enabled, the refresh limit is limited to that of the
	 * monitor's.  This is generally around 60 Hz. */
	public void setVSyncEnabled(boolean enable);
	
	/** Get the title string that appears in the window frame
	 * if isUndecorated() returns false.  It may also be used
	 * in a dock, system tray or other OS ui element to represent
	 * the window. */
	public String getTitle();
	
	/** Set the title string to be used by the window. 
	 * If title == null, the empty string is used. */
	public void setTitle(String title);
	
	/** Return the actual object corresponding to the window
	 * displayed by the windowing system.  This is dependent
	 * on the Renderer that was used to create the surface, but
	 * could be something such as java.awt.Frame or javax.swing.JFrame.
	 * 
	 * Return null if there is no actual object that is returnable
	 * for the window impl (e.g. Display in LWJGL).
	 * 
	 * Renderers must document the return class of this method for
	 * WindowSurfaces and FullscreenSurfaces. */
	public Object getWindowImpl();
	
	/** Return true if this surface is visible. This will
	 * return false if the surface was destroyed, closed,
	 * minimized, or hidden (e.g. the user can no longer see the
	 * surface.  It does not mean that the surface is no
	 * longer usable, since it may not have been destroyed. */
	public boolean isVisible();
}
