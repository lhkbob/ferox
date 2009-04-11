package com.ferox.resource.texture.converter;

import com.ferox.math.Color;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.texture.TextureFormat;
import com.ferox.resource.texture.converter.TextureConverter.DataBlock;
import com.ferox.resource.texture.converter.TextureConverter.Decoder;
import com.ferox.resource.texture.converter.TextureConverter.Encoder;

/** Implements the majority of the work necessary for a Decoder and
 * Encoder involving formats that don't pack their data or compress
 * it in any other way.
 * 
 * @author Michael Ludwig
 *
 */
public abstract class UnpackedFormatConverter implements Decoder, Encoder {
	private DataType validType;
	
	/** SupportedType is the single type that this decoder and
	 * encoder knows how to read.  It determines the type of
	 * array passed into get() and set(). */
	protected UnpackedFormatConverter(DataType supportedType) {
		this.validType = supportedType;
	}
	
	@Override
	public boolean canDecode(DataType type, TextureFormat format) {
		return this.supported(type, format);
	}

	@Override
	public void getColor(DataBlock data, float u, float v, float w, Color store) {
		int x = (int) u * data.getWidth();
		int y = (int) v * data.getHeight();
		int z = (int) w * data.getDepth();
		
		int numC = data.getFormat().getPrimitivesPerColor();
		// first index of the color 
		int index = numC * (x + y * data.getWidth() + z * data.getHeight() * data.getWidth());
		Object array = data.getData().getData();
		
		switch(data.getFormat()) {
		case BGRA: 
			store.set(this.get(array, index + 2), 
					  this.get(array, index + 1),
					  this.get(array, index + 0),
					  this.get(array, index + 3));
			break;
		case BGR:
			store.set(this.get(array, index + 2), 
					  this.get(array, index + 1),
					  this.get(array, index + 0),
					  1f);
			break;
		case RGBA: case RGBA_FLOAT:
			store.set(this.get(array, index + 0), 
					  this.get(array, index + 1),
					  this.get(array, index + 2),
					  this.get(array, index + 3));
			break;
		case RGB: case RGB_FLOAT:
			store.set(this.get(array, index + 0), 
					  this.get(array, index + 1),
					  this.get(array, index + 2),
					  1f);
			break;
		case ALPHA: case ALPHA_FLOAT:
			store.set(0f, 0f, 0f, this.get(array, index));
			break;	
		case LUMINANCE_ALPHA: case LUMINANCE_ALPHA_FLOAT: {
			float l = this.get(array, index);
			store.set(l, l, l, this.get(array, index + 1));
			break; }
		case DEPTH: case LUMINANCE: case LUMINANCE_FLOAT: {
			float ld = this.get(array, index);
			store.set(ld, ld, ld, 1);
			break; }
		}
	}

	@Override
	public boolean canEncode(DataType type, TextureFormat format) {
		return this.supported(type, format);
	}

	@Override
	public void setColor(DataBlock data, int x, int y, int z, Color color) {
		int numC = data.getFormat().getPrimitivesPerColor();
		// first index of the color 
		int index = numC * (x + y * data.getWidth() + z * data.getHeight() * data.getWidth());
		Object array = data.getData().getData();
		
		switch(data.getFormat()) {
		case BGRA: 
			this.set(array, index + 2, color.getRed());
			this.set(array, index + 1, color.getGreen());
			this.set(array, index + 0, color.getBlue());
			this.set(array, index + 3, color.getAlpha());
			break;
		case BGR: {
			float a = color.getAlpha();
			this.set(array, index + 2, color.getRed() * a);
			this.set(array, index + 1, color.getGreen() * a);
			this.set(array, index + 0, color.getBlue() * a);
			break; }
		case RGBA: case RGBA_FLOAT:
			this.set(array, index + 0, color.getRed());
			this.set(array, index + 1, color.getGreen());
			this.set(array, index + 2, color.getBlue());
			this.set(array, index + 3, color.getAlpha());
			break;
		case RGB: case RGB_FLOAT: {
			float a = color.getAlpha();
			this.set(array, index + 0, color.getRed() * a);
			this.set(array, index + 1, color.getGreen() * a);
			this.set(array, index + 2, color.getBlue() * a);
			break; }
		case ALPHA: case ALPHA_FLOAT:
			this.set(array, index, color.getAlpha());
			break;	
		case LUMINANCE_ALPHA: case LUMINANCE_ALPHA_FLOAT: {
			this.set(array, index, (color.getRed() + color.getGreen() + color.getBlue()) / 3f);
			this.set(array, index + 1, color.getAlpha());
			break; }
		case DEPTH: case LUMINANCE: case LUMINANCE_FLOAT: {
			this.set(array, index, (color.getRed() + color.getGreen() + color.getBlue()) / 3f);
			break; }
		}
	}
	
	/** Set the given array value at index to the floating
	 * point value. This will likely involve scaling and
	 * casting the value.  This value will be between
	 * 0 and 1. */
	protected abstract void set(Object array, int index, float value);
	
	/** Return a floating point value that represents the
	 * final color value, as if it were converted into a
	 * texture in the graphics card. */
	protected abstract float get(Object array, int index);
	
	private boolean supported(DataType type, TextureFormat format) {
		return type == this.validType && !format.isCompressed() && !format.isPackedFormat();
	}

}
