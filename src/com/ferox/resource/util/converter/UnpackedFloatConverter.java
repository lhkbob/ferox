package com.ferox.resource.util.converter;

import com.ferox.resource.BufferData.DataType;

/** An encoder and decoder that works with all uncompressed
 * and unpacked texture formats (including _FLOAT) that
 * have a data type of FLOAT.
 * 
 * @author Michael Ludwig
 *
 */
public class UnpackedFloatConverter extends UnpackedFormatConverter {
	public UnpackedFloatConverter() {
		super(DataType.FLOAT);
	}

	@Override
	protected final float get(Object array, int index) {
		return ((float[]) array)[index];
	}

	@Override
	protected final void set(Object array, int index, float value) {
		((float[]) array)[index] = value;
	}
	
}
