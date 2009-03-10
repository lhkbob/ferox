package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceDriver;
import com.ferox.renderer.impl.jogl.JoglContext;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.PackUnpackRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord.TextureUnit;
import com.ferox.resource.BufferData;
import com.ferox.resource.Resource;
import com.ferox.resource.Texture3D;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Texture3D.Texture3DDirtyDescriptor;
import com.ferox.resource.TextureImage.MipmapDirtyRegion;
import com.ferox.resource.util.TextureConverter;

/** JoglTexture3DResourceDriver provides the functionality to load
 * and delete Texture3D instances in the graphics card.  It will
 * re-scale npot textures if the card doesn't have npot support.
 * 
 * A Texture3D will not have an ERROR status
 * It will be DIRTY if it was an unclamped float format and they're not
 * supported, or if an NPOT texture had to be resized.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglTexture3DResourceDriver implements ResourceDriver {
	private JoglSurfaceFactory factory;
	private TextureImageDriver imageDriver;
	
	public JoglTexture3DResourceDriver(JoglSurfaceFactory factory) {
		RenderCapabilities caps = factory.getRenderer().getCapabilities();
		
		this.factory = factory;
		this.imageDriver = new TextureImageDriver(caps);
	}
	
	@Override
	public void cleanUp(Resource resource, ResourceData data) {
		GL gl = this.factory.getCurrentContext().getContext().getGL();
		TextureHandle handle = (TextureHandle) data.getHandle();
		
		this.imageDriver.destroyTexture(gl, handle);
	}

	@Override
	public void update(Resource resource, ResourceData data, boolean fullUpdate) {
		JoglContext current = this.factory.getCurrentContext();
		
		GL gl = current.getContext().getGL();
		PackUnpackRecord pr = current.getStateRecord().packRecord;
		TextureRecord tr = current.getStateRecord().textureRecord;

		TextureHandle handle = (TextureHandle) data.getHandle();
		Texture3D t3d = (Texture3D) resource;
		boolean newTex = false;

		if (handle == null) {
			// ok to get the handle now
			handle = this.imageDriver.createNewTexture(gl, t3d);

			data.setHandle(handle);
			if (this.imageDriver.isDirty(t3d, handle)) {
				data.setStatus(Status.DIRTY);
				data.setStatusMessage(this.imageDriver.getDirtyStatusMessage(t3d, handle));
			} else {
				data.setStatus(Status.OK);
				data.setStatusMessage("");
			}

			newTex = true; // must set this to force a full update down-the-line
		}

		Texture3DDirtyDescriptor dirty = (Texture3DDirtyDescriptor) t3d.getDirtyDescriptor();
		if (newTex || fullUpdate || dirty.areMipmapsDirty() || dirty.isAnisotropicFilteringDirty() 
				|| dirty.isDepthCompareDirty() || dirty.isFilterDirty() || dirty.isTextureWrapDirty()) {
			// we must actually update the texture
			gl.glBindTexture(handle.glTarget, handle.id);

			this.imageDriver.setTextureParameters(gl, handle, t3d, newTex || fullUpdate);

			TextureFormat f = t3d.getFormat();
			boolean rescale = handle.width != t3d.getWidth(0) || handle.height != t3d.getHeight(0) || handle.depth != t3d.getDepth(0);
			if (newTex || rescale || f.isCompressed() || f == TextureFormat.DEPTH) {
				// we have to re-allocate the image data, or make it for the first time
				// re-allocate on rescale for simplicity.  re-allocate for formats because of driver issues
				this.doTexImage(gl, pr, handle, t3d, newTex);
			} else {
				// we can use glTexSubImage for better performance
				this.doTexSubImage(gl, pr, (fullUpdate ? null : dirty), handle, t3d);
			}

			// restore the texture binding on the active unit
			TextureUnit active = tr.textureUnits[tr.activeTexture];
			int restoreId = (active.enabledTarget == handle.glTarget ? active.texBinding : 0);
			gl.glBindTexture(handle.glTarget, restoreId);
		}

		// we've update successfully, so we can clear this
		resource.clearDirtyDescriptor();
	}
	
	/* Invoke glTexImage3D() on all mipmap layers, no matter what.  This is good for a first time texture or
	 * for textures that aren't suitable for use with doTexSubImage().  For simplicity, this ignores the
	 * mipmap dirty regions.  This method will properly resize images and use compressed function calls if needed. */
	private void doTexImage(GL gl, PackUnpackRecord pr, TextureHandle handle, Texture3D tex, boolean newTex) {
		boolean needsResize = handle.width != tex.getWidth(0) || handle.height != tex.getHeight(0);
		
		int w, h, d;
		BufferData bd;
		for (int i = 0; i < handle.numMipmaps; i++) {
			// actual level's dimensions
			w = Math.max(1, handle.width >> i);
			h = Math.max(1, handle.height >> i);
			d = Math.max(1, handle.depth >> i);
			
			// possibly rescale the data
			bd = tex.getData(i);
			if (bd != null && bd.getData() != null) {
				if (needsResize) {
					// resize the image to meet POT requirements
					bd = TextureConverter.convert(// src
							                      bd, tex.getFormat(), 
							                      tex.getWidth(i), tex.getHeight(i), tex.getDepth(i), 
							                      // dst
							                      null, tex.getFormat(), bd.getType(), 
							                      w, h, d
												 );
				}
				// proceed with glTexImage
				this.imageDriver.setUnpackRegion(gl, pr, 0, 0, 0, w, h);
				gl.glTexImage3D(handle.glTarget, i, handle.glDstFormat, w, h, d, 0, handle.glSrcFormat, handle.glType, this.imageDriver.wrap(bd));
			} else if (newTex) {
				// we'll just allocate an empty image
				gl.glTexImage3D(handle.glTarget, i, handle.glDstFormat, w, h, d, 0, handle.glSrcFormat, handle.glType, null);
			} // else .. ignore this layer
		}
	}

	/* Invoke glTexSubImage3D() on all dirty mipmap regions (or all if dirty == null).  This assumes that the given
	 * texture doesn't need rescaling and doesn't have a compressed format.  It is recommended not to use the DEPTH 
	 * format, either since that seems to cause problems. */
	private void doTexSubImage(GL gl, PackUnpackRecord pr, Texture3DDirtyDescriptor dirty, TextureHandle handle, Texture3D tex) {
		int w, h, d;
		MipmapDirtyRegion mdr;
		BufferData bd;
		for (int i = 0; i < handle.numMipmaps; i++) {
			bd = tex.getData(i);
			w = Math.max(1, handle.width >> i);
			h = Math.max(1, handle.height >> i);
			d = Math.max(1, handle.depth >> i);
			
			if (bd != null && bd.getData() != null) {
				if (dirty == null || dirty.isDataDirty(i)) {
					// we'll have to call glTexSubImage here, but we don't
					// have to call glCompressedTexSubImage since compressed images always use doTexImage()
					mdr = (dirty == null ? null : dirty.getDirtyRegion(i));
					if (mdr != null) {
						// use the region descriptor
						this.imageDriver.setUnpackRegion(gl, pr, mdr.getDirtyXOffset(), mdr.getDirtyYOffset(), mdr.getDirtyZOffset(), w, h);
						gl.glTexSubImage3D(handle.glTarget, i, 
										   mdr.getDirtyXOffset(), mdr.getDirtyYOffset(), mdr.getDirtyZOffset(),
										   mdr.getDirtyWidth(), mdr.getDirtyHeight(), mdr.getDirtyDepth(),
										   handle.glSrcFormat, handle.glType, this.imageDriver.wrap(bd));
					} else {
						// we'll update the whole image level
						this.imageDriver.setUnpackRegion(gl, pr, 0, 0, 0, w, h);
						gl.glTexSubImage3D(handle.glTarget, i, 0, 0, 0, w, h, d, handle.glSrcFormat, handle.glType, this.imageDriver.wrap(bd));
					}
				}
			} // else ignore null data levels
		}
	}
}
