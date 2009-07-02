package com.ferox.util.texture.converter;

import java.util.ArrayList;
import java.util.List;

import com.ferox.math.Color4f;
import com.ferox.resource.BufferData;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.BufferData.DataType;

/**
 * <p>
 * TextureConverter provides utilities to register decoders and encoders of
 * texture data, as well as provide a utility methods that will convert formats
 * and scale dimensions.
 * </p>
 * <p>
 * There are default encoders and decoders supported all TextureFormats (with
 * valid types) except for the DXT_x formats, that have already been registered.
 * </p>
 * <p>
 * This utility is not intended to be an efficient imaging processor, but is
 * instead meant to be flexible, allowing for many formats to be converted and
 * scaled.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class TextureConverter {
	private static List<Encoder> encoders = new ArrayList<Encoder>();
	private static List<Decoder> decoders = new ArrayList<Decoder>();

	/**
	 * <p>
	 * This method will re-scale and change the data type and format of a single
	 * mipmap layer of a texture image. This image can be 2d or 3d (if it's 2d,
	 * the depth should be 1).
	 * </p>
	 * <p>
	 * If the result BufferData is null, doesn't match newType, or doesn't have
	 * the correct size, a new BufferData is used. Otherwise, its data array is
	 * used to store the converted image (which may have to be allocated if it
	 * is null), and result will be returned.
	 * </p>
	 * <p>
	 * If the backing array behind result is null, then a new one is created. If
	 * a data's backing array is null, an empty BufferData is created with an
	 * array sized appropriately for the requested type, format and dimensions.
	 * </p>
	 * <p>
	 * Returns the BufferData holding the converted image.
	 * </p>
	 * 
	 * @param data The original data that will be converted, can't be null
	 * @param oldFormat The TextureFormat of data, can't be null
	 * @param oldWidth The width of the image held by data
	 * @param oldHeight The height of the image held by data
	 * @param oldDepth The depth of the image held by data (1 if there's no 3rd
	 *            dimension)
	 * @param result The BufferData result to hold converted image, if it's not
	 *            null, it must be compatible with newFormat and new dimensions
	 * @param newFormat The TextureFormat for the converted image
	 * @param newType The DataType for the converted image
	 * @param newWidth The width dimension of the new image
	 * @param newHeight The height dimension of the new image
	 * @param newDepth The depth dimension of the new image
	 * @return The converted image, either in result or a new BufferData if
	 *         result was null or didn't match the requirements for the
	 *         converted image
	 * @throws NullPointerException if data, oldFormat, newFormat, or newType
	 *             are null
	 * @throws IllegalArgumentException if the oldFormat and old dimensions
	 *             don't match with data, or if newFormat and newType and the
	 *             new dimensions are invalid
	 * @throws UnsupportedOperationException if there are no registered Encoders
	 *             or Decoders to handle the conversion
	 */
	public static BufferData convert(BufferData data, TextureFormat oldFormat, 
							    	 int oldWidth, int oldHeight, int oldDepth, 
							    	 BufferData result, TextureFormat newFormat, 
							    	 DataType newType, int newWidth, int newHeight, int newDepth) {
		// validate input parameters that involve the source data
		if (data == null || oldFormat == null)
			throw new NullPointerException("data and oldFormat cannot be null: " + data + " " + oldFormat);
		if (!oldFormat.isTypeValid(data.getType()))
			throw new IllegalArgumentException("data's type must be supported for the given oldFormat: " 
											   + data.getType() + " " + oldFormat);
		if (oldFormat.getBufferSize(oldWidth, oldHeight, oldDepth) != data.getCapacity())
			throw new IllegalArgumentException("data's capacity doesn't match expected size, based on old dimensions");

		// validate input parameters for the dst data
		if (newFormat == null || newType == null)
			throw new NullPointerException("newFormat and newType cannot be null: " + newFormat + " " + newType);
		if (!newFormat.isTypeValid(newType))
			throw new IllegalArgumentException("newType is not supported by newFormat: " + newType + " " + newFormat);

		int newCapacity = newFormat.getBufferSize(newWidth, newHeight, newDepth);
		if (newCapacity < 0)
			throw new IllegalArgumentException("new dimensions are invalid for newFormat: " + newWidth + "x" 
											   + newHeight + "x" + newDepth + " " + newFormat);

		// make a new BufferData if needed
		if (result == null || result.getCapacity() != newCapacity || result.getType() != newType)
			result = new BufferData(newCapacity, newType);
		// make sure its data array is not null
		if (result.getData() == null) {
			Object array = newArray(newCapacity, newType);
			result.setData(array);
		}

		if (data.getData() != null) {
			// actually convert the old image data
			Decoder decoder = getDecoder(data.getType(), oldFormat);
			if (decoder == null)
				throw new UnsupportedOperationException("There is no registered Decoder supporting " 
														+ data.getType() + " and " + oldFormat);
			DataBlock src = new DataBlock(data, oldWidth, oldHeight, oldDepth, oldFormat);

			Encoder encoder = getEncoder(newType, newFormat);
			if (encoder == null)
				throw new UnsupportedOperationException("There is no registered Encoder supporting " 
														+ newType + " and " + newFormat);
			DataBlock dst = new DataBlock(result, newWidth, newHeight, newDepth, newFormat);

			float uScale = 1f / newWidth;
			float vScale = 1f / newHeight;
			float wScale = 1f / newDepth;

			Color4f store = new Color4f();
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

	/**
	 * <p>
	 * Register the given encoder, so that it can be used in subsequent
	 * convert() calls. If multiple registered encoders return true from
	 * canEncode(), the newest added encoder is used.
	 * </p>
	 * <p>
	 * Does nothing if e is null. If e has already been registered, then e
	 * becomes the "newest" with regards to resolving conflicts.
	 * </p>
	 * 
	 * @param e The Encoder to register for use
	 */
	public static void registerEncoder(Encoder e) {
		synchronized (encoders) {
			if (e != null) {
				int index = encoders.indexOf(e);
				if (index >= 0)
					encoders.remove(index);
				encoders.add(e);
			}
		}
	}

	/**
	 * Remove the given encoder. Does nothing if it's null or was never
	 * registered. After a call to this method, that encoder instance will not
	 * be used in calls to convert().
	 * 
	 * @param e The Encoder to no longer use
	 */
	public static void unregisterEncoder(Encoder e) {
		synchronized (encoders) {
			if (e != null)
				encoders.remove(e);
		}
	}

	/**
	 * <p>
	 * Register the given decoder, so that it can be used in subsequent
	 * convert() calls. If multiple registered decoders return true from
	 * canDecode(), the newest added decoder is used.
	 * </p>
	 * <p>
	 * Does nothing if d is null. If d has already been registered, then d
	 * becomes the "newest" with regards to resolving conflicts.
	 * </p>
	 * 
	 * @param d The Decoder to register
	 */
	public static void registerDecoder(Decoder d) {
		synchronized (decoders) {
			if (d != null) {
				int index = decoders.indexOf(d);
				if (index >= 0)
					decoders.remove(index);
				decoders.add(d);
			}
		}
	}

	/**
	 * Remove the given decoder. Does nothing if it's null or was never
	 * registered. After a call to this method, that decoder instance will not
	 * be used in calls to convert().
	 * 
	 * @param d The Decoder to no longer use
	 */
	public static void unregisterDecoder(Decoder d) {
		synchronized (decoders) {
			if (d != null)
				decoders.remove(d);
		}
	}

	/*
	 * Return an Encoder that's been registered that can encode the given type
	 * and format. Returns the newest Encoder that returns true in its
	 * canEncode() method.
	 */
	private static Encoder getEncoder(DataType type, TextureFormat format) {
		Encoder e = null;
		synchronized (encoders) {
			for (int i = encoders.size() - 1; i >= 0; i--)
				if (encoders.get(i).canEncode(type, format)) {
					e = encoders.get(i);
					break;
				}
		}

		return e;
	}

	/*
	 * Return a Decoder that's been registered that can decode the given type
	 * and format. Returns the newest Decoder that returns true in its
	 * canDecode() method.
	 */
	private static Decoder getDecoder(DataType type, TextureFormat format) {
		Decoder d = null;
		synchronized (decoders) {
			for (int i = decoders.size() - 1; i >= 0; i--)
				if (decoders.get(i).canDecode(type, format)) {
					d = decoders.get(i);
					break;
				}
		}

		return d;
	}

	/* Create a new array with the given length and type. */
	private static Object newArray(int newCapacity, DataType newType) {
		Object array = null;
		switch (newType) {
		case BYTE:
		case UNSIGNED_BYTE:
			array = new byte[newCapacity];
			break;
		case INT:
		case UNSIGNED_INT:
			array = new int[newCapacity];
			break;
		case SHORT:
		case UNSIGNED_SHORT:
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
