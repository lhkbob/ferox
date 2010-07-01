package com.ferox.resource;

import java.nio.Buffer;
import java.util.Arrays;

import com.ferox.renderer.Renderer.Comparison;

/**
 * <p>
 * Texture is a complex structure that organizes a set of {@link Mipmap
 * mipmaps} into a usable Resource for applying image data to rendered geometry.
 * A Texture has a Target which describes how the Texture can be
 * accessed. The target currently can be used to create 1D, 2D, 3D and cube map
 * textures. Texture has the concept of a "layer". Multiple layers of image
 * data compose a Texture. Each layer has a single set of image data stored
 * within a {@link Mipmap}. Every Mipmap layer of a Texture has the same
 * dimensions, data type and format. Currently, only the cube map target
 * supports multiple layers. The "layer" is meant to be a forward-compatible
 * feature for use with advanced and unsupported targets such as 2D-array or
 * 3D-array.
 * </p>
 * <p>
 * A Texture's number of layers, number of mipmaps, data type, format, and
 * dimensions are immutable after creation. However, the texture parameters that
 * describe how texture coordinate wrap, how texels are filtered during
 * rendering, etc. are completely mutable. Additionally, the actual Buffer data
 * within a Texture's Mipmaps can be modified or changed. However, when
 * this occurs, the Texture must be notified via markImageDirty() so it can
 * properly update its DirtyState. This is also useful in that many updates can
 * be performed to a Texture's data before issuing one dirty notification.
 * </p>
 * <p>
 * When a Framework updates a Texture, it should not perform updates on any
 * updates on mipmap levels that have null Buffers associated with them. If
 * necessary, based on the level ranged described by
 * {@link #getBaseMipmapLevel()} and {@link #getMaxMipmapLevel()}, it may be
 * necessary to allocate space for those levels, in which case the garbage
 * should remain unmodified. The primary purpose of this is to allow the
 * in-memory Buffers to be garbage collected when possible without then
 * destroying their copy at the driver level. It's also used by TextureSurfaces,
 * which have no in-memory texture data at all.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Texture extends Resource {
    /**
     * Target represents the way in which the Texture's Mipmaps are
     * interpreted.
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
         * Texture coordinates are clamped to be in the range [0, 1].
         */
        CLAMP,
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
        NEAREST, LINEAR, MIPMAP_NEAREST, MIPMAP_LINEAR
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

    private WrapMode wrapS;
    private WrapMode wrapT;
    private WrapMode wrapR;

    private Filter filter;
    private float anisoLevel; // in [0, 1]

    private boolean enableDepthCompare;
    private Comparison depthCompareTest;
    
    private final Target target;
    private final Mipmap[] layers;
    private int baseLevel, maxLevel;
    
    private TextureDirtyState dirty;

    /**
     * Create a Texture that uses the specified target and uses the given
     * Mipmap as its source of image data. The created Texture has a single
     * layer, so this constructor should only be used for Targets that are not
     * {@link Target#T_CUBEMAP}, which requires 6 layers. This is equivalent to
     * <code>Texture(target, new Mipmap[] {mipmap})</code>. See
     * {@link #Texture(Target, Mipmap[])} for more details on the
     * validation performed.
     * 
     * @param target The Target which specifies how the Texture is accessed
     * @param mipmap The Mipmap data for use in the constructed Texture's
     *            single layer
     * @throws NullPointerException if target or mipmap are null
     */
    public Texture(Target target, Mipmap mipmap) {
        this(target, new Mipmap[] {mipmap});
    }

    /**
     * <p>
     * Create a Texture that uses the specified target and array of Mipmap
     * layers. The mipmap layers are interpreted differently depending on the
     * chosen Target for the Texture. If target is one of T_1D, T_2D or
     * T_3D, <tt>mipmaps</tt> must have a length of 1 because those targets only
     * expect a single layer. If the target is T_CUBEMAP, <tt>mipmaps</tt> must
     * have a length of 6, one for each face of a cube.
     * </p>
     * <p>
     * The provided mipmap layers cannot have any null elements within it.
     * Additionally, the Buffer type, dimensions and TextureFormat for each
     * Mipmap within <tt>mipmaps</tt> must match each other. Additionally,
     * mipmaps for T_1D must have a height and depth of 1; mipmaps for T_2D must
     * have a depth of 1; mipmaps for T_CUBEMAP must have a depth of 1 and must
     * be square. It is invalid to create a Texture with 0 layers. In the
     * future, new Targets may allow for different interpretations of the mipmap
     * layers.
     * </p>
     * <p>
     * The order of the mipmap layers when <tt>target</tt> is T_CUBEMAP is
     * important. See {@link #getMipmap(int)} for the required ordering.
     * </p>
     * 
     * @param target The Target which specifies how the Texture is accessed
     * @param mipmaps An array of Mipmaps representing the layers of the
     *            Texture
     * @throws NullPointerException if target or mipmaps is null, or if mipmaps
     *             contains any null elements
     * @throws IllegalArgumentException if any of conditions described above are
     *             not met
     */
    public Texture(Target target, Mipmap[] mipmaps) {
        if (target == null)
            throw new NullPointerException("Target cannot be null");
        if (mipmaps == null)
            throw new NullPointerException("Mipmap array cannot be null");

        // validate number of layers
        if (target == Target.T_CUBEMAP && mipmaps.length != 6)
            throw new IllegalArgumentException("Cube maps require exactly 6 layers, not: " + mipmaps.length);
        else if (target != Target.T_CUBEMAP && mipmaps.length != 1)
            throw new IllegalArgumentException("1D, 2D, and 3D textures require exactly 1 layer, not: " + mipmaps.length);
        
        baseLevel = Integer.MAX_VALUE;
        maxLevel = Integer.MIN_VALUE;
        // validate that each layer has correct dimensions, type and format, and not null
        for (int i = 0; i < mipmaps.length; i++) {
            if (mipmaps[i] == null)
                throw new NullPointerException("Mipmap layer cannot be null: " + i);
            
            if (i == 0) {
                // first pass through, assume 1st layer is representative
                if (target != Target.T_3D && mipmaps[i].getDepth(0) != 1)
                    throw new IllegalArgumentException("Textures with target=" + target + " must have a depth of 1");
                if (target == Target.T_1D && mipmaps[i].getHeight(0) != 1)
                    throw new IllegalArgumentException("Textures with target=" + target + " must have a height of 1");
                if (target == Target.T_CUBEMAP && mipmaps[i].getWidth(0) != mipmaps[i].getHeight(0))
                    throw new IllegalArgumentException("Textures with target=" + target + " must have square mipmap layers");
            } else {
                // remaining mipmaps must match properties of 1st mipmap
                if (mipmaps[i].getDepth(0) != mipmaps[0].getDepth(0) || 
                    mipmaps[i].getHeight(0) != mipmaps[0].getHeight(0) || 
                    mipmaps[i].getWidth(0) != mipmaps[0].getWidth(0))
                    throw new IllegalArgumentException("Mipmap layers must all have the same dimensions");
                if (mipmaps[i].getNumMipmaps() != mipmaps[0].getNumMipmaps())
                    throw new IllegalArgumentException("Mipmap layers must all have the same number of mipmaps");
                if (mipmaps[i].getFormat() != mipmaps[0].getFormat())
                    throw new IllegalArgumentException("Mipmap layers must all have the same TextureFormat");
                if (mipmaps[i].getDataType() != mipmaps[0].getDataType())
                    throw new IllegalArgumentException("Mipmap layers must all have the same Buffer data type");
            }
            
            // perform default detection of valid mipmap levels
            for (int j = 0; j < mipmaps[i].getNumMipmaps(); j++) {
                if (mipmaps[i].getData(j) != null) {
                    baseLevel = Math.min(baseLevel, j);
                    maxLevel = Math.max(maxLevel, j);
                }
            }
        }
        
        
        // at this point we know things are as valid as we can determine them,
        // so proceed with construction
        this.target = target;
        layers = Arrays.copyOf(mipmaps, mipmaps.length); // defensive copy
        
        if (baseLevel > maxLevel) {
            // weren't able to find a valid range, default to entire mipmap range
            baseLevel = 0;
            maxLevel = mipmaps[0].getNumMipmaps() - 1;
        }
        
        // don't use setters to avoid creation of dirty states
        wrapS = WrapMode.CLAMP;
        wrapT = WrapMode.CLAMP;
        wrapR = WrapMode.CLAMP;
        
        filter = Filter.LINEAR;
        anisoLevel = 1f;
        
        enableDepthCompare = false;
        depthCompareTest = Comparison.GREATER;
        dirty = null;
    }

    /**
     * @return The width of the top-most mipmap level of each layer in the
     *         Texture
     */
    public int getWidth() {
        return layers[0].getWidth(0);
    }
    
    /**
     * @return The height of the top-most mipmap level of each layer in the
     *         Texture
     */
    public int getHeight() {
        return layers[0].getHeight(0);
    }
    
    /**
     * @return The depth of the top-most mipmap level of each layer in the
     *         Texture
     */
    public int getDepth() {
        return layers[0].getDepth(0);
    }

    /**
     * @return The TextureFormat of every Mipmap layer within this Texture
     */
    public TextureFormat getFormat() {
        return layers[0].getFormat();
    }
    
    /**
     * @return The Buffer class type of all the Buffer data within each Mipmap
     *         layer within this Texture
     */
    public Class<? extends Buffer> getDataType() {
        return layers[0].getDataType();
    }
    
    /**
     * Set the S, T, and R coordinate's WrapMode to <tt>wrap</tt>.
     * 
     * @param wrap The new WrapMode for every coordinate
     * @throws NullPointerException if wrap is null
     */
    public void setWrapMode(WrapMode wrap) {
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
    public void setWrapMode(WrapMode s, WrapMode t, WrapMode r) {
        if (s == null || t == null || r == null)
            throw new NullPointerException("WrapModes cannot be null: " + s + ", " + t + ", " + r);
        wrapS = s;
        wrapT = t;
        wrapR = r;
        markParametersDirty();
    }
    
    /**
     * Get the type of texture coordinate wrapping when coordinates go beyond
     * the edge of the image, along the S texture coordinate.
     * 
     * @return The WrapMode for the S coordinate
     */
    public WrapMode getWrapModeS() {
        return wrapS;
    }
    
    /**
     * Get the type of texture coordinate wrapping when coordinates go beyond
     * the edge of the image, along the T texture coordinate.
     * 
     * @return The WrapMode for the T coordinate
     */
    public WrapMode getWrapModeT() {
        return wrapT;
    }
    
    /**
     * Get the type of texture coordinate wrapping when coordinates go beyond
     * the edge of the image, along the R texture coordinate.
     * 
     * @return The WrapMode for the R coordinate
     */
    public WrapMode getWrapModeR() {
        return wrapR;
    }

    /**
     * Set the Filter to use when minimizing or magnifying a texel when
     * rendering it to a Surface. The MIPMAP_x values are only valid if this
     * Texture has more than 1 level in its mipmap layers. If its layers
     * are not mipmapped, then MIPMAP_LINEAR becomes LINEAR and MIPMAP_NEAREST
     * becomes NEAREST.
     * 
     * @param filter The new Filter
     * @throws NullPointerException if filter is null
     */
    public void setFilter(Filter filter) {
        if (filter == null)
            throw new NullPointerException("Filter cannot be null");
        if (!layers[0].isMipmapped()) {
            if (filter == Filter.MIPMAP_LINEAR)
                filter = Filter.LINEAR;
            else if (filter == Filter.MIPMAP_NEAREST)
                filter = Filter.NEAREST;
        }
        
        this.filter = filter;
        markParametersDirty();
    }
    
    /**
     * @return The Filter to apply to the texels when rendering the texture
     */
    public Filter getFilter() {
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
    public void setAnisotropicFilterLevel(float level) {
        if (level < 0f || level > 1f)
            throw new IllegalArgumentException("Invalid level, must be in [0, 1], not: " + level);
        anisoLevel = level;
        markParametersDirty();
    }
    
    /**
     * @return The amount of anisotropic filtering from 0 to 1
     */
    public float getAnisotropicFilterLevel() {
        return anisoLevel;
    }

    /**
     * <p>
     * Set the range of mipmap levels to use when rasterizing a mipmapped
     * Texture. Frameworks do not need to allocate driver-level texture
     * data for mipmaps that are not needed. This can be used to do progressive
     * texture loading or to free up resources when textures are applied to far
     * away objects.
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
    public void setValidMipmapLevels(int base, int max) {
        if (base < 0)
            throw new IllegalArgumentException("Base level must be at least 0, not: " + base);
        if (max >= layers[0].getNumMipmaps())
            throw new IllegalArgumentException("Max level must be at most the number of levels in the image (" + layers[0].getNumMipmaps() + "), not: " + max);
        if (base > max)
            throw new IllegalArgumentException("Base level must be less than or equal to max, not: " + base + " > " + max);
        
        baseLevel = base;
        maxLevel = max;
        markParametersDirty();
    }
    
    /**
     * @return The lowest valid mipmap level to use during rasterization
     */
    public int getBaseMipmapLevel() {
        return baseLevel;
    }
    
    /**
     * @return The highest valid mipmap level to use during rasterization
     */
    public int getMaxMipmapLevel() {
        return maxLevel;
    }

    /**
     * Return the chosen Target for this Texture. The Target imposes a
     * number of restrictions on the dimensions of the Texture. For
     * example:
     * <ol>
     * <li>T_1D requires a height and depth of 1 for the root mipmap, and one layer.</li>
     * <li>T_2D requires a depth of 1 for the root mipmap, and one layer.</li>
     * <li>T_3D requires there to be only one layer.</li>
     * <li>T_CUBEMAP requires a depth of 1, its width and height to be equal,
     * and to have 6 layers all with matching dimensions.</li>
     * </ol>
     * 
     * @return The Target of this Texture
     */
    public Target getTarget() {
        return target;
    }

    /**
     * <p>
     * Return the {@link Mipmap} containing the hierarchical image data for a
     * given layer of the Texture. In many cases there will only be 1 layer
     * (for targets T_1D, T_2D and T_3D). For cube maps there are 6 layers with
     * layers arranged as PX, PY, PZ, NX, NY, NZ (one layer for each cube face).
     * </p>
     * <p>
     * Each Mipmap layer within a Texture will have the same dimensions,
     * the same number of mipmap levels, the same Buffer data type, and same
     * TextureFormat. There is always at least one layer in a Texture.
     * </p>
     * 
     * @param layer The layer to fetch (not to be confused with mipmap level),
     *            starting at 0
     * @return The Mipmap data for the specific layer
     * @throws IndexOutOfBoundsException if layer < 0 or layer >=
     *             {@link #getNumLayers()}
     */
    public Mipmap getMipmap(int layer) {
        return layers[layer];
    }
    
    /**
     * @return The number of layers present in this Texture
     */
    public int getNumLayers() {
        return layers.length;
    }

    /**
     * @see #setDepthCompareEnabled(boolean)
     * @return True if depth comparison is enabled when the TextureFormat of the
     *         Texture is {@link TextureFormat#DEPTH}
     */
    public boolean isDepthCompareEnabled() {
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
    public void setDepthCompareEnabled(boolean enable) {
        enableDepthCompare = enable;
        markParametersDirty();
    }
    
    /**
     * @return The Comparison function used when depth comparisons are enabled
     *         for depth textures
     */
    public Comparison getDepthComparison() {
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
    public void setDepthComparison(Comparison compare) {
        if (compare == null)
            throw new NullPointerException("Comparison cannot be null");
        depthCompareTest = compare;
        markParametersDirty();
    }

    /**
     * Mark the specified image region as dirty within the image data pointed to
     * by layer and level. The specified <tt>region</tt> will automatically be
     * constrained to the maximum dimensions of the given level.
     * 
     * @param region The ImageRegion representing the dirty pixels in layer and
     *            level
     * @param layer The layer whose mipmap is being marked dirty
     * @param level The level which is being marked dirty
     * @throws NullPointerException if region is null
     * @throws IndexOutOfBoundsException if layer < 0 or layer >=
     *             {@link #getNumLayers()}, or if level < 0 or level >= #
     *             mipmaps
     */
    public void markImageDirty(ImageRegion region, int layer, int level) {
        Mipmap m = getMipmap(layer);
        int levelWidth = m.getWidth(level);
        int levelHeight = m.getHeight(level);
        int levelDepth = m.getDepth(level);
        
        ImageRegion constrain = new ImageRegion(region, levelWidth, levelHeight, levelDepth);
        if (dirty == null)
            dirty = new TextureDirtyState(layers.length, layers[layer].getNumMipmaps(), false);
        dirty = dirty.updateMipmap(layer, level, constrain);
    }

    /**
     * Mark the specified mipmap <tt>level</tt> dirty within the given
     * <tt>layer</tt>. This is a convenience for invoking
     * {@link #markImageDirty(ImageRegion, int, int)} with an ImageRegion that
     * spans from (0,0,0) to the dimensions of the requested level.
     * 
     * @param layer The layer whose mipmap will be marked dirty
     * @param level The level within layer that will be marked dirty
     * @throws IndexOutOfBoundsException if layer < 0 or layer >=
     *             {@link #getNumLayers()}, or if level < 0 or level >= #
     *             mipmaps
     */
    public void markImageDirty(int layer, int level) {
        Mipmap m = getMipmap(layer);
        markImageDirty(new ImageRegion(0, 0, 0, m.getWidth(level), m.getHeight(level), m.getDepth(level)), 
                       layer, level);
    }

    /**
     * Mark every mipmap level dirty for the given <tt>layer</tt> of the
     * Texture. This can be used to mark a single face of a cube map dirty
     * for example. This is a convenience for invoking
     * {@link #markImageDirty(int, int)} for every mipmap level within the given
     * <tt>layer</tt>
     * 
     * @param layer The layer to mark completely dirty
     * @throws IndexOutOfBoundsException if layer < 0 or layer >=
     *             {@link #getNumLayers()}
     */
    public void markImageDirty(int layer) {
        for (int i = 0; i < layers[layer].getNumMipmaps(); i++)
            markImageDirty(layer, i);
    }

    /**
     * Mark the entirety of the Texture's image data dirty. This is a
     * convenience for invoking {@link #markImageDirty(int)} for each layer of
     * the Texture.
     */
    public void markImageDirty() {
        for (int i = 0; i < layers.length; i++)
            markImageDirty(i);
    }
    
    @Override
    public TextureDirtyState getDirtyState() {
        TextureDirtyState d = dirty;
        dirty = null;
        return d;
    }
    
    private void markParametersDirty() {
        if (dirty == null)
            dirty = new TextureDirtyState(layers.length, layers[0].getNumMipmaps(), true);
        else
            dirty = dirty.setTextureParametersDirty();
    }
}
