package com.ferox.renderer;


/** Represents a window (in the user interface of the computer)
 * that can optionally be decorated and be resizable.  
 * Window surfaces are made visible when createWindowSurface()
 * is called.  They are hidden when destroy(surface) is invoked.
 * If the surface is decorated and a user closes the window, the
 * surface is implicitly destroyed (just as if it were passed into
 * destroy() ).
 * 
 * @author Michael Ludwig
 *
 */
public interface WindowSurface extends OnscreenSurface {
	/** Return true if the visible window doesn't have a frame,
	 * title bar, and other features that are part of the operating
	 * system's windowing system (such as minimize/close buttons). */
	public boolean isUndecorated();
	
	/** Return true if the window is resizable by user action.
	 * The surface can still be resized by calling setWidth()
	 * and setHeight(). */
	public boolean isResizable();
	
	/** Get the x coordinate, in screen space, of the top left
	 * corner of the window. */
	public int getX();
	
	/** Get the y coordinate, in screen space, of the top left
	 * corner of the window. */
	public int getY();
	
	/** Set the dimensions of the window to the given width
	 * and height.  It will extend these values, in pixels,
	 * down and to the right.  
	 * 
	 * This will affect the values returned by getWidth() and
	 * getHeight().  However, they may not be the same.  These
	 * dimensions represent the size of the window (possibly
	 * including a frame), while getWidth/Height() return the
	 * size of the actual drawable area of the surface.
	 * 
	 * If width or height are <= 0, the method does nothing. */
	public void setSize(int width, int height);
	
	/** Set the location of the window to the given screen points.
	 * 0 represents the left edge of the monitor for x and the top
	 * edge for y. */
	public void setLocation(int x, int y);
}
