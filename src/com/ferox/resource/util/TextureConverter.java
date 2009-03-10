package com.ferox.resource.util;

import java.util.ArrayList;
import java.util.List;

import com.ferox.math.Color;
import com.ferox.resource.BufferData;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.BufferData.DataType;

/** TextureConverter provides utilities to register decoders and
 * encoders of texture data, as well as provide a utility methods
 * that will convert formats, scale dimensions, and generate mipmaps
 * for a texture image.
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
		// FIXME: implement this and the encoder/decoders
		return null;
	}
	
	/** Register the given encoder, so that it can be used in subsequent 
	 * convert() calls.  If multiple registered encoders return true
	 * from canEncode(), the newest added encoder is used.
	 * 
	 * Does nothing if e is null.  If e has already been registered,
	 * then e becomes the "newest" with regards to resolving conflicts. */
	public static void registerEncoder(Encoder e) {
		if (e != null) {
			int index = encoders.indexOf(e);
			if (index >= 0)
				encoders.remove(index);
			encoders.add(e);
		}
	}
	
	/** Remove the given encoder.  Does nothing if it's
	 * null or was never registered.  After a call to this method,
	 * that encoder instance will not be used in calls to convert(). */
	public static void unregisterEncoder(Encoder e) {
		if (e != null)
			encoders.remove(e);
	}
	
	/** Register the given decoder, so that it can be used in subsequent 
	 * convert() calls.  If multiple registered decoders return true
	 * from canDecode(), the newest added decoder is used.
	 * 
	 * Does nothing if d is null.  If d has already been registered,
	 * then d becomes the "newest" with regards to resolving conflicts. */
	public static void registerDecoder(Decoder d) {
		if (d != null) {
			int index = decoders.indexOf(d);
			if (index >= 0)
				decoders.remove(index);
			decoders.add(d);
		}
	}
	
	/** Remove the given decoder.  Does nothing if it's
	 * null or was never registered.  After a call to this method,
	 * that decoder instance will not be used in calls to convert(). */
	public static void unregisterDecoder(Decoder d) {
		if (d != null)
			decoders.remove(d);
	}
	
	static {
		// FIXME: register default encoders and decoders for the supported texture formats
	}
}
