package com.ferox.renderer.impl.jogl.resource;

import java.nio.Buffer;

import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.jogl.Utils;
import com.ferox.resource.BufferData;
import com.ferox.resource.ImageRegion;
import com.ferox.resource.TextureCubeMap;
import com.ferox.resource.TextureCubeMapDirtyState;

public class JoglTextureCubeMapResourceDriver extends AbstractTextureImageDriver<TextureCubeMap, TextureCubeMapDirtyState> {
	private final boolean hasS3tcCompression;
	
	public JoglTextureCubeMapResourceDriver(RenderCapabilities caps) {
		super(caps);
		hasS3tcCompression = caps.getS3TextureCompression();
	}

	@Override
	protected BufferData getData(TextureCubeMap tex, int layer, int mipmap) {
		return tex.getData(layer, mipmap);
	}

	@Override
	protected ImageRegion getDirtyRegion(TextureCubeMapDirtyState dirtyState, int layer, int mipmap) {
		return dirtyState.getDirtyMipmap(layer, mipmap);
	}

	@Override
	protected String getNotSupported(TextureCubeMap texture) {
		if (texture.getFormat().isCompressed() && !hasS3tcCompression)
			return "DXT_n TextureFormats aren't supported on this hardware";
		return null;
	}

	@Override
	protected int getNumLayers(TextureCubeMap tex) {
		return 6; // one for each cube face
	}

	@Override
	protected boolean getParametersDirty(TextureCubeMapDirtyState dirtyState) {
		return dirtyState.getTextureParametersDirty();
	}

	@Override
	protected void glTexImage(GL2GL3 gl, TextureHandle h, int layer, int mipmap, 
							  int width, int height, int depth, int capacity, Buffer data) {
		if (h.glSrcFormat > 0)
			gl.glTexImage2D(Utils.getGLCubeFace(layer), mipmap, h.glDstFormat, width, width, 0, 
							h.glSrcFormat, h.glType, data);
		else
			gl.glCompressedTexImage2D(Utils.getGLCubeFace(layer), mipmap, h.glDstFormat, 
									  width, width, 0, capacity, data);
	}

	@Override
	protected void glTexSubImage(GL2GL3 gl, TextureHandle h, int layer, int mipmap, int x, int y, int z, 
								 int width, int height, int depth, Buffer data) {
		gl.glTexSubImage2D(Utils.getGLCubeFace(layer), mipmap, x, y, width, height, 
			   			   h.glSrcFormat, h.glType, data);
	}
}
