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
package com.ferox.resource.texture;

import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.TextureSurface;
import com.ferox.resource.BulkChangeQueue;
import com.ferox.resource.Resource;


public abstract class Texture extends Resource {
    /**
     * Describes the wrapping behavior of the texture image when pixels sample beyond the
     * normal region.
     */
    public static enum WrapMode {
        /**
         * Texture coordinates are clamped to be in the range [0, 1]. Pixels outside of
         * this range will use whatever texel color happens to lie on the edge.
         */
        CLAMP,
        /**
         * Texture coordinates are clamped to the range [0, 1]. Instead of using the edge
         * texel colors when outside of the range, the texture's configured border color
         * is used.
         */
        CLAMP_TO_BORDER,
        /**
         * Texture coordinates wrap around from 1 to 0 or 0 to 1, etc.
         */
        REPEAT,
        /**
         * Texture coordinates mirror from 0 to 1 and back to 0, etc.
         */
        MIRROR
    }

    private final BulkChangeQueue<MipmapRegion> changeQueue;

    private final int mipmapCount;
    private final int width;
    private final int height;
    private final int depth;

    private final TextureFormat format; // FIXME update TextureFormat to represent the destination format?
    // Right now the source/dst formats are encoded by the texture format and data type of
    // the texture, but the data type is more flexible now so it would be nice if we could
    // get that out of the encoding

    private WrapMode wrapS;
    private WrapMode wrapT;
    private WrapMode wrapR;

    private final Vector4 borderColor;

    private boolean interpolate;
    private double anisoLevel; // in [0, 1]

    private boolean enableDepthCompare;
    private Comparison depthCompareTest;

    private int baseLevel;
    private int maxLevel;


    public Texture(TextureFormat format, int width, int height, int depth) {
        if (format == null) {
            throw new NullPointerException("Format cannot be null");
        }
        if (width <= 0 || height <= 0 || depth <= 0) {
            throw new IllegalArgumentException(
                    "Dimensions must be at least 1: " + width + " x " + height + " x " +
                    height);
        }

        this.format = format;
        this.width = width;
        this.height = height;
        this.depth = depth;
        mipmapCount = getMipmapCount(width, height, depth);

        changeQueue = new BulkChangeQueue<MipmapRegion>();
        borderColor = new Vector4();

        setInterpolated(false);
        setValidMipmapLevels(0, 0);
        setWrapMode(WrapMode.CLAMP);
        setAnisotropicFilterLevel(1.0);
        setDepthCompareEnabled(false);
        setDepthComparison(Comparison.GREATER);
    }

    /**
     * Return the BulkChangeQueue used to track a small set of edits to the texture data
     * so that Frameworks can quickly determine if an update must be performed. Reads and
     * modifications of the queue must only perform within synchronized block of this
     * Texture.
     *
     * @return The texture's change queue
     */
    public BulkChangeQueue<MipmapRegion> getChangeQueue() {
        return changeQueue;
    }

    /**
     * Get the border color of this texture. This is relevant only when the wrap mode is
     * CLAMP_TO_BORDER. It is permitted to ignore the @Const annotation if mutating the
     * returned instance occurs within a larger lock on this Texture.
     *
     * @return The border color
     */
    @Const
    public synchronized Vector4 getBorderColor() {
        return borderColor;
    }

    /**
     * Set the border color of this texture. The given color is copied into this texture's
     * internal vector (the same instance as returned by {@link #getBorderColor()}.
     *
     * @param color The new border color
     *
     * @throws NullPointerException if color is null
     */
    public synchronized void setBorderColor(@Const Vector4 color) {
        borderColor.set(color);
    }

    /**
     * @return The width of the top-most mipmap level of each image in the Texture
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return The height of the top-most mipmap level of each image in the Texture
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return The depth of the top-most mipmap level of each image in the Texture
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @return The TextureFormat of every mipmap image within this Texture
     */
    public TextureFormat getFormat() {
        return format;
    }

