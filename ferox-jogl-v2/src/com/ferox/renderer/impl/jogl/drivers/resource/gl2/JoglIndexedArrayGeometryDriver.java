package com.ferox.renderer.impl.jogl.drivers.resource.gl2;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;

import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.GeometryDriver;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.drivers.DriverProfile;
import com.ferox.renderer.impl.jogl.record.VertexArrayRecord;
import com.ferox.resource.Geometry;
import com.ferox.resource.IndexedArrayGeometry;
import com.ferox.resource.Resource;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.resource.IndexedArrayGeometry.VectorBuffer;
import com.ferox.resource.Resource.Status;
import com.ferox.util.UnitList.Unit;
import com.sun.opengl.util.BufferUtil;

/**
 * JoglIndexedArrayGeometryDriver provides a GeometryDriver implementation for
 * IndexedArrayGeometries and supports all compile types, as well as fall-backs
 * for vbos if they don't have support, or if there isn't enough graphics card
 * memory to allocate them.
 * 
 * @author Michael Ludwig
 */
public class JoglIndexedArrayGeometryDriver implements GeometryDriver, DriverProfile<GL2> {
	private static final int PT_VERTICES = 0;
	private static final int PT_NORMALS = 1;
	private static final int PT_TEXCOORDS = 2;
	private static final int PT_VERTATTRIBS = 3;

	/*
	 * Represents a bindable vertex array, such as used with glVertexPointer or
	 * glNormalPointer, etc.
	 */
	private static class VertexArray {
		// if null, use offset with a vbo
		private FloatBuffer buffer;
		private int offset; // only used for vbo
		private int elementSize;

		// the logical unit, depends on ptType
		private int unit; // (1 << unit) checks tcUsage or vaUsage
		private int ptType; // one of PT_x

		public void setData(float[] data, int elementSize) {
			if (buffer == null || buffer.capacity() < data.length)
				buffer = BufferUtil.newFloatBuffer(data.length);
			buffer.limit(data.length).position(0);
			buffer.put(data).position(0);

			offset = 0;
			this.elementSize = elementSize;

		}

		public void setData(int offset, int elementSize) {
			buffer = null;
			this.offset = offset;
			this.elementSize = elementSize;
		}

		public void setType(int type, int unit) {
			this.unit = unit;
			ptType = type;
		}
	}

	/*
	 * Internal handle class holding onto the compiled data used for rendering.
	 */
	private static class IagHandle implements Handle {
		// layout is as follows:
		// vertices, [normals], [all tex coords], [all vert attibs]
		private List<VertexArray> compiledPointers;
		// array vbo to use for VBO_x, ignore if type doesn't match
		private int arrayVbo;
		private int arrayVboSize;

		// tracks the usage for optional vertex arrays
		private boolean hasNormals;
		private int tcUsage;
		private int vaUsage;

		// values for polygon tracking, used by vertex arrays and vbos
		private int glPolyType;
		private int polyCount;

		private int indexCount;
		private int vertexCount;

		// if null, use indicesOffset with a vbo
		private IntBuffer indices;
		// element vbo for use with VBO_X, ignore if type doesn't match
		private int elementVboSize;
		private int elementVbo;

		private int dlId;

		private CompileType compile;

		@Override
		public int getId() {
			return -1;
		}
	}

	private final JoglContextManager factory;
	private final boolean vboSupported;
	private final int maxTextureCoordinates;
	private final int maxVertexAttribs;

	// lastRendered only tracks handles that aren't display lists
	// e.g. they actually modify state while rendering
	private IagHandle lastRendered;

	public JoglIndexedArrayGeometryDriver(JoglContextManager factory) {
		this.factory = factory;
		vboSupported = factory.getFramework().getCapabilities().getVertexBufferSupport();
		maxTextureCoordinates = factory.getFramework().getCapabilities().getMaxTextureCoordinates();
		maxVertexAttribs = factory.getFramework().getCapabilities().getMaxVertexAttributes();
		lastRendered = null;
	}
	
	@Override
	public GL2 convert(GL2ES2 base) {
		return base.getGL2();
	}
	
	@Override
	public GL2 getGL(JoglContextManager context) {
		return context.getGL().getGL2();
	}

