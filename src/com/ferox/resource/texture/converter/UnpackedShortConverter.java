package com.ferox.resource.texture.converter;

import com.ferox.resource.BufferData.DataType;

/** An encoder and decoder that works with all uncompressed
 * and unpacked texture formats that have a data type of 
 * UNSIGNED_SHORT.
 * 
 * @author Michael Ludwig
 *
 */
public class UnpackedShortConverter extends UnpackedFormatConverter {
	private static final int MASK = 0xffff;
	private static final float MAX_VALUE = (float) Short.MAX_VALUE - (float) Short.MIN_VALUE;
	
	public UnpackedShortConverter() {
		super(DataType.UNSIGNED_SHORT);
	}

	@Override
	protected float get(Object array, int index) {
		int signed = ((short[]) array)[index];
		return ((signed & MASK) / MAX_VALUE);
	}

	@Override
	protected void set(Object array, int index, float value) {
		((short[]) array)[index] = (short) (value * MAX_VALUE);
	}
}
