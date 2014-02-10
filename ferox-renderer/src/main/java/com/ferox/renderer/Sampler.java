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
 * Sampler represents the high-level texture object in OpenGL.  There are two main varieties of sampler: color
 * samplers, and depth/stencil samplers.  The color sampler types all extend from the {@link Texture}
 * interface. The depth/stencil samplers extend from {@link DepthMap}. This helps encode how each sampler can
 * be used into the type system.
 * <p/>
 * With each of these subtypes of sampler, there are a number of ways to describe the image data from 1D, 2D,
 * and 3D images, to six-side cubemaps to the newer texture arrays.
 * <p/>
 * When a Sampler object is refreshed via {@link #refresh()}, the current state of the primitive array
 * instances that were used to initially define the sampler are pushed to the GPU. Thus, animated textures are
 * supported by maintaining a reference to the original image arrays and mutating their contents, followed by
 * a refresh.
 * <p/>
 * Across all Samplers, an image (theoretically with three dimensions) has each row of pixel data arranged
 * from left to right. Rows are stacked from bottom to top to form a 2D image. Multiple 2D images (if present)
 * are stacked from back to front.
 *
 * @author Michael Ludwig
 */
public interface Sampler extends Resource {
    /**
     * WrapMode defines thee behavior applied to texture coordinates that lie outside the standard [0, 1]
     * interval.
     */
    public static enum WrapMode {
        /**
         * Coordinates are clamped such that no samples read beyond the last pixel at each edge. There is no
         * mirroring, repeating, or interpolation with the border color. Visually this appears as though the
         * edge colors are extruded beyond their limits.
         */
        CLAMP,
        /**
         * Coordinates are clamped such that samples are clamped to the border, and are thus presented as the
         * solid border color. Visually this appears as though the image is bordered by the color.
         */
        CLAMP_TO_BORDER,
        /**
         * Coordinates are mirrored at each edge, so the sampling pattern creates a seamless pattern, e.g. 0
         * -> 1 -> 0 -> 1, etc.
         */
        MIRROR,
        /**
         * Coordinates are reset to 0 or 1 depending on which edge they cross, so the sampling pattern creates
         * a tiled image, e.g. 0 -> 1, 0 -> 1.
         */
        REPEAT
    }

    /**
     * TexelFormat describes the basic internal texture formats supported by OpenGL. They come in two flavors,
     * either holding color data or depth data and correspond to the two high-level subtypes of Sampler. The
     * exact representation of the components within the pixel also depend on the data type of the sampler. In
     * some cases the components can be packed into a single primitive element.
     */
    public static enum TexelFormat {
        /**
         * The sampler has a single component per pixel that stores depth data. This will only ever be used by
         * samplers that extend {@link DepthMap}.
         */
        DEPTH(1, false),
        /**
         * The sampler has two components per pixel, holding depth and stencil information. In this case, the
         * sampler can provide a stencil buffer when doing render-to-texture. This will only ever be used by
         * samplers that extend {@link DepthMap}.
         */
        DEPTH_STENCIL(2, false),
        /**
         * The sampler provides only the red component values per pixel. When used in a shader, the green and
         * blue components default to 0 and the alpha defaults to 1. On older hardware, the old LUMINANCE
         * OpenGL format may be used to approximate support. This will only ever be used by samplers that
         * extend {@link Texture}.
         */
        R(1, false),
        /**
         * The sampler provides the red and green component values. When used in a shader, the blue component
         * defaults to 0 and alpha to 1. On older hardware, the old LUMINANCE_ALPHA OpenGL format may be used
         * to approximate support. This will only ever be used by samplers that extend {@link Texture}.
         */
        RG(2, false),
        /**
         * The sampler provides red, green, and blue component values. The alpha value defaults to 1. This
         * will only ever be used by samplers that extend {@link Texture}.
         */
        RGB(3, false),
        /**
         * The sampler provides values for all four color components. This will only be used by samplers that
         * extend {@link Texture}.
         */
        RGBA(4, false),
        /**
         * The sampler provides RGB components and a default alpha 1 but the internal data is compressed in a
         * manner supported by the hardware. Compressed formats cannot be used as render targets for a
         * TextureSurface and exceptions will be thrown if attempted. This will only be used by samplers that
         * extend {@link Texture}.
         */
        COMPRESSED_RGB(3, true),
        /**
         * The sampler provides RGBA component data but the internal data is compressed in a manner supported
         * by the hardware. Compressed formats cannot be used as render targets for a TextureSurface and
         * exceptions will be thrown if attempted. This will only be used by samplers that extend {@link
         * Texture}.
         */
        COMPRESSED_RGBA(4, true);

        private final int componentCount;
        private final boolean compressed;

        private TexelFormat(int count, boolean compressed) {
            componentCount = count;
            this.compressed = compressed;
        }

        /**
         * @return The number of components specified by each texel
         */
        public int getComponentCount() {
            return componentCount;
        }

        /**
         * @return True if the format is compressed data
         */
        public boolean isCompressed() {
            return compressed;
        }
    }

    /**
     * RenderTarget encapsulates the internal state needed by a Framework to render into a specific 2D region
     * of the sampler. Depending on the specific type of sampler, it may have multiple render targets. Cube
     * maps have six targets, one for each face of the cube. Texture arrays have a target for each indexed
     * image. 3D textures have a target for each 2D slice. 1D and 2D textures have a single render target.
     */
    public static interface RenderTarget {
        /**
         * @return The sampler that produced the render target
         */
        public Sampler getSampler();
    }

    /**
     * Get whether or not the samples from the sampler should be interpolated by the hardware. The exact
     * strategy differs depending on the base format and mipmaps present.  If color data is interpolated, it
     * will be done by bilinear interpolation if no mipmaps are present, or trilinear interpolation if mipmaps
     * exist. Depth values are interpolated in a way to approximate soft shadows.
     *
     * @return True if samples are interpolated
     */
    public boolean isInterpolated();

    /**
     * Get the width of the sampler at the 0th mipmap level.
     *
     * @return The sampler width
     */
    public int getWidth();

    /**
     * Get the height of the sampler at the 0th mipmap level. For one-dimensional textures, this will return
     * 1.
     *
     * @return The sampler height
     */
    public int getHeight();

    /**
     * Get the depth of the sampler at the 0th mipmap level. For one or two dimensional textures, this will
     * return 1.
     *
     * @return The sampler depth
     */
    public int getDepth();

    /**
     * Get the WrapMode applied to texture coordinates accessing this sampler. The wrap mode is the same for
     * each coordinate, S, T, R, and Q.
     *
     * @return The wrap mode
     */
    public WrapMode getWrapMode();

    /**
     * Get the base or lowest mipmap that contains valid image data. The lowest mipmap corresponds to the
     * mipmap with largest dimensions and most detail. Generally this will be 0, but may be a larger number if
     * the texture contains partially loaded or defined mipmap levels.
     * <p/>
     * Mipmap layers between the base mipmap and the max mipmap will always have image data.
     *
     * @return The base mipmap level
     *
     * @see #getMaxMipmap()
     */
    public int getBaseMipmap();

    /**
     * Get the max or highest mipmap that contains valid image data. The highest mipmap corresponds to the
     * mipmap with the smallest dimensions. Generally this will be equal to the log base 2 of the maximum
     * dimension of the texture, but may be smaller if the texture contains partially defined mipmap levels.
     * <p/>
     * Mipmap layers between the base mipmap and the max mipmap will always have image data.
     *
     * @return The max mipmap level
     *
     * @see #getBaseMipmap()
     */
    public int getMaxMipmap();

    /**
     * @return The texel format of pixels for this sampler
     *
     * @see #getDataType()
     */
    public TexelFormat getFormat();

    /**
     * Get the data type holding the components of the sampler. If the type is INT_BIT_FIELD, the components
     * are most likely packed into single primitives. Otherwise, as long as the data isn't compressed, each
     * primitive corresponds to one of the components in the base format.
     * <p/>
     * The combination of base format and data type do not guarantee a unique description of how to interpret
     * the data. The exact interpretation depends on the data assignment method invoked on the builder that
     * created the resource.
     *
     * @return The data type of pixels
     */
    public DataType getDataType();
}
