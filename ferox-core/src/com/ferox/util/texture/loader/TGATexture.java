package com.ferox.util.texture.loader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.ferox.resource.BufferData;
import com.ferox.resource.Texture2D;
import com.ferox.resource.TextureFormat;

/**
 * <p>
 * The TGATexture class can be used to load in TGA (or Targa) images as
 * Texture2D's.
 * </p>
 * <p>
 * It is recommended to use TextureLoader, since it will delegate to TGATexture
 * when needed.
 * </p>
 * <p>
 * This was based heavily off of the code in
 * com.sun.opengl.util.texture.spi.TGAImage.
 * </p>
 * 
 * @author Michael Ludwig
 */
@SuppressWarnings("unused")
public class TGATexture {
	private final Header header;
	private TextureFormat format;
	private BufferData data;

	/**
	 * <p>
	 * Load in a Targa image from the given stream as a Texture2D. This loader
	 * supports true color and color maps with pixel depths of 16, 24, and 32
	 * bits. The image will be flipped if necessary.
	 * </p>
	 * <p>
	 * It does not support runlength compressed images, black and white images,
	 * or images that go right to left. The tga footer present in newer tga
	 * files is ignored.
	 * </p>
	 * 
	 * @param stream The InputStream to read the tga texture from
	 * @return The read Texture2D
	 * @throws IOException if there was an IO error or if the tga file is
	 *             invalid or unsupported
	 */
	public static Texture2D readTexture(InputStream stream) throws IOException {
		if (stream == null)
			throw new IOException("Cannot read from a null stream");
		TGATexture res = new TGATexture(stream);
		return new Texture2D(new BufferData[] { res.data }, res.header.width, res.header.height, 
							 res.format, res.data.getType());
	}

	/**
	 * <p>
	 * Non-destructively read the header from the given stream and determine if
	 * it's a valid header. This checks the different byte offsets against
	 * expected values, and it is assumed that if everything checks out that it
	 * is a valid tga file (we need this assumption since the Targa format
	 * doesn't come with a magic number at the beginning).
	 * </p>
	 * <p>
	 * Returns false if it's invalid or an IOException occurred while checking.
	 * </p>
	 * 
	 * @param stream The InputStream to check format type
	 * @return Whether or not this stream is a tga file, doesn't check if it's
	 *         valid or supported
	 * @throws NullPointerException if stream is null
	 */
	public static boolean isTGATexture(InputStream stream) {
		if (stream == null)
			throw new NullPointerException("Cannot test a null stream");

		if (!(stream instanceof BufferedInputStream))
			stream = new BufferedInputStream(stream); // this way marking is supported
		Header header;
		try {
			stream.mark(18);
			header = new Header(stream, false);
		} catch (IOException ioe) {
			return false;
		} finally {
			// must reset the stream no matter what
			try {
				stream.reset();
			} catch (IOException e) {
			}
		}

		return validateHeader(header) == null;
	}

	private TGATexture(InputStream stream) throws IOException {
		header = new Header(stream, true);
		String msg = validateHeader(header);
		if (msg != null)
			throw new IOException("TGA header failed validation with: " + msg);
		msg = checkHeaderSupported(header);
		if (msg != null)
			throw new IOException("TGA image is unsupported, because: " + msg);
		// we should be supported now, so decode the image
		decodeImage(stream);
	}

	/* Set of possible image types in TGA file */
	private final static int TYPE_NO_IMAGE = 0; // no image data
	private final static int TYPE_COLORMAP = 1; // uncompressed color mapped
	// image
	private final static int TYPE_TRUECOLOR = 2; // uncompressed true color
	// image
	private final static int TYPE_BLACKWHITE = 3; // uncompressed black and
	// white image
	private final static int TYPE_RL_COLORMAP = 9; // compressed color mapped
	// image
	private final static int TYPE_RL_TRUECOLOR = 10; // compressed true color
	// image
	private final static int TYPE_RL_BLACKWHITE = 11; // compressed black and
	// white image
	private final static int TYPE_HDRL_COLORMAP = 32; // compressed color mapped
	// dating using
	// runlength encoding
	private final static int TYPE_HDRL_BLACKWHITE = 33; // compressed bw mapped
	// data with runlength
	// encoding

	/* Field image descriptor bitfield values definitions */
	private final static int ID_ATTRIBPERPIXEL = 0xF;
	private final static int ID_RIGHTTOLEFT = 0x10;
	private final static int ID_TOPTOBOTTOM = 0x20;
	private final static int ID_INTERLEAVE = 0xC0;

	/* Field image descriptor / interleave values */
	private final static int I_NOTINTERLEAVED = 0;
	private final static int I_TWOWAY = 1;
	private final static int I_FOURWAY = 2;

