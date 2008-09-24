package com.ferox.core.states.atoms;

import java.nio.Buffer;

import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class TextureCubeMap extends TextureData {
	public static enum Face {
		PX, PY, PZ, NX, NY, NZ
	}
	
	private Buffer[] px, nx, py, ny, pz, nz;
	private int side;
	private int numMips;
	private boolean inited;
	
	public TextureCubeMap(Buffer[] px, Buffer[] nx, Buffer[] py, Buffer[] ny, Buffer[] pz, Buffer[] nz, int side, TextureType dataType, TextureFormat dataFormat, MinFilter min, MagFilter mag) {
		super(dataType, dataFormat, min, mag);
		this.setTextureData(px, nx, py, ny, pz, nz, side);
		this.inited = true;
	}
	
	public TextureCubeMap(Buffer[] px, Buffer[] nx, Buffer[] py, Buffer[] ny, Buffer[] pz, Buffer[] nz, int side, TextureType dataType, TextureFormat dataFormat, TexClamp clamp) {
		super(dataType, dataFormat, clamp);
		this.setTextureData(px, nx, py, ny, pz, nz, side);
		this.inited = true;
	}
	
	public TextureCubeMap(Buffer[] px, Buffer[] nx, Buffer[] py, Buffer[] ny, Buffer[] pz, Buffer[] nz, int side, TextureType dataType, TextureFormat dataFormat, TexClamp clamp, MinFilter min, MagFilter mag) {
		super(dataType, dataFormat, clamp, min, mag);
		this.setTextureData(px, nx, py, ny, pz, nz, side);
		this.inited = true;
	}
	
	public TextureCubeMap(Buffer[] px, Buffer[] nx, Buffer[] py, Buffer[] ny, Buffer[] pz, Buffer[] nz, int side, TextureType dataType, TextureFormat dataFormat, TextureCompression comp, TexClamp clampS, TexClamp clampT, TexClamp clampR, MinFilter min, MagFilter mag) {
		super(dataType, dataFormat, comp, clampS, clampT, clampR, min, mag);
		this.setTextureData(px, nx, py, ny, pz, nz, side);
		this.inited = true;
	}
	
	private TextureCubeMap() {
		super();
		this.inited = true;
	}
	
	private Buffer[] compressBuffer(Buffer[] data) {
		int nonNullCount = 0;
		if (data != null) {
			for (int i = 0; i < data.length; i++)
				if (data[i] != null)
					nonNullCount++;
		}
		if (nonNullCount > 0) {
			Buffer[] nD = new Buffer[nonNullCount];
			nonNullCount = 0;
			for (int i = 0; i < data.length; i++)
				if (data[i] != null) 
					nD[nonNullCount++] = data[i];
			return nD;
		} else 
			return null;
	}
	
	private void validateCubeFace(Buffer[] face, int side) throws IllegalArgumentException {
		for (int i = 0; i < face.length; i++) {
			if (!TextureData.isBufferValid(this.getDataType(), this.getDataFormat(), this.getDataCompression(), side, side, face[i]))
				throw new IllegalArgumentException("Improper buffer data size at mipmap level: " + i);
			side = Math.max(1, (side >> 1));
		}
	}

	public void setTextureFormat(TextureFormat format, TextureType type, TextureCompression comp) {
		if (this.inited)
			this.setTexture(this.px, this.nx, this.py, this.ny, this.pz, this.nz, this.side, format, type, comp);
		else
			super.setTextureFormat(format, type, comp);
	}
	
	public void setTexture(Buffer[] px, Buffer[] nx, Buffer[] py, Buffer[] ny, Buffer[] pz, Buffer[] nz, int side, TextureFormat format, TextureType type, TextureCompression comp) throws IllegalArgumentException {
		TextureFormat oldFormat = this.getDataFormat();
		TextureType oldType = this.getDataType();
		TextureCompression oldComp = this.getDataCompression();
		try {
			super.setTextureFormat(format, type, comp);
			this.setTextureData(px, nx, py, ny, pz, nz, side);
		} catch (RuntimeException e) {
			super.setTextureFormat(oldFormat, oldType, oldComp);
			throw e;
		}
	}
	
	public void setTextureData(Buffer[] px, Buffer[] nx, Buffer[] py, Buffer[] ny, Buffer[] pz, Buffer[] nz) {
		this.setTextureData(px, nx, py, ny, pz, nz, this.side);
	}
	
	public void setTextureData(Buffer[] px, Buffer[] nx, Buffer[] py, Buffer[] ny, Buffer[] pz, Buffer[] nz, int side) throws IllegalArgumentException {
		int oldSide = this.side;
		Buffer[] oldPX = this.px;
		Buffer[] oldNX = this.nx;
		Buffer[] oldPY = this.py;
		Buffer[] oldNY = this.ny;
		Buffer[] oldPZ = this.pz;
		Buffer[] oldNZ = this.nz;
		
		try {
			if (this.getDataFormat() == TextureFormat.DEPTH)
				throw new IllegalArgumentException("Depth textures can only be 2D textures");
		
			this.side = side;
		
			this.px = this.compressBuffer(px);
			this.nx = this.compressBuffer(nx);
			this.py = this.compressBuffer(py);
			this.ny = this.compressBuffer(ny);
			this.pz = this.compressBuffer(pz);
			this.nz = this.compressBuffer(nz);
			
			if (this.px == null) {
				// assume we want a headless texture, the rest of the data must be null too
				if (this.nx != null || this.py != null || this.ny != null || this.pz != null || this.nz != null)
					throw new IllegalArgumentException("If using null buffers, all cube faces must be null");
				if (this.getDataFormat().isClientCompressed() || this.getDataCompression() != TextureCompression.NONE)
					throw new IllegalArgumentException("Headless texture can't be compressed");
			} else {
				if (this.nx == null || this.py == null || this.ny == null || this.pz == null || this.nz == null)
					throw new IllegalArgumentException("If using non-null buffers, all cube faces must be non-null");
				if (this.px.length != this.nx.length || this.px.length != this.py.length || this.px.length != this.ny.length 
						|| this.px.length != this.pz.length || this.px.length != this.nz.length)
						throw new IllegalArgumentException("Non-null data buffers must have the same number of mipmap levels");
				
				this.validateCubeFace(this.px, this.side);
				this.validateCubeFace(this.nx, this.side);
				this.validateCubeFace(this.py, this.side);
				this.validateCubeFace(this.ny, this.side);
				this.validateCubeFace(this.pz, this.side);
				this.validateCubeFace(this.nz, this.side);
			}
		
			this.numMips = (this.px == null ? 1 : this.px.length);
			
			if (this.isMipmapped() && this.px.length != (int)(Math.log(this.side) / Math.log(2) + 1))
				throw new IllegalArgumentException("Can't specify mipmaps using too few mipmap buffers");
		} catch (RuntimeException e) {
			// invalid parameters, restore old texture state
			this.side = oldSide;
			this.px = oldPX;
			this.nx = oldNX;
			this.py = oldPY;
			this.ny = oldNY;
			this.pz = oldPZ;
			this.nz = oldNZ;
			throw e;
		}
	}
	
	public Buffer getPositiveXMipmap(int level) {
		return (this.px == null ? null : this.px[level]);
	}
	
	public Buffer getNegativeXMipmap(int level) {
		return (this.nx == null ? null : this.nx[level]);
	}
	
	public Buffer getPositiveYMipmap(int level) {
		return (this.py == null ? null : this.py[level]);
	}
	
	public Buffer getNegativeYMipmap(int level) {
		return (this.ny == null ? null : this.ny[level]);
	}
	
	public Buffer getPositiveZMipmap(int level) {
		return (this.pz == null ? null : this.pz[level]);
	}
	
	public Buffer getNegativeZMipmap(int level) {
		return (this.nz == null ? null : this.nz[level]);
	}

	public Buffer getMipmap(int level, Face face) throws NullPointerException {
		if (face == null)
			throw new NullPointerException("Can't return a mipmap buffer for a null face");
		switch(face) {
		case NX:
			return getNegativeXMipmap(level);
		case PX:
			return getPositiveXMipmap(level);
		case NY:
			return getNegativeYMipmap(level);
		case PY:
			return getPositiveYMipmap(level);
		case NZ:
			return getNegativeZMipmap(level);
		case PZ:
			return getPositiveZMipmap(level);
		}
		return null;
	}
	
	public void setTextureData(Buffer[] data, Face face) throws IllegalArgumentException, NullPointerException {
		if (face == null)
			throw new NullPointerException("Can't set texture data for a null face");
		switch(face) {
		case PX:
			this.setTextureData(data, this.nx, this.py, this.ny, this.pz, this.nz);
			break;
		case NX:
			this.setTextureData(this.px, data, this.py, this.ny, this.pz, this.nz);
			break;
		case PY:
			this.setTextureData(this.px, this.nx, data, this.ny, this.pz, this.nz);
			break;
		case NY:
			this.setTextureData(this.px, this.nx, this.py, data, this.pz, this.nz);
			break;
		case PZ:
			this.setTextureData(this.px, this.nx, this.py, this.ny, data, this.nz);
			break;
		case NZ:
			this.setTextureData(this.px, this.nx, this.py, this.ny, this.pz, data);
			break;
		}
	}
	
	public int getSideLength() {
		return this.getSideLength(0);
	}
	
	public int getSideLength(int level) {
		return Math.max(1, this.side >> level);
	}
	
	public boolean isDataInClientMemory() {
		return this.px == null;
	}
	
	public void clearClientMemory() {
		int oldMips = this.numMips;
		this.setTextureData(null, null, null, null, null, null, this.side);
		this.numMips = oldMips;
	}
	
	public int getNumMipmaps() {
		return this.numMips;
	}
	
	@Override
	public boolean isMipmapped() {
		return this.numMips > 1;
	}

	@Override
	public TextureTarget getTarget() {
		return TextureTarget.CUBEMAP;
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.set("side", this.side);
		out.set("numMipmaps", this.numMips);
		
		out.set("levels", (this.px == null ? 0 : this.px.length));
		if (this.px != null) {
			for (int i = 0; i < this.px.length; i++)
				out.set("px_" + i, this.px[i]);
			for (int i = 0; i < this.px.length; i++)
				out.set("nx_" + i, this.nx[i]);
			for (int i = 0; i < this.px.length; i++)
				out.set("py_" + i, this.py[i]);
			for (int i = 0; i < this.px.length; i++)
				out.set("ny_" + i, this.ny[i]);
			for (int i = 0; i < this.px.length; i++)
				out.set("pz_" + i, this.pz[i]);
			for (int i = 0; i < this.px.length; i++)
				out.set("nz_" + i, this.nz[i]);
		}
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.side = in.getInt("side");
		this.numMips = in.getInt("numMipmaps");
		
		int levels = in.getInt("levels");
		if (levels > 0) {
			this.px = new Buffer[levels];
			for (int i = 0; i < levels; i++)
				this.px[i] = in.getBuffer("px_" + i);
			this.nx = new Buffer[levels];
			for (int i = 0; i < levels; i++)
				this.nx[i] = in.getBuffer("nx_" + i);
			this.py = new Buffer[levels];
			for (int i = 0; i < levels; i++)
				this.py[i] = in.getBuffer("py_" + i);
			this.ny = new Buffer[levels];
			for (int i = 0; i < levels; i++)
				this.ny[i] = in.getBuffer("ny_" + i);
			this.pz = new Buffer[levels];
			for (int i = 0; i < levels; i++)
				this.pz[i] = in.getBuffer("pz_" + i);
			this.nz = new Buffer[levels];
			for (int i = 0; i < levels; i++)
				this.nz[i] = in.getBuffer("nz_" + i);
		} else {
			this.px = null;
			this.nx = null;
			this.py = null;
			this.ny = null;
			this.pz = null;
			this.nz = null;
		}
	}
}
