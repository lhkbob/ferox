package com.ferox.resource;

import com.ferox.resource.BufferData.DataType;

/** VertexBufferGeometry relies on VertexBufferObjects to 
 * provide the geometry data.  This gives VertexBufferGeometries
 * an edge on speed compared with VertexArrayGeometries, with a few
 * drawbacks.
 * 
 * Because the VertexBufferObjects are stored on the graphics card,
 * each VBO must be updated for the geometry to see any change.
 * VertexBufferObjects are not supported on all hardware, and if
 * they're unavailable, a VertexBufferGeometry cannot be used, either.
 * 
 * Important notes about updating of the VertexBufferGeometry and
 * VertexBufferObjects:
 *  - all vbos used by the geometry must be updated before the geometry is updated
 *  - if any vbo has a status of ERROR/CLEANED, the geometry should have a status of ERROR
 *  - a geometry only needs to be updated when its buffer bindings change, if a vbo
 *    is updated, changes to the vbo will be visible in the rendered geometry afterwards.
 * 
 * @author Michael Ludwig
 *
 */
public class VertexBufferGeometry extends BufferedGeometry<VertexBufferObject> {
	public VertexBufferGeometry(VertexBufferObject vertices, VertexArray vertAccessor, VertexBufferObject indices, VertexArray indexAccessor, PolygonType type) {
		super(vertices, vertAccessor, indices, indexAccessor, type);
	}

	@Override
	protected float get(VertexBufferObject data, int index) {
		Object array = data.getData().getData();
		
		if (array != null) {
			switch(data.getData().getType()) {
			case BYTE: case UNSIGNED_BYTE:
				return ((byte[]) array)[index];
			case SHORT: case UNSIGNED_SHORT:
				return ((short[]) array)[index];
			case INT: case UNSIGNED_INT:
				return ((int[]) array)[index];
			case FLOAT:
				return ((float[]) array)[index];
			}
		}
		
		// this is the best we can do
		return 0f;
	}

	@Override
	protected int getNumElements(VertexBufferObject data, VertexArray accessor) {
		return accessor.getNumElements(data.getData().getCapacity());
	}

	@Override
	protected DataType getEffectiveDataType(VertexBufferObject data, boolean asIndices) {
		return data.getData().getType();
	}
}
