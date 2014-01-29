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
import com.ferox.renderer.ElementBuffer;
import com.ferox.renderer.Framework;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.renderer.VertexAttribute;
import com.ferox.renderer.VertexBuffer;

/**
 * <p/>
 * Box contains factory methods for creating 6-sided rectangular prism Geometries.
 *
 * @author Michael Ludwig
 */
public final class Box {
    private Box() {
    }

    /**
     * Construct a box centered on its origin, with the given side length. So, Box(1f) creates a unit cube.
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param side      The side length of the created cube
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if side is negative
     */
    public static Geometry create(Framework framework, double side) {
        return create(framework, side, side, side);
    }

    /**
     * Construct a box centered on its origin, with the given side lengths along each local axis.
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param xExtent   The side length along the x axis
     * @param yExtent   The side length along the y axis
     * @param zExtent   The side length along the z axis
     *
     * @return The new geometry
     *
     * @throws NullPointerException         if mode is null
     * @throws IllegalMonitorStateException if any dimension is negative
     */
    public static Geometry create(Framework framework, double xExtent, double yExtent, double zExtent) {
        return create(framework, new Vector3(-xExtent / 2, -yExtent / 2, -zExtent / 2),
                      new Vector3(xExtent / 2, yExtent / 2, zExtent / 2));
    }

    /**
     * Construct a new Box with the given minimum and maximum points. These points are opposite corners of the
     * box.
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param min       Minimum corner of the box
     * @param max       Maximum corner of the box
     *
     * @return The new geometry
     *
     * @throws NullPointerException     if min, max or mode are null
     * @throws IllegalArgumentException if min has any coordinate less than the corresponding coordinate of
     *                                  max
     */
    public static Geometry create(Framework framework, @Const Vector3 min, @Const Vector3 max) {
        return new BoxImpl(framework, min, max);
    }

    private static class BoxImpl implements Geometry {
        // Holds vertices, normals, texture coordinates packed as V3F_N3F_T2F
        private final VertexBuffer vertexAttributes;
        private final ElementBuffer indices;

        private final VertexAttribute vertices;
        private final VertexAttribute normals;
        private final VertexAttribute texCoords;

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
            float[] va = new float[192]; // 72v + 72n + 48t

            // 3 verts per triangle, 2 triangles per face, 6 faces = 36
            int[] indices = new int[] {
                                              0, 1, 2, 2, 3, 0, // BACK
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

            vertexAttributes = framework.newVertexBuffer().from(va).build();
            vertices = new VertexAttribute(vertexAttributes, 3, 0, 5);
            normals = new VertexAttribute(vertexAttributes, 3, 3, 5);
            texCoords = new VertexAttribute(vertexAttributes, 2, 6, 6);

            bounds = new AxisAlignedBox(new Vector3(minX, minY, minZ), new Vector3(maxX, maxY, maxZ));
        }

        @Override
        public PolygonType getPolygonType() {
            return PolygonType.TRIANGLES;
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
            throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
        }

        @Override
        @Const
        public AxisAlignedBox getBounds() {
            return bounds;
        }
    }
}
