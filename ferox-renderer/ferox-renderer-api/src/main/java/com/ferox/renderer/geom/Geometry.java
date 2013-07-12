/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.geom;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.renderer.ElementBuffer;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.renderer.VertexAttribute;

/**
 * <p/>
 * Geometry is utility provider of indices and vertex attributes to represent renderable
 * shapes. The geometry information should be considered immutable unless something else
 * edits them. Geometry implementations will provide the geometry but will not modify it.
 *
 * @author Michael Ludwig
 */
public interface Geometry {
    /**
     * Return a reasonably tight-fitting bounds over the vertices of this Geometry. The
     * returned bounds should not be modified.
     *
     * @return The bounds of the geometry in its local coordinate system
     */
    @Const
    public AxisAlignedBox getBounds();

    /**
     * Return the polygon type that determines how consecutive vertex elements or indices
     * are converted into polygons.
     *
     * @return The polygon type
     */
    public PolygonType getPolygonType();

    /**
     * <p/>
     * Return a VertexBufferObject containing the index information used to access the
     * vertex attributes of the geometry. Each index selects an element from each of the
     * attributes of the geometry. Consecutive vertices are then combined into polygons
     * based on the {@link #getPolygonType() polygon type} of the geometry.
     * <p/>
     * This can return null if the geometry can be rendered by processing the vertex
     * elements consecutively. See {@link #getIndexCount()} and {@link #getIndexOffset()}
     * for how they are interpreted when the indices are null.
     *
     * @return The indices of the geometry, must have a data type that is not FLOAT if not
     *         null
     */
    public ElementBuffer getIndices();

    /**
     * Return the offset into the indices before the first index is read. If {@link
     * #getIndices()} returns null, it is the vertex offset before the first vertex is
     * rendered.
     *
     * @return The index offset before rendering is started
     */
    public int getIndexOffset();

    /**
     * Return the number of indices to render, or if {@link #getIndices()} returns null,
     * it is the number of vertex elements to render.
     *
     * @return The number of indices or elements to render
     */
    public int getIndexCount();

    /**
     * Return a VertexAttribute containing position vector information describing the
     * geometry. The exact layout within the VertexAttribute is left to the
     * implementation, including the element size of the position elements.
     *
     * @return The vertices for the geometry, cannot be null
     */
    public VertexAttribute getVertices();

    /**
     * Return the VertexAttribute containing normal vector information associated with
     * each vertex returned by {@link #getVertices()}. The exact layout within the
     * returned VertexAttribute is left to the implementation (it may even use the same
     * VertexBufferObject as the vertices and other attributes), but its element size must
     * be 3.
     *
     * @return The normals for the geometry, cannot be null
     */
    public VertexAttribute getNormals();

    /**
     * Return the VertexAttribute containing texture coordinates associated with each
     * vertex returned by {@link #getVertices()}. The exact layout within the attribute is
     * left to the implementation. It is assumed that the texture coordinates represent
     * the logical unwrapping of the surface so the element size should be 2.
     *
     * @return The 2D texture coordinates for the geometry, cannot be null
     */
    public VertexAttribute getTextureCoordinates();

    /**
     * Return the VertexAttribute containing the tangent vectors for the surface. The
     * tangent vectors are orthogonal to the normal vectors at each vertex and form the
     * tangent space of the geometry. It is most commonly used for normal mapping light
     * techniques. It is assumed that the element size is 3.
     *
     * @return The tangent vectors for the geometry, cannot be null
     */
    public VertexAttribute getTangents();
}