	/*
	 * This class reads in all of the TGA image header in addition it also reads
	 * in the imageID field as it is convenient to handle that here.
	 * 
	 * @author Robin Luiten
	 * 
	 * @author Michael Ludwig
	 */
	private static class Header {
		/* initial TGA image data fields */
		int idLength; // byte value
		int colorMapType; // byte value
		int imageType; // byte value

		/* TGA image colour map fields */
		int firstEntryIndex;
		int colorMapLength;
		byte colorMapEntrySize;

		/* TGA image specification fields */
		int xOrigin;
		int yOrigin;
		int width;
		int height;
		byte pixelDepth;
		byte imageDescriptor;

		String imageID;

		public Header(InputStream in, boolean readImageId) throws IOException {
			// initial header fields
			idLength = readUnsignedByte(in);
			colorMapType = readUnsignedByte(in);
			imageType = readUnsignedByte(in);

			// color map header fields
			firstEntryIndex = readUnsignedLEShort(in);
			colorMapLength = readUnsignedLEShort(in);
			colorMapEntrySize = (byte) readUnsignedByte(in);

			// TGA image specification fields
			xOrigin = readUnsignedLEShort(in);
			yOrigin = readUnsignedLEShort(in);
			width = readUnsignedLEShort(in);
			height = readUnsignedLEShort(in);
			pixelDepth = (byte) readUnsignedByte(in);
			imageDescriptor = (byte) readUnsignedByte(in);

			if (idLength > 0 && readImageId) {
				byte[] imageIDbuf = new byte[idLength];
				readAll(in, imageIDbuf);
				imageID = new String(imageIDbuf, "US-ASCII");
			} else
				imageID = "";
		}

		/* bitfields in imageDescriptor */
		public byte getAttribsPerPixel() {
			return (byte) (imageDescriptor & ID_ATTRIBPERPIXEL);
		}

		public boolean isRightToLeft() {
			return ((imageDescriptor & ID_RIGHTTOLEFT) != 0);
		}

		public boolean isTopToBottom() {
			return ((imageDescriptor & ID_TOPTOBOTTOM) != 0);
		}

		public byte getInterleavedType() {
			return (byte) ((imageDescriptor & ID_INTERLEAVE) >> 6);
		}

		public boolean isBlackAndWhite() {
			switch (imageType) {
			case TYPE_BLACKWHITE:
			case TYPE_HDRL_BLACKWHITE:
			case TYPE_RL_BLACKWHITE:
				return true;
			default:
				return false;
			}
		}

		public boolean hasColorMap() {
			switch (imageType) {
			case TYPE_COLORMAP:
			case TYPE_HDRL_COLORMAP:
			case TYPE_RL_COLORMAP:
				return true;
			default:
				return false;
			}
		}

		public boolean isCompressed() {
			switch (imageType) {
			case TYPE_HDRL_BLACKWHITE:
			case TYPE_HDRL_COLORMAP:
			case TYPE_RL_BLACKWHITE:
			case TYPE_RL_COLORMAP:
			case TYPE_RL_TRUECOLOR:
				return true;
			default:
				return false;
			}
		}
	}

	/*
	 * Class that stores the color map information for the image types that need
	 * color maps.
	 */
	private static class ColorMap {
		byte[] colorMapData;
		int startIndex;
		int elementByteCount;
		int numElements;

		/*
		 * Should only be constructed if the color map is the next bytes of the
		 * stream.
		 */
		public ColorMap(Header header, InputStream in) throws IOException {
			startIndex = header.firstEntryIndex;
			elementByteCount = header.colorMapEntrySize >> 8;
			numElements = header.colorMapLength;

			colorMapData = new byte[numElements * elementByteCount];
			readAll(in, colorMapData);
		}
	}

	/*
	 * Return error message if the header isn't supported. Returns null if it is
	 * supported. This assumes that the header has already been validated.
	 */
	private static String checkHeaderSupported(Header h) {
		if (h.isBlackAndWhite())
			return "Cannot load black and white image data";
		if (h.isRightToLeft())
			return "Cannot load image data that goes right-to-left";
		if (h.isCompressed())
			return "Cannot load images with runlength encoding";
		if (h.imageType == TYPE_NO_IMAGE)
			return "Cannot load an image with no image data";
		if (h.getInterleavedType() != 0)
			return "Interleaved image data is not supported";

		// we're supported
		return null;
	}