	@Override
	public int render(Geometry geom, ResourceData data) {
		GL2 gl = getGL(factory);
		VertexArrayRecord vr = factory.getRecord().vertexArrayRecord;

		IagHandle handle = (IagHandle) data.getHandle();

		if (handle.compile == CompileType.DISPLAY_LIST)
			// just call the list, we don't care about vbos
			// since the dl data was fetched at compile time
			gl.glCallList(handle.dlId);
		else {
			if (lastRendered != handle) {
				// if we're none, we just auto-update
				if (handle.compile == CompileType.NONE) {
					update(geom, data, false);
					if (data.getStatus() == Status.ERROR)
						return 0; // we've failed, so don't render
				}

				// make sure the vbo state is correct
				if (handle.compile == CompileType.VBO_DYNAMIC || handle.compile == CompileType.VBO_STATIC)
					bindVbos(gl, vr, handle.arrayVbo, handle.elementVbo);
				else
					bindVbos(gl, vr, 0, 0);

				setVertexArrays(gl, vr, handle, lastRendered);
				lastRendered = handle;
			}

			if (handle.indices == null)
				gl.glDrawRangeElements(handle.glPolyType, 0, handle.vertexCount, handle.indexCount, 
									   GL2.GL_UNSIGNED_INT, 0);
			else
				gl.glDrawRangeElements(handle.glPolyType, 0, handle.vertexCount, handle.indexCount,
									   GL2.GL_UNSIGNED_INT, handle.indices.rewind());
		}

		return handle.polyCount;
	}

	@Override
	public void reset() {
		if (lastRendered != null) {
			GL2 gl = getGL(factory);
			VertexArrayRecord vr = factory.getRecord().vertexArrayRecord;

			bindVbos(gl, vr, 0, 0);
			setVertexArrays(gl, vr, null, lastRendered);

			lastRendered = null;
		}
	}

	@Override
	public void cleanUp(Renderer renderer, Resource resource, ResourceData data) {
		GL2 gl = getGL(factory);
		IagHandle handle = (IagHandle) data.getHandle();

		switch (handle.compile) {
		case DISPLAY_LIST:
			// clean-up the list
			gl.glDeleteLists(handle.dlId, 1);
			break;
		case VBO_DYNAMIC:
		case VBO_STATIC:
			// delete the created vbos
			gl.glDeleteBuffers(2, new int[] { handle.elementVbo, handle.arrayVbo }, 0);
			break;
		}
	}

	@Override
	public void update(Renderer renderer, Resource resource, ResourceData data, boolean fullUpdate) {
		update(resource, data, fullUpdate);
	}

	private void update(Resource resource, ResourceData data, boolean fullUpdate) {
		GL2 gl = getGL(factory);
		IndexedArrayGeometry geom = (IndexedArrayGeometry) resource;
		IagHandle handle = (IagHandle) data.getHandle();

		if (handle == null) {
			// must make a new handle, possibly fall back to VAs
			handle = new IagHandle();
			handle.compiledPointers = new ArrayList<VertexArray>();
			if (!vboSupported && (geom.getCompileType() == CompileType.VBO_DYNAMIC || 
				geom.getCompileType() == CompileType.VBO_STATIC))
				handle.compile = CompileType.VERTEX_ARRAY; // fallback
			else
				handle.compile = geom.getCompileType();

			data.setHandle(handle);
		}

		// make sure it's okay to continue
		if (!validateElementCounts(geom)) {
			data.setStatus(Status.ERROR);
			data.setStatusMessage("Element counts in geometry don't match");
			return;
		}

		// generate values for the handle, depending on the compile type
		if (handle.compile == CompileType.DISPLAY_LIST) {
			// we update it for vertex array so that we can render it,
			// and we don't need to create vbos for it
			updateForVertexArray(geom, handle);
			handle.compile = CompileType.VERTEX_ARRAY;

			if (handle.dlId == 0)
				// make a new list id
				handle.dlId = gl.glGenLists(1);

			gl.glNewList(handle.dlId, GL2.GL_COMPILE);
			render(geom, data);
			reset();
			gl.glEndList();

			// reset compile type after rendering
			handle.compile = CompileType.DISPLAY_LIST;

			// we don't need these structures anymore
			handle.compiledPointers.clear();
			handle.indices = null;
		} else if (handle.compile == CompileType.VBO_DYNAMIC || 
				   handle.compile == CompileType.VBO_STATIC) {
			// delegate to this method to allocate and set data
			if (!updateForVbo(gl, factory.getRecord().vertexArrayRecord, geom, handle)) {
				// delete the vbos and reset the ids and sizes
				gl.glDeleteBuffers(2, new int[] { handle.elementVbo, handle.arrayVbo }, 0);
				handle.elementVbo = 0;
				handle.arrayVbo = 0;
				handle.elementVboSize = 0;
				handle.arrayVboSize = 0;
				// now just set up everything as if it were a vertex array
				// geometry
				updateForVertexArray(geom, handle);

				// modify status and finish
				data.setStatus(Status.DIRTY);
				data.setStatusMessage("Not enough memory to allocate VBOs, falling back to vertex arrays");
				return;
			}
		} else
			// delegate to this method to copy the array data into Buffer's
			updateForVertexArray(geom, handle);

		// assign status
		if (handle.compile != geom.getCompileType()) {
			data.setStatus(Status.DIRTY);
			data.setStatusMessage("Using " + handle.compile + " instead of " + geom.getCompileType());
		} else {
			data.setStatus(Status.OK);
			data.setStatusMessage("");
		}
	}

