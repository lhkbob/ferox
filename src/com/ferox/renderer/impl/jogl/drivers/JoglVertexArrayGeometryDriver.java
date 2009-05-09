package com.ferox.renderer.impl.jogl.drivers;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.GeometryDriver;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.drivers.BufferedGeometryDriver.ArrayPointer;
import com.ferox.renderer.impl.jogl.drivers.BufferedGeometryDriver.BufferedGeometryHandle;
import com.ferox.renderer.impl.jogl.drivers.BufferedGeometryDriver.PointerType;
import com.ferox.renderer.impl.jogl.record.VertexArrayRecord;
import com.ferox.resource.BufferData;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.IndexedArrayGeometry.GeometryArray;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.geometry.VertexArray;
import com.ferox.resource.geometry.VertexArrayGeometry;
import com.sun.opengl.util.BufferUtil;

/**
 * JoglVertexArrayGeometryDriver handles the rendering of VertexArrayGeometries.
 * It is somewhat liberal in its assumptions about the state record. To be
 * efficient, it assumes that no state driver modifies the VertexArrayRecord and
 * that other geometry drivers properly reset the state that they've modified.
 * 
 * This maintains a mapping of BufferDatas to the native, direct buffers used in
 * the actual opengl calls. When no BufferData references the Buffer anymore,
 * the references are cleared.
 * 
 * @author Michael Ludwig
 * 
 */
public class JoglVertexArrayGeometryDriver implements GeometryDriver {
	/* Class used to keep track of Buffers and the geometries that use them. */
	private static class BufferMap {
		Buffer buffer; // must be direct, with native byte ordering
		Set<VertexArrayGeometry> references;

		public BufferMap(Buffer data) {
			buffer = data;
			references = new HashSet<VertexArrayGeometry>();
		}
	}

	private final JoglContextManager factory;
	private final BufferedGeometryDriver<BufferData, VertexArrayGeometry> geomDriver;
	private final Map<BufferData, BufferMap> bufferMap;

	private final int maxTextureUnits;
	private final int maxVertexAttribs;

	private BufferedGeometryHandle<BufferData> lastRendered;

	public JoglVertexArrayGeometryDriver(JoglContextManager factory) {
		this.factory = factory;
		geomDriver = new BufferedGeometryDriver<BufferData, VertexArrayGeometry>();
		bufferMap = new HashMap<BufferData, BufferMap>();

		maxTextureUnits = factory.getRenderer().getCapabilities()
				.getMaxTextureCoordinates();
		maxVertexAttribs = factory.getRenderer().getCapabilities()
				.getMaxVertexAttributes();
	}

	@Override
	@SuppressWarnings("unchecked")
	public int render(Geometry geom, ResourceData data) {
		GL gl = factory.getGL();
		VertexArrayRecord vr = factory.getRecord().vertexArrayRecord;
		BufferedGeometryHandle<BufferData> toRender = (BufferedGeometryHandle<BufferData>) data
				.getHandle();

		if (toRender != lastRendered) {
			// disable unused pointers
			if (lastRendered != null)
				disablePointers(gl, vr, lastRendered, toRender);

			// update the vertex pointers to match this geometry
			if (toRender.glInterleavedType < 0)
				setVertexPointers(gl, vr, toRender);
			else
				setInterleavedArray(gl, vr, toRender);
			lastRendered = toRender;
		}

		// render the geometry
		GeometryArray<BufferData> indices = toRender.indices;
		Buffer bd = getBuffer(indices.getArray());
		bd.position(indices.getAccessor().getOffset());
		bd.limit(bd.position() + indices.getElementCount());
		gl.glDrawRangeElements(toRender.glPolyType, 0, toRender.vertexCount,
				indices.getElementCount(), JoglUtil
						.getGLType(indices.getType()), bd);

		return toRender.polyCount;
	}

