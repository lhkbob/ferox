package com.ferox.resource.geometry;

import com.ferox.resource.BufferData;


/** VertexArrayGeometry is a more dynamic version
 * of BufferedGeometry compared to VertexBufferGeometry.
 * Instead of having to manage VertexBufferObjects, too,
 * VertexArrayGeometrys work by submitting the BufferData
 * each time it is rendered.  
 * 
 * Whenever a VertexArrayGeometry is updated, the 
 * BufferDatas being used have the effect of being updated
 * (since they aren't a Resource), and any VertexArrayGeometry
 * referencing that BufferData will be rendered with 
 * the new changes.
 * 
 * @author Michael Ludwig
 *
 */
public class VertexArrayGeometry extends BufferedGeometry<BufferData> {
	public VertexArrayGeometry(BufferData vertices, VertexArray vertAccessor, BufferData indices, VertexArray indexAccessor, PolygonType type) {
		super(vertices, vertAccessor, indices, indexAccessor, type);
	}
	
	public VertexArrayGeometry(BufferedGeometryDescriptor geom) {
		super(geom.getVertices(), geom.getVerticesVertexArray(), geom.getIndices(), geom.getIndicesVertexArray(), geom.getPolygonType());
		this.setNormals(geom.getNormals(), geom.getNormalsVertexArray());
		this.setTextureCoordinates(0, geom.getTextureCoordinates(), geom.getTextureVertexArray());
	}
	
	protected VertexArrayGeometry() {
		super();
	}

	@Override
	protected BufferData getData(BufferData data) {
		return data;
	}
}
