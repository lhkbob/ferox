package com.ferox.util.geom;

import com.ferox.resource.Geometry;
import com.ferox.resource.VectorBuffer;

/**
 * PrimitiveGeometry represents a "primitive" shape. Here primitive is
 * interpreted very loosely to mean anything such as a cube, sphere, or other
 * automatically generatable shape. Each PrimitiveGeometry has a subset of three
 * attributes: vertices, normals and a single set of texture coordinates. The
 * geometry can be configured to use different names for these attributes as
 * needed.
 * 
 * @author Michael Ludwig
 */
public abstract class PrimitiveGeometry extends Geometry {
	private String vertexName;
	private String normalName;
	private String tcName;

	/**
	 * Configure the PrimitiveGeometry to use the given CompileType and use the
	 * given names as the initial attribute configuration.
	 * 
	 * @param compileType CompileType for the Geometry
	 * @param vertexName The name for the vertex attribute
	 * @param normalName The name for the normal attribute
	 * @param tcName The name for the texture coordinate attribute
	 * @throws NullPointerException if vertexName, normalName or tcName are null
	 */
	public PrimitiveGeometry(CompileType compileType, String vertexName, String normalName, String tcName) {
		super(compileType);
		if (vertexName == null || normalName == null || tcName == null)
			throw new NullPointerException("Cannot specify null attribute name");
		
		this.vertexName = vertexName;
		this.normalName = normalName;
		this.tcName = tcName;
	}
	
	/**
	 * Return the attribute name used to store normals.
	 * 
	 * @return The normal attribute name
	 */
	public String getNormalName() {
		return normalName;
	}

	/**
	 * Return the attribute name used to store texture coordinates
	 * 
	 * @return The texture coordinate attribute name
	 */
	public String getTextureCoordinateName() {
		return tcName;
	}

	/**
	 * Return the attribute name used to store vertices.
	 * 
	 * @return The vertex attribute name
	 */
	public String getVertexName() {
		return vertexName;
	}
	
	/**
	 * Return the VectorBuffer that holds onto the vertices for this
	 * PrimitiveGeometry. If it does not have any vertices with the
	 * correct attribute name, this will return null.
	 * 
	 * @return VectorBuffer for the vertices attribute
	 */
	public VectorBuffer getVertices() {
		return getAttribute(vertexName);
	}

	/**
	 * Return the VectorBuffer that holds onto the normals for this
	 * PrimitiveGeometry. If it does not have any normals with the correct
	 * attribute name, this will return null.
	 * 
	 * @return VectorBuffer for the normals attribute
	 */
	public VectorBuffer getNormals() {
		return getAttribute(normalName);
	}

	/**
	 * Return the VectorBuffer that holds onto the texture coordinates for this
	 * PrimitiveGeometry. If it does not have any texture coordinates with the
	 * correct attribute name, this will return null.
	 * 
	 * @return VectorBuffer for the texture coordinate attribute
	 */
	public VectorBuffer getTextureCoordinates() {
		return getAttribute(tcName);
	}

	/**
	 * Redefine the attribute names used for vertices, normals and texture
	 * coordinates with this Box. By default vertices, normals and texture
	 * coordinates are assigned attributes using the default names defined in
	 * Geometry. When invoking this method it transfers the VectorBuffers over
	 * to the given names. It will also reuse these names each time
	 * {@link #setAttribute(String, VectorBuffer)} is called again.
	 * 
	 * @param vertices New attribute name for vertices
	 * @param normals New attribute name for normals
	 * @param texCoords New attribute name for texture coordinates
	 * @throws NullPointerException if vertices, normals, or texCoords are null
	 */
	public void redefineAttributes(String vertices, String normals, String texCoords) {
		if (vertices == null || normals == null || texCoords == null)
			throw new NullPointerException("Cannot specify null attribute name");
		
		VectorBuffer verts = removeAttribute(vertexName);
		VectorBuffer norms = removeAttribute(normalName);
		VectorBuffer tcs = removeAttribute(tcName);
		
		setAttribute(vertices, verts);
		setAttribute(normals, norms);
		setAttribute(texCoords, tcs);
		
		vertexName = vertices;
		normalName = normals;
		tcName = texCoords;
	}
}