	@Override
	public void reset() {
		if (lastRendered != null) {
			GL gl = factory.getGL();
			VertexArrayRecord vr = factory.getRecord().vertexArrayRecord;
			disablePointers(gl, vr, lastRendered, null);
			lastRendered = null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void cleanUp(Resource resource, ResourceData data) {
		// cleanup the Buffer mappings for this resource
		BufferedGeometryHandle<BufferData> handle = (BufferedGeometryHandle<BufferData>) data
				.getHandle();

		int count = handle.compiledPointers.size();
		BufferMap bm;
		BufferData bd;
		for (int i = 0; i < count; i++) {
			bd = handle.compiledPointers.get(i).array.getArray();
			bm = bufferMap.get(bd);
			if (bm != null && bm.references.contains(resource)) {
				bm.references.remove(resource);
				if (bm.references.size() == 0)
					// no more references left, so remove it
					bufferMap.remove(bd);
			}
		}

		// do indices now
		bd = handle.indices.getArray();
		bm = bufferMap.get(bd);
		if (bm != null && bm.references.contains(resource)) {
			bm.references.remove(resource);
			if (bm.references.size() == 0)
				bufferMap.remove(bd);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void update(Resource resource, ResourceData data, boolean fullUpdate) {
		VertexArrayGeometry geom = (VertexArrayGeometry) resource;
		BufferedGeometryHandle<BufferData> handle = (BufferedGeometryHandle<BufferData>) data
				.getHandle();
		handle = geomDriver.updateHandle(geom, handle, maxTextureUnits,
				maxVertexAttribs);
		data.setHandle(handle);

		if (!geomDriver.getElementCountsValid(handle)) {
			data.setStatus(Status.ERROR);
			data
					.setStatusMessage("Element counts in geometry are not all equal");
		} else {
			data.setStatus(Status.OK);
			data.setStatusMessage("");

			// make sure bufferMap is up-to-date
			BufferMap bm;
			BufferData bd;
			Set<BufferData> usedKeys = new HashSet<BufferData>();
			int count = handle.compiledPointers.size();
			for (int i = 0; i < count; i++) {
				bd = handle.compiledPointers.get(i).array.getArray();
				bm = bufferMap.get(bd);
				if (updateBufferDataEntry(bd, bm, geom, data))
					usedKeys.add(bd);
			}
			// now do indices
			bd = handle.indices.getArray();
			if (updateBufferDataEntry(bd, bufferMap.get(bd), geom, data))
				usedKeys.add(bd);

			// clean-up Buffers no longer used by this geometry,
			// correctly handles indices
			List<BufferData> toRemove = new ArrayList<BufferData>();
			for (Entry<BufferData, BufferMap> entry : bufferMap.entrySet()) {
				bd = entry.getKey();
				bm = entry.getValue();
				if (!usedKeys.contains(bd)) {
					bm.references.remove(resource);
					if (bm.references.size() == 0)
						// no more references left, so remove it
						toRemove.add(bd);
				}
			}
			// must do this in a separate list, to prevent concurrent
			// modifications above
			for (BufferData tr : toRemove)
				bufferMap.remove(tr);
		}

		// not really necessary, but just in case
		resource.clearDirtyDescriptor();
	}

	private boolean updateBufferDataEntry(BufferData bd, BufferMap bm,
			VertexArrayGeometry geom, ResourceData data) {
		if (bd.getData() == null) {
			// error check
			data.setStatus(Status.ERROR);
			data
					.setStatusMessage("Cannot update a VertexArrayGeometry that uses a BufferData with a null array");

			return false;
		} else {
			if (bm == null) {
				// need a new Buffer
				bm = new BufferMap(wrap(bd));
				bm.references.add(geom);
				bufferMap.put(bd, bm);
			} else
				// refill the Buffer
				fill(bm.buffer, bd);

			return true;
		}
	}

	// Must only be called if the VertexArrayGeometry has been updated
	// otherwise there might not be an associated key.
	private Buffer getBuffer(BufferData data) {
		return bufferMap.get(data).buffer;
	}

	/*
	 * Disable all client vertex arrays that were in use by lastUsed that will
	 * not be used by next. If next is null, then no client arrays are used by
	 * next.
	 */
	private void disablePointers(GL gl, VertexArrayRecord vr,
			BufferedGeometryHandle<BufferData> lastUsed,
			BufferedGeometryHandle<BufferData> next) {
		ArrayPointer<BufferData> ap;
		int count = lastUsed.compiledPointers.size();
		for (int i = 0; i < count; i++) {
			ap = lastUsed.compiledPointers.get(i);
			if (next == null || next.allPointers[ap.arrayIndex] == null)
				// need to disable this array pointer
				switch (ap.type) {
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

	/*
	 * Call glInterleavedArrays() with handle's interleaved format. Assumes that
	 * this isn't -1 and one of the formats chosen by BufferedGeometryDriver. It
	 * correctly sets the client active texture and updates the vr record to
	 * have the correct pointers enabled. It doesn't flag anything as disabled,
	 * since this should have been done before a call to this method.
	 */
	private void setInterleavedArray(GL gl, VertexArrayRecord vr,
			BufferedGeometryHandle<BufferData> toRender) {
		int texUnit = -1; // will be >= 0 if a tex coord is present

		// search for the texture unit in use by the interleaved array
		ArrayPointer<BufferData> ap;
		int count = toRender.compiledPointers.size();
		// this loop is short, will be at most 3 iterations (T2F_N3F_V3F has 3
		// compiled buffers).
		for (int i = 0; i < count; i++) {
			ap = toRender.compiledPointers.get(i);
			if (ap.type == PointerType.TEXTURE_COORDINATES) {
				texUnit = ap.unit;
				break;
			}
		}

		// set the client texture - only needed if > 0
		// in other cases, it will already be disabled, and should remain
		// disabled
		if (texUnit > 0 && texUnit != vr.clientActiveTexture) {
			vr.clientActiveTexture = texUnit;
			gl.glClientActiveTexture(GL.GL_TEXTURE0 + texUnit);
		}

		// set the enabled state of the record
		vr.enableVertexArray = true;
		vr.enableNormalArray = (toRender.glInterleavedType == GL.GL_N3F_V3F || toRender.glInterleavedType == GL.GL_T2F_N3F_V3F);
		if (texUnit >= 0)
			vr.enableTexCoordArrays[texUnit] = toRender.glInterleavedType != GL.GL_N3F_V3F;

		Buffer interleaved = getBuffer(
				toRender.compiledPointers.get(0).array.getArray()).clear();
		gl.glInterleavedArrays(toRender.glInterleavedType, 0, interleaved);
	}

	/*
	 * Enable and set the client array pointers based on the given handle. It is
	 * assumed that it is not null, and that any unused pointers will be
	 * disabled elsewhere.
	 */
	private void setVertexPointers(GL gl, VertexArrayRecord vr,
			BufferedGeometryHandle<BufferData> toRender) {
		ArrayPointer<BufferData> ap;

		VertexArray va;
		DataType type;
		Buffer binding;
		int byteStride;
		int elementSize;

		int count = toRender.compiledPointers.size();
		for (int i = 0; i < count; i++) {
			ap = toRender.compiledPointers.get(i);

			type = ap.array.getType();
			binding = getBuffer(ap.array.getArray());

			va = ap.array.getAccessor();
			byteStride = va.getStride() * type.getByteSize();
			elementSize = va.getElementSize();

			// set the buffer region accordingly
			binding.limit(binding.capacity());
			binding.position(va.getOffset());

			// possibly enable and set the appropriate array pointer
			switch (ap.type) {
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
				gl.glNormalPointer(JoglUtil.getGLType(type), byteStride,
						binding);
				break;

			case VERTICES:
				if (!vr.enableVertexArray) {
					vr.enableVertexArray = true;
					gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
				}
				gl.glVertexPointer(elementSize, JoglUtil.getGLType(type),
						byteStride, binding);
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
				gl.glTexCoordPointer(elementSize, JoglUtil.getGLType(type),
						byteStride, binding);
				break;

			case VERTEX_ATTRIBUTES:
				if (!vr.enableVertexAttribArrays[ap.unit]) {
					vr.enableVertexAttribArrays[ap.unit] = true;
					gl.glEnableVertexAttribArray(ap.unit);
				}
				gl.glVertexAttribPointer(ap.unit, elementSize, JoglUtil
						.getGLType(type), false, byteStride, binding);
				break;
			}
		}
	}

	/* Return a nio buffer wrapping bd's array, can't be null. */
	private static Buffer wrap(BufferData bd) {
		switch (bd.getType()) {
		case BYTE:
		case UNSIGNED_BYTE: {
			byte[] array = (byte[]) bd.getData();
			ByteBuffer buffer = BufferUtil.newByteBuffer(array.length);
			buffer.put(array);
			return buffer;
		}
		case INT:
		case UNSIGNED_INT: {
			int[] array = (int[]) bd.getData();
			IntBuffer buffer = BufferUtil.newIntBuffer(array.length);
			buffer.put(array);
			return buffer;
		}
		case SHORT:
		case UNSIGNED_SHORT: {
			short[] array = (short[]) bd.getData();
			ShortBuffer buffer = BufferUtil.newShortBuffer(array.length);
			buffer.put(array);
			return buffer;
		}
		case FLOAT: {
			float[] array = (float[]) bd.getData();
			FloatBuffer buffer = BufferUtil.newFloatBuffer(array.length);
			buffer.put(array);
			return buffer;
		}
		}

		// shouldn't happen
		return null;
	}

	/* Fill an existing array with the given BufferData, can't be null. */
	private static void fill(Buffer nioBuff, BufferData bd) {
		switch (bd.getType()) {
		case BYTE:
		case UNSIGNED_BYTE: {
			byte[] array = (byte[]) bd.getData();
			ByteBuffer buffer = (ByteBuffer) nioBuff.clear();
			buffer.put(array);
		}
		case INT:
		case UNSIGNED_INT: {
			int[] array = (int[]) bd.getData();
			IntBuffer buffer = (IntBuffer) nioBuff.clear();
			buffer.put(array);
		}
		case SHORT:
		case UNSIGNED_SHORT: {
			short[] array = (short[]) bd.getData();
			ShortBuffer buffer = (ShortBuffer) nioBuff.clear();
			buffer.put(array);
		}
		case FLOAT: {
			float[] array = (float[]) bd.getData();
			FloatBuffer buffer = (FloatBuffer) nioBuff.clear();
			buffer.put(array);
		}
		}
	}
}
