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
 * OnscreenSurfaceOptions represents the set of configurable parameters used to create an
 * OnscreenSurface. These parameters are requests to the Framework and may be ignored, but
 * this is unlikely. Each OnscreenSurfaceOptions instance is immutable, the setters
 * available return new instances that match the calling instance except for the new
 * parameter value.
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

    public OnscreenSurfaceOptions() {
        this(24, 0, 0, false, false, 0, 0, 600, 600, null);
    }

    private OnscreenSurfaceOptions(int depthBits, int msaa, int stencilBits,
                                   boolean undecorated, boolean resizable, int x, int y,
                                   int width, int height, DisplayMode fullMode) {
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

    public OnscreenSurfaceOptions windowed(int width, int height) {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, undecorated,
                                          resizable, x, y, width, height, null);
    }

    public OnscreenSurfaceOptions locatedAt(int x, int y) {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, undecorated,
                                          resizable, x, y, width, height, null);
    }

    public OnscreenSurfaceOptions fullScreen(DisplayMode mode) {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, true, false, 0, 0,
                                          mode.getWidth(), mode.getHeight(), mode);
    }

    public OnscreenSurfaceOptions withDepthBuffer(int depthBits) {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, undecorated,
                                          resizable, x, y, width, height, fullMode);
    }

    public OnscreenSurfaceOptions withStencilBuffer(int stencilBits) {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, undecorated,
                                          resizable, x, y, width, height, fullMode);
    }

    public OnscreenSurfaceOptions antiAliased(int samples) {
        return new OnscreenSurfaceOptions(depthBits, samples, stencilBits, undecorated,
                                          resizable, x, y, width, height, fullMode);
    }

    public OnscreenSurfaceOptions undecorated() {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, true, resizable,
                                          x, y, width, height, fullMode);
    }

    public OnscreenSurfaceOptions resizable() {
        return new OnscreenSurfaceOptions(depthBits, msaa, stencilBits, undecorated,
                                          false, x, y, width, height, fullMode);
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
     * @return The requested DisplayMode. If null then the surface will initially be
     *         windowed, else it will be fullscreen with a supported DisplayMode closest
     *         to the requested
     */
    public DisplayMode getFullscreenMode() {
        return fullMode;
    }

    public int getDepthBufferBits() {
        return depthBits;
    }

    public int getStencilBufferBits() {
        return stencilBits;
    }

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