    /**
     * Set the S, T, and R coordinate's WrapMode to <var>wrap</var>.
     *
     * @param wrap The new WrapMode for every coordinate
     *
     * @throws NullPointerException if wrap is null
     */
    public synchronized void setWrapMode(WrapMode wrap) {
        setWrapMode(wrap, wrap, wrap);
    }

    /**
     * Set the S, T and R coordinate's WrapMode to <var>s</var>, <var>t</var>,
     * <var>r</var>, respectively.
     *
     * @param s The new WrapMode for the S coordinate
     * @param t The new WrapMode for the T coordinate
     * @param r The new WrapMode for the R coordinate
     *
     * @throws NullPointerException if s, t, or r are null
     */
    public synchronized void setWrapMode(WrapMode s, WrapMode t, WrapMode r) {
        if (s == null || t == null || r == null) {
            throw new NullPointerException(
                    "WrapModes cannot be null: " + s + ", " + t + ", " + r);
        }
        wrapS = s;
        wrapT = t;
        wrapR = r;
    }

    /**
     * Get the type of texture coordinate wrapping when coordinates go beyond the edge of
     * the image, along the S texture coordinate.
     *
     * @return The WrapMode for the S coordinate
     */
    public synchronized WrapMode getWrapModeS() {
        return wrapS;
    }

    /**
     * Get the type of texture coordinate wrapping when coordinates go beyond the edge of
     * the image, along the T texture coordinate.
     *
     * @return The WrapMode for the T coordinate
     */
    public synchronized WrapMode getWrapModeT() {
        return wrapT;
    }

    /**
     * Get the type of texture coordinate wrapping when coordinates go beyond the edge of
     * the image, along the R texture coordinate.
     *
     * @return The WrapMode for the R coordinate
     */
    public synchronized WrapMode getWrapModeR() {
        return wrapR;
    }

    /**
     * Set whether or not texels in the image should be lineary interpolated.
     *
     * @param interpolate True if texels are interpolated
     *
     * @see #isInterpolated()
     */
    public synchronized void setInterpolated(boolean interpolate) {
        this.interpolate = interpolate;
    }

    /**
     * Get whether or not texels in the texture image are linearly interpolated if the
     * image is magnified or minified. If the image is configured to use mipmapping, this
     * will do trilinear filtering across the image and mipmap levels. If not, bilinear
     * filtering will be used.
     *
     * @return True if texels are interpolated when magnifying or minifying the texture
     */
    public synchronized boolean isInterpolated() {
        return interpolate;
    }

    /**
     * Set the amount of anisotropic filtering to apply when rasterizing the texture. This
     * is measured as a number from 0 to 1, where 0 represents no anisotropic filtering
     * and 1 represents the maximum filtering allowed for the running hardware.
     *
     * @param level The amount of anisotropic filtering to use
     *
     * @throws IllegalArgumentException if level is outside of [0, 1]
     */
    public synchronized void setAnisotropicFilterLevel(double level) {
        if (level < 0.0 || level > 1.0) {
            throw new IllegalArgumentException(
                    "Invalid level, must be in [0, 1], not: " + level);
        }
        anisoLevel = level;
    }

    /**
     * @return The amount of anisotropic filtering from 0 to 1
     */
    public synchronized double getAnisotropicFilterLevel() {
        return anisoLevel;
    }

    /**
     * <p/>
     * Set the range of mipmap levels to use when rasterizing a mipmapped Texture.
     * Frameworks do not need to allocate driver-level texture data for mipmaps that are
     * not needed. This can be used to do progressive texture loading or to free up
     * resources when textures are applied to far away objects.
     * <p/>
     * By default the base and max level are set to 0, so textures are not mipmapped.
     *
     * @param base The lowest valid mipmap level, across all layers
     * @param max  The highest valid mipmap level, across all layers
     *
     * @throws IllegalArgumentException if base < 0, if max >= # mipmaps, or if base >
     *                                  max
     */
    public synchronized void setValidMipmapLevels(int base, int max) {
        if (base < 0) {
            throw new IllegalArgumentException(
                    "Base level must be at least 0, not: " + base);
        }
        if (max >= mipmapCount) {
            throw new IllegalArgumentException(
                    "Max level must be less than the mipmap count (" +
                    mipmapCount + "), not: " + max);
        }
        if (base > max) {
            throw new IllegalArgumentException(
                    "Base level must be less than or equal to max, not: " + base + " > " +
                    max);
        }

        baseLevel = base;
        maxLevel = max;
    }

