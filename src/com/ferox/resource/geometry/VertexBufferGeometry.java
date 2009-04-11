package com.ferox.resource.geometry;

import com.ferox.resource.BufferData;


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
	
	public VertexBufferGeometry(BufferedGeometryDescriptor geom) {
		super(geom.getVboVertices(), geom.getVerticesVertexArray(), geom.getVboIndices(), geom.getIndicesVertexArray(), geom.getPolygonType());
		this.setNormals(geom.getVboNormals(), geom.getNormalsVertexArray());
		this.setTextureCoordinates(0, geom.getVboTextureCoordinates(), geom.getTextureVertexArray());
	}
	
	protected VertexBufferGeometry() {
		super();
	}

	@Override
	protected BufferData getData(VertexBufferObject data) {
		return data.getData();
	}
}
