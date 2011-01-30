package com.ferox.resource;

/**
 * <p>
 * MipmapRegion represents an immutable region in 3D space that is associated
 * with a rasterized image or texture within that space. In texturing space, we
 * can consider the origin to be the lower left corner of the image. The x axis
 * represents the horizontal direction of the image and extends to the right.
 * Similarly, the y axis represents the vertical axis and extends upward. The z
 * axis extends outward. This configuration is identical to the layout of the
 * image data within a {@link Texture}.
 * </p>
 * <p>
 * The offsets and dimensions of an MipmapRegion can be assumed to form a region
 * within the space described above, that will be equal to or contained within
 * the region (0, 0, 0) - (maxWidth, maxHeight, maxDepth). Initially,
 * ImageRegions created via {@link #MipmapRegion(int, int)},
 * {@link #MipmapRegion(int, int, int, int)} and
 * {@link #MipmapRegion(int, int, int, int, int, int)} have each maximum
 * dimensional value set to {@link Integer#MAX_VALUE}. These maximum extents are
 * later constrained via {@link #MipmapRegion(MipmapRegion, int, int, int)} when
 * needed by a Texture.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class MipmapRegion {
    private final int maxWidth;
    private final int maxHeight;
    private final int maxDepth;
    
    private final int x, y, z, width, height, depth;
    private final int layer, mipmapLevel;

    /**
     * Convenience constructor to create an MipmapRegion for a 1D texture that is
     * marked as dirty from x to (x + width). This is equivalent to
     * MipmapRegion(x, 0, width, 1).
     * 
     * @param x The x offset into the 1D texture
     * @param width The number of pixels from x that are dirty
     * @throws IllegalArgumentException if width < 1
     */
    public MipmapRegion(int x, int width) {
        this(x, 0, width, 1);
    }

    /**
     * Convenience constructor to create an MipmapRegion for a 2D texture that is
     * marked as dirty from (x, y) to (x + width, y + height). This is
     * equivalent to MipmapRegion(x, y, 0, width, height, 1).
     * 
     * @param x The x offset into the 2D texture
     * @param y The y offset into the 2D texture
     * @param width The width of the dirty region, from x
     * @param height The height of the dirty region, from y
     * @throws IllegalArgumentException if width, height < 1
     */
    public MipmapRegion(int x, int y, int width, int height) {
        this(x, y, 0, width, height, 1);
    }

    /**
     * <p>
     * Construct a new MipmapRegion that has the given x, y, z offsets and
     * dimensions of (width, height, and depth). Each dimension given must be at
     * least 1. Thus if an MipmapRegion is desired to have fewer than 3
     * dimensions, specify a dimension of 1.
     * </p>
     * <p>
     * Initially, MipmapRegion's constructed with this constructor are only
     * constrained to have dimensions of at least 1, and to have offsets that
     * are positive. However, when an MipmapRegion is used to mark a Texture
     * as dirty, a new MipmapRegion is created with the maximum valid dimensions
     * for the texture using {@link #MipmapRegion(MipmapRegion, int, int, int)}.
     * </p>
     * 
     * @param x Initial x offset
     * @param y Initial y offset
     * @param z Initial z offset
     * @param width Initial width of region, must be at least 1
     * @param height Initial height of region, must be at least 1
     * @param depth Initial depth of region, must be at least 1
     * @throws IllegalArgumentException if width, height, depth < 1
     */
    public MipmapRegion(int x, int y, int z, int width, int height, int depth) {
        this(x, y, z, width, height, depth, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
    }

    /**
     * Create a new MipmapRegion that matches <tt>r</tt> except that its
     * dimensions are constrained to the maxWidth, maxHeight, and maxDepth. The
     * other constructors should generally be used in preference to this one,
     * this constructor's purpose is to allow Texture to constrain ImageRegions
     * correctly within its markDirty() functions, and to specify the layer
     * and mipmap level.
     * 
     * @param r The MipmapRegion to constrain
     * @param maxWidth The maximum allowed width
     * @param maxHeight The maximum allowed height
     * @param maxDepth The maximum allowed depth
     * @param layer The layer within the Texture to update
     * @param level The mipmap level within the provided layer
     * @throws IllegalArgumentException if maxWidth, maxHeight, or maxDepth < 1
     */
    public MipmapRegion(MipmapRegion r, int maxWidth, int maxHeight, int maxDepth,
                        int layer, int level) {
        this(r.x, r.y, r.z, r.width, r.height, r.depth, maxWidth, maxHeight, maxDepth, layer, level);
    }

    private MipmapRegion(int x, int y, int z, int width, int height, int depth, 
                       int maxWidth, int maxHeight, int maxDepth, int layer, int level) {
        if (width < 1 || height < 1 || depth < 1)
            throw new IllegalArgumentException("Cannot have dimensions less than 1");
        if (maxWidth < 1 || maxHeight < 1 || maxDepth < 1)
            throw new IllegalArgumentException("Cannot have maximum dimensions less than 1");
        if (layer < 0)
            throw new IllegalArgumentException("Layer must be at least 0");
        if (level < 0)
            throw new IllegalArgumentException("Mipmap level must be at least 0");
        
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxDepth = maxDepth;
        
        this.layer = layer;
        this.mipmapLevel = level;
        
        this.x = Math.max(0, Math.min(x, maxWidth - 1));
        this.y = Math.max(0, Math.min(y, maxHeight - 1));
        this.z = Math.max(0, Math.min(z, maxDepth - 1));
        
        this.width = Math.min(width, maxWidth - this.x);
        this.height = Math.min(height, maxHeight - this.y);
        this.depth = Math.min(depth, maxDepth - this.z);
    }
    
    /**
     * Return the x offset of this region within the image block.
     * 
     * @return The x offset, will be between [0, maxWidth]
     */
    public int getXOffset() {
        return x;
    }

    /**
     * Return the y offset of this region within the image block.
     * 
     * @return The y offset, will be between [0, maxHeight]
     */
    public int getYOffset() {
        return y;
    }

    /**
     * Return the z offset of this region within the image block.
     * 
     * @return The z offset, will be between [0, maxDepth]
     */
    public int getZOffset() {
        return z;
    }

    /**
     * Return the width of this region within the image block. The returned
     * width will not cause the region to extend beyond the maxWidth, based on
     * the x offset.
     * 
     * @return Width of region
     */
    public int getWidth() {
        return width;
    }

    /**
     * Return the height of this region within the image block. The returned
     * height will not cause the region to extend beyond the maxHeight, based on
     * the y offset.
     * 
     * @return Height of region
     */
    public int getHeight() {
        return height;
    }

    /**
     * Return the depth of this region within the image block. The returned
     * depth will not cause the region to extend beyond the maxDepth, based on
     * the z offset.
     * 
     * @return Depth of region
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Construct a new MipmapRegion that is the union of this MipmapRegion, and
     * the region formed by [(x, y, z) - (x + width, y + height, z + depth)].
     * The returned MipmapRegion will have its dimensions clamped within the
     * valid maximum size of this MipmapRegion: [(0, 0, 0) - (maxWidth,
     * maxHeight, maxDepth)].
     * 
     * @param x X offset of new rectangle to merge in
     * @param y Y offset of new rectangle to merge in
     * @param z Z offset of the new rectangle to merge in
     * @param width Width of new rectangle, must be > 0
     * @param height Height of the new rectangle, must be > 0
     * @param depth Depth of the new rectangle, must be > 0
     * @return New MipmapRegion representing the combined region
     * @throws IllegalArgumentException if width, height, depth < 1
     */
    public MipmapRegion merge(int x, int y, int z, 
                             int width, int height, int depth) {
        if (width < 1 || height < 1 || depth < 1)
            throw new IllegalArgumentException("Cannot have dimensions less than 1");
        
        // extents of the dirty region
        int minX = Math.min(x, this.x);
        int minY = Math.min(y, this.y);
        int minZ = Math.min(z, this.z);
        int maxX = Math.max(x + width, this.x + this.width);
        int maxY = Math.max(y + height, this.y + this.height);
        int maxZ = Math.max(z + depth, this.z + this.depth);
        
        // constrained to valid region
        return new MipmapRegion(minX, minY, minZ, maxX - minX, maxY - minY, maxZ - minZ,
                               maxWidth, maxHeight, maxDepth, layer, mipmapLevel);
    }

    /**
     * Construct a new MipmapRegion that is the union of this MipmapRegion and the
     * given MipmapRegion. The constructed region will be constrained to the
     * maximum dimensions of this MipmapRegion. If region is null, it returns this
     * MipmapRegion.
     * 
     * @param region The MipmapRegion to merge with
     * @return A new MipmapRegion representing the union
     */
    public MipmapRegion merge(MipmapRegion region) {
        if (region != null) {
            if (region.layer != layer || region.mipmapLevel != mipmapLevel)
                throw new IllegalArgumentException("Layer and mipmap levels  must match");
            return merge(region.x, region.y, region.z, region.width, region.height, region.depth);
        } else
            return this;
        
    }
}