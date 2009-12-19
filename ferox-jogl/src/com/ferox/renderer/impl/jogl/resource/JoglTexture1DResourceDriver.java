package com.ferox.renderer.impl.jogl.resource;

import java.nio.Buffer;

import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.resource.BufferData;
import com.ferox.resource.ImageRegion;
import com.ferox.resource.Texture1D;
import com.ferox.resource.TextureDirtyState;

/**
 * A concrete subclass of {@link AbstractTextureImageDriver} that supports
 * Texture1D images.
 * 
 * @author Michael Ludwig
 */
public class JoglTexture1DResourceDriver extends AbstractTextureImageDriver<Texture1D, TextureDirtyState> {
	public JoglTexture1DResourceDriver(RenderCapabilities caps) {
		super(caps);
	}
	
	@Override
	protected BufferData getData(Texture1D tex, int layer, int mipmap) {
		return tex.getData(mipmap);
	}

	@Override
	protected ImageRegion getDirtyRegion(TextureDirtyState dirtyState, int layer, int mipmap) {
		return dirtyState.getDirtyMipmap(mipmap);
	}

	@Override
	protected String getNotSupported(Texture1D texture) {
		// a Texture1D should always be supported
		return null;
	}

	@Override
	protected int getNumLayers(Texture1D tex) {
		return 1;
	}

	@Override
	protected boolean getParametersDirty(TextureDirtyState dirtyState) {
		return dirtyState.getTextureParametersDirty();
	}

	@Override
	protected void glTexImage(GL2GL3 gl, TextureHandle h, int layer, int mipmap, 
							  int width, int height, int depth, int capacity, Buffer data) {
		// there is no compressed 1d command
		gl.glTexImage1D(h.glTarget, mipmap, h.glDstFormat, width, 0, 
						h.glSrcFormat, h.glType, data);
	}

	@Override
	protected void glTexSubImage(GL2GL3 gl, TextureHandle h, int layer, int mipmap, int x, int y, int z,
								 int width, int height, int depth, Buffer data) {
		gl.glTexSubImage1D(h.glTarget, mipmap, x, width, h.glSrcFormat, h.glType, data);
	}
}
