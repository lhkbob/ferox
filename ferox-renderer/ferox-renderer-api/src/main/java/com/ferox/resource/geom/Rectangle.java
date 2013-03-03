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
package com.ferox.resource.geom;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.BufferData;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.VertexBufferObject.StorageMode;

/**
 * <p/>
 * Rectangle contains factory methods to create a single quad aligned with a specified x
 * and y axis, in three dimensions. It is very useful for fullscreen effects that require
 * rendering a rectangle across the entire screen.
 *
 * @author Michael Ludwig
 */
public final class Rectangle {
    private Rectangle() {
    }

    /**
     * Create a Rectangle with an x basis vector of (1, 0, 0) and a y basis vector of (0,
     * 1, 0), and the given edge dimensions. The storage mode is IN_MEMORY.
     *
     * @param left   The left edge of the rectangle
     * @param right  The right edge of the rectangle
     * @param bottom The bottom edge of the rectangle
     * @param top    The top edge of the rectangle
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if left > right or bottom > top
     */
    public static Geometry create(double left, double right, double bottom, double top) {
        return create(left, right, bottom, top, new Vector3(1f, 0f, 0f),
                      new Vector3(0f, 1f, 0f));
    }

    /**
     * Create a Rectangle with the given basis vectors and edge dimensions and a storage
     * mode of IN_MEMORY.
     *
     * @param left   The left edge of the rectangle
     * @param right  The right edge of the rectangle
     * @param bottom The bottom edge of the rectangle
     * @param top    The top edge of the rectangle
     * @param xAxis  Local x-axis of the rectangle
     * @param yAxis  Local y-axis of the rectangle
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if left > right or bottom > top
     * @throws NullPointerException     if xAxis or yAxis are null
     */
    public static Geometry create(double left, double right, double bottom, double top,
                                  @Const Vector3 xAxis, @Const Vector3 yAxis) {
        return create(left, right, bottom, top, xAxis, yAxis, StorageMode.IN_MEMORY);
    }

    /**
     * Create a Rectangle with the given basis vectors, edge dimensions and storage mode
     *
     * @param xAxis
     * @param yAxis
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param mode   The storage mode to use
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if left > right or bottom > top
     * @throws NullPointerException     if xAxis, yAxis, or mode are null
     */
    public static Geometry create(double left, double right, double bottom, double top,
                                  @Const Vector3 xAxis, @Const Vector3 yAxis,
                                  StorageMode mode) {
        return new RectangleImpl(left, right, bottom, top, xAxis, yAxis, mode);
    }

    private static class RectangleImpl implements Geometry {
        // Holds vertices, normals, texture coordinates packed as V3F_N3F_T2F
        // ordered in such a way as to not need indices
        private final VertexBufferObject vertexAttributes;

        private final VertexAttribute vertices;
        private final VertexAttribute normals;
        private final VertexAttribute texCoords;

        private final AxisAlignedBox bounds;

        public RectangleImpl(double left, double right, double bottom, double top,
                             @Const Vector3 xAxis, @Const Vector3 yAxis,
                             StorageMode mode) {
            if (left > right || bottom > top) {
                throw new IllegalArgumentException(
                        "Side positions of the square are incorrect");
            }
            if (xAxis == null || yAxis == null) {
                throw new NullPointerException("Axis cannot be null");
            }
            if (mode == null) {
                throw new NullPointerException("StorageMode cannot be null");
            }

            Vector3 normal = new Vector3().cross(xAxis, yAxis);

            float[] va = new float[32];
            int i = 0;

            // lower-left
            va[i++] = (float) (xAxis.x * left + yAxis.x * bottom);
            va[i++] = (float) (xAxis.y * left + yAxis.y * bottom);
            va[i++] = (float) (xAxis.z * left + yAxis.z * bottom);

            va[i++] = (float) normal.x;
            va[i++] = (float) normal.y;
            va[i++] = (float) normal.z;

            va[i++] = 0f;
            va[i++] = 0f;

            // lower-right
            va[i++] = (float) (xAxis.x * right + yAxis.x * bottom);
            va[i++] = (float) (xAxis.y * right + yAxis.y * bottom);
            va[i++] = (float) (xAxis.z * right + yAxis.z * bottom);

            va[i++] = (float) normal.x;
            va[i++] = (float) normal.y;
            va[i++] = (float) normal.z;

            va[i++] = 1f;
            va[i++] = 0f;

            // upper-right
            va[i++] = (float) (xAxis.x * right + yAxis.x * top);
            va[i++] = (float) (xAxis.y * right + yAxis.y * top);
            va[i++] = (float) (xAxis.z * right + yAxis.z * top);

            va[i++] = (float) normal.x;
            va[i++] = (float) normal.y;
            va[i++] = (float) normal.z;

            va[i++] = 1f;
            va[i++] = 1f;

            // upper-left
            va[i++] = (float) (xAxis.x * left + yAxis.x * top);
            va[i++] = (float) (xAxis.y * left + yAxis.y * top);
            va[i++] = (float) (xAxis.z * left + yAxis.z * top);

            va[i++] = (float) normal.x;
            va[i++] = (float) normal.y;
            va[i++] = (float) normal.z;

            va[i++] = 0f;
            va[i++] = 1f;

            vertexAttributes = new VertexBufferObject(new BufferData(va), mode);
            vertices = new VertexAttribute(vertexAttributes, 3, 0, 5);
            normals = new VertexAttribute(vertexAttributes, 3, 3, 5);
            texCoords = new VertexAttribute(vertexAttributes, 2, 6, 6);

            bounds = new AxisAlignedBox(va, 0, 5, 4);
        }

        @Override
        public PolygonType getPolygonType() {
            return PolygonType.QUADS;
        }

        @Override
        public VertexBufferObject getIndices() {
            return null;
        }

        @Override
        public int getIndexOffset() {
            return 0;
        }

        @Override
        public int getIndexCount() {
            return 4;
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
