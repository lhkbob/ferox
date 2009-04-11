package com.ferox.resource.geometry;

import com.ferox.resource.BufferData;
import com.ferox.resource.geometry.BufferedGeometry.PolygonType;

/** BufferedGeometryDescriptor incapsulates the most commonly
 * used configuration for a BufferedGeometry:
 * vertices, normals, and 1 set of texture coordinates,
 * plus indices to access them.
 * 
 * All this is is a description of the Geometry, both 
 * varieties of BufferedGeometry can take one as a constructor,
 * making it easy to switch between using vertex arrays
 * and vertex buffers.
 * 
 * The individual methods are not documented.  None of them
 * can return null, and the returned objects must be suitable
 * for use with the appropriately named BufferedGeometry method.
 * 
 * The VertexBufferObjects returned by getVboX() must be updated
 * and cleaned-up manually.
 * 
 * @author Michael Ludwig
 *
 */
public interface BufferedGeometryDescriptor {
	/** Used to describe the geometry of the shape. */
	public BufferData getVertices();
	public BufferData getNormals();
	public BufferData getTextureCoordinates();
	
	public VertexBufferObject getVboVertices();
	public VertexBufferObject getVboNormals();
	public VertexBufferObject getVboTextureCoordinates();
	
	/** Used to describe the connectivity of the shape. */
	public BufferData getIndices();
	public VertexBufferObject getVboIndices();
	public PolygonType getPolygonType();
	
	/** Used to describe how to access the resources for the shape. */
	public VertexArray getVerticesVertexArray();
	public VertexArray getNormalsVertexArray();
	public VertexArray getTextureVertexArray();
	public VertexArray getIndicesVertexArray();
}
