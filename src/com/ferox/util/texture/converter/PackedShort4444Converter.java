package com.ferox.util.texture.converter;

import com.ferox.math.Color;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.BufferData.DataType;

/**
 * Provides an implementation of Decoder and Encoder for the packed formats
 * RGBA_4444, BGRA_4444, ARGB_4444, and ABGR_4444; assuming that the type is
 * UNSIGNED_SHORT.
 * 
 * @author Michael Ludwig
 * 
 */
public class PackedShort4444Converter implements Decoder, Encoder {
	private static final int C1_MASK = 0xf000;
	private static final int C2_MASK = 0x0f00;
	private static final int C3_MASK = 0x00f0;
	private static final int C4_MASK = 0x000f;

	private static final float MAX_VALUE = 15;

	@Override
	public boolean canDecode(DataType type, TextureFormat format) {
		return canEncode(type, format);
	}

	@Override
	public boolean canEncode(DataType type, TextureFormat format) {
		return type == DataType.UNSIGNED_SHORT
				&& (format == TextureFormat.ABGR_4444
						|| format == TextureFormat.BGRA_4444
						|| format == TextureFormat.ARGB_4444 || format == TextureFormat.RGBA_4444);
	}

	@Override
	public void getColor(DataBlock data, float u, float v, float w, Color store) {
		int x = (int) u * data.getWidth();
		int y = (int) v * data.getHeight();
		int z = (int) w * data.getDepth();

		int index = x + y * data.getWidth() + z * data.getWidth()
				* data.getHeight();
		short val = ((short[]) data.getData().getData())[index];

		switch (data.getFormat()) {
		case ABGR_4444:
			store.set(((val & C4_MASK) >> 0) / MAX_VALUE,
					((val & C3_MASK) >> 4) / MAX_VALUE, ((val & C2_MASK) >> 8)
							/ MAX_VALUE, ((val & C1_MASK) >> 12) / MAX_VALUE);
			break;
		case ARGB_4444:
			store.set(((val & C2_MASK) >> 8) / MAX_VALUE,
					((val & C3_MASK) >> 4) / MAX_VALUE, ((val & C4_MASK) >> 0)
							/ MAX_VALUE, ((val & C1_MASK) >> 12) / MAX_VALUE);
			break;
		case BGRA_4444:
			store.set(((val & C3_MASK) >> 4) / MAX_VALUE,
					((val & C2_MASK) >> 8) / MAX_VALUE, ((val & C1_MASK) >> 12)
							/ MAX_VALUE, ((val & C4_MASK) >> 0) / MAX_VALUE);
			break;
		case RGBA_4444:
			store.set(((val & C1_MASK) >> 12) / MAX_VALUE,
					((val & C2_MASK) >> 8) / MAX_VALUE, ((val & C3_MASK) >> 4)
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

		short val = 0;

		// pack the color into a short
		switch (data.getFormat()) {
		case ABGR_4444:
			val = (short) (((red << 0) & C4_MASK) | ((green << 4) & C3_MASK)
					| ((blue << 8) & C2_MASK) | ((alpha << 12) & C1_MASK));
			break;
		case ARGB_4444:
			val = (short) (((red << 8) & C2_MASK) | ((green << 4) & C3_MASK)
					| ((blue << 0) & C4_MASK) | ((alpha << 12) & C1_MASK));
			break;
		case BGRA_4444:
			val = (short) (((red << 4) & C3_MASK) | ((green << 8) & C2_MASK)
					| ((blue << 12) & C1_MASK) | ((alpha << 0) & C4_MASK));
			break;
		case RGBA_4444:
			val = (short) (((red << 12) & C1_MASK) | ((green << 8) & C2_MASK)
					| ((blue << 4) & C3_MASK) | ((alpha << 0) & C4_MASK));
			break;
		}

		// write the packed color back into the array
		((short[]) data.getData().getData())[index] = val;
	}
}
