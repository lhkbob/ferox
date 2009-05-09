package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceDriver;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PackUnpackRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord.TextureUnit;
import com.ferox.resource.BufferData;
import com.ferox.resource.Resource;
import com.ferox.resource.Texture2D;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Texture2D.Texture2DDirtyDescriptor;
import com.ferox.resource.TextureImage.MipmapDirtyRegion;
import com.ferox.util.texture.converter.TextureConverter;

/**
 * JoglTexture2DResourceDriver provides the functionality to load and delete
 * Texture2D instances in the graphics card. It will re-scale npot textures if
 * the card doesn't have npot support.
 * 
 * A Texture2D will only have an ERROR status if it is compressed and the DXT_n
 * compressions aren't supported. It will be DIRTY if it was an unclamped float
 * format and they're not supported, or if an NPOT texture had to be resized.
 * 
 * @author Michael Ludwig
 * 
 */
public class JoglTexture2DResourceDriver implements ResourceDriver {
	private final JoglContextManager factory;
	private final TextureImageDriver imageDriver;
	private final boolean hasS3tcCompression;

	public JoglTexture2DResourceDriver(JoglContextManager factory) {
		RenderCapabilities caps = factory.getRenderer().getCapabilities();

		this.factory = factory;
		imageDriver = new TextureImageDriver(caps);
		hasS3tcCompression = caps.getS3TextureCompression();
	}

	@Override
	public void cleanUp(Resource resource, ResourceData data) {
		GL gl = factory.getGL();
		TextureHandle handle = (TextureHandle) data.getHandle();

		if (handle != null)
			imageDriver.destroyTexture(gl, handle);
	}

	@Override
	public void update(Resource resource, ResourceData data, boolean fullUpdate) {
		JoglStateRecord sr = factory.getRecord();

		GL gl = factory.getGL();
		PackUnpackRecord pr = sr.packRecord;
		TextureRecord tr = sr.textureRecord;

		if (data.getStatus() != Status.ERROR) {
			// only do an update if we haven't got an error
			TextureHandle handle = (TextureHandle) data.getHandle();
			Texture2D t2d = (Texture2D) resource;
			boolean newTex = false;

			if (handle == null) {
				// make a new texture
				if (t2d.getFormat().isCompressed() && !hasS3tcCompression) {
					// can't make the texture
					data.setStatus(Status.ERROR);
					data
							.setStatusMessage("DXT_n TextureFormats aren't supported on this hardware");
					return; // abort the update
				}

				// ok to get the handle now
				handle = imageDriver.createNewTexture(gl, t2d);

				data.setHandle(handle);
				if (imageDriver.isDirty(t2d, handle)) {
					data.setStatus(Status.DIRTY);
					data.setStatusMessage(imageDriver.getDirtyStatusMessage(
							t2d, handle));
				} else {
					data.setStatus(Status.OK);
					data.setStatusMessage("");
				}

				newTex = true; // must set this to force a full update
				// down-the-line
			}

			Texture2DDirtyDescriptor dirty = (Texture2DDirtyDescriptor) t2d
					.getDirtyDescriptor();
			if (newTex || fullUpdate || dirty.areMipmapsDirty()
					|| dirty.isAnisotropicFilteringDirty()
					|| dirty.isDepthCompareDirty() || dirty.isFilterDirty()
					|| dirty.isTextureWrapDirty()) {
				// we must actually update the texture
				gl.glBindTexture(handle.glTarget, handle.id);

				imageDriver.setTextureParameters(gl, handle, t2d, newTex
						|| fullUpdate);

				TextureFormat f = t2d.getFormat();
				boolean rescale = handle.width != t2d.getWidth(0)
						|| handle.height != t2d.getHeight(0);
				if (newTex || rescale || f.isCompressed()
						|| f == TextureFormat.DEPTH)
					// we have to re-allocate the image data, or make it for the
					// first time
					// re-allocate on rescale for simplicity. re-allocate for
					// formats because of driver issues
					doTexImage(gl, pr, handle, t2d, newTex);
				else
					// we can use glTexSubImage for better performance
					doTexSubImage(gl, pr, (fullUpdate ? null : dirty), handle,
							t2d);

				// restore the texture binding on the active unit
				TextureUnit active = tr.textureUnits[tr.activeTexture];
				int restoreId = (active.enabledTarget == handle.glTarget ? active.texBinding
						: 0);
				gl.glBindTexture(handle.glTarget, restoreId);
			}

			// we've update successfully, so we can clear this
			resource.clearDirtyDescriptor();
		}
	}

