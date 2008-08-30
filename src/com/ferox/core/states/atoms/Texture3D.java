package com.ferox.core.states.atoms;

import java.nio.Buffer;

import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class Texture3D extends TextureData {
	private Buffer[] data;
	private int width;
	private int height;
	private int depth;
	
	public Texture3D(Buffer[] data, int width, int height, int depth, TextureType dataType, TextureFormat dataFormat, MinFilter min, MagFilter mag) {
		super(dataType, dataFormat, min, mag);
		this.setTextureData(data, width, height, depth);
	}
	
	public Texture3D(Buffer[] data, int width, int height, int depth, TextureType dataType, TextureFormat dataFormat, TexClamp clamp) {
		super(dataType, dataFormat, clamp);
		this.setTextureData(data, width, height, depth);
	}
	
	public Texture3D(Buffer[] data, int width, int height, int depth, TextureType dataType, TextureFormat dataFormat, TexClamp clamp, MinFilter min, MagFilter mag) {
		super(dataType, dataFormat, clamp, min, mag);
		this.setTextureData(data, width, height, depth);
	}
	
	public Texture3D(Buffer[] data, int width, int height, int depth, TextureType dataType, TextureFormat dataFormat, TexClamp clampS, TexClamp clampT, TexClamp clampR, MinFilter min, MagFilter mag) {
		super(dataType, dataFormat, clampS, clampT, clampR, min, mag);
		this.setTextureData(data, width, height, depth);
	}
	
	public void setTextureFormatAndType(TextureFormat format, TextureType type) {
		this.setTexture(this.data, this.width, this.height, this.depth, format, type);
	}
	
	public void setTexture(Buffer[] data, int width, int height, int depth, TextureFormat format, TextureType type) {
		TextureFormat oldFormat = this.getDataFormat();
		TextureType oldType = this.getDataType();
		try {
			this.setFormatAndType(format, type);
			this.setTextureData(data, width, height, depth);
		} catch (RuntimeException e) {
			this.setFormatAndType(oldFormat, oldType);
			throw e;
		}
		
		if (oldFormat != this.getDataFormat() || oldType != this.getDataType())
			this.cleanupStateAtom();
	}
	
	public void setTextureData(Buffer[] data) {
		this.setTextureData(data, this.width, this.height, this.depth);
	}
	
	public void setTextureData(Buffer[] data, int width, int height, int depth) {
		int oldWidth = this.width;
		int oldHeight = this.height;
		int oldDepth = this.depth;
		
		Buffer[] oldData = this.data;
		
		try {
			if (this.getDataFormat().isServerCompressed())
				throw new IllegalArgumentException("Can't create a compressed 3D texture");
			if (this.getDataFormat() == TextureFormat.DEPTH)
				throw new IllegalArgumentException("Depth textures can only be 2D textures");
		
			this.width = width;
			this.height = height;
			this.depth = depth;
			
			int nonNullCount = 0;
			if (data != null) {
				for (int i = 0 ; i < data.length; i++) {
					if (data[i] != null)
						nonNullCount++;
				}
			}
			if (nonNullCount > 0) {
				this.data = new Buffer[nonNullCount];
			
				nonNullCount = 0;
				for (int i = 0; i < data.length; i++) {
					if (data[i] != null)
						this.data[nonNullCount++] = data[i];
				}
			
				for (int i = 0; i < this.data.length; i++) {
					if (!TextureData.isBufferValid(this.getDataType(), this.getDataFormat(), width, height, depth, this.data[i]))
						throw new IllegalArgumentException("Improper buffer data size at mipmap level: " + i);
					width = Math.max(1, (width >> 1));
					height = Math.max(1, (height >> 1));
					depth = Math.max(1, (depth >> 1));
				}
			} else {
				this.data = null;
			}
		
			if (this.width != this.height) 
				throw new IllegalArgumentException("2D slices of texture data must be square");
			if (this.isMipmapped() && this.data.length != (int)(Math.log(this.width) / Math.log(2) + 1))
				throw new IllegalArgumentException("Can't specify mipmaps using too few mipmap buffers");
		} catch (RuntimeException e) {
			// invalid parameters, restore old texture state
			this.width = oldWidth;
			this.height = oldHeight;
			this.depth = oldDepth;
			this.data = oldData;
			throw e;
		}
		
		// new texture data is valid, see if we need a new texture object (ie new size, everything else new is covered in peer)
		if (oldWidth != this.width || oldHeight != this.height || oldDepth != this.depth)
			this.cleanupStateAtom();
	}
	
	public boolean isDataInClientMemory() {
		return this.data == null;
	}
	
	public void clearClientMemory() {
		this.setTextureData(null, this.width, this.height, this.depth);
	}
	
	public Buffer getMipmapLevel(int level) {
		return (this.data == null ? null : this.data[level]);
	}
	
	public Buffer[] getMipmaps() {
		return this.data;
	}
	
	public int getNumMipmaps() {
		return (this.data == null ? 1 : this.data.length);
	}
	
	@Override	
	public boolean isMipmapped() {
		return this.data != null && this.data.length > 1;
	}
	
	public int getWidth() {
		return this.getWidth(0);
	}
	
	public int getHeight() {
		return this.getHeight(0);
	}
	
	public int getDepth() {
		return this.getDepth(0);
	}
	
	public int getWidth(int level) {
		return Math.max(1, this.width >> level);
	}
	
	public int getHeight(int level) {
		return Math.max(1, this.height >> level);
	}
	
	public int getDepth(int level) {
		return Math.max(1, this.depth >> level);
	}

	@Override
	public TextureTarget getTarget() {
		return TextureTarget.TEX3D;
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.setInt("width", this.width);
		out.setInt("height", this.height);
		out.setInt("depth", this.depth);
		
		if (this.data == null) 
			out.setInt("numMipmaps", 0);
		else {
			out.setInt("numMipmaps", this.data.length);
			for (int i = 0; i < this.data.length; i++)
				out.setBuffer("data_" + i, this.data[i]);
		}
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.width = in.getInt("width");
		this.height = in.getInt("height");
		this.depth = in.getInt("depth");
		
		int numMipmaps = in.getInt("numMipmaps");
		if (numMipmaps > 0) {
			this.data = new Buffer[numMipmaps];
			for (int i = 0; i < numMipmaps; i++)
				this.data[i] = in.getBuffer("data_" + i);
		} else 
			this.data = null;
	}
}
