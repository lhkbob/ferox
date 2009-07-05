package com.ferox.renderer.impl.jogl.drivers.resource;

import java.nio.Buffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceDriver;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.TextureHandle;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PackUnpackRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord.TextureUnit;
import com.ferox.resource.BufferData;
import com.ferox.resource.Resource;
import com.ferox.resource.Texture1D;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Texture1D.Texture1DDirtyDescriptor;
import com.ferox.resource.TextureImage.MipmapDirtyRegion;
import com.ferox.util.texture.converter.TextureConverter;

/**
 * JoglTexture1DResourceDriver provides the functionality to load and delete
 * Texture1D instances in the graphics card. It will re-scale npot textures if
 * the card doesn't have npot support. A Texture1D will never have an ERROR
 * status. It will be DIRTY if it was an unclamped float format and they're not
 * supported, or if an NPOT texture had to be resized.
 * 
 * @author Michael Ludwig
 */
public class JoglTexture1DResourceDriver implements ResourceDriver {
	private final JoglContextManager factory;
	private final TextureImageDriver imageDriver;

	public JoglTexture1DResourceDriver(JoglContextManager factory) {
		RenderCapabilities caps = factory.getFramework().getCapabilities();

		this.factory = factory;
		imageDriver = new TextureImageDriver(caps);
	}

	@Override
	public void cleanUp(Renderer renderer, Resource resource, ResourceData data) {
		GL gl = factory.getGL();
		TextureHandle handle = (TextureHandle) data.getHandle();

		// a T_1D should always have a valid handle
		imageDriver.destroyTexture(gl, handle);
	}

	@Override
	public void update(Renderer renderer, Resource resource, ResourceData data, boolean fullUpdate) {
		JoglStateRecord sr = factory.getRecord();

		GL gl = factory.getGL();
		PackUnpackRecord pr = sr.packRecord;
		TextureRecord tr = sr.textureRecord;

		TextureHandle handle = (TextureHandle) data.getHandle();
		Texture1D t1d = (Texture1D) resource;
		boolean newTex = false;

		if (handle == null) {
			// make a new texture
			handle = imageDriver.createNewTexture(gl, t1d);

			data.setHandle(handle);
			if (!gl.isGL2() && !gl.isGL3()) {
				// can't proceed
				data.setStatus(Status.ERROR);
				data.setStatusMessage("Requires a GL2 or GL3 for Texture1D functionality");
				return;
			}
			
			if (imageDriver.isDirty(t1d, handle)) {
				data.setStatus(Status.DIRTY);
				data.setStatusMessage(imageDriver.getDirtyStatusMessage(t1d, handle));
			} else {
				data.setStatus(Status.OK);
				data.setStatusMessage("");
			}

			newTex = true; // must set this to force a full update down-the-line
		}

		Texture1DDirtyDescriptor dirty = t1d.getDirtyDescriptor();
		if (newTex || fullUpdate || dirty.areMipmapsDirty() || dirty.isAnisotropicFilteringDirty() || 
			dirty.isDepthCompareDirty() || dirty.isFilterDirty() || dirty.isTextureWrapDirty()) {
			// we must actually update the texture
			gl.glBindTexture(handle.glTarget, handle.id);

			imageDriver.setTextureParameters(gl, handle, t1d, newTex || fullUpdate);

			TextureFormat f = t1d.getFormat();
			boolean rescale = handle.width != t1d.getWidth(0);
			if (newTex || rescale || f == TextureFormat.DEPTH)
				// we have to re-allocate the image data, or make it for the
				// first time
				// re-allocate on rescale for simplicity. re-allocate for
				// formats because of driver issues
				doTexImage(gl, pr, handle, t1d, newTex);
			else
				// we can use glTexSubImage for better performance
				doTexSubImage(gl, pr, (fullUpdate ? null : dirty), handle, t1d);

			// restore the texture binding on the active unit
			TextureUnit active = tr.textureUnits[tr.activeTexture];
			int restoreId = (active.enabledTarget == handle.glTarget ? active.texBinding : 0);
			gl.glBindTexture(handle.glTarget, restoreId);
		}

		// we've update successfully, so we can clear this
		resource.clearDirtyDescriptor();
	}