	/*
	 * Invoke glTexImage2D() on all mipmap layers, no matter what. This is good
	 * for a first time texture or for textures that aren't suitable for use
	 * with doTexSubImage(). For simplicity, this ignores the mipmap dirty
	 * regions. This method will properly resize images and use compressed
	 * function calls if needed.
	 */
	private void doTexImage(GL gl, PackUnpackRecord pr, TextureHandle handle,
			Texture2D tex, boolean newTex) {
		boolean needsResize = handle.width != tex.getWidth(0)
				|| handle.height != tex.getHeight(0);

		int w, h;
		BufferData bd;
		for (int i = 0; i < handle.numMipmaps; i++) {
			// actual level's dimensions
			w = Math.max(1, handle.width >> i);
			h = Math.max(1, handle.height >> i);

			// possibly rescale the data
			bd = tex.getData(i);
			if (bd != null && bd.getData() != null) {
				if (needsResize)
					// resize the image to meet POT requirements
					bd = TextureConverter.convert(
					// src
							bd, tex.getFormat(), tex.getWidth(i), tex
									.getHeight(i), 1,
							// dst
							null, tex.getFormat(), bd.getType(), w, h, 1);
				// proceed with glTexImage
				imageDriver.setUnpackRegion(gl, pr, 0, 0, 0, w, h);
				if (handle.glSrcFormat > 0)
					gl.glTexImage2D(handle.glTarget, i, handle.glDstFormat, w,
							h, 0, handle.glSrcFormat, handle.glType,
							imageDriver.wrap(bd));
				else
					gl.glCompressedTexImage2D(handle.glTarget, i,
							handle.glDstFormat, w, h, 0, bd.getCapacity(),
							imageDriver.wrap(bd));
			} else if (newTex)
				// we'll just allocate an empty image
				if (handle.glSrcFormat > 0)
					gl.glTexImage2D(handle.glTarget, i, handle.glDstFormat, w,
							h, 0, handle.glSrcFormat, handle.glType, null);
				else
					gl.glCompressedTexImage2D(handle.glTarget, i,
							handle.glDstFormat, w, h, 0, tex.getFormat()
									.getBufferSize(w, h, 0), null);
		}
	}

	/*
	 * Invoke glTexSubImage2D() on all dirty mipmap regions (or all if dirty ==
	 * null). This assumes that the given texture doesn't need rescaling and
	 * doesn't have a compressed format. It is recommended not to use the DEPTH
	 * format, either since that seems to cause problems.
	 */
	private void doTexSubImage(GL gl, PackUnpackRecord pr,
			Texture2DDirtyDescriptor dirty, TextureHandle handle, Texture2D tex) {
		int w, h;
		MipmapDirtyRegion mdr;
		BufferData bd;
		for (int i = 0; i < handle.numMipmaps; i++) {
			bd = tex.getData(i);
			w = Math.max(1, handle.width >> i);
			h = Math.max(1, handle.height >> i);

			if (bd != null && bd.getData() != null)
				if (dirty == null || dirty.isDataDirty(i)) {
					// we'll have to call glTexSubImage here, but we don't
					// have to call glCompressedTexSubImage since compressed
					// images always use doTexImage()
					mdr = (dirty == null ? null : dirty.getDirtyRegion(i));
					if (mdr != null) {
						// use the region descriptor
						imageDriver.setUnpackRegion(gl, pr, mdr
								.getDirtyXOffset(), mdr.getDirtyYOffset(), 0,
								w, h);
						gl.glTexSubImage2D(handle.glTarget, i, mdr
								.getDirtyXOffset(), mdr.getDirtyYOffset(), mdr
								.getDirtyWidth(), mdr.getDirtyHeight(),
								handle.glSrcFormat, handle.glType, imageDriver
										.wrap(bd));
					} else {
						// we'll update the whole image level
						imageDriver.setUnpackRegion(gl, pr, 0, 0, 0, w, h);
						gl.glTexSubImage2D(handle.glTarget, i, 0, 0, w, h,
								handle.glSrcFormat, handle.glType, imageDriver
										.wrap(bd));
					}
				}
		}
	}
}
