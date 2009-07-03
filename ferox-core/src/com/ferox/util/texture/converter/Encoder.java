package com.ferox.util.texture.converter;

import com.ferox.math.Color4f;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.BufferData.DataType;

/**
 * An Encoder provides functionality to set the color at specific texel
 * locations for a DataBlock of certain types and formats.
 * 
 * @author Michael Ludwig
 */
public interface Encoder {
	/**
	 * <p>
	 * Set the given color on the data block at the texel <x,y,z>. It can be
	 * assumed that x, y, and z are within the dimensions of the data block. The
	 * data block and color will not be null.
	 * </p>
	 * <p>
	 * If the Encoder doesn't support an alpha channel, the stored color
	 * components should be pre-multiplied by the alpha of color. If the Encoder
	 * represents a non-standard format, it must provide a reasonable
	 * implementation of extracting those values from the RGBA color.
	 * </p>
	 * 
	 * @param data The DataBlock representing the texture image being converted
	 * @param x The x coordinate to access, from 0 to data.getWidth() - 1
	 * @param y The y coordinate to access, from 0 to data.getHeight() - 1
	 * @param z The z coordinate to access, from 0 to data.getDepth() - 1
	 * @param color The color value to store at <x, y, z> in data
	 */
	public void setColor(DataBlock data, int x, int y, int z, Color4f color);

	/**
	 * Determine if this encoder can convert the given type and format. It can
	 * be assumed that the combination will be valid and not null.
	 * 
	 * @param type The DataType of a texture image
	 * @param format The TextureFormat of an image
	 * @return True if the type and format are supported
	 */
	public boolean canEncode(DataType type, TextureFormat format);
}