	/*
	 * Invoke glTexImage1D() on all mipmap layers, no matter what. This is good
	 * for a first time texture or for textures that aren't suitable for use
	 * with doTexSubImage(). For simplicity, this ignores the mipmap dirty
	 * regions. This method will properly resize images if needed.
	 */
	private void doTexImage(GL gl, PackUnpackRecord pr, TextureHandle handle, 
							Texture1D tex, boolean newTex) {
		boolean needsResize = handle.width != tex.getWidth(0);

		int w;
		BufferData bd;
		for (int i = 0; i < handle.numMipmaps; i++) {
			// actual level's dimensions
			w = Math.max(1, handle.width >> i);

			// possibly rescale the data
			bd = tex.getData(i);
			if (bd != null && bd.getData() != null) {
				if (needsResize) {
					// resize the image to meet POT requirements
					bd = TextureConverter.convert(
								// src
								bd, tex.getFormat(), tex.getWidth(i), 1, 1,
								// dst
								null, tex.getFormat(), bd.getType(), w, 1, 1);
				}
				// proceed with glTexImage
				imageDriver.setUnpackRegion(gl, pr, 0, 0, 0, w, 1);
				glTexImage1D(gl, handle.glTarget, i, handle.glDstFormat, w, 0, handle.glSrcFormat, 
							 handle.glType, imageDriver.wrap(bd));
			} else if (newTex) {
				// we'll just allocate an empty image
				glTexImage1D(gl, handle.glTarget, i, handle.glDstFormat, w, 0, handle.glSrcFormat, 
							 handle.glType, null);
			}
		}
	}

	/*
	 * Invoke glTexSubImage1D() on all dirty mipmap regions (or all if dirty ==
	 * null). This assumes that the given texture doesn't need rescaling and
	 * doesn't have a compressed format. It is recommended not to use the DEPTH
	 * format, either since that seems to cause problems.
	 */
	private void doTexSubImage(GL gl, PackUnpackRecord pr, Texture1DDirtyDescriptor dirty,
							   TextureHandle handle, Texture1D tex) {
		int w;
		MipmapDirtyRegion mdr;
		BufferData bd;
		for (int i = 0; i < handle.numMipmaps; i++) {
			bd = tex.getData(i);
			w = Math.max(1, handle.width >> i);

			if (bd != null && bd.getData() != null) {
				if (dirty == null || dirty.isDataDirty(i)) {
					// we'll have to call glTexSubImage here, but we don't
					// have to call glCompressedTexSubImage since compressed
					// images always use doTexImage()
					mdr = (dirty == null ? null : dirty.getDirtyRegion(i));
					if (mdr != null) {
						// use the region descriptor
						imageDriver.setUnpackRegion(gl, pr, mdr.getDirtyXOffset(), 0, 0, w, 1);
						glTexSubImage1D(gl, handle.glTarget, i, mdr.getDirtyXOffset(), mdr.getDirtyWidth(), 
										handle.glSrcFormat, handle.glType, imageDriver.wrap(bd));
					} else {
						// we'll update the whole image level
						imageDriver.setUnpackRegion(gl, pr, 0, 0, 0, w, 1);
						glTexSubImage1D(gl, handle.glTarget, i, 0, w, handle.glSrcFormat,
										handle.glType, imageDriver.wrap(bd));
					}
				}
			}	
		}
	}
	
	private void glTexImage1D(GL gl, int target, int layer, int dstFormat, int width,
							  int border, int srcFormat, int type, Buffer data) {
		if (gl.isGL2()) {
			GL2 gl2 = gl.getGL2();
			gl2.glTexImage1D(target, layer, dstFormat, width, border, srcFormat, type, data);
		} else { // assume GL3
			GL3 gl3 = gl.getGL3();
			gl3.glTexImage1D(target, layer, dstFormat, width, border, srcFormat, type, data);
		}
	}

	private void glTexSubImage1D(GL gl, int target, int layer, int xOffset, 
							     int width, int srcFormat, int type, Buffer data) {
		if (gl.isGL2()) {
			GL2 gl2 = gl.getGL2();
			gl2.glTexSubImage1D(target, layer, xOffset, width, srcFormat, type, data);
		} else { // assumes GL3
			GL3 gl3 = gl.getGL3();
			gl3.glTexSubImage1D(target, layer, xOffset, width, srcFormat, type, data);
		} 
	}
}
