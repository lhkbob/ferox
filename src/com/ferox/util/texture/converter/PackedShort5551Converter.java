package com.ferox.util.texture.converter;

import com.ferox.math.Color;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.BufferData.DataType;

/**
 * Provides an implementation of Decoder and Encoder for the packed formats
 * RGBA_5551, BGRA_5551, ARGB_1555, and ABGR_1555; assuming that the type is
 * UNSIGNED_SHORT.
 * 
 * @author Michael Ludwig
 * 
 */
public class PackedShort5551Converter implements Decoder, Encoder {
	// used for RGBA and BGRA
	private static final int C1_MASK = 0xf800;
	private static final int C2_MASK = 0x07c0;
	private static final int C3_MASK = 0x003e;
	private static final int C4_MASK = 0x0001;

	// used for ARGB and ABGR
	private static final int C1_REV_MASK = 0x8000;
	private static final int C2_REV_MASK = 0x7c00;
	private static final int C3_REV_MASK = 0x03e0;
	private static final int C4_REV_MASK = 0x001f;

	private static final float MAX_VALUE = 31;

	@Override
	public boolean canDecode(DataType type, TextureFormat format) {
		return canEncode(type, format);
	}

	@Override
	public boolean canEncode(DataType type, TextureFormat format) {
		return type == DataType.UNSIGNED_SHORT
				&& (format == TextureFormat.ABGR_1555
						|| format == TextureFormat.BGRA_5551
						|| format == TextureFormat.ARGB_1555 || format == TextureFormat.RGBA_5551);
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
		case ABGR_1555:
			store.set(((val & C4_REV_MASK) >> 0) / MAX_VALUE,
					((val & C3_REV_MASK) >> 5) / MAX_VALUE,
					((val & C2_REV_MASK) >> 10) / MAX_VALUE,
					(val & C1_REV_MASK) != 0 ? 1f : 0f);
			break;
		case ARGB_1555:
			store.set(((val & C2_REV_MASK) >> 10) / MAX_VALUE,
					((val & C3_REV_MASK) >> 5) / MAX_VALUE,
					((val & C4_REV_MASK) >> 0) / MAX_VALUE,
					(val & C1_REV_MASK) != 0 ? 1f : 0f);
			break;
		case BGRA_5551:
			store.set(((val & C3_MASK) >> 1) / MAX_VALUE,
					((val & C2_MASK) >> 6) / MAX_VALUE, ((val & C1_MASK) >> 11)
							/ MAX_VALUE, (val & C4_MASK) != 0 ? 1f : 0f);
			break;
		case RGBA_5551:
			store.set(((val & C1_MASK) >> 11) / MAX_VALUE,
					((val & C2_MASK) >> 6) / MAX_VALUE, ((val & C3_MASK) >> 1)
							/ MAX_VALUE, (val & C4_MASK) != 0 ? 1f : 0f);
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
		int alpha = color.getAlpha() > 0f ? 1 : 0;

		short val = 0;

		// pack the color into a short
		switch (data.getFormat()) {
		case ABGR_1555:
			val = (short) (((red << 0) & C4_REV_MASK)
					| ((green << 5) & C3_REV_MASK)
					| ((blue << 10) & C2_REV_MASK) | ((alpha << 15) & C1_REV_MASK));
			break;
		case ARGB_1555:
			val = (short) (((red << 10) & C2_REV_MASK)
					| ((green << 5) & C3_REV_MASK)
					| ((blue << 0) & C4_REV_MASK) | ((alpha << 15) & C1_REV_MASK));
			break;
		case BGRA_5551:
			val = (short) (((red << 1) & C3_MASK) | ((green << 6) & C2_MASK)
					| ((blue << 11) & C1_MASK) | ((alpha << 0) & C4_MASK));
			break;
		case RGBA_5551:
			val = (short) (((red << 11) & C1_MASK) | ((green << 6) & C2_MASK)
					| ((blue << 1) & C3_MASK) | ((alpha << 0) & C4_MASK));
			break;
		}

		// write the packed color back into the array
		((short[]) data.getData().getData())[index] = val;
	}
}
