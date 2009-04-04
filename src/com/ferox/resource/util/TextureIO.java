package com.ferox.resource.util;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import com.ferox.resource.BufferData;
import com.ferox.resource.Texture1D;
import com.ferox.resource.Texture2D;
import com.ferox.resource.TextureCubeMap;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureRectangle;
import com.ferox.resource.BufferData.DataType;

/** TextureIO provides functionality to load image files
 * as TextureImage objects.  It provides some utilities to generate
 * a TextureImage from a BufferedImage.
 * 
 * TextureIO supports the DDS texture format (via DDSTexture) and
 * any file format supported by javax.imageio.ImageIO.
 * 
 * @author Michael Ludwig
 *
 */
public class TextureIO {
	/** Read the texture from the given file, functions identically
	 * to readTexture(stream). */
	public static TextureImage readTexture(File file) throws IOException {
		if (file == null)
			throw new IOException("Cannot load a texture image from a null file");
		
		InputStream stream = new FileInputStream(file);
		TextureImage image;
		try {
			image = readTexture(stream);
		} finally {
			stream.close();
		}
		
		return image;
	}
	
	/** Read the texture from the given URL, functions identically
	 * to readTexture(stream). */
	public static TextureImage readTexture(URL url) throws IOException {
		if (url == null)
			throw new IOException("Cannot read from a null URL");
		InputStream urlStream = url.openStream();
		TextureImage image;
		try {
			image = readTexture(url.openStream());
		} finally {
			urlStream.close();
		}
		
		return image;
	}
	
	/** Read a texture from the given stream.  This assumes that the
	 * texture begins with the next bytes read from the stream, and that
	 * the stream is already opened. Throws an exception if the stream 
	 * is null, or if its not a recognizable image.
	 * 
	 * For standard images (e.g. jpg, png, gif, etc.) Texture2D is used.
	 * If a TextureRectangle is desired, use convertToRectangle(). 
	 * 
	 * This method does not close the stream, in case it's to be used
	 * later on. */
	public static TextureImage readTexture(InputStream stream) throws IOException {
		try {
			// make sure we're buffered
			if (!(stream instanceof BufferedInputStream))
				stream = new BufferedInputStream(stream);
			
			// load the file
			TextureImage t;
			
			if (DDSTexture.isDDSTexture(stream)) // we're a DDS texture
				t = DDSTexture.readTexture(stream);
			else if (TGATexture.isTGATexture(stream)) // we should be a TGA texture
				t = TGATexture.readTexture(stream);
			else {
				BufferedImage i = ImageIO.read(stream);
				if (i == null)
					throw new IOException("Unsupported file format");
				if (i.getHeight() == 1)
					t = createTexture1D(i);
				else
					t = createTexture2D(i);
			}
			// return the result
			return t;
		} catch (Exception io) {
			throw new IOException(io);
		}
	}
	
	/** Utility function to convert a Textur2D into a TextureRectangle.  This can
	 * be useful because TextureIO always uses Texture2D's, but it may be necessary
	 * or desired to use a TextureRectangle.  
	 * 
	 * All this does is grab the first mipmap layer, dimensions and texture parameters
	 * and set them on a new instance.  No loading is performed.
	 * 
	 * Throws an exception if texture is null. */
	public static TextureRectangle convertToRectangle(Texture2D texture) throws NullPointerException {
		TextureRectangle tr = null;
		if (texture != null) {
			tr = new TextureRectangle(texture.getData(0), texture.getWidth(0), texture.getHeight(0), texture.getFormat(), texture.getType());
			
			tr.setDepthCompareEnabled(texture.isDepthCompareEnabled());
			tr.setDepthCompareTest(texture.getDepthCompareTest());
			tr.setDepthMode(texture.getDepthMode());
			tr.setFilter(texture.getFilter());
			tr.setWrapSTR(texture.getWrapS(), texture.getWrapT(), texture.getWrapT());
		}
		return tr;
	}
	
