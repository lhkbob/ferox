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
import com.ferox.math.Vector3;
import com.ferox.renderer.*;

/**
 *
 */
class BoxImpl implements Geometry {
    // Holds vertices, normals, texture coordinates packed as V3F_N3F_TC2F_T4F
    private final VertexBuffer vertexAttributes;
    private final ElementBuffer indices;

    private final VertexAttribute vertices;
    private final VertexAttribute normals;
    private final VertexAttribute texCoords;
    private final VertexAttribute tangents;

    private final AxisAlignedBox bounds;

    public BoxImpl(Framework framework, @Const Vector3 min, @Const Vector3 max) {

        if (min == null || max == null) {
            throw new NullPointerException("Min and max vectors cannot be null");
        }

        if (min.x > max.x || min.y > max.y || min.z > max.z) {
            throw new IllegalArgumentException("Min vertex has coordinate greater than 'max': " + min +
                                               " - " +
                                               max);
        }

        float maxX = (float) max.x;
        float maxY = (float) max.y;
        float maxZ = (float) max.z;
        float minX = (float) min.x;
        float minY = (float) min.y;
        float minZ = (float) min.z;

        int i = 0;
        float[] va = new float[288]; // 72v + 72n + 48tc + 96t

        // 3 verts per triangle, 2 triangles per face, 6 faces = 36
        int[] indices = new int[] { 0, 1, 2, 2, 3, 0, // BACK
                                    4, 5, 6, 6, 7, 4, // RIGHT
                                    8, 9, 10, 10, 11, 8, // FRONT
                                    12, 13, 14, 14, 15, 12, // LEFT
                                    16, 17, 18, 18, 19, 16, // TOP
                                    20, 21, 22, 22, 23, 20, // BOTTOM
        };
        this.indices = framework.newElementBuffer().fromUnsigned(indices).build();

        // back
        /* v */
        va[i++] = minX;
        va[i++] = maxY;
        va[i++] = minZ;
        /* n */
        va[i++] = 0f;
        va[i++] = 0f;
        va[i++] = -1f;
        /* t */
        va[i++] = 1f;
        va[i++] = 1f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = maxX;
        va[i++] = maxY;
        va[i++] = minZ;
        /* n */
        va[i++] = 0f;
        va[i++] = 0f;
        va[i++] = -1f;
        /* t */
        va[i++] = 0f;
        va[i++] = 1f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = maxX;
        va[i++] = minY;
        va[i++] = minZ;
        /* n */
        va[i++] = 0f;
        va[i++] = 0f;
        va[i++] = -1f;
        /* t */
        va[i++] = 0f;
        va[i++] = 0f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = minX;
        va[i++] = minY;
        va[i++] = minZ;
        /* n */
        va[i++] = 0f;
        va[i++] = 0f;
        va[i++] = -1f;
        /* t */
        va[i++] = 1f;
        va[i++] = 0f;
        // skip tangent
        i += 4;

        // right
        /* v */
        va[i++] = maxX;
        va[i++] = maxY;
        va[i++] = minZ;
        /* n */
        va[i++] = 1f;
        va[i++] = 0f;
        va[i++] = 0f;
        /* t */
        va[i++] = 1f;
        va[i++] = 1f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = maxX;
        va[i++] = maxY;
        va[i++] = maxZ;
        /* n */
        va[i++] = 1f;
        va[i++] = 0f;
        va[i++] = 0f;
        /* t */
        va[i++] = 0f;
        va[i++] = 1f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = maxX;
        va[i++] = minY;
        va[i++] = maxZ;
        /* n */
        va[i++] = 1f;
        va[i++] = 0f;
        va[i++] = 0f;
        /* t */
        va[i++] = 0f;
        va[i++] = 0f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = maxX;
        va[i++] = minY;
        va[i++] = minZ;
        /* n */
        va[i++] = 1f;
        va[i++] = 0f;
        va[i++] = 0f;
        /* t */
        va[i++] = 1f;
        va[i++] = 0f;
        // skip tangent
        i += 4;

        // front
        /* v */
        va[i++] = maxX;
        va[i++] = maxY;
        va[i++] = maxZ;
        /* n */
        va[i++] = 0f;
        va[i++] = 0f;
        va[i++] = 1f;
        /* t */
        va[i++] = 1f;
        va[i++] = 1f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = minX;
        va[i++] = maxY;
        va[i++] = maxZ;
        /* n */
        va[i++] = 0f;
        va[i++] = 0f;
        va[i++] = 1f;
        /* t */
        va[i++] = 0f;
        va[i++] = 1f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = minX;
        va[i++] = minY;
        va[i++] = maxZ;
        /* n */
        va[i++] = 0f;
        va[i++] = 0f;
        va[i++] = 1f;
        /* t */
        va[i++] = 0f;
        va[i++] = 0f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = maxX;
        va[i++] = minY;
        va[i++] = maxZ;
        /* n */
        va[i++] = 0f;
        va[i++] = 0f;
        va[i++] = 1f;
        /* t */
        va[i++] = 1f;
        va[i++] = 0f;
        // skip tangent
        i += 4;

        // left
        /* v */
        va[i++] = minX;
        va[i++] = maxY;
        va[i++] = maxZ;
        /* n */
        va[i++] = -1f;
        va[i++] = 0f;
        va[i++] = 0f;
        /* t */
        va[i++] = 1f;
        va[i++] = 1f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = minX;
        va[i++] = maxY;
        va[i++] = minZ;
        /* n */
        va[i++] = -1f;
        va[i++] = 0f;
        va[i++] = 0f;
        /* t */
        va[i++] = 0f;
        va[i++] = 1f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = minX;
        va[i++] = minY;
        va[i++] = minZ;
        /* n */
        va[i++] = -1f;
        va[i++] = 0f;
        va[i++] = 0f;
        /* t */
        va[i++] = 0f;
        va[i++] = 0f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = minX;
        va[i++] = minY;
        va[i++] = maxZ;
        /* n */
        va[i++] = -1f;
        va[i++] = 0f;
        va[i++] = 0f;
        /* t */
        va[i++] = 1f;
        va[i++] = 0f;
        // skip tangent
        i += 4;

        // top
        /* v */
        va[i++] = maxX;
        va[i++] = maxY;
        va[i++] = minZ;
        /* n */
        va[i++] = 0f;
        va[i++] = 1f;
        va[i++] = 0f;
        /* t */
        va[i++] = 1f;
        va[i++] = 1f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = minX;
        va[i++] = maxY;
        va[i++] = minZ;
        /* n */
        va[i++] = 0f;
        va[i++] = 1f;
        va[i++] = 0f;
        /* t */
        va[i++] = 0f;
        va[i++] = 1f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = minX;
        va[i++] = maxY;
        va[i++] = maxZ;
        /* n */
        va[i++] = 0f;
        va[i++] = 1f;
        va[i++] = 0f;
        /* t */
        va[i++] = 0f;
        va[i++] = 0f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = maxX;
        va[i++] = maxY;
        va[i++] = maxZ;
        /* n */
        va[i++] = 0f;
        va[i++] = 1f;
        va[i++] = 0f;
        /* t */
        va[i++] = 1f;
        va[i++] = 0f;
        // skip tangent
        i += 4;

        // bottom
        /* v */
        va[i++] = minX;
        va[i++] = minY;
        va[i++] = minZ;
        /* n */
        va[i++] = 0f;
        va[i++] = -1f;
        va[i++] = 0f;
        /* t */
        va[i++] = 1f;
        va[i++] = 1f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = maxX;
        va[i++] = minY;
        va[i++] = minZ;
        /* n */
        va[i++] = 0f;
        va[i++] = -1f;
        va[i++] = 0f;
        /* t */
        va[i++] = 0f;
        va[i++] = 1f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = maxX;
        va[i++] = minY;
        va[i++] = maxZ;
        /* n */
        va[i++] = 0f;
        va[i++] = -1f;
        va[i++] = 0f;
        /* t */
        va[i++] = 0f;
        va[i++] = 0f;
        // skip tangent
        i += 4;

        /* v */
        va[i++] = minX;
        va[i++] = minY;
        va[i++] = maxZ;
        /* n */
        va[i++] = 0f;
        va[i++] = -1f;
        va[i++] = 0f;
        /* t */
        va[i++] = 1f;
        va[i++] = 0f;
        // skip tangent
        i += 4;

        TriangleIterator ti = TriangleIterator.Builder.newBuilder().vertices(va, 0, 9).normals(va, 3, 9)
                                                      .textureCoordinates(va, 6, 10).tangents(va, 8, 8)
                                                      .fromElements(indices, 0, indices.length).build();
        Tangents.compute(ti);

        vertexAttributes = framework.newVertexBuffer().from(va).build();
        vertices = new VertexAttribute(vertexAttributes, 3, 0, 9);
        normals = new VertexAttribute(vertexAttributes, 3, 3, 9);
        texCoords = new VertexAttribute(vertexAttributes, 2, 6, 10);
        tangents = new VertexAttribute(vertexAttributes, 4, 8, 8);

        bounds = new AxisAlignedBox(new Vector3(minX, minY, minZ), new Vector3(maxX, maxY, maxZ));
    }

    @Override
    public Renderer.PolygonType getPolygonType() {
        return Renderer.PolygonType.TRIANGLES;
    }

    @Override
    public ElementBuffer getIndices() {
        return indices;
    }

    @Override
    public int getIndexOffset() {
        return 0;
    }

    @Override
    public int getIndexCount() {
        return 36;
    }

    @Override
    public VertexAttribute getVertices() {
        return vertices;
    }

    @Override
    public VertexAttribute getNormals() {
        return normals;
    }

    @Override
    public VertexAttribute getTextureCoordinates() {
        return texCoords;
    }

    @Override
    public VertexAttribute getTangents() {
        return tangents;
    }

    @Override
    @Const
    public AxisAlignedBox getBounds() {
        return bounds;
    }
}
