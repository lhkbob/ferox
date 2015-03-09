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
package com.ferox.math.mesh;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;

import java.util.Map;

/**
 * Triangle represents the primitive formed by three positions in 3D as well as arbitrary attributes stored
 * per vertex. A triangle belongs to a particular mesh, which is a soup of triangles. The mesh is responsible
 * for determining connectivity between the triangles based on colocality and attribute values.
 *
 * @author Michael Ludwig
 */
public final class Triangle {
    /**
     * Enumeration labeling the three vertices of a triangle. Each vertex has its own position and attribute
     * state.
     */
    public static enum Vertex {
        A(Edge.CA, Edge.AB),
        B(Edge.AB, Edge.BC),
        C(Edge.BC, Edge.CA);

        private final Edge in, out;

        private Vertex(Edge i, Edge o) {
            in = i;
            out = o;
        }

        /**
         * @return The edge that has this vertex as its tail
         */
        public Edge getIncomingEdge() {
            return in;
        }

        /**
         * @return The edge that has this vertex as its head
         */
        public Edge getOutgoingEdge() {
            return out;
        }
    }

    /**
     * Enumeration specifying the three possible edges of the triangle.
     */
    public static enum Edge {
        AB(Vertex.A, Vertex.B),
        BC(Vertex.B, Vertex.C),
        CA(Vertex.C, Vertex.A);

        private final Vertex first, second;

        private Edge(Vertex f, Vertex s) {
            first = f;
            second = s;
        }

        /**
         * @return The vertex at the start of the edge
         */
        public Vertex getHead() {
            return first;
        }

        /**
         * @return The vertex at the end of the edge
         */
        public Vertex getTail() {
            return second;
        }
    }

    final Mesh mesh;
    final int index;

    Triangle(Mesh mesh, int index) {
        this.mesh = mesh;
        this.index = index;
    }

    /**
     * Return the position of the specified vertex. This creates a new vector to store the position. Use
     * {@link #getPosition(com.ferox.math.mesh.Triangle.Vertex, com.ferox.math.Vector3)} with an existing
     * instance for optimized allocation patterns.
     *
     * @param v The vertex to fetch
     *
     * @return The position of the vertex in 3D
     */
    public Vector3 getPosition(Vertex v) {
        return getPosition(v, null);
    }

