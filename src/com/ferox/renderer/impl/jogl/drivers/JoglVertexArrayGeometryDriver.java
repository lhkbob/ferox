package com.ferox.renderer.impl.jogl.drivers;

import java.nio.Buffer;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.GeometryDriver;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.drivers.BufferedGeometryDriver.ArrayPointer;
import com.ferox.renderer.impl.jogl.drivers.BufferedGeometryDriver.BufferedGeometryHandle;
import com.ferox.renderer.impl.jogl.drivers.BufferedGeometryDriver.PointerType;
import com.ferox.renderer.impl.jogl.record.VertexArrayRecord;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource;
import com.ferox.resource.VertexArray;
import com.ferox.resource.VertexArrayGeometry;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.BufferedGeometry.GeometryArray;
import com.ferox.resource.Resource.Status;

/** JoglVertexArrayGeometryDriver handles the rendering of VertexArrayGeometries.
 * It is somewhat liberal in its assumptions about the state record.  To be
 * efficient, it assumes that no state driver modifies the VertexArrayRecord 
 * and that other geometry drivers properly reset the state that they've modified.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglVertexArrayGeometryDriver implements GeometryDriver {
	private JoglSurfaceFactory factory;
	private BufferedGeometryDriver<Buffer, VertexArrayGeometry> geomDriver;
	
	private int maxTextureUnits;
	private int maxVertexAttribs;
	
	private BufferedGeometryHandle<Buffer> lastRendered;
	
	public JoglVertexArrayGeometryDriver(JoglSurfaceFactory factory) {
		this.factory = factory;
		this.geomDriver = new BufferedGeometryDriver<Buffer, VertexArrayGeometry>();
		
		this.maxTextureUnits = factory.getRenderer().getCapabilities().getMaxTextureCoordinates();
		this.maxVertexAttribs = factory.getRenderer().getCapabilities().getMaxVertexAttributes();
	}

	@Override
	@SuppressWarnings("unchecked")
	public int render(Geometry geom, ResourceData data) {
		GL gl = this.factory.getGL();
		VertexArrayRecord vr = this.factory.getRecord().vertexArrayRecord;
		
		BufferedGeometryHandle<Buffer> toRender = (BufferedGeometryHandle<Buffer>) data.getHandle();

		if (toRender != this.lastRendered) {
			// disable unused pointers
			if (this.lastRendered != null)
				disablePointers(gl, vr, this.lastRendered, toRender);
			
			// update the vertex pointers to match this geometry
			if (toRender.glInterleavedType < 0)
				setVertexPointers(gl, vr, toRender);
			else
				setInterleavedArray(gl, vr, toRender);
			this.lastRendered = toRender;
		}
		
		// render the geometry
		GeometryArray<Buffer> indices = toRender.indices;
		Buffer bd = indices.getArray();
		bd.position(indices.getAccessor().getOffset());
		bd.limit(bd.capacity());
		gl.glDrawRangeElements(toRender.glPolyType, 0, toRender.vertexCount, indices.getElementCount(), 
							   EnumUtil.getGLType(indices.getEffectiveType()), indices.getArray().clear());
		
		return toRender.polyCount;
	}

	@Override
	public void reset() {
		if (this.lastRendered != null) {
			GL gl = this.factory.getGL();
			VertexArrayRecord vr = this.factory.getRecord().vertexArrayRecord;
			disablePointers(gl, vr, this.lastRendered, null);
			this.lastRendered = null;
		}
	}

	@Override
	public void cleanUp(Resource resource, ResourceData data) {
		// do nothing -> no cleanUp necessary
	}

	@Override
	@SuppressWarnings("unchecked")
	public void update(Resource resource, ResourceData data, boolean fullUpdate) {
		BufferedGeometryHandle<Buffer> handle = (BufferedGeometryHandle<Buffer>) data.getHandle();
		handle = this.geomDriver.updateHandle((VertexArrayGeometry) resource, handle, this.maxTextureUnits, this.maxVertexAttribs);
		data.setHandle(handle);
		
		if (!this.geomDriver.getElementCountsValid(handle)) {
			data.setStatus(Status.ERROR);
			data.setStatusMessage("Element counts in geometry are not all equal");
		} else {
			data.setStatus(Status.OK);
			data.setStatusMessage("");
		}
		
		// not really necessary, but just in case
		resource.clearDirtyDescriptor();
	}
	
	/* Disable all client vertex arrays that were in use by lastUsed that will not be used by
	 * next.  If next is null, then no client arrays are used by next. */
	private static void disablePointers(GL gl, VertexArrayRecord vr, BufferedGeometryHandle<Buffer> lastUsed, BufferedGeometryHandle<Buffer> next) {
		ArrayPointer<Buffer> ap;
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
	 * disabled, since this should have been done before a call to this method. */
	private static void setInterleavedArray(GL gl, VertexArrayRecord vr, BufferedGeometryHandle<Buffer> toRender) {
		int texUnit = -1; // will be >= 0 if a tex coord is present
		
		// search for the texture unit in use by the interleaved array
		ArrayPointer<Buffer> ap;
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
		
		Buffer interleaved = toRender.compiledPointers.get(0).array.getArray().clear();
		gl.glInterleavedArrays(toRender.glInterleavedType, 0, interleaved);
	}
	
	/* Enable and set the client array pointers based on the given handle.  It is
	 * assumed that it is not null, and that any unused pointers will be disabled
	 * elsewhere. */
	private static void setVertexPointers(GL gl, VertexArrayRecord vr, BufferedGeometryHandle<Buffer> toRender) {
		ArrayPointer<Buffer> ap;
		
		VertexArray va;
		DataType type;
		Buffer binding;
		int byteStride;
		int elementSize;
		
		int count = toRender.compiledPointers.size();
		for (int i = 0; i < count; i++) {
			ap = toRender.compiledPointers.get(i);
			
			type = ap.array.getEffectiveType();
			binding = ap.array.getArray();
			
			va = ap.array.getAccessor();
			byteStride = va.getStride() * type.getByteSize();
			elementSize = va.getElementSize();
			
			// set the buffer region accordingly
			binding.limit(binding.capacity());
			binding.position(va.getOffset());
			
			// possibly enable and set the appropriate array pointer
			switch(ap.type) {
			case FOG_COORDINATES:
				if (!vr.enableFogCoordArray) {
					vr.enableFogCoordArray = true;
					gl.glEnableClientState(GL.GL_FOG_COORD_ARRAY);
				}
				gl.glFogCoordPointer(GL.GL_FLOAT, byteStride, binding);
				break;
				
			case NORMALS:
				if (!vr.enableNormalArray) {
					vr.enableNormalArray = true;
					gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
				}
				gl.glNormalPointer(EnumUtil.getGLType(type), byteStride, binding);
				break;
				
			case VERTICES:
				if (!vr.enableVertexArray) {
					vr.enableVertexArray = true;
					gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
				}
				gl.glVertexPointer(elementSize, EnumUtil.getGLType(type), byteStride, binding);
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
				gl.glTexCoordPointer(elementSize, EnumUtil.getGLType(type), byteStride, binding);
				break;
				
			case VERTEX_ATTRIBUTES:
				if (!vr.enableVertexAttribArrays[ap.unit]) {
					vr.enableVertexAttribArrays[ap.unit] = true;
					gl.glEnableVertexAttribArray(ap.unit);
				}
				gl.glVertexAttribPointer(ap.unit, elementSize, EnumUtil.getGLType(type), false, byteStride, binding);
				break;
			}
		}
	}
}
