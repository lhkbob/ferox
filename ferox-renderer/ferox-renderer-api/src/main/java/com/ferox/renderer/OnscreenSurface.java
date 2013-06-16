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

import com.ferox.input.MouseKeyEventSource;

/**
 * A renderable Surface that has a visible element, either because it's a window or it's
 * fullscreen. Multiple windowed OnscreenSurfaces can be used at the same time. Like
 * {@link Surface}, all methods exposed by OnscreenSurface have no defined return value
 * after the surface has been destroyed.
 *
 * @author Michael Ludwig
 */
public interface OnscreenSurface extends Surface, MouseKeyEventSource {
    /**
     * @return The display mode of the surface, which is either the default display mode
     *         for windowed surfaces, or the activated fullscreen mode
     */
    public DisplayMode getDisplayMode();

    /**
     * @return The number of MSAA samples used in the surface, or 0 if MSAA is disabled
     *         for the surface
     */
    public int getMultiSamples();

    /**
     * @return True if this OnscreenSurface is the active fullscreen surface
     */
    public boolean isFullscreen();

    /**
     * Return true if the surface should have its update rate limited to the refresh rate
     * of the monitor.
     *
     * @return True if rendering is limited to refresh rate
     *
     * @see #setVSyncEnabled(boolean)
     */
    public boolean isVSyncEnabled();

    /**
     * Set whether or not vsync should be enabled. If it is enabled, the refresh limit is
     * limited to that of the monitor's. This is generally around 60 Hz. This is not a
     * guaranteed operation, it should be considered a request to the underlying system.
     *
     * @param enable Whether or not vsyncing is used
     */
    public void setVSyncEnabled(boolean enable);

    /**
     * Get the title string that appears in the window frame is undecorated. It may also
     * be used in a dock, system tray or other OS UI element to represent the window.
     *
     * @return The title displayed for this window element by the OS
     */
    public String getTitle();

    /**
     * Set the title string to be used by the window. If title == null, the empty string
     * is used.
     *
     * @param title The new title for this surface, null == ""
     */
    public void setTitle(String title);

    /**
     * Get the x coordinate, in screen space, of the top left corner of the window.
     *
     * @return X coordinate of the window in the OS's desktop
     */
    public int getX();

    /**
     * Get the y coordinate, in screen space, of the top left corner of the window.
     *
     * @return Y coordinate of the window in the OS's desktop
     */
    public int getY();

    /**
     * <p/>
     * Set the dimensions of the window to the given width and height. It will extend
     * these values, in pixels, down and to the right.
     * <p/>
     * This will affect the values returned by getWidth() and getHeight(). However, they
     * may not be the same. These dimensions represent the size of the window (possibly
     * including a frame), while getWidth/Height() return the size of the actual drawable
     * area of the surface.
     * <p/>
     * Fullscreen windows cannot be resized, so an exception is thrown if this is
     * attempted on a fullscreen window.
     *
     * @param width  Width of the window, may not represent the drawable width
     * @param height Height of the window, may not represent the drawable height
     *
     * @throws IllegalArgumentException if width or height are less than or equal to 0
     * @throws IllegalStateException    if the surface is fullscreen
     */
    public void setWindowSize(int width, int height);

    /**
     * Set the location of the window to the given screen points. 0 represents the left
     * edge of the monitor for x and the top edge for y. Fullscreen windows are always
     * located at (0, 0), so an exception is thrown if this is attempted on a fullscreen
     * window.
     *
     * @param x The new x coordinate for the window
     * @param y The new y coordinate for the window
     *
     * @throws IllegalStateException if the surface is fullscreen
     */
    public void setLocation(int x, int y);

    /**
     * Return whether or not a user can close this window using the close button that most
     * windowing managers provide. An undecorated window can still be "closable" although
     * it might not be possible if the OS provides no alternate close method for the user.
     * The default value is true.
     *
     * @return True if the window can be closed by the user
     */
    public boolean isCloseable();

    /**
     * Set whether or not a user is allowed to close the window via a close button in the
     * window's frame or decorations. When a user closes a window manually, the surface is
     * destroyed just as if {@link #destroy()} had been called.
     *
     * @param userCloseable True if the user can close the window
     */
    public void setCloseable(boolean userCloseable);
}
