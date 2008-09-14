package com.ferox.impl.jsr231.peers;

import java.nio.Buffer;

import javax.media.opengl.GL;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.states.atoms.TextureCubeMap;
import com.ferox.core.states.atoms.TextureCubeMap.Face;
import com.ferox.core.states.atoms.TextureData.TextureCompression;
import com.ferox.core.states.atoms.TextureData.TextureFormat;
import com.ferox.core.states.atoms.TextureData.TextureType;
import com.ferox.core.util.TextureUtil;
import com.ferox.core.util.DataTransfer.Block;
import com.ferox.core.util.DataTransfer.Slice;
import com.ferox.impl.jsr231.peers.JOGLTextureDataPeer.TextureTransfer;

public class TextureCubeMapTransfer implements TextureTransfer<TextureCubeMap> {
	public void validate(TextureCubeMap texture, GL gl) {
		int side = JOGLTextureDataPeer.potCeil(texture.getSideLength());
		int[] v = new int[1];
		gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, v, 0);
		if ((!RenderManager.getSystemCapabilities().areNpotTexturesSupported() && (side != texture.getSideLength())) ||
			(texture.getSideLength() > v[0])) {
			if (RenderManager.getSystemCapabilities().areNpotTexturesSupported()) {
				side = texture.getSideLength();
			}
			side = Math.min(v[0], side);
			
			Buffer[] nPX = null;
			Buffer[] nNX = null;
			Buffer[] nPY = null;
			Buffer[] nNY = null;
			Buffer[] nPZ = null;
			Buffer[] nNZ = null;
			
			if (texture.getMipmap(0, Face.PX) != null) {
				int mS = side;
				nPX = new Buffer[texture.getNumMipmaps()];
				nNX = new Buffer[texture.getNumMipmaps()];
				nPY = new Buffer[texture.getNumMipmaps()];
				nNY = new Buffer[texture.getNumMipmaps()];
				nPZ = new Buffer[texture.getNumMipmaps()];
				nNZ = new Buffer[texture.getNumMipmaps()];
				
				for (int i = 0; i < nPX.length; i++) {
					nPX[i] = TextureUtil.scaleImage2D(texture.getPositiveXMipmap(i), texture.getDataFormat(), texture.getDataType(), texture.getSideLength(), texture.getSideLength(), mS, mS);
					nNX[i] = TextureUtil.scaleImage2D(texture.getNegativeXMipmap(i), texture.getDataFormat(), texture.getDataType(), texture.getSideLength(), texture.getSideLength(), mS, mS);
					nPY[i] = TextureUtil.scaleImage2D(texture.getPositiveYMipmap(i), texture.getDataFormat(), texture.getDataType(), texture.getSideLength(), texture.getSideLength(), mS, mS);
					nNY[i] = TextureUtil.scaleImage2D(texture.getNegativeYMipmap(i), texture.getDataFormat(), texture.getDataType(), texture.getSideLength(), texture.getSideLength(), mS, mS);
					nPZ[i] = TextureUtil.scaleImage2D(texture.getPositiveZMipmap(i), texture.getDataFormat(), texture.getDataType(), texture.getSideLength(), texture.getSideLength(), mS, mS);
					nNZ[i] = TextureUtil.scaleImage2D(texture.getNegativeZMipmap(i), texture.getDataFormat(), texture.getDataType(), texture.getSideLength(), texture.getSideLength(), mS, mS);

					mS = Math.max(1, mS >> 1);
				}
			}
			texture.setTextureData(nPX, nNX, nPY, nNY, nPZ, nNZ, side);
		}
	}
	
	public void submitData(TextureRecord t, TextureCubeMap texture, GL gl) {
		int side = texture.getSideLength();
		
		for (int i = 0; i < texture.getNumMipmaps(); i++) {
			if (!this.reallocateOnUpdate(side, side, texture.getNumMipmaps(), texture.getDataFormat()) || texture.getPositiveXMipmap(i) == null) {
				this.setTexImage(true, gl, GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X, i, 0, 0, side, side, t.srcFormat, t.dstFormat, t.dataType, texture.getDataFormat(), texture.getMipmap(i, Face.PX));
				this.setTexImage(true, gl, GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, i, 0, 0, side, side, t.srcFormat, t.dstFormat, t.dataType, texture.getDataFormat(), texture.getMipmap(i, Face.NX));
				this.setTexImage(true, gl, GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, i, 0, 0, side, side, t.srcFormat, t.dstFormat, t.dataType, texture.getDataFormat(), texture.getMipmap(i, Face.PY));
				this.setTexImage(true, gl, GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, i, 0, 0, side, side, t.srcFormat, t.dstFormat, t.dataType, texture.getDataFormat(), texture.getMipmap(i, Face.NY));
				this.setTexImage(true, gl, GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, i, 0, 0, side, side, t.srcFormat, t.dstFormat, t.dataType, texture.getDataFormat(), texture.getMipmap(i, Face.PZ));
				this.setTexImage(true, gl, GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, i, 0, 0, side, side, t.srcFormat, t.dstFormat, t.dataType, texture.getDataFormat(), texture.getMipmap(i, Face.NZ));
			}
			side = Math.max(1, (side >> 1));
		}
		this.updateData(t, texture, gl);
	}
	
	public void updateData(TextureRecord t, TextureCubeMap texture, GL gl) {
		this.updateFace(t, texture, GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X, gl);
		this.updateFace(t, texture, GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, gl);
		this.updateFace(t, texture, GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, gl);
		this.updateFace(t, texture, GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, gl);
		this.updateFace(t, texture, GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, gl);
		this.updateFace(t, texture, GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, gl);
	}
	
	private void updateFace(TextureRecord t, TextureCubeMap texture, int face, GL gl) {
		Buffer data = null;
		int side = texture.getSideLength();
					
		for (int i = 0; i < texture.getNumMipmaps(); i++) {
			switch(face) {
			case GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X:
				data = texture.getPositiveXMipmap(i);
				break;
			case GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X:
				data = texture.getNegativeXMipmap(i);
				break;
			case GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y:
				data = texture.getPositiveYMipmap(i);
				break;
			case GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y:
				data = texture.getNegativeYMipmap(i);
				break;
			case GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z:
				data = texture.getPositiveZMipmap(i);
				break;
			case GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z:
				data = texture.getNegativeZMipmap(i);
				break;
			}
			if (data != null) 
				this.setTexImage(this.reallocateOnUpdate(side, side, texture.getNumMipmaps(), texture.getDataFormat()), gl, face, i, 0, 0, side, side, t.srcFormat, t.dstFormat, t.dataType, texture.getDataFormat(), data);
			side = Math.max(1, (side >> 1));
		}
	}
	
	private void setTexImage(boolean realloc, GL gl, int target, int level, int xOff, int yOff, int width, int height, int srcFormat, int dstFormat, int type, TextureFormat rFormat, Buffer data) {
		if (data != null)
			data.clear();
		if (realloc) {
			if (srcFormat < 0)
				gl.glCompressedTexImage2D(target, level, dstFormat, width, height, 0, rFormat.getBufferSize(TextureType.UNSIGNED_BYTE, width, height), data);
			else
				gl.glTexImage2D(target, level, dstFormat, width, height, 0, srcFormat, type, data);
		} else {
			if (srcFormat < 0) 
				gl.glCompressedTexSubImage2D(target, level, xOff, yOff, width, height, dstFormat, rFormat.getBufferSize(TextureType.UNSIGNED_BYTE, width, height), data);
			else
				gl.glTexSubImage2D(target, level, xOff, yOff, width, height, srcFormat, type, data);
		}
	}

	private void setTexImage(boolean realloc, GL gl, int target, int level, int xOff, int yOff, int width, int height, int srcFormat, int dstFormat, int type, TextureFormat rFormat, int offset) {
		if (realloc) {
			if (srcFormat < 0)
				gl.glCompressedTexImage2D(target, level, dstFormat, width, height, 0, rFormat.getBufferSize(TextureType.UNSIGNED_BYTE, width, height), offset);
			else
				gl.glTexImage2D(target, level, dstFormat, width, height, 0, srcFormat, type, offset);
		} else {
			if (srcFormat < 0) 
				gl.glCompressedTexSubImage2D(target, level, xOff, yOff, width, height, dstFormat, rFormat.getBufferSize(TextureType.UNSIGNED_BYTE, width, height), offset);
			else
				gl.glTexSubImage2D(target, level, xOff, yOff, width, height, srcFormat, type, offset);
		}
	}
	
	private boolean reallocateOnUpdate(int width, int height, int numMips, TextureFormat format) {
		if (format.isClientCompressed())
			return numMips > 1;
		return false;
	}

	public void copyData(TextureRecord r, TextureCubeMap data, Block region, Face face, int level, int sx, int sy, GL gl) {
		gl.glCopyTexSubImage2D(getFaceTarget(face), level, region.getXOffset(), region.getYOffset(), sx, sy, region.getWidth(), region.getHeight());
	}

	private static int getFaceTarget(Face face) {
		int target = 0;
		switch(face) {
		case PX: target = GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X; break;
		case PY: target = GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y; break;
		case PZ: target = GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z; break;
		case NX: target = GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X; break;
		case NY: target = GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y; break;
		case NZ: target = GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z; break;
		}
		return target;
	}
	
	public void getData(TextureRecord r, TextureCubeMap data, Face face, int level, Buffer out, Slice slice, GL gl) {
		PackPixelStore s = PackPixelStore.get(gl);
		PackPixelStore.setUseful(gl);
			
		int target = getFaceTarget(face);
		
		if (out != null) {
			int pos = out.position();
			int lim = out.limit();
			out.clear();
			out.position(slice.getOffset());
			out.limit(slice.getOffset() + slice.getLength());
		
			if (!data.getDataFormat().isClientCompressed() && data.getDataCompression() == TextureCompression.NONE)
				gl.glGetTexImage(target, level, r.srcFormat, r.dataType, out);
			else
				gl.glGetCompressedTexImage(target, level, out);
			
			out.limit(lim);
			out.position(pos);
		} else {
			if (!data.getDataFormat().isClientCompressed() && data.getDataCompression() == TextureCompression.NONE)
				gl.glGetTexImage(target, level, r.srcFormat, r.dataType, slice.getOffset() * data.getDataType().getByteSize());
			else
				gl.glGetCompressedTexImage(target, level, slice.getOffset() * data.getDataType().getByteSize());
		}
		
		s.set(gl);
	}

	public void setData(TextureRecord r, TextureCubeMap data, Block region, Face face, int level, Buffer in, Slice slice, GL gl) {
		UnpackPixelStore s = UnpackPixelStore.get(gl);
		UnpackPixelStore.setUseful(gl);
		
		gl.glPixelStorei(GL.GL_UNPACK_SKIP_PIXELS, region.getXOffset());
		gl.glPixelStorei(GL.GL_UNPACK_SKIP_ROWS, region.getYOffset());
		gl.glPixelStorei(GL.GL_UNPACK_SKIP_IMAGES, region.getZOffset());
		
		gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, data.getSideLength(level));
		gl.glPixelStorei(GL.GL_UNPACK_IMAGE_HEIGHT, data.getSideLength(level));
		
		int target = getFaceTarget(face);
		
		if (in != null) {
			int pos = in.position();
			int lim = in.limit();
			in.clear();
			in.position(slice.getOffset());
			in.limit(slice.getOffset() + slice.getLength());
		
			this.setTexImage(this.reallocateOnUpdate(data.getSideLength(level), data.getSideLength(level), data.getNumMipmaps(), data.getDataFormat()), gl, 
							 target, level, region.getXOffset(), region.getYOffset(), region.getWidth(), region.getHeight(), 
							 r.srcFormat, r.dstFormat, r.dataType, data.getDataFormat(), in);
					
			in.limit(lim);
			in.position(pos);
		} else {
			this.setTexImage(this.reallocateOnUpdate(data.getSideLength(level), data.getSideLength(level), data.getNumMipmaps(), data.getDataFormat()), gl, 
					 target, level, region.getXOffset(), region.getYOffset(), region.getWidth(), region.getHeight(), 
					 r.srcFormat, r.dstFormat, r.dataType, data.getDataFormat(), data.getDataType().getByteSize() * slice.getOffset());
		}
		
		s.set(gl);
	}
}
