package com.ferox.util.texture.converter;

import com.ferox.resource.BufferData.DataType;

/**
 * An encoder and decoder that works with all uncompressed and unpacked texture
 * formats that have a data type of UNSIGNED_BYTE.
 * 
 * @author Michael Ludwig
 */
public class UnpackedByteConverter extends UnpackedFormatConverter {
	private static final int MASK = 0xff;
	private static final float MAX_VALUE = (float) Byte.MAX_VALUE - (float) Byte.MIN_VALUE;

	public UnpackedByteConverter() {
		super(DataType.UNSIGNED_BYTE);
	}

	@Override
	protected float get(Object array, int index) {
		byte signed = ((byte[]) array)[index];
		return ((signed & MASK) / MAX_VALUE);
	}

	@Override
	protected void set(Object array, int index, float value) {
		((byte[]) array)[index] = (byte) (value * MAX_VALUE);
	}
}
