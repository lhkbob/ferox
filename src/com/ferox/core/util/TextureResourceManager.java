package com.ferox.core.util;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.ferox.core.states.atoms.Texture2D;
import com.ferox.core.states.atoms.Texture3D;
import com.ferox.core.states.atoms.TextureCubeMap;
import com.ferox.core.states.atoms.TextureData;
import com.ferox.core.states.atoms.TextureData.MagFilter;
import com.ferox.core.states.atoms.TextureData.MinFilter;
import com.ferox.core.states.atoms.TextureData.TextureFormat;
import com.ferox.core.states.atoms.TextureData.TextureType;
//FIXME: make tga loader
public class TextureResourceManager {
	public static final int DXT1 = 1;
	public static final int DXT3 = 3;
	public static final int DXT5 = 5;
	public static final int NO_DXT = 0;
	
	private static HashMap<String, TextureData> pathToTex = new HashMap<String, TextureData>();
	private static HashMap<TextureData, String> texToPath = new HashMap<TextureData, String>();
	
	public static boolean isTextureCached(TextureData t) {
		return texToPath.containsKey(t);
	}
	
	public static boolean isTextureCached(File file) {
		try {
			return pathToTex.containsKey(file.getCanonicalPath());
		} catch (IOException e) {
			return false;
		}
	}
	
	public static TextureData getCachedTexture(File file) {
		try {
			return getCachedTexture(file.getCanonicalPath());
		} catch (IOException e) {
			return null;
		}
	}
	
	public static TextureData getCachedTexture(String file) {
		return pathToTex.get(file);
	}
	
	public static String getCachedTextureLocation(TextureData tex) {
		return texToPath.get(tex);
	}
	
	public static Texture2D readTexture2D(File file) throws IOException {
		return readTexture2D(file, true);
	}
	
	public static Texture2D readTexture2D(File file, boolean useCache) throws IOException {
		return readTexture2D(file, useCache, MinFilter.LINEAR, MagFilter.LINEAR);
	}
	
	public static Texture2D readTexture2D(File file, boolean useCache, MinFilter min, MagFilter mag) throws IOException {
		return readTexture2D(file, useCache, min, mag, NO_DXT, false);
	}

	public static Texture2D readTexture2D(File file, boolean useCache, MinFilter min, MagFilter mag, int dxt, boolean buildMips) throws IOException {
		try {
			if (useCache) {
				Texture2D t = (Texture2D)getCachedTexture(file);
				if (t != null)
					return t;
			}
			BufferedImage i = ImageIO.read(file);
			Texture2D t;
			if (i == null)
				t = (Texture2D)loadTargaDDSMaybe(file, min, mag, buildMips);
			else 
				t = createTexture2DFromImage(i, dxt, buildMips, min, mag);
			if (useCache) {
				String key = file.getCanonicalPath();
				pathToTex.put(key, t);
				texToPath.put(t, key);
			}
			return t;
		} catch (IOException ioe) {
			throw ioe;
		} catch (ClassCastException ce) {
			throw new IOException("Attempt to load " + file.getPath() + ", when file is for a different texture data type");
		}
	}
		
	public static TextureCubeMap readTextureCubeMap(File file) throws IOException {
		return readTextureCubeMap(file, true);
	}
	
	public static TextureCubeMap readTextureCubeMap(File file, boolean useCache) throws IOException {
		return readTextureCubeMap(file, useCache, MinFilter.LINEAR, MagFilter.LINEAR);
	}
	
	public static TextureCubeMap readTextureCubeMap(File file, boolean useCache, MinFilter min, MagFilter mag) throws IOException {
		return readTextureCubeMap(file, useCache, min, mag, NO_DXT, false);
	}
	
