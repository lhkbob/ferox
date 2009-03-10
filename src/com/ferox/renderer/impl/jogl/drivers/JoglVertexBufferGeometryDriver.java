package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.AbstractRenderer;
import com.ferox.renderer.impl.GeometryDriver;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.drivers.BufferedGeometryDriver.ArrayPointer;
import com.ferox.renderer.impl.jogl.drivers.BufferedGeometryDriver.BufferedGeometryHandle;
import com.ferox.renderer.impl.jogl.drivers.BufferedGeometryDriver.PointerType;
import com.ferox.renderer.impl.jogl.record.VertexArrayRecord;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource;
import com.ferox.resource.VertexArray;
import com.ferox.resource.VertexBufferGeometry;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.BufferedGeometry.GeometryArray;
import com.ferox.resource.Resource.Status;

/** JoglVertexBufferGeometryDriver handles the rendering of VertexBufferGeometries.
 * It is somewhat liberal in its assumptions about the state record.  To be
 * efficient, it assumes that no state driver modifies the VertexArrayRecord 
 * and that other geometry drivers properly reset the state that they've modified.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglVertexBufferGeometryDriver implements GeometryDriver {
	private JoglSurfaceFactory factory;
	private BufferedGeometryDriver<VertexBufferObject, VertexBufferGeometry> geomDriver;
	
	private int maxTextureUnits;
	private int maxVertexAttribs;
	
	private BufferedGeometryHandle<VertexBufferObject> lastRendered;
	
	public JoglVertexBufferGeometryDriver(JoglSurfaceFactory factory) {
		this.factory = factory;
		this.geomDriver = new BufferedGeometryDriver<VertexBufferObject, VertexBufferGeometry>();
		
		this.maxTextureUnits = factory.getRenderer().getCapabilities().getMaxTextureCoordinates();
		this.maxVertexAttribs = factory.getRenderer().getCapabilities().getMaxVertexAttributes();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public int render(Geometry geom, ResourceData data) {
		GL gl = this.factory.getGL();
		VertexArrayRecord vr = this.factory.getCurrentContext().getStateRecord().vertexArrayRecord;
		AbstractRenderer renderer = this.factory.getRenderer();
		
		BufferedGeometryHandle<VertexBufferObject> toRender = (BufferedGeometryHandle<VertexBufferObject>) data.getHandle();

		if (toRender != this.lastRendered) {
			// disable unused pointers
			if (this.lastRendered != null)
				disablePointers(gl, vr, this.lastRendered, toRender);
			
			// update the vertex pointers to match this geometry
			boolean abort = !(toRender.glInterleavedType < 0 ? setVertexPointers(gl, renderer, vr, toRender) :
											  				   setInterleavedArray(gl, renderer, vr, toRender));

			this.lastRendered = toRender;
			if (abort) {
				data.setStatus(Status.ERROR);
				data.setStatusMessage("VertexBufferObjects used by this geometry have been cleaned-up");
				// don't proceed with the rendering
				return 0;
			}
		}
		
		// render the geometry
		GeometryArray<VertexBufferObject> indices = toRender.indices;
		Handle elementVbo = renderer.getHandle(indices.getArray());
		
		if (elementVbo == null) {
			// have to abort
			data.setStatus(Status.ERROR);
			data.setStatusMessage("VertexBufferObject used for indices has been cleaned-up");
			return 0;
		} else {
			// bind the index buffer
			if (elementVbo.getId() != vr.elementBufferBinding) {
				vr.elementBufferBinding = elementVbo.getId();
				gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER_ARB, vr.elementBufferBinding);
			}
			
			DataType type = indices.getEffectiveType();
			int byteOffset = indices.getAccessor().getOffset() * type.getByteSize();
			gl.glDrawRangeElements(toRender.glPolyType, 0, toRender.vertexCount, indices.getElementCount(), 
							       EnumUtil.getGLType(type), byteOffset);
		return toRender.polyCount;

		}
	}

	@Override
	public void reset() {
		if (this.lastRendered != null) {
			GL gl = this.factory.getGL();
			VertexArrayRecord vr = this.factory.getCurrentContext().getStateRecord().vertexArrayRecord;
			disablePointers(gl, vr, this.lastRendered, null);

			// reset buffer bindings so normal vertex array functions will work
			if (vr.arrayBufferBinding != 0) {
				vr.arrayBufferBinding = 0;
				gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, 0);
			}
			if (vr.elementBufferBinding != 0) {
				vr.elementBufferBinding = 0;
				gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER_ARB, 0);
			}
			

			this.lastRendered = null;
		}
	}

	@Override
	public void cleanUp(Resource resource, ResourceData data) {
		// do nothing -> nothing to cleanup since the vbos are cleaned up separately
	}

	@Override
	@SuppressWarnings("unchecked")
	public void update(Resource resource, ResourceData data, boolean fullUpdate) {
		BufferedGeometryHandle<VertexBufferObject> handle = (BufferedGeometryHandle<VertexBufferObject>) data.getHandle();
		handle = this.geomDriver.updateHandle((VertexBufferGeometry) resource, handle, this.maxTextureUnits, this.maxVertexAttribs);
		data.setHandle(handle);
		
		if (!this.geomDriver.getElementCountsValid(handle)) {
			data.setStatus(Status.ERROR);
			data.setStatusMessage("Element counts in geometry are not all equal");
		} else if (!this.getVboStatusValid(this.factory.getRenderer(), handle)) {
			// this correctly checks for vbo support
			data.setStatus(Status.ERROR);
			data.setStatusMessage("At least one VertexBufferObject in use have a status of CLEANED or ERROR");
		} else {
			data.setStatus(Status.OK);
			data.setStatusMessage("");
		}
		
		// not really necessary, but just in case
		resource.clearDirtyDescriptor();
	}
	
	/* Check the status of each of the handles buffers to make sure that they're
	 * aren't CLEANED or ERROR.  A more inlined approach is necessary in render()
	 * to make sure a vbo hasn't been cleaned behind our backs. */
	private boolean getVboStatusValid(Renderer renderer, BufferedGeometryHandle<VertexBufferObject> handle) {
		Status s;
		
		// check all vertex vbos
		int count = handle.compiledPointers.size();
		for (int i = 0; i < count; i++) {
			s = renderer.getStatus(handle.compiledPointers.get(i).array.getArray());
			if (s == Status.CLEANED || s == Status.ERROR)
				return false;
		}
		
		// check index vbo
		s = renderer.getStatus(handle.indices.getArray());
		if (s == Status.CLEANED || s == Status.ERROR)
			return false;
		
		// at this point, we've checked everything
		return true;
	}
	
	/* Disable all client vertex arrays that were in use by lastUsed that will not be used by
	 * next.  If next is null, then no client arrays are used by next. 
	 * 
	 * This works even if the last used handle was aborted because it referenced an invalid vbo,
	 * since we don't do any vbo binding here. */
	private static void disablePointers(GL gl, VertexArrayRecord vr, BufferedGeometryHandle<VertexBufferObject> lastUsed, BufferedGeometryHandle<VertexBufferObject> next) {
		ArrayPointer<VertexBufferObject> ap;
		int count = lastUsed.compiledPointers.size();
		for (int i = 0; i < count; i++) {
			ap = lastUsed.compiledPointers.get(i);
			if (next == null || next.allPointers[ap.arrayIndex] == null) {
				// need to disable this array pointer
				switch(ap.type) {
				case FOG_COORDINATES:
					vr.enableFogCoordArray = false;
					gl.glDisableClientState(GL.GL_FOG_COORD_ARRAY);
					break;
				case NORMALS:
					vr.enableNormalArray = false;
					gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
					break;
				case VERTICES:
					vr.enableVertexArray = false;
					gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
					break;
				case TEXTURE_COORDINATES:
					// we must also set the clientActiveTexture
					if (ap.unit != vr.clientActiveTexture) {
						vr.clientActiveTexture = ap.unit;
						gl.glClientActiveTexture(GL.GL_TEXTURE0 + ap.unit);
					}
					
					vr.enableTexCoordArrays[ap.unit] = false;
					gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
					break;
				case VERTEX_ATTRIBUTES:
					vr.enableVertexAttribArrays[ap.unit] = false;
					gl.glDisableVertexAttribArray(ap.unit);
					break;
				}
			}
		}
	}
	
	/* Call glInterleavedArrays() with handle's interleaved format.  Assumes that this isn't -1 and
	 * one of the formats chosen by BufferedGeometryDriver.  It correctly sets the client active texture
	 * and updates the vr record to have the correct pointers enabled.  It doesn't flag anything as
	 * disabled, since this should have been done before a call to this method. 
	 * 
	 * As below, this modifies the array buffer binding, and returns true if there were no problems
	 * about the vbos. */
	private static boolean setInterleavedArray(GL gl, AbstractRenderer renderer, VertexArrayRecord vr, BufferedGeometryHandle<VertexBufferObject> toRender) {
		Handle vbo = renderer.getHandle(toRender.compiledPointers.get(0).array.getArray());
		if (vbo == null)
			return false; // abort
		
		// bind the vbo for the interleaved arrays
		if (vbo.getId() != vr.arrayBufferBinding) {
			vr.arrayBufferBinding = vbo.getId();
			gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, vr.arrayBufferBinding);
		}
		
		int texUnit = -1; // will be >= 0 if a tex coord is present
		
		// search for the texture unit in use by the interleaved array
		ArrayPointer<VertexBufferObject> ap;
		int count = toRender.compiledPointers.size();
		// this loop is short, will be at most 3 iterations (T2F_N3F_V3F has 3 compiled buffers).
		for (int i = 0; i < count; i++) {
			ap = toRender.compiledPointers.get(i);
			if (ap.type == PointerType.TEXTURE_COORDINATES) {
				texUnit = ap.unit;
				break;
			}
		}
		
		// set the client texture - only needed if > 0
		// in other cases, it will already be disabled, and should remain disabled
		if (texUnit > 0 && texUnit != vr.clientActiveTexture) {
			vr.clientActiveTexture = texUnit;
			gl.glClientActiveTexture(GL.GL_TEXTURE0 + texUnit);
		}
		
		// set the enabled state of the record
		vr.enableVertexArray = true;
		vr.enableNormalArray = (toRender.glInterleavedType == GL.GL_N3F_V3F || toRender.glInterleavedType == GL.GL_T2F_N3F_V3F);
		if (texUnit >= 0) {
			vr.enableTexCoordArrays[texUnit] = toRender.glInterleavedType != GL.GL_N3F_V3F;
		}
		
		gl.glInterleavedArrays(toRender.glInterleavedType, 0, 0);
		// if we're here, everything is okay
		return true;
	}
	
	/* Enable and set the client array pointers based on the given handle.  It is
	 * assumed that it is not null, and that any unused pointers will be disabled
	 * elsewhere. 
	 * 
	 * This modifies the arrayBufferBinding, so it's necessary to bind that to 0 
	 * in the reset() method.  
	 * 
	 * Returns true if all vbos were bound successfully, or false if the renderer
	 * returned a null handle and the rendering should be aborted. */
	private static boolean setVertexPointers(GL gl, AbstractRenderer renderer, VertexArrayRecord vr, BufferedGeometryHandle<VertexBufferObject> toRender) {
		ArrayPointer<VertexBufferObject> ap;
		
		VertexArray va;
		DataType type;
		
		Handle vbo;
		
		int byteStride;
		int byteOffset;
		int elementSize;
		
		int count = toRender.compiledPointers.size();
		for (int i = 0; i < count; i++) {
			ap = toRender.compiledPointers.get(i);
			vbo = renderer.getHandle(ap.array.getArray());
			if (vbo == null)
				return false; // abort
			
			type = ap.array.getEffectiveType();
			
			va = ap.array.getAccessor();
			byteStride = va.getStride() * type.getByteSize();
			byteOffset = va.getOffset() * type.getByteSize();
			elementSize = va.getElementSize();
			
			// bind the vbo
			if (vbo.getId() != vr.arrayBufferBinding) {
				vr.arrayBufferBinding = vbo.getId();
				gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, vr.arrayBufferBinding);
			}
			
			// possibly enable and set the appropriate array pointer
			switch(ap.type) {
			case FOG_COORDINATES:
				if (!vr.enableFogCoordArray) {
					vr.enableFogCoordArray = true;
					gl.glEnableClientState(GL.GL_FOG_COORD_ARRAY);
				}
				gl.glFogCoordPointer(GL.GL_FLOAT, byteStride, byteOffset);
				break;
				
			case NORMALS:
				if (!vr.enableNormalArray) {
					vr.enableNormalArray = true;
					gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
				}
				gl.glNormalPointer(EnumUtil.getGLType(type), byteStride, byteOffset);
				break;
				
			case VERTICES:
				if (!vr.enableVertexArray) {
					vr.enableVertexArray = true;
					gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
				}
				gl.glVertexPointer(elementSize, EnumUtil.getGLType(type), byteStride, byteOffset);
				break;
				
			case TEXTURE_COORDINATES:
				// we also have to change the clientActiveTexture
				if (vr.clientActiveTexture != ap.unit) {
					vr.clientActiveTexture = ap.unit;
					gl.glClientActiveTexture(GL.GL_TEXTURE0 + ap.unit);
				}
				
				if (!vr.enableTexCoordArrays[ap.unit]) {
					vr.enableTexCoordArrays[ap.unit] = true;
					gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
				}
				gl.glTexCoordPointer(elementSize, EnumUtil.getGLType(type), byteStride, byteOffset);
				break;
				
			case VERTEX_ATTRIBUTES:
				if (!vr.enableVertexAttribArrays[ap.unit]) {
					vr.enableVertexAttribArrays[ap.unit] = true;
					gl.glEnableVertexAttribArray(ap.unit);
				}
				gl.glVertexAttribPointer(ap.unit, elementSize, EnumUtil.getGLType(type), false, byteStride, byteOffset);
				break;
			}
		}
		
		// all vertex vbos were okay, so we can return true
		return true;
	}
}
