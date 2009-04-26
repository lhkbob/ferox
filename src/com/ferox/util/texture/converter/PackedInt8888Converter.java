package com.ferox.util.texture.converter;

import com.ferox.math.Color;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.texture.TextureFormat;
import com.ferox.util.texture.converter.TextureConverter.DataBlock;
import com.ferox.util.texture.converter.TextureConverter.Decoder;
import com.ferox.util.texture.converter.TextureConverter.Encoder;

/**
 * Provides an implementation of Decoder and Encoder for the packed formats
 * RGBA_8888, BGRA_8888, ARGB_8888, and ABGR_8888; assuming that the type is
 * UNSIGNED_INT.
 * 
 * @author Michael Ludwig
 * 
 */
public class PackedInt8888Converter implements Decoder, Encoder {
	private static final long C1_MASK = 0xff000000;
	private static final int C2_MASK = 0x00ff0000;
	private static final int C3_MASK = 0x0000ff00;
	private static final int C4_MASK = 0x000000ff;

	private static final float MAX_VALUE = 255;

	@Override
	public boolean canDecode(DataType type, TextureFormat format) {
		return canEncode(type, format);
	}

	@Override
	public boolean canEncode(DataType type, TextureFormat format) {
		return type == DataType.UNSIGNED_INT
				&& (format == TextureFormat.ABGR_8888
						|| format == TextureFormat.BGRA_8888
						|| format == TextureFormat.ARGB_8888 || format == TextureFormat.RGBA_8888);
	}

	@Override
	public void getColor(DataBlock data, float u, float v, float w, Color store) {
		int x = (int) u * data.getWidth();
		int y = (int) v * data.getHeight();
		int z = (int) w * data.getDepth();

		int index = x + y * data.getWidth() + z * data.getWidth()
				* data.getHeight();
		int val = ((int[]) data.getData().getData())[index];

		switch (data.getFormat()) {
		case ABGR_4444:
			store.set(((val & C4_MASK) >> 0) / MAX_VALUE,
					((val & C3_MASK) >> 8) / MAX_VALUE, ((val & C2_MASK) >> 16)
							/ MAX_VALUE, ((val & C1_MASK) >> 24) / MAX_VALUE);
			break;
		case ARGB_4444:
			store.set(((val & C2_MASK) >> 16) / MAX_VALUE,
					((val & C3_MASK) >> 8) / MAX_VALUE, ((val & C4_MASK) >> 0)
							/ MAX_VALUE, ((val & C1_MASK) >> 24) / MAX_VALUE);
			break;
		case BGRA_4444:
			store.set(((val & C3_MASK) >> 8) / MAX_VALUE,
					((val & C2_MASK) >> 16) / MAX_VALUE,
					((val & C1_MASK) >> 24) / MAX_VALUE, ((val & C4_MASK) >> 0)
							/ MAX_VALUE);
			break;
		case RGBA_4444:
			store.set(((val & C1_MASK) >> 24) / MAX_VALUE,
					((val & C2_MASK) >> 16) / MAX_VALUE, ((val & C3_MASK) >> 8)
							/ MAX_VALUE, ((val & C4_MASK) >> 0) / MAX_VALUE);
			break;
		}
	}

	@Override
	public void setColor(DataBlock data, int x, int y, int z, Color color) {
		int index = x + y * data.getWidth() + z * data.getWidth()
				* data.getHeight();

		int red = (int) (color.getRed() * MAX_VALUE);
		int green = (int) (color.getGreen() * MAX_VALUE);
		int blue = (int) (color.getBlue() * MAX_VALUE);
		int alpha = (int) (color.getAlpha() * MAX_VALUE);

		int val = 0;

		// pack the color into a int
		switch (data.getFormat()) {
		case ABGR_4444:
			val = (int) (((red << 0) & C4_MASK) | ((green << 8) & C3_MASK)
					| ((blue << 16) & C2_MASK) | ((alpha << 24) & C1_MASK));
			break;
		case ARGB_4444:
			val = (int) (((red << 16) & C2_MASK) | ((green << 8) & C3_MASK)
					| ((blue << 0) & C4_MASK) | ((alpha << 24) & C1_MASK));
			break;
		case BGRA_4444:
			val = (int) (((red << 8) & C3_MASK) | ((green << 16) & C2_MASK)
					| ((blue << 24) & C1_MASK) | ((alpha << 0) & C4_MASK));
			break;
		case RGBA_4444:
			val = (int) (((red << 24) & C1_MASK) | ((green << 16) & C2_MASK)
					| ((blue << 8) & C3_MASK) | ((alpha << 0) & C4_MASK));
			break;
		}

		// write the packed color back into the array
		((int[]) data.getData().getData())[index] = val;
	}
}