	/*
	 * Return the error message if header is invalid; null if it's valid.
	 */
	private static String validateHeader(Header h) {
		if (h.idLength < 0 || h.idLength > 255)
			return "Bad idLength value: " + h.idLength;
		if (h.colorMapType != 0 && h.colorMapType != 1)
			return "Bad color map type: " + h.colorMapType;

		switch (h.imageType) {
		case TYPE_BLACKWHITE:
		case TYPE_COLORMAP:
		case TYPE_HDRL_BLACKWHITE:
		case TYPE_HDRL_COLORMAP:
		case TYPE_NO_IMAGE:
		case TYPE_RL_BLACKWHITE:
		case TYPE_RL_COLORMAP:
		case TYPE_RL_TRUECOLOR:
		case TYPE_TRUECOLOR:
			break;
		default:
			return "Bad image type: " + h.imageType;
		}

		if (h.hasColorMap()) {
			if (h.firstEntryIndex < 0)
				return "Bad first entry index for a color map: " + h.firstEntryIndex;
			if (h.colorMapLength < 0)
				return "Bad number of color map entries: " + h.colorMapLength;
			if (!h.isBlackAndWhite())
				if (h.colorMapEntrySize != 16 && h.colorMapEntrySize != 24 && h.colorMapEntrySize != 32)
					return "Unsupported color map entry size: " + h.colorMapEntrySize;
			if (h.pixelDepth != 8 && h.pixelDepth != 16)
				return "Pixel depth doesn't have a valid value: " + h.pixelDepth;
			if (h.hasColorMap() && h.colorMapType == 0)
				return "Image type expects a color map, but one is not specified";
		} else if (!h.isBlackAndWhite())
			switch (h.pixelDepth) {
			case 16:
				if (h.getAttribsPerPixel() != 1)
					return "Bad attribs pixel count, must be 1 for 16 bit colors: " + h.getAttribsPerPixel();
				break;
			case 24:
				if (h.getAttribsPerPixel() != 0)
					return "Bad attribs pixel count, must be 0 for 24 bit colors: " + h.getAttribsPerPixel();
				break;
			case 32:
				if (h.getAttribsPerPixel() != 8)
					return "Bad attribs pixel count, must be 8 for 32 bit colors: " + h.getAttribsPerPixel();
				break;
			default:
				return "Unsupported pixel depth: " + h.pixelDepth;
			}

		// ignore the x and y origins of the image (pertain only to screen
		// location)
		if (h.width < 0)
			return "Bad width, must be positive: " + h.width;
		if (h.height < 0)
			return "Bad height, must be positive: " + h.height;

		// we should be valid
		return null;
	}

	/*
	 * Identifies the image type of the tga image data and loads it into a
	 * byte[] array.
	 */
	private void decodeImage(InputStream in) throws IOException {
		switch (header.imageType) {
		case TYPE_COLORMAP:
			// load the color map
			ColorMap cm = (header.hasColorMap() ? new ColorMap(header, in) : null);

			if (cm.elementByteCount == 2)
				decodeColorMap16(cm, in);
			else
				decodeColorMap24_32(cm, in);
			break;
		case TYPE_TRUECOLOR:
			if (header.pixelDepth == 16)
				decodeTrueColor16(in);
			else
				decodeTrueColor24_32(in);
			break;
		default:
			throw new IOException("Unsupported image type: " + header.imageType);
		}
	}

	/*
	 * Decode the image based on the given color map, assuming that the color
	 * map has a bit depth of 16.
	 */
	private void decodeColorMap16(ColorMap cm, InputStream dIn) throws IOException {
		int i; // input row index
		int y; // output row index
		int c; // column index
		short[] tmpData = new short[header.width * header.height];

		format = (cm.elementByteCount == 3 ? TextureFormat.BGR : TextureFormat.BGRA);
		if (header.pixelDepth == 8) {
			// indices use 2 bytes
			byte[] rawIndices = new byte[header.width << 1];
			int index;
			for (i = 0; i < header.height; i++) {
				readAll(dIn, rawIndices);
				y = (header.isTopToBottom() ? header.height - i - 1 : i);

				for (c = 0; c < header.width; c++) {
					index = bytesToLittleEndianShort(rawIndices, c << 1);
					tmpData[y * header.width + c] = (short) bytesToLittleEndianShort(cm.colorMapData, index);
				}
			}
		} else {
			// indices use only 1 byte
			byte[] rawIndices = new byte[header.width];
			int index;
			for (i = 0; i < header.height; i++) {
				readAll(dIn, rawIndices);
				y = (header.isTopToBottom() ? header.height - i - 1 : i);

				for (c = 0; c < header.width; c++) {
					index = rawIndices[c];
					tmpData[y * header.width + c] = (short) bytesToLittleEndianShort(cm.colorMapData, index);
				}
			}
		}

		data = new BufferData(tmpData, true);
	}

