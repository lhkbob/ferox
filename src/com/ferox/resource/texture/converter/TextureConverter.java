package com.ferox.resource.texture.converter;

import java.util.ArrayList;
import java.util.List;

import com.ferox.math.Color;
import com.ferox.resource.BufferData;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.texture.TextureFormat;

/** TextureConverter provides utilities to register decoders and
 * encoders of texture data, as well as provide a utility methods
 * that will convert formats and scale dimensions.
 * 
 * There are default encoders and decoders supported all TextureFormats (with
 * valid types) except for the DXT_x formats, that have already been 
 * registered.
 * 
 * This utility is not intended to be an efficient imaging processor,
 * but is instead meant to be flexible, allowing for many formats to
 * be converted and scaled.
 * 
 * @author Michael Ludwig
 *
 */
public class TextureConverter {
	/** A DataBlock is a TextureImage agnostic way of handling
	 * a single layer in an image.  It has three dimensions, but height
	 * and depth may be 1. */
	public static class DataBlock {
		private int width, height, depth;
		private BufferData data;
		private TextureFormat format;
		
		protected DataBlock(BufferData data, int width, int height, int depth, TextureFormat format) {
			this.data = data;
			this.width = width;
			this.height = height;
			this.depth = depth;
		}

		/** Get the width, in texels of the data block. 
		 * It will be >= 1. */
		public int getWidth() {
			return this.width;
		}

		/** Get the height, in texels of the data block.
		 * It will be >= 1. */
		public int getHeight() {
			return this.height;
		}

		/** Get the depth, in texels of the data block. 
		 * It will be >= 1. */
		public int getDepth() {
			return this.depth;
		}

		/** Get the BufferData that is the texture data for this
		 * DataBlock. This will not be null, and its array won't be either. */
		public BufferData getData() {
			return this.data;
		}

		/** Get the TextureFormat for this data block.  It will
		 * be compatible with the type of the BufferData. */
		public TextureFormat getFormat() {
			return this.format;
		}
	}
	
	/** An Encoder provides functionality to set the color at
	 * specific texel locations for a DataBlock of certain types
	 * and formats. */
	public static interface Encoder {
		/** Set the given color on the data block at the texel <x,y,z>.
		 * It can be assumed that x, y, and z are within the dimensions
		 * of the data block.  The data block and color will not be null.
		 * 
		 * If the Encoder doesn't support an alpha channel, the stored
		 * color components should be pre-multiplied by the alpha of color.
		 * If the Encoder represents a non-standard format, it must
		 * provide a reasonable implementation of extracting those values 
		 * from the RGBA color. */
		public void setColor(DataBlock data, int x, int y, int z, Color color);
		
		/** Determine if this encoder can convert the given type and format.
		 * It can be assumed that the combination will be valid and not null. */
		public boolean canEncode(DataType type, TextureFormat format);
	}
	
	/** An Decoder provides functionality to get the color at
	 * specific texel locations for a DataBlock of certain types
	 * and formats. */
	public static interface Decoder {
		/** Get the given color on the data block at the point <u,v,w>.
		 * It can be assumed that u, v, and w are in [0, 1], representing
		 * normalized texture coordinates across the valid dimensions.
		 * The data block and store will not be null. store must hold the color result.
		 * 
		 * If the Decoder's format doesn't support an alpha channel, the stored
		 * alpha value should be set 1.  The decoder can decide how to compute the
		 * final color, either using some form of filtering or just nearest-neighbor.
		 * 
		 * If the Decoder represents a non-standard format, it must
		 * provide a reasonable implementation for determining rgba colors.
		 * For example, luminance and depth may store that value in the RGB
		 * components. */
		public void getColor(DataBlock data, float u, float v, float w, Color store);
		
		/** Determine if this decoder can convert the given type and format.
		 * It can be assumed that the combination will be valid and not null. */
		public boolean canDecode(DataType type, TextureFormat format);
	}
	
	private static List<Encoder> encoders = new ArrayList<Encoder>();
	private static List<Decoder> decoders = new ArrayList<Decoder>();
	