	/*
	 * Update the given indexed array geometry, assuming that it will be
	 * rendered using client-side vertex arrays. This requires that the geom's
	 * arrays be put into direct, native ordered buffers associated with the
	 * handle.
	 * 
	 * This operation should always be successful, and afterwards the handle's
	 * compiled pointers must accurately reflect the vertex data state of geom.
	 */
	private void updateForVertexArray(IndexedArrayGeometry geom, IagHandle handle) {
		// mark everything as unused, to be filled in later
		handle.hasNormals = false;
		handle.tcUsage = 0;
		handle.vaUsage = 0;

		List<VertexArray> compile = handle.compiledPointers;
		int compileIndex = 0;
		VertexArray current;

		current = getVertexArray(compile, compileIndex++);
		current.setType(PT_VERTICES, 0);
		current.setData(geom.getVertices(), 3);

		if (geom.getNormals() != null) {
			current = getVertexArray(compile, compileIndex++);
			current.setType(PT_NORMALS, 0);
			current.setData(geom.getNormals(), 3);
			handle.hasNormals = true;
		}

		List<Unit<VectorBuffer>> vbList = geom.getTextureCoordinates();
		int count = vbList.size();
		Unit<VectorBuffer> vb;
		for (int i = 0; i < count; i++) {
			vb = vbList.get(i);

			if (vb.getUnit() < maxTextureCoordinates) {
				current = getVertexArray(compile, compileIndex++);
				current.setData(vb.getData().getBuffer(), vb.getData().getElementSize());
				current.setType(PT_TEXCOORDS, vb.getUnit());
				handle.tcUsage |= (1 << vb.getUnit());
			}
		}

		vbList = geom.getVertexAttributes();
		count = vbList.size();
		for (int i = 0; i < count; i++) {
			vb = vbList.get(i);

			if (vb.getUnit() < maxVertexAttribs) {
				current = getVertexArray(compile, compileIndex++);
				current.setData(vb.getData().getBuffer(), vb.getData().getElementSize());
				current.setType(PT_VERTATTRIBS, vb.getUnit());
				handle.vaUsage |= (1 << vb.getUnit());
			}
		}

		// remove all vertex arrays that aren't used
		while (compileIndex < compile.size())
			compile.remove(compile.size() - 1);

		// now we have to setup the indices
		handle.polyCount = geom.getPolygonCount();
		handle.vertexCount = geom.getVertexCount();
		handle.glPolyType = JoglUtil.getGLPolygonConnectivity(geom.getPolygonType());

		int[] indices = geom.getIndices();
		if (handle.indices == null || handle.indices.capacity() < indices.length)
			handle.indices = BufferUtil.newIntBuffer(indices.length);
		handle.indices.limit(indices.length).position(0);
		handle.indices.put(indices).position(0);
		handle.indexCount = indices.length;

		// we ignore the values in variables not needed for vertex array
		// rendering
	}