	public static TextureCubeMap readTextureCubeMap(File file, boolean useCache, MinFilter min, MagFilter mag, int dxt, boolean buildMips) throws IOException {
		try {
			if (useCache) {
				TextureCubeMap t = (TextureCubeMap)getCachedTexture(file);
				if (t != null)
					return t;
			}
			BufferedImage i = ImageIO.read(file);
			TextureCubeMap t;
			if (i == null)
				t = (TextureCubeMap)loadTargaDDSMaybe(file, min, mag, buildMips);
			else 
				t = createTextureCubeMapFromImage(i, dxt, buildMips, min, mag);
			if (useCache) {
				String key = file.getCanonicalPath();
				pathToTex.put(key, t);
				texToPath.put(t, key);
			}
			return t;
		} catch (IOException ioe) {
			throw ioe;
		} catch (ClassCastException ce) {
			throw new IOException("Attempt to load " + file.getPath() + ", when file is for a different texture data type");
		}
	}
	
	public static Texture3D readTexture3D(File file) throws IOException {
		return readTexture3D(file, true);
	}
	
	public static Texture3D readTexture3D(File file, boolean useCache) throws IOException {
		return readTexture3D(file, useCache, MinFilter.LINEAR, MagFilter.LINEAR);
	}
	
	public static Texture3D readTexture3D(File file, boolean useCache, MinFilter min, MagFilter mag) throws IOException {
		return readTexture3D(file, useCache, min, mag, false);
	}
	
	public static Texture3D readTexture3D(File file, boolean useCache, MinFilter min, MagFilter mag, boolean buildMips) throws IOException {
		try {
			if (useCache) {
				Texture3D t = (Texture3D)getCachedTexture(file);
				if (t != null)
					return t;
			}
			Texture3D t = (Texture3D)loadTargaDDSMaybe(file, min, mag, buildMips);
			if (useCache) {
				String key = file.getCanonicalPath();
				pathToTex.put(key, t);
				texToPath.put(t, key);
			}
			return t;
		} catch (IOException ioe) {
			throw ioe;
		} catch (ClassCastException ce) {
			throw new IOException("Attempt to load " + file.getPath() + ", when file is for a different texture data type");
		}
	}
	
	private static TextureData loadTargaDDSMaybe(File file, MinFilter min, MagFilter mag, boolean mip) throws IOException {
		String name = file.getName();
		if (name.toLowerCase().endsWith("dds")) {
			return DDSReader.readDDSTexture(file, min, mag, mip);
		} else // Assume tga?
			return null;
	}

