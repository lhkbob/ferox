package com.ferox.impl.jsr231.peers;

import java.nio.Buffer;

import javax.media.opengl.GL;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.states.atoms.Texture3D;
import com.ferox.core.states.atoms.TextureCubeMap.Face;
import com.ferox.core.util.TextureUtil;
import com.ferox.core.util.DataTransfer.Block;
import com.ferox.core.util.DataTransfer.Slice;
import com.ferox.impl.jsr231.peers.JOGLTextureDataPeer.TextureTransfer;

public class Texture3DTransfer implements TextureTransfer<Texture3D> {
	public void submitData(TextureRecord t, Texture3D texture, GL gl) {
		int width = potCeil(texture.getWidth());
		int height = potCeil(texture.getHeight());
		int depth = potCeil(texture.getDepth());
		int[] v = new int[1];
		gl.glGetIntegerv(GL.GL_MAX_3D_TEXTURE_SIZE, v, 0);
		int maxSide = v[0];
		
		if (((width != texture.getWidth() || height != texture.getHeight() || depth != texture.getDepth()) && !RenderManager.getSystemCapabilities().areNpotTexturesSupported())
			|| (texture.getWidth() > maxSide || texture.getHeight() > maxSide || texture.getDepth() > maxSide)) {
			// Rescale the texture because it's not npot or because it's too large (in which case, buffer allocation might
			// cause a out-of-memory error anyway).
			if (RenderManager.getSystemCapabilities().areNpotTexturesSupported()) {
				width = texture.getWidth();
				height = texture.getHeight();
				depth = texture.getDepth();
			}
			
			width = Math.min(maxSide, width);
			height = Math.min(maxSide, height);
			depth = Math.min(maxSide, depth);
			
			if (texture.getMipmapLevel(0) != null) {
				Buffer[] newData = new Buffer[texture.getNumMipmaps()];
				
				int mW = width;
				int mH = height;
				int mD = depth;
				for (int i = 0; i < newData.length; i++) {
					newData[i] = TextureUtil.scaleImage3D(texture.getMipmapLevel(i), texture.getDataFormat(), texture.getDataType(), texture.getWidth(), texture.getHeight(), texture.getDepth(), mW, mH, mD);
					mW = Math.max(1, mW >> 1);
					mH = Math.max(1, mH >> 1);
					mD = Math.max(1, mD >> 1);
				}
				
				texture.setTextureData(newData, width, height, depth);
			}
		} else {
			width = texture.getWidth();
			height = texture.getHeight();
		}
		
		for (int i = 0; i < texture.getNumMipmaps(); i++) {	
			gl.glTexImage3D(t.target, i, t.dstFormat, width, height, depth, 0, t.srcFormat, t.dataType, null);
			width = Math.max(1, (width >> 1));
			height = Math.max(1, (height >> 1));
			depth = Math.max(1, (depth >> 1));
		}
		
		this.updateData(t, texture, gl);
	}

	private static int potCeil(int num) {
		int pot = 1;
		while (pot < num)
			pot *= 2;
		return pot;
	}
	
	public void updateData(TextureRecord t, Texture3D texture, GL gl) {
		Buffer data = null;
		int width = texture.getWidth();
		int height = texture.getHeight();
		int depth = texture.getDepth();
			
		for (int i = 0; i < texture.getNumMipmaps(); i++) {
			data = texture.getMipmapLevel(i);
			if (data != null) 
				gl.glTexSubImage3D(t.target, i, 0, 0, 0, width, height, depth, t.srcFormat, t.dataType, data.clear());
			width = Math.max(1, (width >> 1));
			height = Math.max(1, (height >> 1));
			depth = Math.max(1, (depth >> 1));
		}
	}

	public void copyData(TextureRecord t, Texture3D data, Block region,	Face face, int level, int sx, int sy, GL gl) {
		gl.glCopyTexSubImage3D(t.target, level, region.getXOffset(), region.getYOffset(), region.getZOffset(), sx, sy, region.getWidth(), region.getHeight());
	}

	public void getData(TextureRecord t, Texture3D data, Face face, int level, Buffer out, Slice slice, GL gl) {
		PackPixelStore s = PackPixelStore.get(gl);
		PackPixelStore.setUseful(gl);
			
		if (out != null) {
			int pos = out.position();
			int lim = out.limit();
			out.clear();
			out.position(slice.getOffset());
			out.limit(slice.getOffset() + slice.getLength());
		
			gl.glGetTexImage(GL.GL_TEXTURE_3D, level, t.srcFormat, t.dataType, out);
				
			out.limit(lim);
			out.position(pos);
		} else {
			gl.glGetTexImage(GL.GL_TEXTURE_3D, level, t.srcFormat, t.dataType, slice.getOffset() * data.getDataType().getByteSize());
		}
		
		s.set(gl);
	}

	public void setData(TextureRecord t, Texture3D data, Block region, Face face, int level, Buffer in, Slice slice, GL gl) {
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
		
			gl.glTexSubImage3D(t.target, level, region.getXOffset(), region.getYOffset(), region.getZOffset(), region.getWidth(), region.getHeight(), region.getDepth(), t.dstFormat, t.dataType, in);
			
			in.limit(lim);
			in.position(pos);
		} else {
			gl.glTexSubImage3D(t.target, level, region.getXOffset(), region.getYOffset(), region.getZOffset(), region.getWidth(), region.getHeight(), region.getDepth(), t.dstFormat, t.dataType, slice.getOffset() * data.getDataType().getByteSize());
		}
		
		s.set(gl);
	}		
}