	/** Utility function to convert the given BufferedImage into a Texture1D.
	 * This can be a slower operation because it has to redraw the buffered image
	 * to make sure it has an appropriate raster and color model. 
	 * 
	 * Fails if the image doesn't have a height of 1 or if image is null. */
	public static Texture1D createTexture1D(BufferedImage image) throws NullPointerException, IllegalArgumentException {
		if (image == null)
			throw new NullPointerException("Cannot convert a null BufferedImage");
		if (image.getHeight() != 1)
			throw new IllegalArgumentException("A BufferedImage can only be converted to a Texture1D with height == 1, not: " + image.getHeight());
		
		RasterImage im = new RasterImage(image.getType(), image.getWidth(), 1);
		
		BufferedImage formatted = new BufferedImage(im.colorModel, im.data, false, null);
		Graphics2D g2 = formatted.createGraphics();
		g2.drawImage(formatted, 0, 0, null);
		g2.dispose();
		
		BufferData data;
		if (im.type == DataType.UNSIGNED_BYTE) {
			byte[] rd = ((DataBufferByte)formatted.getRaster().getDataBuffer()).getData();
			data = new BufferData(rd, true);
		} else {
			short[] rd = ((DataBufferUShort)formatted.getRaster().getDataBuffer()).getData();
			data = new BufferData(rd, true);
		}
		
		return new Texture1D(new BufferData[] {data}, image.getWidth(), im.format, im.type);
	}

	/** Utility function to convert the given BufferedImage into a Texture2D.
	 * This can be a slower operation because it has to redraw the buffered image
	 * to make sure it has an appropriate raster and color model.  It also flips
	 * the converted image to match the coordinate system of TextureImage (origin at the
	 * lower left). 
	 * 
	 * Fails if image is null. */
	public static Texture2D createTexture2D(BufferedImage image) throws NullPointerException {
		if (image == null)
			throw new NullPointerException("Cannot convert a null BufferedImage");
		
		RasterImage im = new RasterImage(image.getType(), image.getWidth(), image.getHeight());
		
		BufferedImage formatted = new BufferedImage(im.colorModel, im.data, false, null);
		Graphics2D g2 = formatted.createGraphics();
		AffineTransform t = AffineTransform.getScaleInstance(1, -1);
		t.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
		g2.drawImage(image, t, null);
		g2.dispose();
		
		BufferData data;
		if (im.type == DataType.UNSIGNED_BYTE) {
			byte[] rd = ((DataBufferByte)formatted.getRaster().getDataBuffer()).getData();
			data = new BufferData(rd, true);
		} else {
			short[] rd = ((DataBufferUShort)formatted.getRaster().getDataBuffer()).getData();
			data = new BufferData(rd, true);
		}
		
		return new Texture2D(new BufferData[] {data}, image.getWidth(), image.getHeight(), im.format, im.type);
	}
	
	/** Like createTexture2D(), except it identifies 6 faces from the cube map like so:
	 * *---*---*---*---*
	 * |   |NZ |   |   |
	 * *---*---*---*---*
	 * |NX |NY |PX |PY |   e.g. an unwrapped cube
	 * *---*---*---*---*
	 * |   |PZ |   |   |
	 * *---*---*---*---*
	 * 
	 * Because of this, the specified image must have an aspect ration of 4/3 for the creation
	 * to work.  Other than this, the creation process functions like createTexture2D().
	 * 
	 * Throws an exception if the image is null, or if width/4 != height/3. */
	public static TextureCubeMap createTextureCubeMap(BufferedImage image) throws NullPointerException, IllegalArgumentException {
		if (image == null)
			throw new NullPointerException("Cannot create a cube map from a null BufferedImage");
		
		int side = image.getWidth() / 4;
		if (side * 4 != image.getWidth() || side * 3 != image.getHeight())
			throw new IllegalArgumentException("Base image doesn't have the 4x3 aspect ration necessary for a cube map");
		
		RasterImage im = new RasterImage(image.getType(), side, side);
		BufferedImage formatted = new BufferedImage(im.colorModel, im.data, false, null);

		BufferData[] px = createCubeMapFace(image, formatted, TextureCubeMap.PX, im.type);
		BufferData[] py = createCubeMapFace(image, formatted, TextureCubeMap.PY, im.type);
		BufferData[] pz = createCubeMapFace(image, formatted, TextureCubeMap.PZ, im.type);
		BufferData[] nx = createCubeMapFace(image, formatted, TextureCubeMap.NX, im.type);
		BufferData[] ny = createCubeMapFace(image, formatted, TextureCubeMap.NY, im.type);
		BufferData[] nz = createCubeMapFace(image, formatted, TextureCubeMap.NZ, im.type);
		
		return new TextureCubeMap(px, py, pz, nx, ny, nz, side, im.format, im.type);
	}
	
