package com.ferox.util.texture.converter;

import com.ferox.math.Color;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.texture.TextureFormat;

/**
 * An Decoder provides functionality to get the color at specific texel
 * locations for a DataBlock of certain types and formats.
 * 
 * @author Michael Ludwig
 */
public interface Decoder {
	/**
	 * <p>
	 * Get the given color on the data block at the point <u,v,w>. It can be
	 * assumed that u, v, and w are in [0, 1], representing normalized
	 * texture coordinates across the valid dimensions. The data block and
	 * store will not be null. store must hold the color result.
	 * </p>
	 * <p>
	 * If the Decoder's format doesn't support an alpha channel, the stored
	 * alpha value should be set 1. The decoder can decide how to compute
	 * the final color, either using some form of filtering or just
	 * nearest-neighbor.
	 * </p>
	 * <p>
	 * If the Decoder represents a non-standard format, it must provide a
	 * reasonable implementation for determining rgba colors. For example,
	 * luminance and depth may store that value in the RGB components.
	 * </p>
	 * 
	 * @param data The DataBlock representing the texture image being
	 *            converted
	 * @param u The x coordinate to access, from 0 to 1
	 * @param v The y coordinate to access, from 0 to 1
	 * @param w The z coordinate to access, from 0 to 1
	 * @param color The color value to hold the read color from data
	 */
	public void getColor(DataBlock data, float u, float v, float w,
			Color store);

	/**
	 * Determine if this decoder can convert the given type and format. It
	 * can be assumed that the combination will be valid and not null.
	 * 
	 * @param type The DataType of a texture image
	 * @param format The TextureFormat of an image
	 * @return True if the type and format are supported
	 */
	public boolean canDecode(DataType type, TextureFormat format);
}