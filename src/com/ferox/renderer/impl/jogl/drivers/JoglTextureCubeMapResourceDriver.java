package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceDriver;
import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PackUnpackRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord.TextureUnit;
import com.ferox.resource.BufferData;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.texture.TextureCubeMap;
import com.ferox.resource.texture.TextureFormat;
import com.ferox.resource.texture.TextureCubeMap.TextureCubeMapDirtyDescriptor;
import com.ferox.resource.texture.TextureImage.MipmapDirtyRegion;
import com.ferox.resource.texture.converter.TextureConverter;

/** JoglTextureCubeMapResourceDriver provides the functionality to load
 * and delete TextureCubeMap instances in the graphics card.  It will
 * re-scale npot textures if the card doesn't have npot support.
 * 
 * A TextureCubeMap will only have an ERROR status if it is compressed and
 * the DXT_n compressions aren't supported.
 * It will be DIRTY if it was an unclamped float format and they're not
 * supported, or if an NPOT texture had to be resized.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglTextureCubeMapResourceDriver implements ResourceDriver {
	private JoglSurfaceFactory factory;
	private TextureImageDriver imageDriver;
	private boolean hasS3tcCompression;
	
	public JoglTextureCubeMapResourceDriver(JoglSurfaceFactory factory) {
		RenderCapabilities caps = factory.getRenderer().getCapabilities();
		
		this.factory = factory;
		this.imageDriver = new TextureImageDriver(caps);
		this.hasS3tcCompression = caps.getS3TextureCompression();
	}
	
	@Override
	public void cleanUp(Resource resource, ResourceData data) {
		GL gl = this.factory.getGL();
		TextureHandle handle = (TextureHandle) data.getHandle();
		
		if (handle != null) {
			this.imageDriver.destroyTexture(gl, handle);
		} // else status became ERROR before handle was made
	}

	@Override
	public void update(Resource resource, ResourceData data, boolean fullUpdate) {
		JoglStateRecord sr = this.factory.getRecord();
		
		GL gl = this.factory.getGL();
		PackUnpackRecord pr = sr.packRecord;
		TextureRecord tr = sr.textureRecord;
		
		if (data.getStatus() != Status.ERROR) {
			// only do an update if we haven't got an error
			TextureHandle handle = (TextureHandle) data.getHandle();
			TextureCubeMap tcm = (TextureCubeMap) resource;
			boolean newTex = false;
			
			if (handle == null) {
				// make a new texture
				if (tcm.getFormat().isCompressed() && !this.hasS3tcCompression) {
					// can't make the texture
					data.setStatus(Status.ERROR);
					data.setStatusMessage("DXT_n TextureFormats aren't supported on this hardware");
					return; // abort the update
				}
				
				// ok to get the handle now
				handle = this.imageDriver.createNewTexture(gl, tcm);
				
				data.setHandle(handle);
				if (this.imageDriver.isDirty(tcm, handle)) {
					data.setStatus(Status.DIRTY);
					data.setStatusMessage(this.imageDriver.getDirtyStatusMessage(tcm, handle));
				} else {
					data.setStatus(Status.OK);
					data.setStatusMessage("");
				}
				
				newTex = true; // must set this to force a full update down-the-line
			}
			
			TextureCubeMapDirtyDescriptor dirty = (TextureCubeMapDirtyDescriptor) tcm.getDirtyDescriptor();
			if (newTex || fullUpdate || dirty.areMipmapsDirty() || dirty.isAnisotropicFilteringDirty() 
				|| dirty.isDepthCompareDirty() || dirty.isFilterDirty() || dirty.isTextureWrapDirty()) {
				// we must actually update the texture
				gl.glBindTexture(handle.glTarget, handle.id);
				
				this.imageDriver.setTextureParameters(gl, handle, tcm, newTex || fullUpdate);
				
				TextureFormat f = tcm.getFormat();
				boolean rescale = handle.width != tcm.getWidth(0);
				if (newTex || rescale || f.isCompressed() || f == TextureFormat.DEPTH) {
					// we have to re-allocate the image data, or make it for the first time
					// re-allocate on rescale for simplicity.  re-allocate for formats because of driver issues
					this.doTexImage(gl, pr, handle, tcm, newTex);
				} else {
					// we can use glTexSubImage for better performance
					this.doTexSubImage(gl, pr, (fullUpdate ? null : dirty), handle, tcm);
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
	private void doTexImage(GL gl, PackUnpackRecord pr, TextureHandle handle, TextureCubeMap tex, boolean newTex) {
		boolean needsResize = handle.width != tex.getWidth(0) || handle.height != tex.getHeight(0);
		
		for (int i = 0; i < handle.numMipmaps; i++) {
			// loop over all faces in this level
			for (int f = 0; f < 6; f++) {
				// delegate to the other function
				this.doTexImage(gl, pr, handle, tex, newTex, needsResize, i, f);
			}
		}
	}

	/* Invoke glTexSubImage2D() on all dirty mipmap regions (or all if dirty == null).  This assumes that the given
	 * texture doesn't need rescaling and doesn't have a compressed format.  It is recommended not to use the DEPTH 
	 * format, either since that seems to cause problems. */
	private void doTexSubImage(GL gl, PackUnpackRecord pr, TextureCubeMapDirtyDescriptor dirty, TextureHandle handle, TextureCubeMap tex) {
		for (int i = 0; i < handle.numMipmaps; i++) {
			// loop over all faces in this level
			for (int f = 0; f < 6; f++) {
				if (dirty.isDataDirty(f, i)) {
					// delegate to the other function now
					this.doTexSubImage(gl, pr, dirty.getDirtyRegion(f, i), handle, tex, i, f);
				}
			}
		}
	}
	
	private void doTexImage(GL gl, PackUnpackRecord pr, TextureHandle handle, TextureCubeMap tex, boolean newTex, boolean needsResize, int level, int face) {		
		int s = Math.max(1, handle.width >> level);
		BufferData bd = tex.getData(face, level);

		if (bd != null && bd.getData() != null) {
			if (needsResize) {
				// resize the image to meet POT requirements
				bd = TextureConverter.convert(// src
						bd, tex.getFormat(), 
						tex.getWidth(level), tex.getHeight(level), 1, 
						// dst
						null, tex.getFormat(), bd.getType(), 
						s, s, 1
				);
			}
			// proceed with glTexImage
			this.imageDriver.setUnpackRegion(gl, pr, 0, 0, 0, s, s);
			if (handle.glSrcFormat > 0)
				gl.glTexImage2D(EnumUtil.getGLCubeFace(face), level, handle.glDstFormat, s, s, 0, handle.glSrcFormat, handle.glType, this.imageDriver.wrap(bd));
			else
				gl.glCompressedTexImage2D(EnumUtil.getGLCubeFace(face), level, handle.glDstFormat, s, s, 0, bd.getCapacity(), this.imageDriver.wrap(bd));
		} else if (newTex) {
			// we'll just allocate an empty image
			if (handle.glSrcFormat > 0)
				gl.glTexImage2D(EnumUtil.getGLCubeFace(face), level, handle.glDstFormat, s, s, 0, handle.glSrcFormat, handle.glType, null);
			else
				gl.glCompressedTexImage2D(EnumUtil.getGLCubeFace(face), level, handle.glDstFormat, s, s, 0, tex.getFormat().getBufferSize(s, s, 0), null);
		} // else .. ignore this layer
	}
	
	/* Do glTexSubImage2D() for the given mipmap and face. */
	private void doTexSubImage(GL gl, PackUnpackRecord pr, MipmapDirtyRegion region, TextureHandle handle, TextureCubeMap tex, int level, int face) {
		int s = Math.max(1, handle.width >> level);
		BufferData bd = tex.getData(face, level);
		
		if (bd != null && bd.getData() != null) {
			// we won't have to call glTexSubImage here -> that should always use doTexImage()
			if (region != null) {
				// use the region descriptor
				this.imageDriver.setUnpackRegion(gl, pr, region.getDirtyXOffset(), region.getDirtyYOffset(), 0, s, s);
				gl.glTexSubImage2D(EnumUtil.getGLCubeFace(face), level, 
								   region.getDirtyXOffset(), region.getDirtyYOffset(), 
								   region.getDirtyWidth(), region.getDirtyHeight(), 
								   handle.glSrcFormat, handle.glType, this.imageDriver.wrap(bd));
			} else {
				// update the whole region
				this.imageDriver.setUnpackRegion(gl, pr, 0, 0, 0, s, s);
				gl.glTexSubImage2D(EnumUtil.getGLCubeFace(face), level, 0, 0, s, s, handle.glSrcFormat, handle.glType, this.imageDriver.wrap(bd));
			}
		} // else ignore the null data level
	}
}
