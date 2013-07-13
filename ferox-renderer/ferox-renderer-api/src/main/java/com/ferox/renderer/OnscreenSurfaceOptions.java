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
 * OnscreenSurfaceOptions represents the set of configurable parameters used to create an OnscreenSurface.
 * Each OnscreenSurfaceOptions instance is immutable, the setters available return new instances that match
 * the calling instance except for the new parameter value.
 *
 * @author Michael Ludwig
 */
public final class OnscreenSurfaceOptions {
    private final boolean undecorated;
    private final boolean resizable;

    private final int width;
    private final int height;

    private final int x;
    private final int y;

    private final DisplayMode fullMode;

    private final int depthBits;
    private final int msaa;
    private final int stencilBits;

    /**
     * Create a default options configuration, which is a 24-bit depth buffer, no stencil buffer, no MSAA, and
     * a decorated, resizable window at (0, 0) with dimensions 600 x 600.
     */
    public OnscreenSurfaceOptions() {
        this(24, 0, 0, false, true, 0, 0, 600, 600, null);
    }

    private OnscreenSurfaceOptions(int depthBits, int msaa, int stencilBits, boolean undecorated,
                                   boolean resizable, int x, int y, int width, int height,
                                   DisplayMode fullMode) {
        this.undecorated = undecorated;
        this.resizable = resizable;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;

        this.fullMode = fullMode;
        this.depthBits = depthBits;
        this.msaa = msaa;
        this.stencilBits = stencilBits;
    }

    /**
     * Create a new options that is updated to use a windowed OnscreenSurface with the given starting
     * dimensions. Any fullscreen mode is ignored.
     *
     * @param width  The width of the window in pixels
     * @param height The height of the window in pixels
     *
     * @return New options configured for the given dimensions
     */
    public OnscreenSurfaceOptions windowed(int width, int height) {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, undecorated, resizable, x, y, width,
                                          height, null);
    }

    /**
     * Create a new options that is updated to set the location of the of a windowed OnscreenSurface. The
     * options are coerced to a windowed surface if there was a non-null fullscreen display mode.
     *
     * @param x The x location of the window in pixels from the left edge of the monitor
     * @param y The y location of the window in pixels from the top of the monitor
     *
     * @return New options configured for the given location
     */
    public OnscreenSurfaceOptions locatedAt(int x, int y) {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, undecorated, resizable, x, y, width,
                                          height, null);
    }

    /**
     * Create a new options that is updated to use a fullscreen OnscreenSurface that changes the monitor's
     * display mode to the given configuration. The location of the window is set to (0, 0), the size is set
     * to the dimensions of the display, and it is marked as an undecorated, fixed-size window.
     * <p/>
     * The display mode should have come from or be equal to one of the available display modes reported by
     * {@link com.ferox.renderer.Capabilities#getAvailableDisplayModes()}.
     *
     * @param mode The fullscreen display mode to activate
     *
     * @return New options configured for a fullscreen surface
     */
    public OnscreenSurfaceOptions fullScreen(DisplayMode mode) {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, true, false, 0, 0, mode.getWidth(),
                                          mode.getHeight(), mode);
    }

    /**
     * Create a new options that is updated to request a depth buffer with given number of bits. The bit size
     * must be one of the values reported by {@link com.ferox.renderer.Capabilities#getAvailableDepthBufferSizes()}.
     *
     * @param depthBits The requested depth buffer size
     *
     * @return New options with the new depth buffer configuration
     */
    public OnscreenSurfaceOptions withDepthBuffer(int depthBits) {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, undecorated, resizable, x, y, width,
                                          height, fullMode);
    }

    /**
     * Create a new options that is updated to request a stencil buffer with given number of bits. The bit
     * size must be one of the values reported by {@link com.ferox.renderer.Capabilities#getAvailableStencilBufferSizes()}.
     *
     * @param stencilBits The requested stencil buffer size
     *
     * @return New options with the new depth stencil configuration
     */
    public OnscreenSurfaceOptions withStencilBuffer(int stencilBits) {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, undecorated, resizable, x, y, width,
                                          height, fullMode);
    }

    /**
     * Create a new options that is updated to request a surface using MSAA with the number of samples. Zero
     * samples disables MSAA, but this is the default. The sample count must be one of the values reported by
     * {@link Capabilities#getAvailableSamples()}.
     *
     * @param samples The requested MSAA sample count
     *
     * @return New options with the new MSAA configuration
     */
    public OnscreenSurfaceOptions withMSAA(int samples) {
        return new OnscreenSurfaceOptions(depthBits, samples, stencilBits, undecorated, resizable, x, y,
                                          width, height, fullMode);
    }

    /**
     * Create a new options that marks the surface as being undecorated. Because {@link
     * #fullScreen(DisplayMode)} already marks the surface as undecorated, this only needs to be called when
     * using a windowed surface that should be undecorated.
     *
     * @return New options marking the surface as undecorated
     */
    public OnscreenSurfaceOptions undecorated() {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, true, resizable, x, y, width, height,
                                          fullMode);
    }

    /**
     * Create a new options that marks the surface as not being resizable by the user. The surface can still
     * be programmatically resized. Because {@link #fullScreen(DisplayMode)} marks the surface as fixed size,
     * this only needs to called when using a windowed surface that should be a fixed size.
     *
     * @return New options marking the surface as not resizable
     */
    public OnscreenSurfaceOptions fixedSize() {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, undecorated, false, x, y, width,
                                          height, fullMode);
    }

    /**
     * @return The initial x coordinate of the OnscreenSurface when windowed
     */
    public int getX() {
        return x;
    }

    /**
     * @return The initial y coordinate of the OnscreenSurface when windowed
     */
    public int getY() {
        return y;
    }

    /**
     * @return Whether or not the window is undecorated
     */
    public boolean isUndecorated() {
        return undecorated;
    }

    /**
     * @return Whether or not the window is user-resizable
     */
    public boolean isResizable() {
        return resizable;
    }

    /**
     * @return The requested DisplayMode. If null then the surface will initially be windowed, else it will be
     *         fullscreen with a supported DisplayMode closest to the requested
     */
    public DisplayMode getFullscreenMode() {
        return fullMode;
    }

    /**
     * @return The requested number of bits in the depth buffer, or 0 for no depth buffer
     */
    public int getDepthBufferBits() {
        return depthBits;
    }

    /**
     * @return The requested number of bits in the stencil buffer, or 0 for no stencil buffer
     */
    public int getStencilBufferBits() {
        return stencilBits;
    }

    /**
     * @return The requested number of samples to use in MSAA, or 0 to disable MSAA
     */
    public int getSampleCount() {
        return msaa;
    }

    /**
     * @return The initial width of the surface when windowed
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return The initial height of the surface when windowed
     */
    public int getHeight() {
        return height;
    }
}
