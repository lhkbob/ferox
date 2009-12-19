package com.ferox.renderer.impl.jogl.resource;

import java.nio.Buffer;

import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.resource.BufferData;
import com.ferox.resource.ImageRegion;
import com.ferox.resource.TextureRectangle;
import com.ferox.resource.TextureRectangleDirtyState;

/**
 * A concrete subclass of {@link AbstractTextureImageDriver} that supports
 * TextureRectangle images.
 * 
 * @author Michael Ludwig
 */
public class JoglTextureRectangleResourceDriver extends AbstractTextureImageDriver<TextureRectangle, TextureRectangleDirtyState> {
	private final boolean hasS3tcCompression;
	private final boolean hasRectSupport;
	
	public JoglTextureRectangleResourceDriver(RenderCapabilities caps) {
		super(caps);
		hasS3tcCompression = caps.getS3TextureCompression();
		hasRectSupport = caps.getRectangularTextureSupport();
	}

	@Override
	protected BufferData getData(TextureRectangle tex, int layer, int mipmap) {
		return tex.getData();
	}

	@Override
	protected ImageRegion getDirtyRegion(TextureRectangleDirtyState dirtyState, int layer, int mipmap) {
		return dirtyState.getDirtyRegion();
	}

	@Override
	protected String getNotSupported(TextureRectangle texture) {
		if (!hasRectSupport) {
			// rectangle textures aren't supported
			return "TextureRectangles aren't supported on this hardware";
		}
		if (texture.getFormat().isCompressed() && !hasS3tcCompression) {
			// can't make compressed images
			return "DXT_n TextureFormats aren't supported on this hardware";
		}
		
		return null;
	}

	@Override
	protected int getNumLayers(TextureRectangle tex) {
		return 1;
	}

	@Override
	protected boolean getParametersDirty(TextureRectangleDirtyState dirtyState) {
		return dirtyState.getTextureParametersDirty();
	}

	@Override
	protected void glTexImage(GL2GL3 gl, TextureHandle h, int layer, int mipmap, 
							  int width, int height, int depth, int capacity, Buffer data) {
		if (h.glSrcFormat > 0)
			gl.glTexImage2D(h.glTarget, 0, h.glDstFormat, h.width, h.height, 0, 
							h.glSrcFormat, h.glType, data);
		else
			gl.glCompressedTexImage2D(h.glTarget, 0, h.glDstFormat, h.width, h.height, 0, 
									  capacity, data);
	}

	@Override
	protected void glTexSubImage(GL2GL3 gl, TextureHandle h, int layer, int mipmap, int x, int y, int z, 
								 int width, int height, int depth, Buffer data) {
		gl.glTexSubImage2D(h.glTarget, 0, x, y, width, height, h.glSrcFormat, h.glType, data);
	}
}