	/*
	 * Perform an update on the given geometry assuming it will be rendered with
	 * vbos (possible already specified in the associated handle). Return true
	 * if the update was successful, otherwise a value of false tells the
	 * primary updater to clean up any existing vbos and fallback to vertex
	 * array rendering for geom.
	 */
	private boolean updateForVbo(GL gl, VertexArrayRecord var, 
								 IndexedArrayGeometry geom, IagHandle handle) {
		// mark everything as unused, to be filled in later
		handle.hasNormals = false;
		handle.tcUsage = 0;
		handle.vaUsage = 0;

		List<VertexArray> compile = handle.compiledPointers;
		int compileIndex = 0;
		int vboOffset = 0;
		VertexArray current;

		current = getVertexArray(compile, compileIndex++);
		current.setType(PT_VERTICES, 0);
		current.setData(vboOffset, 3);
		vboOffset += geom.getVertices().length * 4;

		if (geom.getNormals() != null) {
			current = getVertexArray(compile, compileIndex++);
			current.setType(PT_NORMALS, 0);
			current.setData(vboOffset, 3);
			vboOffset += geom.getNormals().length * 4;
			handle.hasNormals = true;
		}

		List<Unit<VectorBuffer>> vbList = geom.getTextureCoordinates();
		int count = vbList.size();
		Unit<VectorBuffer> vb;
		for (int i = 0; i < count; i++) {
			vb = vbList.get(i);

			if (vb.getUnit() < maxTextureCoordinates) {
				current = getVertexArray(compile, compileIndex++);
				current.setData(vboOffset, vb.getData().getElementSize());
				current.setType(PT_TEXCOORDS, vb.getUnit());
				vboOffset += (vb.getData().getBuffer().length * 4);
				handle.tcUsage |= (1 << vb.getUnit());
			}
		}

		vbList = geom.getVertexAttributes();
		count = vbList.size();
		for (int i = 0; i < count; i++) {
			vb = vbList.get(i);

			if (vb.getUnit() < maxVertexAttribs) {
				current = getVertexArray(compile, compileIndex++);
				current.setData(vboOffset, vb.getData().getElementSize());
				current.setType(PT_VERTATTRIBS, vb.getUnit());
				vboOffset += (vb.getData().getBuffer().length * 4);
				handle.vaUsage |= (1 << vb.getUnit());
			}
		}

		// remove all vertex arrays that aren't used
		while (compileIndex < compile.size())
			compile.remove(compile.size() - 1);

		// at this point, vboOffset is also the size of the needed array vbo
		if (!updateArrayVboData(gl, var, geom, handle, vboOffset))
			// return false to signify status of ERROR
			return false;

		// now we have to setup the indices
		int[] indices = geom.getIndices();
		if (!updateElementVboData(gl, var, indices, handle))
			// return false to set status to ERROR
			return false;

		handle.indexCount = indices.length;
		handle.polyCount = geom.getPolygonCount();
		handle.vertexCount = geom.getVertexCount();
		handle.glPolyType = JoglUtil.getGLPolygonConnectivity(geom.getPolygonType());

		// we ignore the values in variables not needed for vertex array
		// rendering
		return true;
	}

	/*
	 * Fill the array vbo with all of the vertex/normal/etc. data necessary for
	 * the given indexed array geometry. It is assumed that all vertex arrays
	 * will total up to have vboSize number of bytes. If necessary, a new vbo is
	 * allocated.
	 * 
	 * Returns true if everything was updated successfully, otherwise the
	 * geometry driver should fallback to using vertex arrays because there
	 * wasn't enough graphics memory for the update.
	 */
	private boolean updateArrayVboData(GL gl, VertexArrayRecord var, 
									   IndexedArrayGeometry geom, IagHandle handle, int vboSize) {
		if (handle.arrayVbo == 0) {
			int[] id = new int[1];
			gl.glGenBuffers(1, id, 0);
			handle.arrayVbo = id[0];
		}

		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, handle.arrayVbo);
		if (vboSize > handle.arrayVboSize) {
			handle.arrayVboSize = vboSize;
			gl.glBufferData(GL2.GL_ARRAY_BUFFER, vboSize * 4, null, 
							(handle.compile == CompileType.VBO_DYNAMIC ? GL2.GL_STREAM_DRAW 
								   									   : GL2.GL_STATIC_DRAW));
			int error = gl.glGetError();

			if (error == GL2.GL_OUT_OF_MEMORY)
				// fail here
				return false;
		}