	public static Texture2D createTexture2DFromImage(BufferedImage image, int dxt, boolean buildMips, MinFilter min, MagFilter mag) {
		ColorModel cm = null;
		WritableRaster sm = null;
		
		TextureFormat texFormat;
		TextureType texType = TextureType.UNSIGNED_BYTE;
	
		switch(image.getType()) {
		case BufferedImage.TYPE_3BYTE_BGR: case BufferedImage.TYPE_INT_BGR: case BufferedImage.TYPE_INT_RGB:
		case BufferedImage.TYPE_USHORT_555_RGB: case BufferedImage.TYPE_USHORT_565_RGB:
			cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8, 0},
					false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
			sm = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, image.getWidth(), image.getHeight(), 3, null);
			texFormat = (dxt != NO_DXT ? TextureFormat.RGB_DXT1 : TextureFormat.RGB);
			break;
		case BufferedImage.TYPE_USHORT_GRAY:
			cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {16},
					false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
			sm = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, image.getWidth(), image.getHeight(), 1, null);
			
			texType = TextureType.UNSIGNED_SHORT;
			texFormat = TextureFormat.LUMINANCE;
			break;
		case BufferedImage.TYPE_BYTE_GRAY:
			cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {8},
					false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
			sm = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, image.getWidth(), image.getHeight(), 1, null);
				
			texType = TextureType.UNSIGNED_BYTE;
			texFormat = TextureFormat.LUMINANCE;
			break;
		default:
			cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8, 8},
					true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
			sm = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, image.getWidth(), image.getHeight(), 4, null);
			switch(dxt) {
			case DXT1:
				texFormat = TextureFormat.RGBA_DXT1;
				break;
			case DXT3:
				texFormat = TextureFormat.RGBA_DXT3;
				break;
			case DXT5:
				texFormat = TextureFormat.RGBA_DXT5;
				break;
			default:
				texFormat = TextureFormat.RGBA;
				break;
			}
			break;
		}
		
		BufferedImage formatted = new BufferedImage(cm, sm, false, null);
		Graphics2D g2 = formatted.createGraphics();
		AffineTransform t = AffineTransform.getScaleInstance(1, -1);
		t.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
		g2.drawImage(image, t, null);
		g2.dispose();
		
		Buffer data;
		if (texType == TextureType.UNSIGNED_BYTE) {
			byte[] rd = ((DataBufferByte)formatted.getRaster().getDataBuffer()).getData();
			ByteBuffer b = null;
			b = ByteBuffer.allocate(rd.length);
			b.put(rd);
			data = b;
		} else {
			short[] rd = ((DataBufferUShort)formatted.getRaster().getDataBuffer()).getData();
			ShortBuffer s = null;
			s = ShortBuffer.allocate(rd.length);
			s.put(rd);
			data = s;
		}
		
		if (buildMips)
			return new Texture2D(new Buffer[] {data.rewind()}, image.getWidth(), image.getHeight(), texType, texFormat, min, mag);
		else
			return new Texture2D(TextureUtil.buildMipmaps2D(data.rewind(), texFormat, texType, image.getWidth(), image.getHeight()), image.getWidth(), image.getHeight(), texType, texFormat, min, mag);
	}
	
	public static TextureCubeMap createTextureCubeMapFromImage(BufferedImage image, int dxt, boolean buildMips, MinFilter min, MagFilter mag) {
		ColorModel cm = null;
		WritableRaster sm = null;
		
		int side = image.getWidth() / 4;
		if (side * 4 != image.getWidth() || side * 3 != image.getHeight())
			throw new RuntimeException("Base image doesn't have the 4x3 aspect ration necessary for a cube map");
		
		TextureFormat texFormat;
		TextureType texType = TextureType.UNSIGNED_BYTE;
			
		switch(image.getType()) {
		case BufferedImage.TYPE_3BYTE_BGR: case BufferedImage.TYPE_INT_BGR: case BufferedImage.TYPE_INT_RGB:
		case BufferedImage.TYPE_USHORT_555_RGB: case BufferedImage.TYPE_USHORT_565_RGB:
			cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8, 0},
					                     false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
			sm = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, side, side, 3, null);
			texFormat = (dxt != NO_DXT ? TextureFormat.RGB_DXT1 : TextureFormat.RGB);
			break;
		case BufferedImage.TYPE_USHORT_GRAY:
			cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {16},
					 					 false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
			sm = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, side, side, 1, null);
				
			texType = TextureType.UNSIGNED_SHORT;
			texFormat = TextureFormat.LUMINANCE;
			break;
		case BufferedImage.TYPE_BYTE_GRAY:
			cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {8},
					 					 false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
			sm = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, side, side, 1, null);
				
			texType = TextureType.UNSIGNED_BYTE;
			texFormat = TextureFormat.LUMINANCE;
			break;
		default:
			cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8, 8},
										 true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
			sm = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, side, side, 4, null);
			switch(dxt) {
			case DXT1:
				texFormat = TextureFormat.RGBA_DXT1;
				break;
			case DXT3:
				texFormat = TextureFormat.RGBA_DXT3;
				break;
			case DXT5:
				texFormat = TextureFormat.RGBA_DXT5;
				break;
			default:
				texFormat = TextureFormat.RGBA;
				break;
			}
			break;
		}
		
		BufferedImage formatted = new BufferedImage(cm, sm, false, null);

		Buffer[] px = createCubeMapFace(image, formatted, 0, texFormat, texType, buildMips);
		Buffer[] nx = createCubeMapFace(image, formatted, 1, texFormat, texType, buildMips);
		Buffer[] py = createCubeMapFace(image, formatted, 2, texFormat, texType, buildMips);
		Buffer[] ny = createCubeMapFace(image, formatted, 3, texFormat, texType, buildMips);
		Buffer[] pz = createCubeMapFace(image, formatted, 4, texFormat, texType, buildMips);
		Buffer[] nz = createCubeMapFace(image, formatted, 5, texFormat, texType, buildMips);
		
		return new TextureCubeMap(px, nx, py, ny, pz, nz, side, texType, texFormat, min, mag);
	}
	
	private static Buffer[] createCubeMapFace(BufferedImage fullImage, BufferedImage faceStore, int face, TextureFormat texFormat, TextureType texType, boolean buildMipmap) {
		Graphics2D g2 = faceStore.createGraphics();
		AffineTransform t = AffineTransform.getScaleInstance(1, 1);
		
		switch(face) {
		case 0: // PX
			t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getScaleInstance(-1, 1));
			t.concatenate(AffineTransform.getRotateInstance(-Math.PI / 2));
			t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getTranslateInstance(-2 * faceStore.getWidth(), -1 * faceStore.getHeight()));
			break;
		case 1: // NX
			t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getScaleInstance(-1, 1));
			t.concatenate(AffineTransform.getRotateInstance(Math.PI / 2));
			t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getTranslateInstance(0 * faceStore.getWidth(), -1 * faceStore.getHeight()));
			break;
		case 2: // PY
			t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getScaleInstance(-1, 1));
			t.concatenate(AffineTransform.getRotateInstance(0));
			t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getTranslateInstance(-3 * faceStore.getWidth(), -1 * faceStore.getHeight()));
			break;
		case 3: // NY
			t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getScaleInstance(1, -1));
			t.concatenate(AffineTransform.getRotateInstance(0));
			t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getTranslateInstance(-1 * faceStore.getWidth(), -1 * faceStore.getHeight()));
			break;
		case 4: // PZ
			t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getScaleInstance(-1, 1));
			t.concatenate(AffineTransform.getRotateInstance(-Math.PI));
			t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getTranslateInstance(-1 * faceStore.getWidth(), -2 * faceStore.getHeight()));
			break;
		case 5: // NZ
			t.concatenate(AffineTransform.getTranslateInstance(.5 * faceStore.getWidth(), .5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getScaleInstance(1, 1));
			t.concatenate(AffineTransform.getRotateInstance(0));
			t.concatenate(AffineTransform.getTranslateInstance(-.5 * faceStore.getWidth(), -.5 * faceStore.getHeight()));
			t.concatenate(AffineTransform.getTranslateInstance(-1 * faceStore.getWidth(), -0 * faceStore.getHeight()));
			break;
		}
		
		g2.drawImage(fullImage, t, null);
		g2.dispose();
		
		Buffer data = null;
		if (texType == TextureType.UNSIGNED_BYTE) {
			byte[] rd = ((DataBufferByte)faceStore.getRaster().getDataBuffer()).getData();
			ByteBuffer b = null;
			b = ByteBuffer.allocate(rd.length);
			b.put(rd);
			data = b;
		} else if (texType == TextureType.UNSIGNED_SHORT) {
			short[] rd = ((DataBufferUShort)faceStore.getRaster().getDataBuffer()).getData();
			ShortBuffer s = null;
			s = ShortBuffer.allocate(rd.length);
			s.put(rd);
			data = s;
		} else 
			throw new RuntimeException("Unsupported texture data type");
		
		if (buildMipmap)
			return TextureUtil.buildMipmaps2D(data.rewind(), texFormat, texType, faceStore.getWidth(), faceStore.getWidth());
		else
			return new Buffer[] {data.rewind()};
	}
}