    /**
     * @return The lowest valid mipmap level to use during rasterization
     */
    public synchronized int getBaseMipmapLevel() {
        return baseLevel;
    }

    /**
     * @return The highest valid mipmap level to use during rasterization
     */
    public synchronized int getMaxMipmapLevel() {
        return maxLevel;
    }

    /**
     * @return The total number of mipmaps available based on the dimensions of the
     *         texture
     */
    public int getMipmapCount() {
        return mipmapCount;
    }

    /**
     * <p/>
     * Return the number of 2D layers in this Texture object that can be used for
     * render-to-texture effects with a TextureSurface.
     * <p/>
     * Depending on the type of texture, a layer has different interpretations. It does
     * not necessarily have a one-to-one correspondence with the number of separate images
     * in the texture. As an example, a cube map has 6 images and 6 layers while a 3D map
     * has 1 image and <var>depth</var> layers.
     *
     * @return The number of selectable layers for rendering into
     */
    public abstract int getLayerCount();

    /**
     * Return the number of separate mipmappable images in the Texture object. A
     * mipmappable image is a logical "image" that can be filtered into smaller
     * resolutions for its mipmap levels. A cube map has 6 images, 1D/2D/3D maps have 1
     * image, and 1D/2D array maps have an arbitrary number of images.
     * <p/>
     * This is the significant difference between 3D and 2D array textures, even though
     * they can store the same amount of data and are treated as stacks of 2D images when
     * rendering-to-texture.
     *
     * @return The number of separate images
     */
    public abstract int getImageCount();

    /**
     * @return True if depth comparison is enabled when the TextureFormat of the Texture
     *         is {@link TextureFormat#DEPTH}
     *
     * @see #setDepthCompareEnabled(boolean)
     */
    public synchronized boolean isDepthCompareEnabled() {
        return enableDepthCompare;
    }

    /**
     * <p/>
     * Set whether or not depth comparisons should be used when rendering with this
     * Texture. This parameter is ignored if the image's TextureFormat is not DEPTH. When
     * a texture is a depth texture, the depth values can be interpreted in multiple ways.
     * When depth comparison is disabled, each depth value is treated as a grayscale color
     * that's rendered like any other texture value.
     * <p/>
     * When comparisons are enabled, the depth value is compared to the R coordinate of
     * the texture coordinate used to access the image. Based on this, it takes the value
     * 0 or 1 based on the result of the comparison function, which is configured via
     * {@link #setDepthComparison(Comparison)} .
     *
     * @param enable True if depth comparisons are enabled
     */
    public synchronized void setDepthCompareEnabled(boolean enable) {
        enableDepthCompare = enable;
    }

    /**
     * @return The Comparison function used when depth comparisons are enabled for depth
     *         textures
     */
    public synchronized Comparison getDepthComparison() {
        return depthCompareTest;
    }

    /**
     * Set the Comparison function to use when depth comparisons are enabled. See {@link
     * #setDepthCompareEnabled(boolean)} for more information on depth comparisons with
     * depth textures. This value is ignored for non-depth textures.
     *
     * @param compare The new Comparison function
     *
     * @throws NullPointerException if compare is null
     */
    public synchronized void setDepthComparison(Comparison compare) {
        if (compare == null) {
            throw new NullPointerException("Comparison cannot be null");
        }
        depthCompareTest = compare;
    }

    /**
     * Mark the specified image region as dirty within the image. It is permitted to
     * specify dimensions, layer or mipmap level that do not exist in the image. Those
     * parts of the region that extend past valid areas of the image should be silently
     * ignored by Frameworks processing the change queue.
     *
     * @param region The MipmapRegion representing the dirty pixels in layer and level
     *
     * @return The new version of the texture's change queue
     *
     * @throws NullPointerException if region is null
     */
    public synchronized int markDirty(MipmapRegion region) {
        if (region == null) {
            throw new NullPointerException("MipmapRegion cannot be null");
        }
        return changeQueue.push(region);
    }

