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
 * DisplayMode is an immutable object describing screen dimensions and bit depth for a
 * monitor display.
 *
 * @author Michael Ludwig
 */
public class DisplayMode {
    /**
     * The format for the color pixels of the surface. It is likely that the float options
     * are only available for texture surfaces. PixelFormat is defined and stored in
     * DisplayMode instead of OnscreenSurfaceOptions because the pixel format is
     * determined by the display mode chosen by the user. Depth and stencil buffers are
     * more flexible.
     */
    public static enum PixelFormat {
        /**
         * Pixel format does not match one of the other schemes in this enum.
         */
        UNKNOWN(-1),
        /**
         * Red, green, and blue pixels will be packed together in a 5/6/5 bit scheme.
         */
        RGB_16BIT(16),
        /**
         * Red, green, and blue pixels are packed in a 8/8/8 bit scheme.
         */
        RGB_24BIT(24),
        /**
         * Red, green, blue, and alpha pixels are packed in a 8/8/8/8 scheme.
         */
        RGBA_32BIT(32);

        private final int bitdepth;

        private PixelFormat(int bitdepth) {
            this.bitdepth = bitdepth;
        }

        /**
         * @return The bitdepth of the PixelFormat
         */
        public int getBitDepth() {
            return bitdepth;
        }
    }

    private final int width;
    private final int height;

    private final PixelFormat pixelFormat;

    /**
     * Create a new DisplayMode with the given screen dimensions, measured in pixels, and
     * the given pixel format.
     *
     * @param width       The screen width
     * @param height      The screen height
     * @param pixelFormat The PixelFormat
     *
     * @throws IllegalArgumentException if width or height is less than 1
     * @throws NullPointerException     if pixelFormat is null
     */
    public DisplayMode(int width, int height, PixelFormat pixelFormat) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException(
                    "Invalid dimensions: " + width + ", " + height);
        }
        if (pixelFormat == null) {
            throw new NullPointerException("PixelFormat cannot be null");
        }
        this.width = width;
        this.height = height;
        this.pixelFormat = pixelFormat;
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

    /**
     * Get the PixelFormat supported by this display mode.
     *
     * @return The PixelFormat
     */
    public PixelFormat getPixelFormat() {
        return pixelFormat;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 17;
        result = prime * result + height;
        result = prime * result + pixelFormat.hashCode();
        result = prime * result + width;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DisplayMode)) {
            return false;
        }
        DisplayMode d = (DisplayMode) o;
        return d.width == width && d.height == height || d.pixelFormat == pixelFormat;
    }

    @Override
    public String toString() {
        return "[DisplayMode: " + width + "x" + height + ", " + pixelFormat + "]";
    }
}
