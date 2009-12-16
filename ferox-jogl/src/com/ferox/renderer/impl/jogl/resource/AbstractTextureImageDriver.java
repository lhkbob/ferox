package com.ferox.renderer.impl.jogl.resource;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.renderer.impl.jogl.BoundObjectState;
import com.ferox.renderer.impl.jogl.JoglContext;
import com.ferox.renderer.impl.jogl.Utils;
import com.ferox.resource.BufferData;
import com.ferox.resource.DirtyState;
import com.ferox.resource.ImageRegion;
import com.ferox.resource.Resource;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.TextureImage;
import com.ferox.resource.Resource.Status;
import com.ferox.util.texture.converter.TextureConverter;

public abstract class AbstractTextureImageDriver<T extends TextureImage, D extends DirtyState<D>> implements ResourceDriver {
	// extensions present
	private final boolean npotSupported; 
	private final boolean floatSupported; 
	
	// maximum allowed dimensions
	private final int maxRectSize;
	private final int maxCubeSize;
	private final int max3dSize;
	private final int maxTexSize;

	private final float maxAnisoLevel;

	public AbstractTextureImageDriver(RenderCapabilities caps) {
		npotSupported = caps.getNpotTextureSupport();
		floatSupported = caps.getUnclampedFloatTextureSupport();

		maxRectSize = caps.getMaxTextureRectangleSize();
		maxCubeSize = caps.getMaxTextureCubeMapSize();
		max3dSize = caps.getMaxTexture3DSize();
		maxTexSize = caps.getMaxTextureSize();

		maxAnisoLevel = caps.getMaxAnisotropicLevel();
	}
	
	@Override
	public void dispose(ResourceHandle handle) {
		if (handle.getStatus() == Status.READY)
			destroyTexture(JoglContext.getCurrent().getGL(), (TextureHandle) handle);
	}

