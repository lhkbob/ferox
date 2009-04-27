package com.ferox.util.texture.converter;

import com.ferox.math.Color;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.BufferData.DataType;

/**
 * Provides an Encoder and Decoder implementation for the TextureFormats BGR_565
 * and RGB_565
 * 
 * @author Michael Ludwig
 * 
 */
public class PackedShort565Converter implements Decoder, Encoder {
	private static final int C1_MASK = 0xF800;
	private static final int C2_MASK = 0x07E0;
	private static final int C3_MASK = 0x001F;

	private static final float MAX_RB = 31;
	private static final float MAX_G = 63;

	@Override
	public boolean canDecode(DataType type, TextureFormat format) {
		return canEncode(type, format);
	}

	@Override
	public boolean canEncode(DataType type, TextureFormat format) {
		return type == DataType.UNSIGNED_SHORT
				&& (format == TextureFormat.BGR_565 || format == TextureFormat.RGB_565);
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
		case BGR_565:
			store.set(((val & C3_MASK) >> 0) / MAX_RB, ((val & C2_MASK) >> 5)
					/ MAX_G, ((val & C1_MASK) >> 11) / MAX_RB, 1f);
			break;
		case RGB_565:
			store.set(((val & C1_MASK) >> 11) / MAX_RB, ((val & C2_MASK) >> 5)
					/ MAX_G, ((val & C3_MASK) >> 0) / MAX_RB, 1f);
			break;
		}
	}

	@Override
	public void setColor(DataBlock data, int x, int y, int z, Color color) {
		int index = x + y * data.getWidth() + z * data.getWidth()
				* data.getHeight();

		float alpha = color.getAlpha();

		int red = (int) (color.getRed() * alpha * MAX_RB);
		int green = (int) (color.getGreen() * alpha * MAX_G);
		int blue = (int) (color.getBlue() * alpha * MAX_RB);

		short val = 0;

		// pack the color into a short
		switch (data.getFormat()) {
		case BGR_565:
			val = (short) (((red << 0) & C3_MASK) | ((green << 5) & C2_MASK) | ((blue << 11) & C1_MASK));
			break;
		case RGB_565:
			val = (short) (((red << 11) & C1_MASK) | ((green << 5) & C2_MASK) | ((blue << 0) & C3_MASK));
			break;
		}

		// write the packed color back into the array
		((short[]) data.getData().getData())[index] = val;
	}
}