	/*
	 * Decode the image based on the given color map, assuming that the color
	 * map has bit depth of 24 or 32.
	 */
	private void decodeColorMap24_32(ColorMap cm, InputStream dIn) throws IOException {
		int i; // input row index
		int y; // output row index
		int c; // column index
		int rawWidth = header.width * cm.elementByteCount; // length of expanded
		// color row
		byte[] tmpData = new byte[rawWidth * header.height];

		format = (cm.elementByteCount == 3 ? TextureFormat.BGR : TextureFormat.BGRA);
		if (header.pixelDepth == 8) {
			// indices use 2 bytes
			byte[] rawIndices = new byte[header.width << 1];
			int index;
			for (i = 0; i < header.height; i++) {
				readAll(dIn, rawIndices);
				y = (header.isTopToBottom() ? header.height - i - 1 : i);

				for (c = 0; c < header.width; c++) {
					index = bytesToLittleEndianShort(rawIndices, c << 1);
					System.arraycopy(cm.colorMapData, index, tmpData, 
									 y * rawWidth + c * cm.elementByteCount, cm.elementByteCount);
				}
			}
		} else {
			// indices use only 1 byte
			byte[] rawIndices = new byte[header.width];
			for (i = 0; i < header.height; i++) {
				readAll(dIn, rawIndices);
				y = (header.isTopToBottom() ? header.height - i - 1 : i);

				for (c = 0; c < header.width; c++)
					System.arraycopy(cm.colorMapData, rawIndices[c], tmpData, 
									 y * rawWidth + c * cm.elementByteCount, cm.elementByteCount);
			}
		}

		data = new BufferData(tmpData, true);
	}

	/* This assumes that the body is a 16 bit ARGB_1555 image. */
	private void decodeTrueColor16(InputStream dIn) throws IOException {
		int i; // input row index
		int y; // output row index
		int c; // column index

		byte[] rawBuf = new byte[header.width << 1];
		short[] swapRow = new short[header.width];
		short[] tmpData = new short[header.width * header.height];

		format = TextureFormat.ARGB_1555;
		for (i = 0; i < header.height; i++) {
			readAll(dIn, rawBuf);
			for (c = 0; c < header.width; c++)
				swapRow[c] = (short) bytesToLittleEndianShort(rawBuf, c << 1);

			y = (header.isTopToBottom() ? header.height - i - 1 : i);
			System.arraycopy(swapRow, 0, tmpData, y * header.width, swapRow.length);
		}

		data = new BufferData(tmpData, true);
	}

	/*
	 * This assumes that the body is for a 24 bit or 32 bit for a BGR and BGRA
	 * image respectively. (really a ARGB image, but its in LE order, but just
	 * using BGRA is faster) FIXME: does this assumption work for 24 bit images?
	 */
	private void decodeTrueColor24_32(InputStream dIn) throws IOException {
		int i; // input row index
		int y; // output row index
		int rawWidth = header.width * (header.pixelDepth >> 3);

		byte[] rawBuf = new byte[rawWidth];
		byte[] tmpData = new byte[rawWidth * header.height];

		format = (header.pixelDepth == 24 ? TextureFormat.BGR : TextureFormat.BGRA);
		for (i = 0; i < header.height; i++) {
			readAll(dIn, rawBuf);
			y = (header.isTopToBottom() ? header.height - i - 1 : i);
			System.arraycopy(rawBuf, 0, tmpData, y * rawWidth, rawBuf.length);
		}

		data = new BufferData(tmpData, true);
	}

	// read bytes from the given stream until the array is full
	// fails if the end-of-stream happens before the array is full
	private static void readAll(InputStream in, byte[] array) throws IOException {
		int remaining = array.length;
		int offset = 0;
		int read = 0;
		while (remaining > 0) {
			read = in.read(array, offset, remaining);
			if (read < 0)
				throw new IOException("Unexpected end of stream");
			offset += read;
			remaining -= read;
		}
	}

	private static int bytesToLittleEndianShort(byte[] b, int offset) {
		return ((b[offset + 0] & 0xff) | 
			   ((b[offset + 1] & 0xff) << 8));
	}

	// read an short represented in little endian from the given input stream
	private static int readUnsignedLEShort(InputStream in) throws IOException {
		byte[] b = new byte[2];
		readAll(in, b);
		return ((b[0] & 0xff) | 
			   ((b[1] & 0xff) << 8));
	}

	private static int readUnsignedByte(InputStream in) throws IOException {
		int ch = in.read();
		if (ch < 0)
			throw new IOException("Unexpected end of stream");
		return ch;
	}
}
