package com.ferox.renderer.impl.jogl.resource;

import java.nio.Buffer;

import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.resource.BufferData;
import com.ferox.resource.ImageRegion;
import com.ferox.resource.Texture2D;
import com.ferox.resource.TextureDirtyState;

public class JoglTexture2DResourceDriver extends AbstractTextureImageDriver<Texture2D, TextureDirtyState> {
	private final boolean hasS3tcCompression;
	
	public JoglTexture2DResourceDriver(RenderCapabilities caps) {
		super(caps);
		hasS3tcCompression = caps.getS3TextureCompression();
	}

	@Override
	protected BufferData getData(Texture2D tex, int layer, int mipmap) {
		return tex.getData(mipmap);
	}

	@Override
	protected ImageRegion getDirtyRegion(TextureDirtyState dirtyState, int layer, int mipmap) {
		return dirtyState.getDirtyMipmap(mipmap);
	}

	@Override
	protected String getNotSupported(Texture2D texture) {
		if (texture.getFormat().isCompressed() && !hasS3tcCompression)
			return "DXT_n TextureFormats aren't supported on this hardware";
		return null;
	}

	@Override
	protected int getNumLayers(Texture2D tex) {
		return 1;
	}

	@Override
	protected boolean getParametersDirty(TextureDirtyState dirtyState) {
		return dirtyState.getTextureParametersDirty();
	}

	@Override
	protected void glTexImage(GL2GL3 gl, TextureHandle h, int layer, int mipmap, 
							  int width, int height, int depth, int capacity, Buffer data) {
		if (h.glSrcFormat > 0)
			gl.glTexImage2D(h.glTarget, mipmap, h.glDstFormat, width, height, 0, 
							h.glSrcFormat, h.glType, data);
		else
			gl.glCompressedTexImage2D(h.glTarget, mipmap, h.glDstFormat, width, height, 0, 
									  capacity, data);
	}

	@Override
	protected void glTexSubImage(GL2GL3 gl, TextureHandle h, int layer, int mipmap, int x, int y, int z,
								 int width, int height, int depth, Buffer data) {
		gl.glTexSubImage2D(h.glTarget, mipmap, x, y, width, height, h.glSrcFormat, h.glType, data);
	}
}
