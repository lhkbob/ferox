package com.ferox.util.geom;

import com.ferox.math.bounds.ReadOnlyAxisAlignedBox;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;

/**
 * <p>
 * Geometry is utility provider of indices and vertex attributes to represent
 * renderable shapes. The geometry information should be considered immutable
 * unless something else edits them. Geometry implementations will provide the
 * geometry but will not modify it.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Geometry {
    /**
     * Return a reasonably tight-fitting bounds over the vertices of this
     * Geometry.
     * 
     * @return The bounds of the geometry in its local coordinate system
     */
    public ReadOnlyAxisAlignedBox getBounds();
    
    /**
     * Return the polygon type that determines how consecutive vertex elements
     * or indices are converted into polygons.
     * 
     * @return The polygon type
     */
    public PolygonType getPolygonType();

    /**
     * <p>
     * Return a VertexBufferObject containing the index information used to
     * access the vertex attributes of the geometry. Each index selects an
     * element from each of the attributes of the geometry. Consecutive vertices
     * are then combined into polygons based on the {@link #getPolygonType()
     * polygon type} of the geometry.
     * </p>
     * <p>
     * This can return null if the geometry can be rendered by processing the
     * vertex elements consecutively. See {@link #getIndexCount()} and
     * {@link #getIndexOffset()} for how they are interpreted when the indices
     * are null.
     * </p>
     * 
     * @return The indices of the geometry, must have a data type that is not
     *         FLOAT if not null
     */
    public VertexBufferObject getIndices();

    /**
     * Return the offset into the indices before the first index is read. If
     * {@link #getIndices()} returns null, it is the vertex offset before the
     * first vertex is rendered.
     * 
     * @return The index offset before rendering is started
     */
    public int getIndexOffset();

    /**
     * Return the number of indices to render, or if {@link #getIndices()}
     * returns null, it is the number of vertex elements to render.
     * 
     * @return The number of indices or elements to render
     */
    public int getIndexCount();

    /**
     * Return a VertexAttribute containing position vector information
     * describing the geometry. The exact layout within the VertexAttribute is
     * left to the implementation, including the element size of the position
     * elements.
     * 
     * @return The vertices for the geometry, cannot be null
     */
    public VertexAttribute getVertices();

    /**
     * Return the VertexAttribute containing normal vector information
     * associated with each vertex returned by {@link #getVertices()}. The exact
     * layout within the returned VertexAttribute is left to the implementation
     * (it may even use the same VertexBufferObject as the vertices and other
     * attributes), but its element size must be 3.
     * 
     * @return The normals for the geometry, cannot be null
     */
    public VertexAttribute getNormals();

    /**
     * Return the VertexAttribute containing texture coordinates associated with
     * each vertex returned by {@link #getVertices()}. The exact layout within
     * the attribute is left to the implementation. It is assumed that the
     * texture coordinates represent the logical unwrapping of the surface so
     * the element size should be 2.
     * 
     * @return The 2D texture coordinates for the geometry, cannot be null
     */
    public VertexAttribute getTextureCoordinates();

    /**
     * Return the VertexAttribute containing the tangent vectors for the
     * surface. The tangent vectors are orthogonal to the normal vectors at each
     * vertex and form the tangent space of the geometry. It is most commonly
     * used for normal mapping light techniques. It is assumed that the element
     * size is 3.
     * 
     * @return The tangent vectors for the geometry, cannot be null
     */
    public VertexAttribute getTangents();
}
