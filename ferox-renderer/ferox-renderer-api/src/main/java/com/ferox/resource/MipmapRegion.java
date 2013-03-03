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
package com.ferox.resource;

/**
 * <p/>
 * MipmapRegion represents an immutable region in 3D space that is associated with a
 * rasterized image or texture within that space. In texturing space, we can consider the
 * origin to be the lower left corner of the image. The x axis represents the horizontal
 * direction of the image and extends to the right. Similarly, the y axis represents the
 * vertical axis and extends upward. The z axis extends outward. This configuration is
 * identical to the layout of the image data within a {@link Texture}.
 * <p/>
 * The offsets and dimensions of an MipmapRegion can be assumed to form a region within
 * the space described above. However, for performance reasons, the region does not
 * constrain or validate the region to the positive edges of any affected texture. The
 * minimum edge (i.e. being at least 0 is validated), but it is possible that an x offset
 * and width could extend past the actual edge of the texture. Any pixel data contained
 * within the sub-region extending past the actual texture can be ignored.
 * <p/>
 * All dimensions and offsets are measured in pixel or texel counts and not in the number
 * of underlying array or buffer indices that must be skipped. This means that the texture
 * format of the data does not affect the values provided to the MipmapRegion.
 *
 * @author Michael Ludwig
 */
public class MipmapRegion {
    private final int x, y, z, width, height, depth;
    private final int layer, mipmapLevel;

    /**
     * <p/>
     * Construct a new MipmapRegion that has the given x, y, z offsets and dimensions of
     * (width, height, and depth). Each dimension given must be at least 1. Thus if an
     * MipmapRegion is desired to have fewer than 3 dimensions, specify a dimension of 1.
     *
     * @param layer  The layer specifying the potential set of mipmaps
     * @param mipmap The mipmap within the specified layer that is modified
     * @param x      X offset
     * @param y      Y offset
     * @param z      Z offset
     * @param width  Initial width of region, must be at least 1
     * @param height Initial height of region, must be at least 1
     * @param depth  Initial depth of region, must be at least 1
     *
     * @throws IllegalArgumentException if width, height, depth < 1, or any other argument
     *                                  is less than 0
     */
    public MipmapRegion(int layer, int mipmap, int x, int y, int z, int width, int height,
                        int depth) {
        if (width < 1 || height < 1 || depth < 1) {
            throw new IllegalArgumentException("Cannot have dimensions less than 1");
        }
        if (x < 0 || y < 0 || z < 0) {
            throw new IllegalArgumentException("Offsets must be at least 0");
        }
        if (layer < 0) {
            throw new IllegalArgumentException("Layer must be at least 0");
        }
        if (mipmap < 0) {
            throw new IllegalArgumentException("Mipmap level must be at least 0");
        }

        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.mipmapLevel = mipmap;
        this.layer = layer;
    }

    /**
     * Return the layer that selects the set of mipmaps from which the mipmap at {@link
     * #getMipmapLevel()} is selected. The selected mipmap is the data marked dirty by the
     * offsets and dimensions contained in this MipmapRegion.
     *
     * @return The texture layer
     */
    public int getLayer() {
        return layer;
    }

    /**
     * Return the mipmap level used to select a mipmap from the layer specifed by {@link
     * #getLayer()}. The selected mipmap is the data marked dirty by the offsets and
     * dimensions contained in this MipmapRegion.
     *
     * @return The mipmap level modified by this region
     */
    public int getMipmapLevel() {
        return mipmapLevel;
    }

    /**
     * Return the x offset of this region within the image block.
     *
     * @return The x offset, will be at least 0
     */
    public int getXOffset() {
        return x;
    }

    /**
     * Return the y offset of this region within the image block.
     *
     * @return The y offset, will be at least 0
     */
    public int getYOffset() {
        return y;
    }

    /**
     * Return the z offset of this region within the image block.
     *
     * @return The z offset, will be at least 0
     */
    public int getZOffset() {
        return z;
    }

    /**
     * Return the width of this region within the image block. It is not verified if the
     * width and x offset extend beyond the actual edge of the image.
     *
     * @return Width of region, will be at least 1
     */
    public int getWidth() {
        return width;
    }

    /**
     * Return the height of this region within the image block. It is not verified if the
     * height and y offset extend beyond the actual edge of the image.
     *
     * @return Height of region, will be at least 1
     */
    public int getHeight() {
        return height;
    }

    /**
     * Return the depth of this region within the image block. It is not verified if the
     * depth and z offset extend beyond the actual edge of the image.
     *
     * @return Depth of region, will be at least 1
     */
    public int getDepth() {
        return depth;
    }
}