	@Override
	@SuppressWarnings("unchecked")
	public ResourceHandle init(Resource res) {
		GL2GL3 gl = JoglContext.getCurrent().getGL();
		
		T t = (T) res;
		TextureHandle handle = createNewTexture(gl, t);
		
		// make a new texture
		String notSupported = getNotSupported(t);
		if (notSupported != null) {
			// can't make the texture
			destroyTexture(gl, handle);
			
			handle.setStatus(Status.ERROR);
			handle.setStatusMessage(notSupported);
			return handle; // don't allocate any texture images
		}

		handle.setStatus(Status.READY);
		if (isDirty(t, handle))
			handle.setStatusMessage(getDirtyStatusMessage(t, handle));
		else
			handle.setStatusMessage("");

		// use update to allocate the textures
		update(gl, t, handle, null, true);
		return handle;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Status update(Resource res, ResourceHandle handle, DirtyState<?> dirtyState) {
		if (handle.getStatus() == Status.READY) {
			GL2GL3 gl = JoglContext.getCurrent().getGL();
			update(gl, (T) res, (TextureHandle) handle, (D) dirtyState, false);
		}
		return handle.getStatus();
	}

	/**
	 * Return a String representing the status message if the given texture is
	 * unsupported. If texture is supported, return null.
	 */
	protected abstract String getNotSupported(T texture);

	/**
	 * Return true if texture parameters are dirty, according to the given dirty
	 * state.  It can be assumed dirtyState is not null.
	 */
	protected abstract boolean getParametersDirty(D dirtyState);

	/**
	 * Return the ImageRegion that's associated with the given layer and mipmap
	 * for the dirtyState. If the layer and mipmap aren't dirty, then return
	 * null. It can be assumed that dirtyState is non-null, and that layer and
	 * mipmap are valid.
	 */
	protected abstract ImageRegion getDirtyRegion(D dirtyState, int layer, int mipmap);

	/**
	 * Return the number of layers that the texture has. Each layer is an
	 * independent source of mipmaps (such as each face of a cubemap).
	 */
	protected abstract int getNumLayers(T tex);

	/**
	 * Return the texture's BufferData for the given layer and mipmap. It can be
	 * assumed that layer and mipmap are valid.
	 */
	protected abstract BufferData getData(T tex, int layer, int mipmap);

	/**
	 * Execute the appropriate glTexImage(1D/2D/3D) command on the GL instance,
	 * with the given values. If h.glSrcFormat < 0, use
	 * glCompressedTexImage(1D/2D/3D) instead.
	 */
	protected abstract void glTexImage(GL2GL3 gl, TextureHandle h, int layer, int mipmap, int width, int height, int depth, int capacity, Buffer data);

	/**
	 * Execute the appropriate glTexSubImage(1D/2D/3D) command on the GL
	 * instance, with the given values. It is not necessary to use
	 * glCompressedTexSubImage().
	 */
	protected abstract void glTexSubImage(GL2GL3 gl, TextureHandle h, int layer, int mipmap, int x, int y, int z, int width, int height, int depth, Buffer data);
	
	// update the given texture's parameters and images as needed based off of dirtyState and newTex
	private void update(GL2GL3 gl, T tex, TextureHandle handle, D dirtyState, boolean newTex) {
		BoundObjectState record = JoglContext.getCurrent().getRecord();
		int aTex = record.getActiveTexture();
		
		int oldId = (record.getTextureTarget(aTex) == handle.glTarget ? record.getTexture(aTex) : 0);
		gl.glBindTexture(handle.glTarget, handle.getId());
		if (dirtyState == null || getParametersDirty(dirtyState))
			setTextureParameters(gl, handle, tex, dirtyState == null);
		
		TextureFormat f = tex.getFormat();
		boolean rescale = tex.getWidth(0) != handle.width || tex.getHeight(0) != handle.height || tex.getDepth(0) != handle.depth;
		if (newTex || rescale || f.isCompressed() || f == TextureFormat.DEPTH)
			doTexImage(gl, tex, handle, newTex);
		else
			doTexSubImage(gl, tex, handle, dirtyState);
		
		gl.glBindTexture(handle.glTarget, oldId);
	}
	
	// iterate through all layers and mipmaps and invoke glTexImage on each combinatio
	private void doTexImage(GL2GL3 gl,  T tex, TextureHandle handle, boolean newTex) {
		boolean needsResize = handle.width != tex.getWidth(0) || handle.height != tex.getHeight(0) || handle.depth != tex.getDepth(0);
		int numLayers = getNumLayers(tex);
		
		int w, h, d;
		BufferData bd;
		for (int i = 0; i < handle.numMipmaps; i++) {
			// actual level's dimensions
			w = Math.max(1, handle.width >> i);
			h = Math.max(1, handle.height >> i);
			d = Math.max(1, handle.depth >> i);
			
			for (int l = 0; l < numLayers; l++) {
				bd = getData(tex, l, i);
				if (bd != null && bd.getData() != null) {
					if (needsResize) {
						// resize the image to meet POT requirements
						bd = TextureConverter.convert(bd, tex.getFormat(), tex.getWidth(i), tex.getHeight(i), tex.getDepth(i), 
													  null, tex.getFormat(), bd.getType(), w, h, d);
					}
					// proceed with image allocation
					setUnpackRegion(gl, 0, 0, 0, w, h);
					glTexImage(gl, handle, l, i, w, h, d, bd.getCapacity(), wrap(bd));
				} else if (newTex) {
					// allocate empty image
					glTexImage(gl, handle, l, i, w, h, d, tex.getFormat().getBufferSize(w, h, d), null);
				}
			}
		}
	}
	
	// iterate through all layers and mipmaps and invoke glTexSubImage on each dirty layer/mipmap pair
	private void doTexSubImage(GL2GL3 gl, T tex, TextureHandle handle, D dirty) {
		int numLayers = getNumLayers(tex);
		int w, h, d;
		ImageRegion mdr;
		BufferData bd;
		
		for (int i = 0; i < handle.numMipmaps; i++) {
			w = Math.max(1, handle.width >> i);
			h = Math.max(1, handle.height >> i);
			d = Math.max(1, handle.depth >> i);
			
			for (int l = 0; l < numLayers; l++) {
				bd = getData(tex, l, i);
				if (bd != null && bd.getData() != null) {
					mdr = (dirty == null ? null : getDirtyRegion(dirty, l, i));
					if (mdr != null) {
						// use the region descriptor
						setUnpackRegion(gl, mdr.getXOffset(), mdr.getYOffset(), mdr.getZOffset(), w, h);
						glTexSubImage(gl, handle, l, i, mdr.getXOffset(), mdr.getYOffset(), mdr.getZOffset(), 
									  mdr.getWidth(), mdr.getHeight(), mdr.getDepth(), wrap(bd));
					} else if (dirty == null) {
						// update the whole image
						setUnpackRegion(gl, 0, 0, 0, w, h);
						glTexSubImage(gl, handle, l, i, 0, 0, 0, w, h, d, wrap(bd));
					}
				}
			}
		}
	}

	// modify the pack record so that gl unpacks the correct region
	private void setUnpackRegion(GL gl, int xOffset, int yOffset, int zOffset, 
								 int blockWidth, int blockHeight) {
		// skip pixels
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, xOffset);
		// skip rows
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, yOffset);
		// skip images
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_IMAGES, zOffset);

		// width of whole face
		gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, blockWidth);
		// height of whole face
		gl.glPixelStorei(GL2.GL_UNPACK_IMAGE_HEIGHT, blockHeight);

		// configure the alignment
		gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
	}

	// update all of the texture parameters for ti
	private void setTextureParameters(GL gl, TextureHandle handle, 
									  TextureImage ti, boolean forceAll) {
		handle.setFilter(gl, ti.getFilter(), forceAll);

		handle.setWrapS(gl, ti.getWrapS(), forceAll);
		handle.setWrapT(gl, ti.getWrapT(), forceAll);
		handle.setWrapR(gl, ti.getWrapR(), forceAll);

		handle.setDepthTest(gl, ti.getDepthCompareTest(), forceAll);
		handle.setDepthMode(gl, ti.getDepthMode(), forceAll);
		handle.setDepthCompareEnabled(gl, ti.isDepthCompareEnabled(), forceAll);

		handle.setAnisotropicLevel(gl, ti.getAnisotropicFiltering(), maxAnisoLevel, forceAll);
	}

	// create a new texture handle that's sized/formatted for ti
	private TextureHandle createNewTexture(GL gl, TextureImage ti) {
		// determine the valid dimensions - based on hardware constraints and extensions
		int width = ti.getWidth(0);
		int height = ti.getHeight(0);
		int depth = ti.getDepth(0);

		if (!npotSupported) {
			// we don't need to scale T_RECT, though
			switch (ti.getTarget()) {
			case T_3D:
				depth = ceilPot(depth);
			case T_2D:
				height = ceilPot(height);
			case T_1D:
			case T_CUBEMAP:
				width = ceilPot(width);
			}
		}

		int maxSize = 0;
		switch (ti.getTarget()) {
		case T_1D:
		case T_2D:
			maxSize = maxTexSize;
			break;
		case T_3D:
			maxSize = max3dSize;
			break;
		case T_CUBEMAP:
			maxSize = maxCubeSize;
			break;
		case T_RECT:
			maxSize = maxRectSize;
			break;
		}

		width = Math.min(width, maxSize);
		height = Math.min(height, maxSize);
		depth = Math.min(depth, maxSize);

		// pick glTarget
		int glTarget = Utils.getGLTextureTarget(ti.getTarget());

		// choose src/dst formats and type
		TextureFormat format = ti.getFormat();
		if (!floatSupported) {
			// our best bet is to just clamp the float values
			switch (format) {
			case ALPHA_FLOAT:
				format = TextureFormat.ALPHA;
			case LUMINANCE_ALPHA_FLOAT:
				format = TextureFormat.LUMINANCE_ALPHA;
			case LUMINANCE_FLOAT:
				format = TextureFormat.LUMINANCE;
			case RGB_FLOAT:
				format = TextureFormat.RGB;
			case RGBA_FLOAT:
				format = TextureFormat.RGBA;
			}
		}

		int glSrcFormat = Utils.getGLSrcFormat(format);
		int glDstFormat = Utils.getGLDstFormat(format, ti.getType());
		int glType = (format.isPackedFormat() ? Utils.getGLPackedType(format) 
											  : Utils.getGLType(ti.getType()));

		// generate the new texture's id
		int[] id = new int[1];
		gl.glGenTextures(1, id, 0);

		return new TextureHandle(id[0], glTarget, glType, glSrcFormat, glDstFormat, 
								 width, height, depth, ti.getNumMipmaps());
	}

	// return true if the texture was modified to meet hardware reqs
	private boolean isDirty(TextureImage image, TextureHandle handle) {
		int expectedDst = Utils.getGLDstFormat(image.getFormat(), image.getType());
		return image.getWidth(0) != handle.width || image.getHeight(0) != handle.height || 
			   image.getDepth(0) != handle.depth || expectedDst != handle.glDstFormat;
	}

	// return a string detailing what was changed
	private String getDirtyStatusMessage(TextureImage image, TextureHandle handle) {
		int expectedDst = Utils.getGLDstFormat(image.getFormat(), image.getType());
		String msg = "";
		if (expectedDst != handle.glDstFormat)
			msg += "TextureFormat was changed to meet hardware support";
		if (image.getWidth(0) != handle.width || image.getHeight(0) != handle.height 
			|| image.getDepth(0) != handle.depth)
			msg += "Texture dimensions scaled to power-of-two sizes";

		return msg;
	}

	// delete the given texture object
	private void destroyTexture(GL2ES2 gl, TextureHandle handle) {
		gl.glDeleteTextures(1, new int[] { handle.getId() }, 0);
	}

	// assumes that data is not null
	private Buffer wrap(BufferData data) {
		Object array = data.getData();

		switch (data.getType()) {
		case BYTE:
		case UNSIGNED_BYTE:
			return ByteBuffer.wrap((byte[]) array);
		case INT:
		case UNSIGNED_INT:
			return IntBuffer.wrap((int[]) array);
		case SHORT:
		case UNSIGNED_SHORT:
			return ShortBuffer.wrap((short[]) array);
		case FLOAT:
			return FloatBuffer.wrap((float[]) array);
		}
		// shouldn't happen
		return null;
	}

	// Return smallest POT >= num
	private static int ceilPot(int num) {
		int pot = 1;
		while (pot < num)
			pot = pot << 1;
		return pot;
	}
}
