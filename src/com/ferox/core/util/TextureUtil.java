package com.ferox.core.util;

import java.nio.*;

import com.ferox.core.states.atoms.TextureData;
import com.ferox.core.states.atoms.TextureData.TextureCompression;
import com.ferox.core.states.atoms.TextureData.TextureFormat;
import com.ferox.core.states.atoms.TextureData.TextureType;

public class TextureUtil {
	static abstract class Sampler {
		int width;
		int height;
		int depth;
		int pos;
		int limit;
		
		public Sampler(Buffer data, int width, int height, int depth) {
			this.pos = data.position();
			this.limit = data.limit();
			this.width = width;
			this.height = height;
			this.depth = depth;
		}
		
		public abstract void setTexel(int x, int y, int z, Sampler inValue);
		public abstract void getTexel(float u, float v, float w);
	}
	
	public static Buffer[] buildMipmaps2D(Buffer data, TextureFormat format, TextureType type, int width, int height) {
		return buildMipmaps3D(data, format, type, width, height, 1);
	}
	
	public static Buffer[] buildMipmaps3D(Buffer data, TextureFormat format, TextureType type, int width, int height, int depth) {
		if (!TextureData.isBufferValid(type, format, TextureCompression.NONE, width, height, depth, data))
			throw new IllegalArgumentException("Original image data is an invalid texture");
		if (format.isClientCompressed())
			throw new IllegalArgumentException("Can't scale a compressed image");
		if (width != height || (depth != 1 && (depth != height || depth != width)))
			throw new IllegalArgumentException("Texture must have same dimensions");
		Buffer[] mipmaps = new Buffer[(int)(Math.log(width) / Math.log(2) + 1)];
		mipmaps[0] = data;
		
		int oW = width;
		int oH = height;
		int oD = depth;
		
		for (int i = 1; i < mipmaps.length; i++) {
			width = Math.max(1, width >> 1);
			height = Math.max(1, height >> 1);
			depth = Math.max(1, depth >> 1);
			mipmaps[i] = scaleImage3D(data, format, type, oW, oH, oD, width, height, depth);
		}
		
		return mipmaps;
	}
	
	public static Buffer scaleImage2D(Buffer data, TextureFormat format, TextureType type, int oldWidth, int oldHeight, int newWidth, int newHeight) {
		return scaleImage3D(data, format, type, oldWidth, oldHeight, 1, newWidth, newHeight, 1);
	}
	
	public static void scaleImage2D(Buffer data, TextureFormat format, TextureType type, int oldWidth, int oldHeight, int newWidth, int newHeight, Buffer newImage) {
		scaleImage3D(data, format, type, oldWidth, oldHeight, 1, newWidth, newHeight, 1, newImage);
	}
	
	public static Buffer scaleImage3D(Buffer data, TextureFormat format, TextureType type, int oldWidth, int oldHeight, int oldDepth, int newWidth, int newHeight, int newDepth) {
		if (!TextureData.isBufferValid(type, format, TextureCompression.NONE, oldWidth, oldHeight, oldDepth, data))
			throw new IllegalArgumentException("Original image data is an invalid texture");
		if (format.isClientCompressed())
			throw new IllegalArgumentException("Can't scale a compressed image");
		
		Buffer newImage = null;
		
		switch(type) {
		case UNSIGNED_INT: case PACKED_INT_8888:
			newImage = BufferUtil.newIntBuffer(format.getBufferSize(type, newWidth, newHeight, newDepth));
			break;
		case PACKED_SHORT_4444: case PACKED_SHORT_5551: 
		case PACKED_SHORT_565: case UNSIGNED_SHORT:
			newImage = BufferUtil.newShortBuffer(format.getBufferSize(type, newWidth, newHeight, newDepth));
			break;
		case UNSIGNED_BYTE:
			newImage = BufferUtil.newByteBuffer(format.getBufferSize(type, newWidth, newHeight, newDepth));
			break;
		case FLOAT:
			newImage = BufferUtil.newFloatBuffer(format.getBufferSize(type, newWidth, newHeight, newDepth));
			break;
		}
		
		scaleImage3D(data, format, type, oldWidth, oldHeight, oldDepth, newWidth, newHeight, newDepth, newImage);
		return newImage;
	}
	
	public static void scaleImage3D(Buffer data, TextureFormat format, TextureType type, int oldWidth, int oldHeight, int oldDepth, int newWidth, int newHeight, int newDepth, Buffer newImage) {
		if (!TextureData.isBufferValid(type, format, TextureCompression.NONE, oldWidth, oldHeight, oldDepth, data))
			throw new IllegalArgumentException("Original image data is an invalid texture");
		if (!TextureData.isBufferValid(type, format, TextureCompression.NONE, newWidth, newHeight, newDepth, newImage))
			throw new IllegalArgumentException("New image data won't be a valid texture");
		if (format.isClientCompressed())
			throw new IllegalArgumentException("Can't scale a compressed image");
		
		int x, y, z;
		float uScaleFactor = 1f / newWidth;
		float vScaleFactor = 1f / newHeight;
		float wScaleFactor = 1f / newDepth;
		
		Sampler in = getSampler(data, oldWidth, oldHeight, oldDepth, format, type);
		Sampler out = getSampler(newImage, newWidth, newHeight, newDepth, format, type);
		
		for (z = 0; z < newDepth; z++) {
			for (y = 0; y < newHeight; y++) {
				for (x = 0; x < newWidth; x++) {
					in.getTexel(x * uScaleFactor, y * vScaleFactor, z * wScaleFactor);
					out.setTexel(x, y, z, in);
				}
			}
		}
	}
	
	private static Sampler getSampler(Buffer data, int width, int height, int depth, TextureFormat format, TextureType type) {
		Sampler s = null;
		
		switch(type) {
		case UNSIGNED_BYTE:
			s = new Samplers.ByteUnpackedSampler(data, width, height, depth, format.getNumComponents());
			break;
		case FLOAT:
			s = new Samplers.FloatUnpackedSampler(data, width, height, depth, format.getNumComponents());
			break;
		case UNSIGNED_INT:
			s = new Samplers.IntUnpackedSampler(data, width, height, depth, format.getNumComponents());
			break;
		case UNSIGNED_SHORT:
			s = new Samplers.ShortUnpackedSampler(data, width, height, depth, format.getNumComponents());
			break;
		case PACKED_INT_8888:
			s = new Samplers.Int8888Sampler(data, width, height, depth);
			break;
		case PACKED_SHORT_4444:
			s = new Samplers.Short4444Sampler(data, width, height, depth);
			break;
		case PACKED_SHORT_5551:
			s = new Samplers.Short5551Sampler(data, width, height, depth);
			break;
		case PACKED_SHORT_565:
			s = new Samplers.Short565Sampler(data, width, height, depth);
			break;
		}
		
		return s;
	}
}
