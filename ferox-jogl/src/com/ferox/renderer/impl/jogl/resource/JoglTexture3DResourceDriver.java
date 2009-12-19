package com.ferox.renderer.impl.jogl.resource;

import java.nio.Buffer;

import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.resource.BufferData;
import com.ferox.resource.ImageRegion;
import com.ferox.resource.Texture3D;
import com.ferox.resource.TextureDirtyState;

/**
 * A concrete subclass of {@link AbstractTextureImageDriver} that supports
 * Texture3D images.
 * 
 * @author Michael Ludwig
 */
public class JoglTexture3DResourceDriver extends AbstractTextureImageDriver<Texture3D, TextureDirtyState> {
	public JoglTexture3DResourceDriver(RenderCapabilities caps) {
		super(caps);
	}
	
	@Override
	protected BufferData getData(Texture3D tex, int layer, int mipmap) {
		return tex.getData(mipmap);
	}

	@Override
	protected ImageRegion getDirtyRegion(TextureDirtyState dirtyState, int layer, int mipmap) {
		return dirtyState.getDirtyMipmap(mipmap);
	}

	@Override
	protected String getNotSupported(Texture3D texture) {
		// Texture3D should always be supported
		return null;
	}

	@Override
	protected int getNumLayers(Texture3D tex) {
		return 1;
	}

	@Override
	protected boolean getParametersDirty(TextureDirtyState dirtyState) {
		return dirtyState.getTextureParametersDirty();
	}

	@Override
	protected void glTexImage(GL2GL3 gl, TextureHandle h, int layer, int mipmap, 
							  int width, int height, int depth, int capacity, Buffer data) {
		gl.glTexImage3D(h.glTarget, mipmap, h.glDstFormat, width, height, depth, 0, 
						h.glSrcFormat, h.glType, data);
	}

	@Override
	protected void glTexSubImage(GL2GL3 gl, TextureHandle h, int layer, int mipmap, int x, int y, int z,
								 int width, int height, int depth, Buffer data) {
		gl.glTexSubImage3D(h.glTarget, mipmap, x, y, z, width, height, depth, h.glSrcFormat, h.glType, data);
	}
}
