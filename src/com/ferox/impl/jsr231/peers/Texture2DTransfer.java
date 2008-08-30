package com.ferox.impl.jsr231.peers;

import java.nio.Buffer;

import javax.media.opengl.GL;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.states.atoms.Texture2D;
import com.ferox.core.states.atoms.TextureCubeMap.Face;
import com.ferox.core.states.atoms.TextureData.TextureFormat;
import com.ferox.core.states.atoms.TextureData.TextureType;
import com.ferox.core.util.TextureUtil;
import com.ferox.core.util.DataTransfer.Block;
import com.ferox.core.util.DataTransfer.Slice;
import com.ferox.impl.jsr231.peers.JOGLTextureDataPeer.TextureTransfer;

class Texture2DTransfer implements TextureTransfer<Texture2D> {
	public void submitData(TextureRecord t, Texture2D texture, GL gl) {
		int width = potCeil(texture.getWidth());
		int height = potCeil(texture.getHeight());
		int[] v = new int[1];
		gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, v, 0);
		int maxSide = v[0];
		
		if (((width != texture.getWidth() || height != texture.getHeight()) && !RenderManager.getSystemCapabilities().areNpotTexturesSupported())
			|| (texture.getWidth() > maxSide || texture.getHeight() > maxSide)) {
			// Rescale the texture because it's not npot or because it's too large (in which case, buffer allocation might
			// cause a out-of-memory error anyway).
			if (RenderManager.getSystemCapabilities().areNpotTexturesSupported()) {
				width = texture.getWidth();
				height = texture.getHeight();
			}
			
			width = Math.min(maxSide, width);
			height = Math.min(maxSide, height);
			
			if (texture.getMipmapLevel(0) != null) {
				Buffer[] newData = new Buffer[texture.getNumMipmaps()];
				
				int mW = width;
				int mH = height;
				for (int i = 0; i < newData.length; i++) {
					newData[i] = TextureUtil.scaleImage2D(texture.getMipmapLevel(i), texture.getDataFormat(), texture.getDataType(), texture.getWidth(), texture.getHeight(), mW, mH);
					mW = Math.max(1, mW >> 1);
					mH = Math.max(1, mH >> 1);
				}
				
				texture.setTextureData(newData, width, height);
			}
		} else {
			width = texture.getWidth();
			height = texture.getHeight();
		}
		
		for (int i = 0; i < texture.getNumMipmaps(); i++) {
			if (!reallocateOnUpdate(width, height, texture.getDataFormat()) || texture.getMipmapLevel(i) == null)
				this.setTexImage(true, gl, t.target, i, 0, 0, width, height, t.srcFormat, t.dstFormat, t.dataType, texture.getDataFormat(), null);
			
			width = Math.max(1, (width >> 1));
			height = Math.max(1, (height >> 1));
		}
		this.updateData(t, texture, gl);
	}

	private static int potCeil(int num) {
		int pot = 1;
		while (pot < num)
			pot *= 2;
		return pot;
	}
	
	public void updateData(TextureRecord t, Texture2D texture, GL gl) {		
		Buffer data = null;
		int width = texture.getWidth();
		int height = texture.getHeight();
				
		for (int i = 0; i < texture.getNumMipmaps(); i++) {
			data = texture.getMipmapLevel(i);
			
			if (data != null)
				this.setTexImage(this.reallocateOnUpdate(width, height, texture.getDataFormat()), gl, t.target, i, 0, 0, width, height, t.srcFormat, t.dstFormat, t.dataType, texture.getDataFormat(), data.clear());
			
			width = Math.max(1, (width >> 1));
			height = Math.max(1, (height >> 1));
		}
	}
	
	private void setTexImage(boolean realloc, GL gl, int target, int level, int xOff, int yOff, int width, int height, int srcFormat, int dstFormat, int type, TextureFormat rFormat, Buffer data) {
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

	private boolean reallocateOnUpdate(int width, int height, TextureFormat format) {
		if (format.isClientCompressed())
			return true;
		else if (format == TextureFormat.DEPTH)
			return true;
		return false;
	}

	public void copyData(TextureRecord r, Texture2D data, Block region, Face face, int level, int sx, int sy, GL gl) {
		gl.glCopyTexSubImage2D(r.target, level, region.getXOffset(), region.getYOffset(), sx, sy, region.getWidth(), region.getHeight());
	}

	public void getData(TextureRecord r, Texture2D data, Face face, int level, Buffer out, Slice slice, GL gl) {
		PackPixelStore s = PackPixelStore.get(gl);
		PackPixelStore.setUseful(gl);
			
		if (out != null) {
			int pos = out.position();
			int lim = out.limit();
			out.clear();
			out.position(slice.getOffset());
			out.limit(slice.getOffset() + slice.getLength());
		
			if (!data.getDataFormat().isServerCompressed())
				gl.glGetTexImage(r.target, level, r.srcFormat, r.dataType, out);
			else
				gl.glGetCompressedTexImage(r.target, level, out);
			
			out.limit(lim);
			out.position(pos);
		} else {
			if (!data.getDataFormat().isServerCompressed())
				gl.glGetTexImage(r.target, level, r.srcFormat, r.dataType, slice.getOffset() * data.getDataType().getByteSize());
			else
				gl.glGetCompressedTexImage(r.target, level, slice.getOffset() * data.getDataType().getByteSize());
		}
		
		s.set(gl);
	}

	public void setData(TextureRecord r, Texture2D data, Block region, Face face, int level, Buffer in, Slice slice, GL gl) {
		UnpackPixelStore s = UnpackPixelStore.get(gl);
		UnpackPixelStore.setUseful(gl);
		
		gl.glPixelStorei(GL.GL_UNPACK_SKIP_PIXELS, region.getXOffset());
		gl.glPixelStorei(GL.GL_UNPACK_SKIP_ROWS, region.getYOffset());
		gl.glPixelStorei(GL.GL_UNPACK_SKIP_IMAGES, region.getZOffset());
		
		gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, data.getWidth(level));
		gl.glPixelStorei(GL.GL_UNPACK_IMAGE_HEIGHT, data.getHeight(level));
		
		if (in != null) {
			int pos = in.position();
			int lim = in.limit();
			in.clear();
			in.position(slice.getOffset());
			in.limit(slice.getOffset() + slice.getLength());
		
			this.setTexImage(this.reallocateOnUpdate(data.getWidth(level), data.getHeight(level), data.getDataFormat()), gl, 
							 r.target, level, region.getXOffset(), region.getYOffset(), region.getWidth(), region.getHeight(), 
							 r.srcFormat, r.dstFormat, r.dataType, data.getDataFormat(), in);
					
			in.limit(lim);
			in.position(pos);
		} else {
			this.setTexImage(this.reallocateOnUpdate(data.getWidth(level), data.getHeight(level), data.getDataFormat()), gl, 
					 r.target, level, region.getXOffset(), region.getYOffset(), region.getWidth(), region.getHeight(), 
					 r.srcFormat, r.dstFormat, r.dataType, data.getDataFormat(), data.getDataType().getByteSize() * slice.getOffset());
		}
		
		s.set(gl);
	}
}