    /**
     * Mark the specified mipmap <var>level</var> dirty within the given <var>image</var>.
     * This is a convenience for invoking {@link #markDirty(MipmapRegion)} with an region
     * that spans from (0,0,0) to the dimensions of the requested level.
     *
     * @param image The image whose mipmap level will be marked dirty
     * @param level The level within the image that will be marked dirty
     *
     * @return The new version reported by this texture's change queue
     *
     * @throws IndexOutOfBoundsException if image < 0 or >= {@link #getImageCount()}
     * @throws IllegalArgumentException  if level < 0
     */
    public synchronized int markDirty(int image, int level) {
        return markDirty(new MipmapRegion(image, level, 0, 0, 0,
                                          getMipmapDimension(level, getWidth()),
                                          getMipmapDimension(level, getHeight()),
                                          getMipmapDimension(level, getDepth())));
    }

    /**
     * Mark every mipmap level dirty for the given <var>image</var> of the Texture. This
     * can be used to mark a single face of a cube map dirty for example. This is a
     * convenience for invoking {@link #markDirty(int, int)} for every mipmap level within
     * the given <var>layer</var>
     *
     * @param image The image to mark completely dirty
     *
     * @return The new version reported by this texture's change queue
     *
     * @throws IndexOutOfBoundsException if image < 0 or >= {@link #getImageCount()}
     */
    public synchronized int markDirty(int image) {
        int lastVersion = 0;
        for (int i = 0; i < mipmapCount; i++) {
            lastVersion = markDirty(image, i);
        }
        return lastVersion;
    }

    /**
     * Mark the entirety of the Texture's image data dirty. This is a convenience for
     * invoking {@link #markDirty(int)} for each image of the Texture.
     *
     * @return The new version reported by this texture's change queue
     */
    public synchronized int markDirty() {
        int lastVersion = 0;
        for (int i = 0; i < getImageCount(); i++) {
            lastVersion = markDirty(i);
        }
        return lastVersion;
    }

    /**
     * Return the TextureSurface that renders into this Texture. If the Texture was not
     * created by a TextureSurface, this will return null. Textures with owners may have
     * more restrictions on the updates that can be performed, such as a smaller texture
     * size, etc.
     *
     * @return The owning TextureSurface or null
     */
    public synchronized TextureSurface getOwner() {
        return null;
    }

    /**
     * Compute and return the required number of mipmap levels needed to provide a
     * complete set of mipmaps for the given dimension. A full set would have levels from
     * <var>(width, height, depth), (width / 2, height / 2, depth / 2), ... , (1, 1,
     * 1)</var>. This is computed as <code>floor(log2(max(width,height,depth))) +
     * 1</code>.
     *
     * @param width  The width of the top mipmap level
     * @param height The height of the top mipmap level
     * @param depth  The depth of the top mipmap level
     *
     * @return The required number of mipmap levels to form a complete set of mipmaps
     *
     * @throws IllegalArgumentException if any dimensions is < 1
     */
    public static int getMipmapCount(int width, int height, int depth) {
        if (width <= 0 || height <= 0 || depth <= 0) {
            throw new IllegalArgumentException(
                    "Dimensions must all be at least 1: " + width + " x " + height +
                    " x " + depth);
        }
        int max = Math.max(width, Math.max(height, depth));
        return (int) Math.floor(Math.log(max) / Math.log(2)) + 1;
    }

    /**
     * Compute and return the dimension length for a mipmapped image at the given
     * <var>level</var>, assuming that the base image (or level 0) has the given
     * <var>dimension</var>.
     *
     * @param level     The mipmap level the returned dimension will be for
     * @param dimension The base level's dimension, be it width, height or depth
     *
     * @return The adjusted dimension for the given mipmap level
     */
    public static int getMipmapDimension(int level, int dimension) {
        return Math.max(dimension >> level, 1);
    }
}
