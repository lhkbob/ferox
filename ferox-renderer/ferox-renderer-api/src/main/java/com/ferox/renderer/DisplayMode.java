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
 * DisplayMode is an immutable object describing screen dimensions, color bit depth and
 * refresh rate for a monitor display.
 *
 * @author Michael Ludwig
 */
public class DisplayMode {
    private final int width;
    private final int height;

    private final int bitDepth;
    private final int refreshRate;

    /**
     * Create a new DisplayMode with the given screen dimensions, color bit depth, and
     * refresh rate. This may not be valid for the current hardware. The only DisplayModes
     * that are guaranteed valid come from {@link com.ferox.renderer.Framework#getAvailableDisplayModes()}
     *
     * @param width       The screen width
     * @param height      The screen height
     * @param bitDepth    The color bit depth
     * @param refreshRate The refresh rate
     *
     * @throws IllegalArgumentException if any argument is less than 1
     */
    public DisplayMode(int width, int height, int bitDepth, int refreshRate) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException(
                    "Invalid dimensions: " + width + ", " + height);
        }
        if (bitDepth < 1) {
            throw new IllegalArgumentException(
                    "Bit depth must be at least 1: " + bitDepth);
        }
        if (refreshRate < 1) {
            throw new IllegalArgumentException(
                    "Refresh rate must be at least 1: " + refreshRate);
        }

        this.width = width;
        this.height = height;
        this.bitDepth = bitDepth;
        this.refreshRate = refreshRate;
    }

    /**
     * Get the width of the screen in pixels.
     *
     * @return The screen width dimension
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get the height of the screen in pixels.
     *
     * @return The screen height dimension
     */
    public int getHeight() {
        return height;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public int getRefreshRate() {
        return refreshRate;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 17;
        result = prime * result + height;
        result = prime * result + bitDepth;
        result = prime * result + refreshRate;
        result = prime * result + width;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DisplayMode)) {
            return false;
        }
        DisplayMode d = (DisplayMode) o;
        return d.width == width && d.height == height && d.bitDepth == bitDepth &&
               d.refreshRate == refreshRate;
    }

    @Override
    public String toString() {
        return "[DisplayMode: " + width + "x" + height + ", " + bitDepth + "-bit, " +
               refreshRate + " Hz]";
    }
}
