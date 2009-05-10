package com.ferox.renderer.impl.jogl.drivers.geom;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.resource.IndexedArrayGeometry;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.IndexedArrayGeometry.GeometryArray;
import com.ferox.resource.geometry.VertexArray;
import com.ferox.util.UnitList.Unit;

/**
 * BufferedGeometryDriver provides some utility functions that will be common
 * for any GeometryDriver implemented for a sub-class of IndexedArrayGeometry.
 * 
 * @author Michael Ludwig
 * 
 */
public class BufferedGeometryDriver<D> {
	/**
	 * Supported glXPointer() types, mirroring the available bindings in
	 * IndexedArrayGeometry. This doesn't include indices since they are not used
	 * that way.
	 */
	public static enum PointerType {
		VERTICES, NORMALS, FOG_COORDINATES, TEXTURE_COORDINATES,
		VERTEX_ATTRIBUTES
	}

	/**
	 * Simple class that represents how a GeometryArray should be used, and
	 * provides an index into its associated Handle for easier random-access
	 * checking.
	 * 
	 * All pointers of the same type and logical unit will have the same
	 * arrayIndex value.
	 */
	public static class ArrayPointer<D> {
		public D array;
		public int arrayIndex; // index into handle's allPointers[]
		public int unit; // logical unit based on pointer type (e.g. texture unit)
		public int elementSize; // size of vector
		public PointerType type;
	}

	/**
	 * A handle that can be used for geometry drivers that handle sub-classes of
	 * IndexedArrayGeometry. The public fields should be considered final, unless
	 * during an update() call to the driver.
	 * 
	 * All BufferedGeometryHandles will have the same sized allPointers array
	 * (which represents all available binding points for ArrayPointers). If an
	 * index into allPointers is null, it means there is no ArrayPointer present
	 * in compiledPointers for that arrayIndex (unique combination of type and
	 * unit). This holds if maxTextureUnits and maxVertexAttribs across constant
	 * between handls and array pointers.
	 */
	public static class BufferedGeometryHandle<D> implements Handle {
		public final List<ArrayPointer<D>> compiledPointers;
		public final ArrayPointer<D>[] allPointers;

		public D indices;

		public int polyCount;
		public int vertexCount;

		public int glPolyType;
		public int glInterleavedType; // -1 if not interleaved

		@SuppressWarnings("unchecked")
		public BufferedGeometryHandle(int maxTextureUnits, int maxVertexAttribs) {
			this.compiledPointers = new ArrayList<ArrayPointer<D>>();
			this.allPointers = new ArrayPointer[3 + maxTextureUnits
					+ maxVertexAttribs];
		}

		@Override
		public int getId() {
			return -1;
		}
	}

	/**
	 * Update the given handle to store the current state of the given geometry.
	 * This also detects if the geometry's rendering efficiency can be improved
	 * by using a call to glInterleavedArrays(). The interleaved type will be
	 * set to one of: -1 = no interleaving GL_N3F_V3F GL_T2F_V3F GL_T4F_V4F
	 * GL_T2F_N3F_V3F
	 * 
	 * An interleaved type involving texture coordinates requires that there be
	 * only one bound texture coordinate array.
	 * 
	 * The compiled pointers in the handle will not include any texture
	 * coordinates or vertex attributes that would be bound to invalid units.
	 * 
	 * If handle is null, a new one is created. This method does no error
	 * checking. It assumes that geom is not null.
	 */
	public BufferedGeometryHandle<D> updateHandle(IndexedArrayGeometry geom,
			BufferedGeometryHandle<D> handle, int maxTextureUnits,
			int maxVertexAttributes) {
		if (handle == null)
			handle = new BufferedGeometryHandle<D>(maxTextureUnits,
					maxVertexAttributes);

		// reset the allPointers array and compiledPointers list
		for (int i = 0; i < handle.allPointers.length; i++)
			handle.allPointers[i] = null;
		handle.compiledPointers.clear();

		// vertices
		ArrayPointer<D> ap = new ArrayPointer<D>();
		ap.array = geom.getVertices();
		ap.type = PointerType.VERTICES;
		ap.unit = 0;
		ap.arrayIndex = 0;
		handle.compiledPointers.add(ap);

		// normals
		if (geom.getNormals() != null) {
			ap = new ArrayPointer<D>();
			ap.array = geom.getNormals();
			ap.type = PointerType.NORMALS;
			ap.unit = 0;
			ap.arrayIndex = 1;
			handle.compiledPointers.add(ap);
		}

		// fog coords
		if (geom.getFogCoordinates() != null) {
			ap = new ArrayPointer<D>();
			ap.array = geom.getFogCoordinates();
			ap.type = PointerType.FOG_COORDINATES;
			ap.unit = 0;
			ap.arrayIndex = 2;
			handle.compiledPointers.add(ap);
		}

		// texture coordinates
		List<Unit<GeometryArray<D>>> list = geom.getTextureCoordinates();
		Unit<GeometryArray<D>> unit;
		int size = list.size();
		for (int i = 0; i < size; i++) {
			unit = list.get(i);
			if (unit.getUnit() < maxTextureUnits) {
				// we only keep valid units
				ap = new ArrayPointer<D>();
				ap.array = unit.getData();
				ap.type = PointerType.TEXTURE_COORDINATES;
				ap.unit = unit.getUnit();
				ap.arrayIndex = 3 + ap.unit;
				handle.compiledPointers.add(ap);
			}
		}

		// vertex attributes
		list = geom.getVertexAttributes();
		size = list.size();
		for (int i = 0; i < size; i++) {
			unit = list.get(i);
			if (unit.getUnit() < maxVertexAttributes) {
				// we only keep valid units
				ap = new ArrayPointer<D>();
				ap.array = unit.getData();
				ap.type = PointerType.VERTEX_ATTRIBUTES;
				ap.unit = unit.getUnit();
				ap.arrayIndex = 3 + maxTextureUnits + ap.unit;
				handle.compiledPointers.add(ap);
			}
		}

		// set all pointers into allPointers
		size = handle.compiledPointers.size();
		for (int i = 0; i < size; i++) {
			ap = handle.compiledPointers.get(i);
			handle.allPointers[ap.arrayIndex] = ap;
		}

		// indices
		handle.indices = geom.getIndices();

		handle.glPolyType = JoglUtil.getGLPolygonConnectivity(geom
				.getPolygonType());
		handle.glInterleavedType = detectInterleavedType(geom.getVertices(),
				geom.getNormals(), geom.getFogCoordinates(), geom
						.getTextureCoordinates(), geom.getVertexAttributes());

		handle.polyCount = geom.getPolygonCount();
		handle.vertexCount = geom.getVertexCount();

		return handle;
	}

	/**
	 * Return true if all of the "vertex" components in the handle have the same
	 * element count as the actual vertices GeometryArray. Returns false
	 * otherwise, which implies that the IndexedArrayGeometry should have a status
	 * of ERROR.
	 * 
	 * Assumes that handle is not null and was created/updated from a call to
	 * updateHandle().
	 */
	public boolean getElementCountsValid(BufferedGeometryHandle<D> handle) {
		int numElements = handle.compiledPointers.get(0).array
				.getElementCount();

		for (int i = 1; i < handle.compiledPointers.size(); i++)
			if (handle.compiledPointers.get(i).array.getElementCount() != numElements)
				return false;

		// we don't have to check indices
		return true;
	}
}
