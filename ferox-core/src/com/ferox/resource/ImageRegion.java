package com.ferox.resource;

/**
 * <p>
 * ImageRegion represents an immutable region in 3D space that is associated
 * with a rasterized image or texture within that space. In texturing space, we
 * can consider the origin to be the lower left corner of the image. The x axis
 * represents the horizontal direction of the image and extends to the right.
 * Similarly, the y axis represents the vertical axis and extends upward. The z
 * axis extends outward. This configuration is identical to the layout of the
 * image data within a {@link Texture3D}.
 * </p>
 * <p>
 * When constructed an ImageRegion specifies a maximum value along each of these
 * three axis, which constrain the region into the valid image space for an
 * associated TextureImage. By setting a maximum dimension to 1, the
 * dimensionality of the ImageRegion is effectively reduced by 1. Intuitively
 * this makes sense since a 2D image is equivalent to a 3D image region with a
 * depth of 1.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class ImageRegion {
	private final int maxWidth;
	private final int maxHeight;
	private final int maxDepth;
	
	private int x, y, z, width, height, depth;

	/**
	 * <p>
	 * Construct a new ImageRegion that has the given x, y, z offsets and
	 * dimensions of (width, height, and depth). maxWidth, maxHeight, and
	 * maxDepth represent the upper bounds of the region along each axis. 0 is
	 * the lower bound for each axis. The initial region will be clamped to be
	 * within these extents.
	 * </p>
	 * <p>
	 * Each dimension given must be at least 1. Thus if an ImageRegion is
	 * desired that has fewer than 3 dimensions, specify a maxDepth of 1, which
	 * effectively clamps to a 2D plane. This can be continued down to 1 or 0
	 * dimensions.
	 * </p>
	 * 
	 * @param x Initial x offset, will be clamped
	 * @param y Initial y offset, will be clamped
	 * @param z Initial z offset, will be clamped
	 * @param width Initial width of region, will be clamped, must be at least 1
	 * @param height Initial height of region, will be clamped, must be at least
	 *            1
	 * @param depth Initial depth of region, will be clamped, must be at least 1
	 * @param maxWidth Maximum extent along x axis for region, must be at least
	 *            1
	 * @param maxHeight Maximum extent along y axis for region, must be at least
	 *            1
	 * @param maxDepth Maximum extent along z axis for region, must be at least
	 *            1
	 * @throws IllegalArgumentException if width, height, depth, maxWidth,
	 *             maxHeight, or maxDepth < 1
	 */
	public ImageRegion(int x, int y, int z, int width, int height, int depth, 
					   int maxWidth, int maxHeight, int maxDepth) {
		if (width < 1 || height < 1 || depth < 1)
			throw new IllegalArgumentException("Cannot have dimensions less than 1");
		if (maxWidth < 1 || maxHeight < 1 || maxDepth < 1)
			throw new IllegalArgumentException("Cannot have maximum dimensions less than 1");
		
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		this.maxDepth = maxDepth;
		
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
	 * Construct a new ImageRegion that is the union of this ImageRegion, and
	 * the region formed by [(x, y, z) - (x + width, y + height, z + depth)].
	 * The returned ImageRegion will have its dimensions to be clamped within
	 * the valid maximum size of this ImageRegion: [(0, 0, 0) - (maxWidth,
	 * maxHeight, maxDepth)].
	 * 
	 * @param x X offset of new rectangle to merge in
	 * @param y Y offset of new rectangle to merge in
	 * @param z Z offset of the new rectangle to merge in
	 * @param width Width of new rectangle, must be > 0
	 * @param height Height of the new rectangle, must be > 0
	 * @param depth Depth of the new rectangle, must be > 0
	 * @return New ImageRegion representing the combined region
	 * @throws IllegalArgumentException if width, height, depth < 1
	 */
	public ImageRegion merge(int x, int y, int z, 
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
		return new ImageRegion(minX, minY, minZ, maxX - minX, maxY - minY, maxZ - minZ,
							   maxWidth, maxHeight, maxDepth);
	}
}