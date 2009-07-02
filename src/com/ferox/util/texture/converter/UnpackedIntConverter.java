package com.ferox.util.texture.converter;

import com.ferox.resource.BufferData.DataType;

/**
 * An encoder and decoder that works with all uncompressed and unpacked texture
 * formats that have a data type of UNSIGNED_INT.
 * 
 * @author Michael Ludwig
 */
public class UnpackedIntConverter extends UnpackedFormatConverter {
	private static final long MASK = 0xffffffffL;
	private static final double MAX_VALUE = (double) Integer.MAX_VALUE - (double) Integer.MIN_VALUE;

	public UnpackedIntConverter() {
		super(DataType.UNSIGNED_INT);
	}

	@Override
	protected float get(Object array, int index) {
		int signed = ((int[]) array)[index];
		return (float) ((signed & MASK) / MAX_VALUE);
	}

	@Override
	protected void set(Object array, int index, float value) {
		// double cast is necessary for large values
		((int[]) array)[index] = (int) ((long) (value * MAX_VALUE));
	}
}