	/** This method will re-scale and change the data type and format of a single
	 * mipmap layer of a texture image.  This image can be 2d or 3d (if it's 2d, the depth
	 * should be 1).
	 * 
	 * If the result BufferData is null, doesn't match newType, or doesn't have the correct size,
	 * a new BufferData is used.  Otherwise, its data array is used to store the converted image
	 * (which may have to be allocated if it is null), and result will be returned.
	 * 
	 * If the backing array behind result is null, then a new one is created.  If a data's backing
	 * array is null, an empty BufferData is created with an array sized appropriately for the
	 * requested type, format and dimensions.
	 * 
	 * Returns the BufferData holding the converted image.
	 * 
	 * Throws an exception if data, oldFormat, oldWidth, oldHeight and oldDepth are incompatible:
	 * 	- dimensions are negative or don't match the size of the buffer data's capacity
	 *  - the format doesn't match the buffer data's type
	 *  - the buffer data or format are null.
	 *  
	 * Throws an exception if the new format, new type and new dimensions are incompatible:
	 * 	- as above, except format and newType must match (if result's type doesn't match, a 
	 * 	  new buffer data is used instead).
	 *  - newType cannot be null, either. 
	 *  
	 * Throws an exception if there's no registered decoder for the combination of oldFormat
	 * and data's type.  Also fails if there's no encoder for the newFormat and newType. */
	public static BufferData convert(BufferData data, TextureFormat oldFormat, int oldWidth, int oldHeight, int oldDepth, 
									 BufferData result, TextureFormat newFormat, DataType newType, 
									 int newWidth, int newHeight, int newDepth) throws NullPointerException, IllegalArgumentException {
		// validate input parameters that involve the source data
		if (data == null || oldFormat == null)
			throw new NullPointerException("data and oldFormat cannot be null: " + data + " " + oldFormat);
		if (!oldFormat.isTypeValid(data.getType()))
			throw new IllegalArgumentException("data's type must be supported for the given oldFormat: " + data.getType() + " " + oldFormat);
		if (oldFormat.getBufferSize(oldWidth, oldHeight, oldDepth) != data.getCapacity())
			throw new IllegalArgumentException("data's capacity doesn't match expected size, based on old dimensions");
		
		// validate input parameters for the dst data
		if (newFormat == null || newType == null)
			throw new NullPointerException("newFormat and newType cannot be null: " + newFormat + " " + newType);
		if (!newFormat.isTypeValid(newType))
			throw new IllegalArgumentException("newType is not supported by newFormat: " + newType + " " + newFormat);
		
		int newCapacity = newFormat.getBufferSize(newWidth, newHeight, newDepth);
		if (newCapacity < 0)
			throw new IllegalArgumentException("new dimensions are invalid for newFormat: " + newWidth + "x" + newHeight + "x" + newDepth + " " + newFormat);
		
		// make a new BufferData if needed
		if (result == null || result.getCapacity() != newCapacity || result.getType() != newType) {
			result = new BufferData(newCapacity, newType);
		}
		// make sure its data array is not null
		if (result.getData() == null) {
			Object array = newArray(newCapacity, newType);
			result.setData(array);
		}
		
		if (data.getData() != null) {
			// actually convert the old image data
			Decoder decoder = getDecoder(data.getType(), oldFormat);
			if (decoder == null)
				throw new IllegalArgumentException("There is no registered Decoder supporting " + data.getType() + " and " + oldFormat);
			DataBlock src = new DataBlock(data, oldWidth, oldHeight, oldDepth, oldFormat);
			
			Encoder encoder = getEncoder(newType, newFormat);
			if (encoder == null)
				throw new IllegalArgumentException("There is no registered Encoder supporting " + newType + " and " + newFormat);
			DataBlock dst = new DataBlock(result, newWidth, newHeight, newDepth, newFormat);
			
			float uScale = 1f / newWidth;
			float vScale = 1f / newHeight;
			float wScale = 1f / newDepth;
			
			Color store = new Color();
			int x, y, z;
			float u, v, w;
			for (z = 0; z < newDepth; z++) {
				w = z * wScale;
				for (y = 0; y < newHeight; y++) {
					v = y * vScale;
					for (x = 0; x < newWidth; x++) {
						u = x * uScale;
						
						// read the color, based on normalized coords
						decoder.getColor(src, u, v, w, store);
						// store the color, based on actual pixel coords
						encoder.setColor(dst, x, y, z, store);
					}
				}
			}
			
			// we've finished converting the image layer
		}
		
		return result;
	}
	