	/* Internal method that redraws fullImage into faceStore, and then copies out the raster data
	 * into a new BufferData.  A copy is made since it is assumed that faceStore is re-used for
	 * each cube face. */
	private static BufferData[] createCubeMapFace(BufferedImage fullImage, BufferedImage faceStore, int face, DataType texType) {
		Graphics2D g2 = faceStore.createGraphics();
		AffineTransform t = AffineTransform.getScaleInstance(1, 1);
		
		// setup up a transform that adjusts the fullImage into the correct space for a single face
		switch(face) {
		case TextureCubeMap.PX:
			t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getScaleInstance(-1, 1));
			t.concatenate(AffineTransform.getRotateInstance(-Math.PI / 2));
			t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getTranslateInstance(-2 * faceStore.getWidth(), -1 * faceStore.getHeight()));
			break;
		case TextureCubeMap.PY:
			t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getScaleInstance(-1, 1));
			t.concatenate(AffineTransform.getRotateInstance(0));
			t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getTranslateInstance(-3 * faceStore.getWidth(), -1 * faceStore.getHeight()));
			break;
		case TextureCubeMap.PZ:
			t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getScaleInstance(-1, 1));
			t.concatenate(AffineTransform.getRotateInstance(-Math.PI));
			t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getTranslateInstance(-1 * faceStore.getWidth(), -2 * faceStore.getHeight()));
			break;
		case TextureCubeMap.NX:
			t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getScaleInstance(-1, 1));
			t.concatenate(AffineTransform.getRotateInstance(Math.PI / 2));
			t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getTranslateInstance(0 * faceStore.getWidth(), -1 * faceStore.getHeight()));	
		case TextureCubeMap.NY:
			t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getScaleInstance(1, -1));
			t.concatenate(AffineTransform.getRotateInstance(0));
			t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getTranslateInstance(-1 * faceStore.getWidth(), -1 * faceStore.getHeight()));
			break;
		case TextureCubeMap.NZ:
			t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getScaleInstance(1, 1));
			t.concatenate(AffineTransform.getRotateInstance(0));
			t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getTranslateInstance(-1 * faceStore.getWidth(), -0 * faceStore.getHeight()));
			break;
		}
		
		g2.drawImage(fullImage, t, null);
		g2.dispose();
		
		BufferData data = null;
		if (texType == DataType.UNSIGNED_BYTE) {
			byte[] rd = ((DataBufferByte)faceStore.getRaster().getDataBuffer()).getData();
			byte[] copy = new byte[rd.length];
			System.arraycopy(rd, 0, copy, 0, rd.length);
			data = new BufferData(copy, true);
		} else {
			short[] rd = ((DataBufferUShort)faceStore.getRaster().getDataBuffer()).getData();
			short[] copy = new short[rd.length];
			System.arraycopy(rd, 0, copy, 0, rd.length);
			data = new BufferData(copy, true);
		}
		
		return new BufferData[] {data};
	}
	
	/* Utility class used to identify a format and type based on the BufferedImage.
	 * It also creates a color model and raster suitable for creating new buffered images. */
	private static class RasterImage {
		// Ferox specific variables
		TextureFormat format;
		DataType type;
		
		// BufferImage specific variables
		ColorModel colorModel;
		WritableRaster data;
		
		public RasterImage(int imageType, int width, int height) {
			switch(imageType) {
			case BufferedImage.TYPE_3BYTE_BGR: case BufferedImage.TYPE_INT_BGR: case BufferedImage.TYPE_INT_RGB:
			case BufferedImage.TYPE_USHORT_555_RGB: case BufferedImage.TYPE_USHORT_565_RGB:
				this.colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8, 0},
														  false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
				this.data = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 3, null);

				this.type = DataType.UNSIGNED_BYTE;
				this.format = TextureFormat.RGB;
				break;
			case BufferedImage.TYPE_USHORT_GRAY:
				this.colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {16},
														  false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
				this.data = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, width, height, 1, null);
				
				this.type = DataType.UNSIGNED_SHORT;
				this.format = TextureFormat.LUMINANCE;
				break;
			case BufferedImage.TYPE_BYTE_GRAY:
				this.colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {8},
														  false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
				this.data = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 1, null);
					
				this.type = DataType.UNSIGNED_BYTE;
				this.format = TextureFormat.LUMINANCE;
				break;
			default:
				this.colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8, 8},
														  true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
				this.data = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 4, null);
				
				this.type = DataType.UNSIGNED_BYTE;
				this.format = TextureFormat.RGBA;
				break;
			}
		}
	}
}