    /**
     * Return the position of the specified vertex. The position is stored in <var>result</var>, which is also
     * returned. If <var>result</var> is null, a new vector is created and returned.
     *
     * @param v      The vertex to fetch
     * @param result The vector that stores the position
     *
     * @return result or a new vector holding the position
     */
    public Vector3 getPosition(Vertex v, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }
        return result.set(mesh.vertices, mesh.toIndex(this, v) * 3);
    }

    /**
     * Set the 3D position of the specified vertex. The state of <var>p</var> is copied into the triangle's
     * state, future modifications to <var>p</var> will not result in changes to this triangle.
     *
     * @param v The vertex to modify
     * @param p The new position of the vertex
     *
     * @throws java.lang.NullPointerException if p is null
     */
    public void setPosition(Vertex v, @Const Vector3 p) {
        p.get(mesh.vertices, mesh.toIndex(this, v) * 3);
    }

    /**
     * Set the location of all three vertices of the triangle in a single method call.
     *
     * @param a The location for vertex A
     * @param b The location for vertex B
     * @param c The location for vertex C
     *
     * @throws java.lang.NullPointerException if a, b, or c are null
     */
    public void set(@Const Vector3 a, @Const Vector3 b, @Const Vector3 c) {
        setPosition(Vertex.A, a);
        setPosition(Vertex.B, b);
        setPosition(Vertex.C, c);
    }

    /**
     * Set the 3D position of the specified vertex. The state of <var>p</var> is copied into the triangle's
     * state. This will trigger recomputing the face normal and edges.  All triangles connected to the vertex
     * <var>v</var> according to {@link Mesh#getConnected(Triangle, com.ferox.math.mesh.Triangle.Vertex)}
     * before the position is changed will also be set to the new location.
     * <p/>
     * This effectively keeps all connected triangle vertices connected after a move and is suitable for use
     * in a modeling program or tool where it is expected that the vertices be shared.
     *
     * @param v The vertex to modify.
     * @param p The new position of the vertex (and all connected vertices)
     *
     * @throws java.lang.NullPointerException if p is null
     */
    public void setStickyPosition(Vertex v, @Const Vector3 p) {
        Map<Triangle, Vertex> neighbors = mesh.getConnected(this, v);
        for (Map.Entry<Triangle, Vertex> n : neighbors.entrySet()) {
            // the connected set is determined by current position, so any triangle connected to n
            // should also be connected to this triangle, use setPosition to prevent infinite recursion
            // from setStickyPosition looping on itself
            n.getKey().setPosition(n.getValue(), p);
        }
        setPosition(v, p);
    }

    /**
     * Get the normal vector for the triangle, oriented with the CCW winding of the vertex positions. This
     * creates a new instance.
     *
     * @return The unit vector orthogonal to the triangle plane
     */
    public Vector3 getNormal() {
        return getNormal(null);
    }

    /**
     * Get the normal vector for the triangle, and store it in <var>result</var>. The normal is oriented based
     * on the CCW winding fo the vertex positions. <var>result</var> is returned, or a new vector is created
     * and returned if it is null.
     *
     * @param result The result vector
     *
     * @return The normal in result, or a new vector
     */
    public Vector3 getNormal(Vector3 result) {
        result = getPosition(Vertex.A, result);
        Vector3 ba = getPosition(Vertex.B, mesh.temp1).sub(result);
        Vector3 ca = getPosition(Vertex.C, mesh.temp2).sub(result);
        result.cross(ba, ca).normalize();
        return result;
    }

    /**
     * Get the edge direction vector pointing from the head to the tail vertex. It is not normalized, e.g. for
     * the edge AB then A + AB = B. A new vector is created and returned.
     *
     * @param e The edge of the triangle
     *
     * @return The vector between the two vertices
     */
    public Vector3 getEdge(Edge e) {
        return getEdge(e, null);
    }

    /**
     * Get the edge direction vector pointing from the head to the tail vertex. It is not normalized, e.g. for
     * the edge AB then A + AB = B. The direction is stored in <var>result</var>, or a new vector is created
     * if it is null.
     *
     * @param e      The edge of the triangle
     * @param result The result to hold the edge
     *
     * @return The direction in result, or new vector if result is null
     */
    public Vector3 getEdge(Edge e, Vector3 result) {
        result = getPosition(e.getTail(), result);
        Vector3 head = getPosition(e.getHead(), mesh.temp1);
        result.sub(head);
        return result;
    }

    /**
     * Get the generic attribute by the given <var>name</var> for the vertex <var>v</var>. Depending on how
     * the attribute was specified in the mesh, the y, z, and w components may be undefined if its element
     * size is less than 4. The attribute must have been allocated for the mesh.
     * <p/>
     * A new vector is created and returned.
     *
     * @param name The attribute name
     * @param v    The vertex of the triangle
     *
     * @return The attribute's value
     *
     * @throws java.lang.NullPointerException if the attribute is not defined for the mesh
     */
    public Vector4 getAttribute(String name, Vertex v) {
        return getAttribute(mesh.getAttributeIndex(name), v, null);
    }

    /**
     * Get the generic attribute by the given <var>name</var> for the vertex <var>v</var>. Depending on how
     * the attribute was specified in the mesh, the y, z, and w components may be undefined if its element
     * size is less than 4. The attribute must have been allocated for the mesh.
     * <p/>
     * The attribute is stored into <var>result</var>, and if it is null a new vector is created and
     * returned.
     *
     * @param attrIndex The attribute index, from {@link Mesh#getAttributeIndex(String)}
     * @param v         The vertex of the triangle
     * @param result    The result to hold the attribute
     *
     * @return The attribute's value
     *
     * @throws java.lang.IndexOutOfBoundsException if the attribute is not defined for the mesh
     */
    public Vector4 getAttribute(int attrIndex, Vertex v, Vector4 result) {
        if (result == null) {
            result = new Vector4();
        }

        int size = mesh.elementSize(attrIndex);
        int baseIndex = mesh.toIndex(this, v) * size;

        switch (size) {
        case 4:
            result.w = mesh.attrs[attrIndex][baseIndex + 3];
        case 3:
            result.z = mesh.attrs[attrIndex][baseIndex + 2];
        case 2:
            result.y = mesh.attrs[attrIndex][baseIndex + 1];
        case 1:
            result.x = mesh.attrs[attrIndex][baseIndex];
        }

        return result;
    }

    /**
     * Set the generic attribute value named <var>name</var> for the vertex <var>v</var>. Although it takes a
     * Vector4, depending on the element size of the attribute, some fields of the vector will not be read.
     * The attribute must have been allocated for the mesh.
     * <p/>
     * The state of <var>a</var> is copied into this triangle.
     *
     * @param name The attribute name
     * @param v    The vertex of the triangle
     * @param a    The new attribute value.
     */
    public void setAttribute(String name, Vertex v, @Const Vector4 a) {
        setAttribute(mesh.getAttributeIndex(name), v, a);
    }

    /**
     * Set the generic attribute value named <var>name</var> for the vertex <var>v</var>. Although it takes a
     * Vector4, depending on the element size of the attribute, some fields of the vector will not be read.
     * The attribute must have been allocated for the mesh.
     * <p/>
     * The state of <var>a</var> is copied into this triangle.
     *
     * @param attrIndex The attribute index, from {@link Mesh#getAttributeIndex(String)}
     * @param v    The vertex of the triangle
     * @param a    The new attribute value.
     */
    public void setAttribute(int attrIndex, Vertex v, @Const Vector4 a) {
        int size = mesh.elementSize(attrIndex);
        int baseIndex = mesh.toIndex(this, v) * size;
        switch(size) {
        case 4:
            mesh.attrs[attrIndex][baseIndex + 3] = (float) a.w;
        case 3:
            mesh.attrs[attrIndex][baseIndex + 2] = (float) a.z;
        case 2:
            mesh.attrs[attrIndex][baseIndex + 1] = (float) a.y;
        case 1:
            mesh.attrs[attrIndex][baseIndex] = (float) a.x;
        }
    }

    /**
     * @return The Mesh that this triangle belongs to
     */
    public Mesh getMesh() {
        return mesh;
    }

    @Override
    public int hashCode() {
        return (mesh.hashCode() + 1) * index;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Triangle))
            return false;
        Triangle t = (Triangle) o;
        return t.mesh == mesh && t.index == index;
    }
}
