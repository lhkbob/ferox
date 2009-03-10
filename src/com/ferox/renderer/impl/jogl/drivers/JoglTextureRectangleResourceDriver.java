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
import com.ferox.resource.TextureFormat;
import com.ferox.resource.TextureRectangle;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.TextureImage.MipmapDirtyRegion;
import com.ferox.resource.TextureRectangle.TextureRectangleDirtyDescriptor;

/** JoglTextureRectangleResourceDriver provides the functionality to load
 * and delete TextureRectangle instances in the graphics card.
 * 
 * A TextureRectangle will have an ERROR status if it is compressed and
 * the DXT_n compressions aren't supported, or if texture rectangles aren't supported.
 * It will be DIRTY if it was an unclamped float format and they're not
 * supported, or if an NPOT texture had to be resized.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglTextureRectangleResourceDriver implements ResourceDriver {
	private JoglSurfaceFactory factory;
	private TextureImageDriver imageDriver;
	private boolean hasS3tcCompression;
	private boolean hasRectSupport;
	
	public JoglTextureRectangleResourceDriver(JoglSurfaceFactory factory) {
		RenderCapabilities caps = factory.getRenderer().getCapabilities();
		
		this.factory = factory;
		this.imageDriver = new TextureImageDriver(caps);
		this.hasS3tcCompression = caps.getS3TextureCompression();
		this.hasRectSupport = caps.getRectangularTextureSupport();
	}
	
	@Override
	public void cleanUp(Resource resource, ResourceData data) {
		GL gl = this.factory.getCurrentContext().getContext().getGL();
		TextureHandle handle = (TextureHandle) data.getHandle();
		
		if (handle != null) {
			this.imageDriver.destroyTexture(gl, handle);
		} // else status became ERROR before handle was made
	}

	@Override
	public void update(Resource resource, ResourceData data, boolean fullUpdate) {
		JoglContext current = this.factory.getCurrentContext();
		
		GL gl = current.getContext().getGL();
		PackUnpackRecord pr = current.getStateRecord().packRecord;
		TextureRecord tr = current.getStateRecord().textureRecord;
		
		if (data.getStatus() != Status.ERROR) {
			// only do an update if we haven't got an error
			TextureHandle handle = (TextureHandle) data.getHandle();
			TextureRectangle tRect = (TextureRectangle) resource;
			boolean newTex = false;
			
			if (handle == null) {
				// make a new texture
				if (!this.hasRectSupport) {
					// rectangle textures aren't supported
					data.setStatus(Status.ERROR);
					data.setStatusMessage("TextureRectangles aren't supported on this hardware");
					return; // abort the update
				}
				if (tRect.getFormat().isCompressed() && !this.hasS3tcCompression) {
					// can't make the texture
					data.setStatus(Status.ERROR);
					data.setStatusMessage("DXT_n TextureFormats aren't supported on this hardware");
					return; // abort the update
				}
				
				// ok to get the handle now
				handle = this.imageDriver.createNewTexture(gl, tRect);
				
				data.setHandle(handle);
				if (this.imageDriver.isDirty(tRect, handle)) {
					data.setStatus(Status.DIRTY);
					data.setStatusMessage(this.imageDriver.getDirtyStatusMessage(tRect, handle));
				} else {
					data.setStatus(Status.OK);
					data.setStatusMessage("");
				}
				
				newTex = true; // must set this to force a full update down-the-line
			}
			
			TextureRectangleDirtyDescriptor dirty = (TextureRectangleDirtyDescriptor) tRect.getDirtyDescriptor();
			if (newTex || fullUpdate || dirty.isDataDirty() || dirty.isAnisotropicFilteringDirty() 
				|| dirty.isDepthCompareDirty() || dirty.isFilterDirty() || dirty.isTextureWrapDirty()) {
				// we must actually update the texture
				gl.glBindTexture(handle.glTarget, handle.id);
				
				this.imageDriver.setTextureParameters(gl, handle, tRect, newTex || fullUpdate);
				
				TextureFormat f = tRect.getFormat();
				if (newTex || f.isCompressed() || f == TextureFormat.DEPTH) {
					// we have to re-allocate the image data, or make it for the first time
					// re-allocate on rescale for simplicity.  re-allocate for formats because of driver issues
					this.doTexImage(gl, pr, handle, tRect, newTex);
				} else {
					// we can use glTexSubImage for better performance
					this.doTexSubImage(gl, pr, (fullUpdate ? null : dirty), handle, tRect);
				}
				
				// restore the texture binding on the active unit
				TextureUnit active = tr.textureUnits[tr.activeTexture];
				int restoreId = (active.enabledTarget == handle.glTarget ? active.texBinding : 0);
				gl.glBindTexture(handle.glTarget, restoreId);
			}
			
			// we've update successfully, so we can clear this
			resource.clearDirtyDescriptor();
		}
	}
	
	/* Invoke glTexImage2D() on all mipmap layers, no matter what.  This is good for a first time texture or
	 * for textures that aren't suitable for use with doTexSubImage().  For simplicity, this ignores the
	 * mipmap dirty regions.  This method will properly resize images and use compressed function calls if needed. */
	private void doTexImage(GL gl, PackUnpackRecord pr, TextureHandle handle, TextureRectangle tex, boolean newTex) {		
		BufferData bd = tex.getData();
			
		// possibly rescale the data
		if (bd != null && bd.getData() != null) {
			// proceed with glTexImage
			this.imageDriver.setUnpackRegion(gl, pr, 0, 0, 0, handle.width, handle.height);
			if (handle.glSrcFormat > 0)
				gl.glTexImage2D(handle.glTarget, 0, handle.glDstFormat, handle.width, handle.height, 0, handle.glSrcFormat, handle.glType, this.imageDriver.wrap(bd));
			else
				gl.glCompressedTexImage2D(handle.glTarget, 0, handle.glDstFormat, handle.width, handle.height, 0, bd.getCapacity(), this.imageDriver.wrap(bd));
		} else if (newTex) {
			// we'll just allocate an empty image
			if (handle.glSrcFormat > 0)
				gl.glTexImage2D(handle.glTarget, 0, handle.glDstFormat, handle.width, handle.height, 0, handle.glSrcFormat, handle.glType, null);
			else
				gl.glCompressedTexImage2D(handle.glTarget, 0, handle.glDstFormat, handle.width, handle.height, 0, tex.getFormat().getBufferSize(handle.width, handle.height, 0), null);
		} // else .. ignore this layer
	}

	/* Invoke glTexSubImage2D() on all dirty mipmap regions (or all if dirty == null).  This assumes that the given
	 * texture doesn't need rescaling and doesn't have a compressed format.  It is recommended not to use the DEPTH 
	 * format, either since that seems to cause problems. */
	private void doTexSubImage(GL gl, PackUnpackRecord pr, TextureRectangleDirtyDescriptor dirty, TextureHandle handle, TextureRectangle tex) {
		MipmapDirtyRegion mdr;
		BufferData bd = tex.getData();

		if (bd != null && bd.getData() != null) {
			if (dirty == null || dirty.isDataDirty()) {
				// we'll have to call glTexSubImage here, but we don't
				// have to call glCompressedTexSubImage since compressed images always use doTexImage()
				mdr = (dirty == null ? null : dirty.getDirtyRegion());
				if (mdr != null) {
					// use the region descriptor
					this.imageDriver.setUnpackRegion(gl, pr, mdr.getDirtyXOffset(), mdr.getDirtyYOffset(), 0, handle.width, handle.height);
					gl.glTexSubImage2D(handle.glTarget, 0, 
							mdr.getDirtyXOffset(), mdr.getDirtyYOffset(), 
							mdr.getDirtyWidth(), mdr.getDirtyHeight(), 
							handle.glSrcFormat, handle.glType, this.imageDriver.wrap(bd));
				} else {
					// we'll update the whole image level
					this.imageDriver.setUnpackRegion(gl, pr, 0, 0, 0, handle.width, handle.height);
					gl.glTexSubImage2D(handle.glTarget, 0, 0, 0, handle.width, handle.height, handle.glSrcFormat, handle.glType, this.imageDriver.wrap(bd));
				}
			}
		} // else ignore null data levels
	}
}
