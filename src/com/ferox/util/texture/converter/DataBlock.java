package com.ferox.util.texture.converter;

import com.ferox.resource.BufferData;
import com.ferox.resource.TextureFormat;

/**
 * A DataBlock is a TextureImage agnostic way of handling a single layer in an
 * image. It has three dimensions, but height and depth may be 1.
 * 
 * @author Michael Ludwig
 */
public class DataBlock {
	private final int width, height, depth;
	private final BufferData data;
	private TextureFormat format;

	protected DataBlock(BufferData data, int width, int height, int depth,
		TextureFormat format) {
		this.data = data;
		this.width = width;
		this.height = height;
		this.depth = depth;
	}

	/**
	 * Get the width, in texels of the data block. It will be >= 1.
	 * 
	 * @return The width of the data block
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Get the height, in texels of the data block. It will be >= 1.
	 * 
	 * @return The height of the data block
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Get the depth, in texels of the data block. It will be >= 1.
	 * 
	 * @return The depth of the data block
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * Get the BufferData that is the texture data for this DataBlock.
	 * 
	 * @return The BufferData for the block, it and its data array will not be
	 *         null
	 */
	public BufferData getData() {
		return data;
	}

	/**
	 * Get the TextureFormat for this data block. It will be compatible with the
	 * type of the BufferData.
	 * 
	 * @return The TextureFormat that describes the colors stored in this
	 *         block's BufferData
	 */
	public TextureFormat getFormat() {
		return format;
	}
}