		int offset = updateSubArray(gl, geom.getVertices(), 0);
		if (geom.getNormals() != null)
			offset = updateSubArray(gl, geom.getNormals(), offset);

		List<Unit<VectorBuffer>> vbList = geom.getTextureCoordinates();
		int count = vbList.size();
		Unit<VectorBuffer> vb;
		for (int i = 0; i < count; i++) {
			vb = vbList.get(i);
			if (vb.getUnit() < maxTextureCoordinates)
				offset = updateSubArray(gl, vb.getData().getBuffer(), offset);
		}

		vbList = geom.getVertexAttributes();
		count = vbList.size();
		for (int i = 0; i < count; i++) {
			vb = vbList.get(i);
			if (vb.getUnit() < maxVertexAttribs)
				offset = updateSubArray(gl, vb.getData().getBuffer(), offset);
		}

		// restore old binding
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, var.arrayBufferBinding);
		return true;
	}

	/*
	 * Convenience function to fill the currently bound array vbo with the float
	 * data, starting at the given offset (in bytes into the vbo).
	 * 
	 * Returns the new offset that starts right after the last byte of data.
	 */
	private int updateSubArray(GL gl, float[] data, int offset) {
		FloatBuffer wrapped = FloatBuffer.wrap(data);
		gl.glBufferSubData(GL2.GL_ARRAY_BUFFER, offset, data.length * 4, wrapped.clear());

		return offset + data.length * 4;
	}

	/*
	 * Update the element vbo to hold the data in indices. A new vbo is made if
	 * the existing size is inadequate. Returns true if the data was updated
	 * successfully, false implies the vbo geometry should fallback onto using
	 * vertex arrays because their wasn't enough gfx memory
	 */
	private boolean updateElementVboData(GL gl, VertexArrayRecord var, 
										 int[] indices, IagHandle handle) {
		if (handle.elementVbo == 0) {
			int[] id = new int[1];
			gl.glGenBuffers(1, id, 0);
			handle.elementVbo = id[0];
		}

		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, handle.elementVbo);

		if (indices.length > handle.elementVboSize) {
			handle.elementVboSize = indices.length;
			gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indices.length * 4, null, 
							(handle.compile == CompileType.VBO_DYNAMIC ? GL2.GL_STREAM_DRAW 
								   								       : GL2.GL_STATIC_DRAW));
			if (gl.glGetError() == GL2.GL_OUT_OF_MEMORY)
				// abort
				return false;
		}

		IntBuffer wrapped = IntBuffer.wrap(indices);
		gl.glBufferSubData(GL2.GL_ELEMENT_ARRAY_BUFFER, 0, indices.length * 4, wrapped.clear());

		// restore old binding
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, var.elementBufferBinding);
		return true;
	}

	/*
	 * This is auto-vivifying, it will add new VertexArrays into compile, until
	 * get(index) would return a non-null va. For safest use, call with index
	 * incrementing by 1.
	 */
	private VertexArray getVertexArray(List<VertexArray> compile, int index) {
		while (index >= compile.size())
			compile.add(new VertexArray());
		return compile.get(index);
	}

	/* Modify the buffer bindings for both arrays and elements. */
	private void bindVbos(GL gl, VertexArrayRecord vr, int arrayId, int elementId) {
		if (vr.arrayBufferBinding != arrayId) {
			vr.arrayBufferBinding = arrayId;
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, arrayId);
		}

		if (vr.elementBufferBinding != elementId) {
			vr.elementBufferBinding = elementId;
			gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, elementId);
		}
	}

	/*
	 * Modify the client state so that vertex arrays are configured correctly
	 * for handle. This is a multi-step process: If lastHandle is not null, all
	 * vertex arrays not used by handle are disabled. If handle is null, then a
	 * vertex array is not in use by it, so it will be disabled. Then, if handle
	 * isn't null, all vertex arrays used by it are enabled and configured.
	 */
	private void setVertexArrays(GL2 gl, VertexArrayRecord vr, 
								 IagHandle handle, IagHandle lastHandle) {
		int count;
		VertexArray array;

		if (lastHandle != null) {
			// disable pointers used by lastHandle not in use by handle
			// if handle is null, it's disable automatically
			count = lastHandle.compiledPointers.size();
			for (int i = 0; i < count; i++) {
				array = lastHandle.compiledPointers.get(i);

				// note: for any encountered vertex array, we know we're enabled
				// already
				switch (array.ptType) {
				case PT_VERTICES:
					if (handle == null) {
						vr.enableVertexArray = false;
						gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
					}
					break;
				case PT_NORMALS:
					if (handle == null || !handle.hasNormals) {
						vr.enableNormalArray = false;
						gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
					}
					break;
				case PT_TEXCOORDS:
					if (handle == null || (handle.tcUsage & (1 << array.unit)) == 0) {
						vr.clientActiveTexture = array.unit;
						gl.glClientActiveTexture(GL2.GL_TEXTURE0 + array.unit);
						vr.enableTexCoordArrays[array.unit] = false;
						gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
					}
					break;
				case PT_VERTATTRIBS:
					if (handle == null || (handle.vaUsage & (1 << array.unit)) == 0) {
						vr.enableVertexAttribArrays[array.unit] = false;
						gl.glDisableVertexAttribArray(array.unit);
					}
					break;
				}
			}
		}

		if (handle != null) {
			// enable all pointers used by handle that aren't already enabled
			count = handle.compiledPointers.size();
			for (int i = 0; i < count; i++) {
				array = handle.compiledPointers.get(i);

				// note: here we have to check if it's already enabled
				switch (array.ptType) {
				case PT_VERTICES:
					if (!vr.enableVertexArray) {
						vr.enableVertexArray = true;
						gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
					}
					if (array.buffer == null)
						gl.glVertexPointer(array.elementSize, GL2.GL_FLOAT, 0, array.offset);
					else
						gl.glVertexPointer(array.elementSize, GL2.GL_FLOAT, 0, array.buffer.rewind());
					break;
				case PT_NORMALS:
					if (!vr.enableNormalArray) {
						vr.enableNormalArray = true;
						gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
					}
					if (array.buffer == null)
						gl.glNormalPointer(GL2.GL_FLOAT, 0, array.offset);
					else
						gl.glNormalPointer(GL2.GL_FLOAT, 0, array.buffer.rewind());
					break;
				case PT_TEXCOORDS:
					if (vr.clientActiveTexture != array.unit) {
						vr.clientActiveTexture = array.unit;
						gl.glClientActiveTexture(GL2.GL_TEXTURE0 + array.unit);
					}
					if (!vr.enableTexCoordArrays[array.unit]) {
						vr.enableTexCoordArrays[array.unit] = true;
						gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
					}
					if (array.buffer == null)
						gl.glTexCoordPointer(array.elementSize, GL2.GL_FLOAT, 0, array.offset);
					else
						gl.glTexCoordPointer(array.elementSize, GL2.GL_FLOAT, 0, array.buffer.rewind());
					break;
				case PT_VERTATTRIBS:
					if (!vr.enableVertexAttribArrays[array.unit]) {
						vr.enableVertexAttribArrays[array.unit] = true;
						gl.glEnableVertexAttribArray(array.unit);
					}
					if (array.buffer == null)
						gl.glVertexAttribPointer(array.unit, array.elementSize, GL2.GL_FLOAT, false, 0, array.offset);
					else
						gl.glVertexAttribPointer(array.unit, array.elementSize, GL2.GL_FLOAT, false, 0, array.buffer.rewind());
					break;
				}
			}
		}
	}

	/*
	 * Go through the float arrays of iag and make sure they all have the same
	 * element counts, based on the vector size for each buffer type.
	 */
	private boolean validateElementCounts(IndexedArrayGeometry iag) {
		int count = iag.getVertexCount();

		if (count != iag.getVertices().length / 3)
			return false;
		if (iag.getNormals() != null && count != iag.getNormals().length / 3)
			return false;

		List<Unit<VectorBuffer>> tcs = iag.getTextureCoordinates();
		VectorBuffer data;
		for (int i = 0; i < tcs.size(); i++) {
			data = tcs.get(i).getData();
			if (data.getBuffer().length / data.getElementSize() != count)
				return false;
		}

		List<Unit<VectorBuffer>> vas = iag.getVertexAttributes();
		for (int i = 0; i < vas.size(); i++) {
			data = vas.get(i).getData();
			if (data.getBuffer().length / data.getElementSize() != count)
				return false;
		}

		return true;
	}
}