	/** Register the given encoder, so that it can be used in subsequent 
	 * convert() calls.  If multiple registered encoders return true
	 * from canEncode(), the newest added encoder is used.
	 * 
	 * Does nothing if e is null.  If e has already been registered,
	 * then e becomes the "newest" with regards to resolving conflicts. */
	public static void registerEncoder(Encoder e) {
		synchronized(encoders) {
			if (e != null) {
				int index = encoders.indexOf(e);
				if (index >= 0)
					encoders.remove(index);
				encoders.add(e);
			}
		}
	}
	
	/** Remove the given encoder.  Does nothing if it's
	 * null or was never registered.  After a call to this method,
	 * that encoder instance will not be used in calls to convert(). */
	public static void unregisterEncoder(Encoder e) {
		synchronized(encoders) {
			if (e != null)
				encoders.remove(e);
		}
	}
	
	/** Register the given decoder, so that it can be used in subsequent 
	 * convert() calls.  If multiple registered decoders return true
	 * from canDecode(), the newest added decoder is used.
	 * 
	 * Does nothing if d is null.  If d has already been registered,
	 * then d becomes the "newest" with regards to resolving conflicts. */
	public static void registerDecoder(Decoder d) {
		synchronized(decoders) {
			if (d != null) {
				int index = decoders.indexOf(d);
				if (index >= 0)
					decoders.remove(index);
				decoders.add(d);
			}
		}
	}
	
	/** Remove the given decoder.  Does nothing if it's
	 * null or was never registered.  After a call to this method,
	 * that decoder instance will not be used in calls to convert(). */
	public static void unregisterDecoder(Decoder d) {
		synchronized(decoders) {
			if (d != null)
				decoders.remove(d);
		}
	}
	
	/* Return an Encoder that's been registered that can encode the
	 * given type and format.  Returns the newest Encoder that returns
	 * true in its canEncode() method. */
	private static Encoder getEncoder(DataType type, TextureFormat format) {
		Encoder e = null;
		synchronized(encoders) {
			for (int i = encoders.size() - 1; i >= 0; i--) {
				if (encoders.get(i).canEncode(type, format)) {
					e = encoders.get(i);
					break;
				}
			}
		}
		
		return e;
	}
	
	/* Return a Decoder that's been registered that can decode the
	 * given type and format.  Returns the newest Decoder that returns
	 * true in its canDecode() method. */
	private static Decoder getDecoder(DataType type, TextureFormat format) {
		Decoder d = null;
		synchronized(decoders) {
			for (int i = decoders.size() - 1; i >= 0; i--) {
				if (decoders.get(i).canDecode(type, format)) {
					d = decoders.get(i);
					break;
				}
			}
		}
		
		return d;
	}
	
	/* Create a new array with the given length and type. */
	private static Object newArray(int newCapacity, DataType newType) {
		Object array = null;
		switch(newType) {
		case BYTE: case UNSIGNED_BYTE:
			array = new byte[newCapacity];
			break;
		case INT: case UNSIGNED_INT:
			array = new int[newCapacity];
			break;
		case SHORT: case UNSIGNED_SHORT:
			array = new short[newCapacity];
			break;
		case FLOAT:
			array = new float[newCapacity];
			break;
		}
		
		return array;
	}
	
	static {
		// unpacked types
		registerEncoder(new UnpackedFloatConverter());
		registerDecoder(new UnpackedFloatConverter());
		registerEncoder(new UnpackedIntConverter());
		registerDecoder(new UnpackedIntConverter());
		registerEncoder(new UnpackedShortConverter());
		registerDecoder(new UnpackedShortConverter());
		registerEncoder(new UnpackedByteConverter());
		registerDecoder(new UnpackedByteConverter());
		
		// packed types
		registerEncoder(new PackedInt8888Converter());
		registerDecoder(new PackedInt8888Converter());
		registerEncoder(new PackedShort4444Converter());
		registerDecoder(new PackedShort4444Converter());
		registerEncoder(new PackedShort5551Converter());
		registerDecoder(new PackedShort5551Converter());
		registerEncoder(new PackedShort565Converter());
		registerDecoder(new PackedShort565Converter());
	}
}
