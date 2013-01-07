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

import java.util.Arrays;

import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.TextureSurface;
import com.ferox.resource.BufferData.DataType;

/**
 * <p>
 * Texture is a complex structure that organizes a set of {@link Mipmap mipmaps}
 * into a usable Resource for applying image data to rendered geometry. A
 * Texture has a Target which describes how the Texture can be accessed. The
 * target currently can be used to create 1D, 2D, 3D and cube map textures.
 * Texture has the concept of a "layer". Multiple layers of image data compose a
 * Texture. Each layer has a single set of image data stored within a
 * {@link Mipmap}. Every Mipmap layer of a Texture has the same dimensions, data
 * type and format. Currently, only the cube map target supports multiple
 * layers. The "layer" is meant to be a forward-compatible feature for use with
 * advanced and unsupported targets such as 2D-array or 3D-array.
 * </p>
 * <p>
 * The BufferData within a Texture's Mipmaps can be modified or changed without
 * reassigning the Mipmaps to a Texture. However, when this occurs, the Texture
 * must be notified via markImageDirty() so it can properly send change events
 * to any {@link ResourceListener listeners}. This is also useful in that many
 * updates can be performed to a Texture's data before issuing one dirty
 * notification.
 * </p>
 * <p>
 * When a Framework updates a Texture, it should not perform updates on any
 * updates on mipmap levels that have null BufferDatas or BufferDatas with null
 * arrays. Depending on the level ranged described by
 * {@link #getBaseMipmapLevel()} and {@link #getMaxMipmapLevel()}, it may be
 * necessary to allocate space for those levels, in which case the garbage
 * should remain unmodified. The primary purpose of this is to allow the
 * in-memory BufferDatas to be garbage collected when possible without
 * destroying their copy at the driver level. It's also used by TextureSurfaces,
 * which have no in-memory texture data at all.
 * </p>
 * <p>
 * When notifying ResourceListeners, Textures send a couple of different objects
 * depending on the change type. If parameters such as {@link #getFilter() the
 * filter} or {@link #getWrapModeR() wrap mode} are changed, the static object
 * PARAMETERS_CHANGED is sent. If the data within mipmap levels is marked as
 * dirty using the markDirty() methods, a {@link MipmapRegion} is sent
 * containing the dirty region. If {@link #setLayers(Mipmap[])} is called, a
 * FULL_UPDATE is sent.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Texture extends Resource {
    /**
     * Target represents the way in which the Texture's Mipmaps are interpreted.
     */
    public static enum Target {
        /**
         * The image is stored within a single row of pixel data, measured by
         * the width of the image. The height and depth of each mipmap must be
         * 1. The texture is accessed by the first texture coordinate, from 0 to
         * 1.
         */
        T_1D,
        /**
         * The image is stored within a grid of pixel data, measured by the
         * width and height of the image. The depth must be 1. The texture is
         * accessed by the first and second texture coordinates, from (0,0) to
         * (1,1).
         */
        T_2D,
        /**
         * The image is an array of 2D images, where the number of images is
         * equal to the depth of the texture. The texture is accessed by all
         * three texture coordinates, from (0,0,0) to (1,1,1).
         */
        T_3D,
        /**
         * A cube map is composed of six 2D images, one for each side of a cube.
         * The size of each image is measured by the width and height, although
         * the width and height must be equal. The texture is accessed by all
         * three coordinates by projecting it onto a specific face.
         */
        T_CUBEMAP
    }

    /**
     * Describes the wrapping behavior of the texture image when pixels sample
     * beyond the normal region.
     */
    public static enum WrapMode {
        /**
         * Texture coordinates are clamped to be in the range [0, 1]. Pixels
         * outside of this range will use whatever texel color happens to lie on
         * the edge.
         */
        CLAMP,
        /**
         * Texture coordinates are clamped to the range [0, 1]. Instead of using
         * the edge texel colors when outside of the range, the texture's
         * configured border color is used.
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

    /**
     * Get the filter applied to the texture. NEAREST and LINEAR do not use any
     * mipmap data, even if present. MIPMAP_NEAREST and MIPMAP_LINEAR default to
     * their non-mipmap versions if a texture doesn't have mipmaps. Filter
     * describes both minification and magnification.
     */
    public static enum Filter {
        NEAREST,
        LINEAR,
        MIPMAP_NEAREST,
        MIPMAP_LINEAR
    }

    /**
     * Layer indices for Textures with target {@link Target#T_CUBEMAP}.
     */
    public static final int PX = 0;
    public static final int PY = 1;
    public static final int PZ = 2;
    public static final int NX = 3;
    public static final int NY = 4;
    public static final int NZ = 5;

    private final BulkChangeQueue<MipmapRegion> changeQueue;

    private WrapMode wrapS;
    private WrapMode wrapT;
    private WrapMode wrapR;

    private final Vector4 borderColor;

    private Filter filter;
    private float anisoLevel; // in [0, 1]

    private boolean enableDepthCompare;
    private Comparison depthCompareTest;

    private final Target target;
    private Mipmap[] layers;
    private int baseLevel, maxLevel;

    /**
     * Create a Texture that uses the specified target and uses the given Mipmap
     * as its source of image data. The created Texture has a single layer, so
     * this constructor only works for targets with a single layer (i.e. not
     * T_CUBEMAP). This is equivalent to
     * <code>Texture(target, new Mipmap[] {mipmap})</code>. See
     * {@link #setLayers(Target, Mipmap[])} for how the parameters are
     * validated.
     * 
     * @param target The Target which specifies how the Texture is accessed
     * @param mipmap The Mipmap data for use in the constructed Texture's single
     *            layer
     * @throws NullPointerException if target or mipmap are null
     */
    public Texture(Target target, Mipmap mipmap) {
        this(target, new Mipmap[] {mipmap});
    }

    /**
     * <p>
     * Create a Texture that uses the specified target and array of Mipmap
     * layers. This constructor is used primarily for creating cubemaps. Other
     * targets can use {@link #Texture(Target, Mipmap)}. See
     * {@link #setLayers(Mipmap[])} for how the parameters are validated.
     * 
     * @param target The Target which specifies how the Texture is accessed
     * @param mipmaps An array of Mipmaps representing the layers of the Texture
     * @throws NullPointerException if target or mipmaps is null, or if mipmaps
     *             contains any null elements
     * @throws IllegalArgumentException if the mipmaps have differing formats
     *             are the number of layers is incorrect for the target
     */
    public Texture(Target target, Mipmap[] mipmaps) {
        if (target == null) {
            throw new NullPointerException("Target cannot be null");
        }
        this.target = target;
        changeQueue = new BulkChangeQueue<MipmapRegion>();
        borderColor = new Vector4();

        filter = Filter.LINEAR;

        setLayers(mipmaps);
        setWrapMode(WrapMode.CLAMP);
        setAnisotropicFilterLevel(1f);
        setDepthCompareEnabled(false);
        setDepthComparison(Comparison.GREATER);
    }

    /**
     * Return the BulkChangeQueue used to track a small set of edits to the
     * vbo's buffer data so that Frameworks can quickly determine if an update
     * must be performed. Reads and modifications of the queue must only perform
     * within synchronized block of this Texture.
     * 
     * @return The texture's change queue
     */
    public BulkChangeQueue<MipmapRegion> getChangeQueue() {
        return changeQueue;
    }

    /**
     * <p>
     * Update the Texture to use the new set of mipmaps. The format and
     * dimensions of the mipmaps can be different from the previous
     * configuration of the texture. The change queue is cleared and all of the
     * new image data is marked dirty.
     * </p>
     * <p>
     * This also updates the valid mipmap range to best fit the mipmap levels
     * that contain BufferData's with non-null arrays. If all mipmap levels have
     * BufferData's with null arrays the full range of mipmaps is used.
     * </p>
     * <p>
     * The mipmap layers are interpreted differently depending on the specified
     * target. If the target is one of T_1D, T_2D, or T_3D, <tt>mipmaps</tt>
     * must have a length of 1 as these targets expect a single layer. If the
     * target is T_CUBEMAP, <tt>mipmaps</tt> must have a length of 6, one for
     * each face of the cube.
     * </p>
     * <p>
     * The mipmaps array cannot have any null elements. Additionally, the
     * dimensions, TextureFormat and DataType of every Mipmap within
     * <tt>mipmaps</tt> must match. Additionally, there are dimension
     * constraints depending on the target:
     * <ul>
     * <li>1D textures must have all mipmaps with a height and depth of 1.</li>
     * <li>2D textures must have a depth of 1.</li>
     * <li>cubemaps must have a depth of 1 and the width and height must be
     * equal.</li>
     * </ul>
     * </p>
     * <p>
     * The order of the mipmap layers for a cubemap is described in
     * {@link #getLayer(int)}.
     * </p>
     * 
     * @param mipmaps The array of new mipmap layers, its length is dependent on
     *            target
     * @return The new version of this texture's change queue
     * @throws NullPointerException if mipmaps are null, or if mipmaps has null
     *             elements
     * @throws IllegalArgumentException if any of the above requirements are not
     *             met
     */
    public synchronized int setLayers(Mipmap[] mipmaps) {
        if (mipmaps == null) {
            throw new NullPointerException("Mipmap array cannot be null");
        }

        // validate number of layers
        if (target == Target.T_CUBEMAP && mipmaps.length != 6) {
            throw new IllegalArgumentException("Cube maps require exactly 6 layers, not: " + mipmaps.length);
        } else if (target != Target.T_CUBEMAP && mipmaps.length != 1) {
            throw new IllegalArgumentException("1D, 2D, and 3D textures require exactly 1 layer, not: " + mipmaps.length);
        }

        int baseLevel = Integer.MAX_VALUE;
        int maxLevel = Integer.MIN_VALUE;
        // validate that each layer has correct dimensions, type and format, and not null
        for (int i = 0; i < mipmaps.length; i++) {
            if (mipmaps[i] == null) {
                throw new NullPointerException("Mipmap layer cannot be null: " + i);
            }

            if (i == 0) {
                // first pass through, assume 1st layer is representative
                if (target != Target.T_3D && mipmaps[i].getDepth(0) != 1) {
                    throw new IllegalArgumentException("Textures with target=" + target + " must have a depth of 1");
                }
                if (target == Target.T_1D && mipmaps[i].getHeight(0) != 1) {
                    throw new IllegalArgumentException("Textures with target=" + target + " must have a height of 1");
                }
                if (target == Target.T_CUBEMAP && mipmaps[i].getWidth(0) != mipmaps[i].getHeight(0)) {
                    throw new IllegalArgumentException("Textures with target=" + target + " must have square mipmap layers");
                }
            } else {
                // remaining mipmaps must match properties of 1st mipmap
                if (mipmaps[i].getDepth(0) != mipmaps[0].getDepth(0) || mipmaps[i].getHeight(0) != mipmaps[0].getHeight(0) || mipmaps[i].getWidth(0) != mipmaps[0].getWidth(0)) {
                    throw new IllegalArgumentException("Mipmap layers must all have the same dimensions");
                }
                if (mipmaps[i].getNumMipmaps() != mipmaps[0].getNumMipmaps()) {
                    throw new IllegalArgumentException("Mipmap layers must all have the same number of mipmaps");
                }
                if (mipmaps[i].getFormat() != mipmaps[0].getFormat()) {
                    throw new IllegalArgumentException("Mipmap layers must all have the same TextureFormat");
                }
                if (mipmaps[i].getDataType() != mipmaps[0].getDataType()) {
                    throw new IllegalArgumentException("Mipmap layers must all have the same Buffer data type");
                }
            }

            // perform default detection of valid mipmap levels
            for (int j = 0; j < mipmaps[i].getNumMipmaps(); j++) {
                if (mipmaps[i].getData(j).getArray() != null) {
                    baseLevel = Math.min(baseLevel, j);
                    maxLevel = Math.max(maxLevel, j);
                }
            }
        }

        // at this point we know things are as valid as we can determine them,
        layers = Arrays.copyOf(mipmaps, mipmaps.length); // defensive copy
        if (baseLevel > maxLevel) {
            // weren't able to find a valid range, default to entire mipmap range
            baseLevel = 0;
            maxLevel = mipmaps[0].getNumMipmaps() - 1;
        }

        setValidMipmapLevels(baseLevel, maxLevel);
        setFilter(filter); // must call this in case we lose mipmaps
        changeQueue.clear();
        return markDirty();
    }

    /**
     * Set the given layer to be the provided mipmap. This new mipmap must have
     * the same DataType, TextureFormat, dimensions and number of mipmaps as the
     * previous mipmap at the given layer. Unlike the setLayers() methods, this
     * does not change the valid range of mipmaps. This will also mark the given
     * layer as dirty in the change queue.
     * 
     * @param layer The layer to update
     * @param mipmap The new mipmap
     * @return The new version of this texture's change queue
     * @throws NullPointerException if mipmap is null
     * @throws IndexOutOfBoundsException if layer is less than 0 or greater than
     *             or equal to the number of layers in this texture
     * @throws IllegalArgumentException if the mipmap's datatype, dimensions,
     *             format and number of mipmaps differ from the last mipmap
     */
    public synchronized int setLayer(int layer, Mipmap mipmap) {
        if (layer < 0 || layer >= layers.length) {
            throw new IndexOutOfBoundsException("Illegal layer argument: " + layer);
        }
        if (mipmap == null) {
            throw new NullPointerException("Mipmap cannot be null");
        }

        Mipmap old = layers[layer];
        if (old.getDataType() != mipmap.getDataType()) {
            throw new IllegalArgumentException("New mipmap does not have expected type (" + old.getDataType() + "), but was " + mipmap.getDataType());
        }
        if (old.getFormat() != mipmap.getFormat()) {
            throw new IllegalArgumentException("New mipmap does not have expected format (" + old.getFormat() + "), but was " + mipmap.getFormat());
        }
        if (old.getWidth(0) != mipmap.getWidth(0) || old.getHeight(0) != mipmap.getHeight(0) || old.getDepth(0) != mipmap.getDepth(0)) {
            throw new IllegalArgumentException("New mipmap does not match required dimensions");
        }
        if (old.getNumMipmaps() != mipmap.getNumMipmaps()) {
            throw new IllegalArgumentException("New mipmap does not have expected number of mipmaps");
        }

        layers[layer] = mipmap;
        return markDirty(layer);
    }

    /**
     * Get the border color of this texture. This is relevant only when the wrap
     * mode is CLAMP_TO_BORDER. It is permitted to ignore the @Const annotation
     * if mutating the returned instance occurs within a larger lock on this
     * Texture.
     * 
     * @return The border color
     */
    @Const
    public synchronized Vector4 getBorderColor() {
        return borderColor;
    }

    /**
     * Set the border color of this texture. The given color is copied into this
     * texture's internal vector (the same instance as returned by
     * {@link #getBorderColor()}.
     * 
     * @param color The new border color
     * @throws NullPointerException if color is null
     */
    public synchronized void setBorderColor(@Const Vector4 color) {
        borderColor.set(color);
    }

    /**
     * @return The width of the top-most mipmap level of each layer in the
     *         Texture
     */
    public synchronized int getWidth() {
        return layers[0].getWidth(0);
    }

    /**
     * @return The height of the top-most mipmap level of each layer in the
     *         Texture
     */
    public synchronized int getHeight() {
        return layers[0].getHeight(0);
    }

    /**
     * @return The depth of the top-most mipmap level of each layer in the
     *         Texture
     */
    public synchronized int getDepth() {
        return layers[0].getDepth(0);
    }

    /**
     * @return The TextureFormat of every Mipmap layer within this Texture
     */
    public synchronized TextureFormat getFormat() {
        return layers[0].getFormat();
    }

    /**
     * @return The Buffer class type of all the Buffer data within each Mipmap
     *         layer within this Texture
     */
    public synchronized DataType getDataType() {
        return layers[0].getDataType();
    }

    /**
     * Set the S, T, and R coordinate's WrapMode to <tt>wrap</tt>.
     * 
     * @param wrap The new WrapMode for every coordinate
     * @throws NullPointerException if wrap is null
     */
    public synchronized void setWrapMode(WrapMode wrap) {
        setWrapMode(wrap, wrap, wrap);
    }

    /**
     * Set the S, T and R coordinate's WrapMode to <tt>s</tt>, <tt>t</tt>,
     * <tt>r</tt>, respectively.
     * 
     * @param s The new WrapMode for the S coordinate
     * @param t The new WrapMode for the T coordinate
     * @param r The new WrapMode for the R coordinate
     * @throws NullPointerException if s, t, or r are null
     */
    public synchronized void setWrapMode(WrapMode s, WrapMode t, WrapMode r) {
        if (s == null || t == null || r == null) {
            throw new NullPointerException("WrapModes cannot be null: " + s + ", " + t + ", " + r);
        }
        wrapS = s;
        wrapT = t;
        wrapR = r;
    }

    /**
     * Get the type of texture coordinate wrapping when coordinates go beyond
     * the edge of the image, along the S texture coordinate.
     * 
     * @return The WrapMode for the S coordinate
     */
    public synchronized WrapMode getWrapModeS() {
        return wrapS;
    }

    /**
     * Get the type of texture coordinate wrapping when coordinates go beyond
     * the edge of the image, along the T texture coordinate.
     * 
     * @return The WrapMode for the T coordinate
     */
    public synchronized WrapMode getWrapModeT() {
        return wrapT;
    }

    /**
     * Get the type of texture coordinate wrapping when coordinates go beyond
     * the edge of the image, along the R texture coordinate.
     * 
     * @return The WrapMode for the R coordinate
     */
    public synchronized WrapMode getWrapModeR() {
        return wrapR;
    }

    /**
     * Set the Filter to use when minimizing or magnifying a texel when
     * rendering it to a Surface. The MIPMAP_x values are only valid if this
     * Texture has more than 1 level in its mipmap layers. If its layers are not
     * mipmapped, then MIPMAP_LINEAR becomes LINEAR and MIPMAP_NEAREST becomes
     * NEAREST.
     * 
     * @param filter The new Filter
     * @throws NullPointerException if filter is null
     */
    public synchronized void setFilter(Filter filter) {
        if (filter == null) {
            throw new NullPointerException("Filter cannot be null");
        }
        if (!layers[0].isMipmapped()) {
            if (filter == Filter.MIPMAP_LINEAR) {
                filter = Filter.LINEAR;
            } else if (filter == Filter.MIPMAP_NEAREST) {
                filter = Filter.NEAREST;
            }
        }

        this.filter = filter;
    }

    /**
     * @return The Filter to apply to the texels when rendering the texture
     */
    public synchronized Filter getFilter() {
        return filter;
    }

    /**
     * Set the amount of anisotropic filtering to apply when rasterizing the
     * texture. This is measured as a number from 0 to 1, where 0 represents no
     * anisotropic filtering and 1 represents the maximum filtering allowed for
     * the running hardware.
     * 
     * @param level The amount of anisotropic filtering to use
     * @throws IllegalArgumentException if level is outside of [0, 1]
     */
    public synchronized void setAnisotropicFilterLevel(float level) {
        if (level < 0f || level > 1f) {
            throw new IllegalArgumentException("Invalid level, must be in [0, 1], not: " + level);
        }
        anisoLevel = level;
    }

    /**
     * @return The amount of anisotropic filtering from 0 to 1
     */
    public synchronized float getAnisotropicFilterLevel() {
        return anisoLevel;
    }

    /**
     * <p>
     * Set the range of mipmap levels to use when rasterizing a mipmapped
     * Texture. Frameworks do not need to allocate driver-level texture data for
     * mipmaps that are not needed. This can be used to do progressive texture
     * loading or to free up resources when textures are applied to far away
     * objects.
     * </p>
     * <p>
     * Initially, the base level is set to the lowest level that has non-null
     * data and the max level is set to the highest level with non-null data. If
     * there is no data in any layer, then it uses
     * <code>(0, # mipmaps - 1)</code> for the range.
     * </p>
     * 
     * @param base The lowest valid mipmap level, across all layers
     * @param max The highest valid mipmap level, across all layers
     * @throws IllegalArgumentException if base < 0, if max >= # mipmaps, or if
     *             base > max
     */
    public synchronized void setValidMipmapLevels(int base, int max) {
        if (base < 0) {
            throw new IllegalArgumentException("Base level must be at least 0, not: " + base);
        }
        if (max >= layers[0].getNumMipmaps()) {
            throw new IllegalArgumentException("Max level must be at most the number of levels in the image (" + layers[0].getNumMipmaps() + "), not: " + max);
        }
        if (base > max) {
            throw new IllegalArgumentException("Base level must be less than or equal to max, not: " + base + " > " + max);
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
     * Return the chosen Target for this Texture. The Target imposes a number of
     * restrictions on the dimensions of the Texture. For example:
     * <ol>
     * <li>T_1D requires a height and depth of 1 for the root mipmap, and one
     * layer.</li>
     * <li>T_2D requires a depth of 1 for the root mipmap, and one layer.</li>
     * <li>T_3D requires there to be only one layer.</li>
     * <li>T_CUBEMAP requires a depth of 1, its width and height to be equal,
     * and to have 6 layers all with matching dimensions.</li>
     * </ol>
     * 
     * @return The Target of this Texture
     */
    public synchronized Target getTarget() {
        return target;
    }

    /**
     * <p>
     * Return the {@link Mipmap} containing the hierarchical image data for a
     * given layer of the Texture. In many cases there will only be 1 layer (for
     * targets T_1D, T_2D and T_3D). For cube maps there are 6 layers with
     * layers arranged as PX, PY, PZ, NX, NY, NZ (one layer for each cube face).
     * </p>
     * <p>
     * Each Mipmap layer within a Texture will have the same dimensions, the
     * same number of mipmap levels, the same Buffer data type, and same
     * TextureFormat. There is always at least one layer in a Texture.
     * </p>
     * 
     * @param layer The layer to fetch (not to be confused with mipmap level),
     *            starting at 0
     * @return The Mipmap data for the specific layer
     * @throws IndexOutOfBoundsException if layer < 0 or layer >=
     *             {@link #getNumLayers()}
     */
    public synchronized Mipmap getLayer(int layer) {
        return layers[layer];
    }

    /**
     * @return The number of layers present in this Texture
     */
    public synchronized int getNumLayers() {
        return layers.length;
    }

    /**
     * @see #setDepthCompareEnabled(boolean)
     * @return True if depth comparison is enabled when the TextureFormat of the
     *         Texture is {@link TextureFormat#DEPTH}
     */
    public synchronized boolean isDepthCompareEnabled() {
        return enableDepthCompare;
    }

    /**
     * <p>
     * Set whether or not depth comparisons should be used when rendering with
     * this Texture. This parameter is ignored if the image's TextureFormat is
     * not DEPTH. When a texture is a depth texture, the depth values can be
     * interpreted in multiple ways. When depth comparison is disabled, each
     * depth value is treated as a grayscale color that's rendered like any
     * other texture value.
     * </p>
     * <p>
     * When comparisons are enabled, the depth value is compared to the R
     * coordinate of the texture coordinate used to access the image. Based on
     * this, it takes the value 0 or 1 based on the result of the comparison
     * function, which is configured via {@link #setDepthComparison(Comparison)}
     * .
     * </p>
     * 
     * @param enable True if depth comparisons are enabled
     */
    public synchronized void setDepthCompareEnabled(boolean enable) {
        enableDepthCompare = enable;
    }

    /**
     * @return The Comparison function used when depth comparisons are enabled
     *         for depth textures
     */
    public synchronized Comparison getDepthComparison() {
        return depthCompareTest;
    }

    /**
     * Set the Comparison function to use when depth comparisons are enabled.
     * See {@link #setDepthCompareEnabled(boolean)} for more information on
     * depth comparisons with depth textures. This value is ignored for
     * non-depth textures.
     * 
     * @param compare The new Comparison function
     * @throws NullPointerException if compare is null
     */
    public synchronized void setDepthComparison(Comparison compare) {
        if (compare == null) {
            throw new NullPointerException("Comparison cannot be null");
        }
        depthCompareTest = compare;
    }

    /**
     * Mark the specified image region as dirty within the image. It is
     * permitted to specify dimensions, layer or mipmap level that do not exist
     * in the image. Those parts of the region that extend past valid areas of
     * the image should be silently ignored by sytems processing the change
     * queue.
     * 
     * @param region The MipmapRegion representing the dirty pixels in layer and
     *            level
     * @return The new version of the texture's change queue
     * @throws NullPointerException if region is null
     */
    public synchronized int markDirty(MipmapRegion region) {
        if (region == null) {
            throw new NullPointerException("MipmapRegion cannot be null");
        }
        return changeQueue.push(region);
    }

    /**
     * Mark the specified mipmap <tt>level</tt> dirty within the given
     * <tt>layer</tt>. This is a convenience for invoking
     * {@link #markDirty(MipmapRegion, int, int)} with an MipmapRegion that
     * spans from (0,0,0) to the dimensions of the requested level.
     * 
     * @param layer The layer whose mipmap will be marked dirty
     * @param level The level within layer that will be marked dirty
     * @return The new version reported by this texture's change queue
     * @throws IndexOutOfBoundsException if layer < 0 or >=
     *             {@link #getNumLayers()}
     * @throws IllegalArgumentException if level < 0
     */
    public synchronized int markDirty(int layer, int level) {
        Mipmap m = getLayer(layer);
        return markDirty(new MipmapRegion(layer,
                                          level,
                                          0,
                                          0,
                                          0,
                                          m.getWidth(level),
                                          m.getHeight(level),
                                          m.getDepth(level)));
    }

    /**
     * Mark every mipmap level dirty for the given <tt>layer</tt> of the
     * Texture. This can be used to mark a single face of a cube map dirty for
     * example. This is a convenience for invoking {@link #markDirty(int, int)}
     * for every mipmap level within the given <tt>layer</tt>
     * 
     * @param layer The layer to mark completely dirty
     * @return The new version reported by this texture's change queue
     * @throws IndexOutOfBoundsException if layer < 0 or >=
     *             {@link #getNumLayers()}
     */
    public synchronized int markDirty(int layer) {
        int lastVersion = 0;
        for (int i = 0; i < layers[layer].getNumMipmaps(); i++) {
            lastVersion = markDirty(layer, i);
        }
        return lastVersion;
    }

    /**
     * Mark the entirety of the Texture's image data dirty. This is a
     * convenience for invoking {@link #markDirty(int)} for each layer of the
     * Texture.
     * 
     * @return The new version reported by this texture's change queue
     */
    public synchronized int markDirty() {
        int lastVersion = 0;
        for (int i = 0; i < layers.length; i++) {
            lastVersion = markDirty(i);
        }
        return lastVersion;
    }

    /**
     * Return the TextureSurface that renders into this Texture. If the Texture
     * was not created by a TextureSurface, this will return null. Textures with
     * owners may have more restrictions on the updates that can be performed,
     * such as a smaller texture size, etc.
     * 
     * @return The owning TextureSurface or null
     */
    public synchronized TextureSurface getOwner() {
        return null;
    }
}
