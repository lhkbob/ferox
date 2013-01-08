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
package com.ferox.util.geom;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.BufferData;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.VertexBufferObject.StorageMode;

/**
 * <p>
 * Sphere contains factory methods for creating approximations of a mathematical
 * sphere with a configurable radius. The accuracy of the approximation depends
 * on a parameter termed <tt>resolution</tt>.
 * <p>
 * The approximated sphere is constructed by rotating a number of circles in the
 * XY-plane about the Y-axis. The number of rotations equals the resolution of
 * the sphere. Each circle is also approximated by a number of points equal to
 * the resolution.
 * 
 * @author Michael Ludwig
 */
public final class Sphere {
    // we need a float PI since we're building float vertices
    private static final float PI = (float) Math.PI;

    private Sphere() {}

    /**
     * Create a new Sphere with the given radius, a resolution of 8, and a
     * StorageMode of IN_MEMORY.
     * 
     * @param radius The radius of the sphere, in local space
     * @return The new geometry
     * @throws IllegalArgumentException if radius <= 0
     */
    public static Geometry create(double radius) {
        return create(radius, 8);
    }

    /**
     * Create a new Sphere with the given radius and resolution. It uses a
     * StorageMode of IN_MEMORY.
     * 
     * @param radius The radius of the sphere, in local space
     * @param res The resolution of the sphere, the higher the value the
     *            smoother the tesselation
     * @return The new geometry
     * @throws IllegalArgumentException if radius <= 0 or if res < 4
     */
    public static Geometry create(double radius, int res) {
        return create(radius, res, StorageMode.IN_MEMORY);
    }

    /**
     * Create a new Sphere with the given radius and StorageMode. It uses a
     * resolution of 8.
     * 
     * @param radius The radius of the sphere, in local space
     * @param mode The StorageMode to use
     * @return The new geometry
     * @throws IllegalArgumentException if radius <= 0
     * @throws NullPointerException if mode is null
     */
    public static Geometry create(double radius, StorageMode mode) {
        return create(radius, 8, mode);
    }

    /**
     * Create a new Sphere with the given radius, resolution and StorageMode.
     * 
     * @param radius The radius of the sphere, in local space
     * @param res The resolution of the sphere
     * @param mode The StorageMode to use
     * @return The new geometry
     * @throws IllegalArgumentException if radius <= 0 or if res < 4
     * @throws NullPointerException if mode is null
     */
    public static Geometry create(double radius, int res, StorageMode mode) {
        return new SphereImpl(radius, res, mode);
    }

    private static class SphereImpl implements Geometry {
        // Holds vertices, normals, texture coordinates packed as V3F_N3F_T2F
        private final VertexBufferObject vertexAttributes;

        private final VertexAttribute vertices;
        private final VertexAttribute normals;
        private final VertexAttribute texCoords;

        private final VertexBufferObject indices;

        private final AxisAlignedBox bounds;

        public SphereImpl(double radius, int res, StorageMode mode) {
            if (radius <= 0) {
                throw new IllegalArgumentException("Invalid radius, must be > 0, not: " + radius);
            }
            if (res < 4) {
                throw new IllegalArgumentException("Invalid resolution, must be > 3, not: " + res);
            }
            if (mode == null) {
                throw new NullPointerException("StorageMode cannot be null");
            }

            int vertexCount = res * (res + 1);

            float[] xCoord = new float[res + 1];
            float[] zCoord = new float[res + 1];
            float[] u = new float[res + 1];

            float xzAngle = 0;
            float dXZ = 2 * PI / res;
            for (int i = 0; i < res; i++) {
                // compute cache for slices
                xCoord[i] = (float) Math.cos(xzAngle);
                zCoord[i] = (float) Math.sin(xzAngle);
                u[i] = i / (float) res;
                xzAngle += dXZ;
            }

            // wrap around to connect the sphere
            xCoord[res] = xCoord[0];
            zCoord[res] = zCoord[0];
            u[res] = 1f;

            float[] va = new float[vertexCount * 8]; // 3v + 3n + 2tc

            float floatRadius = (float) radius;
            float yAngle = PI;
            float dY = -PI / (res - 1);
            int index = 0;
            float y, r, tv;
            for (int dv = 0; dv < res; dv++) {
                // compute y values, since they're constant for the whole ring
                y = (float) Math.cos(yAngle);
                r = (float) Math.sqrt(1 - y * y);
                tv = (float) dv / (res - 1);
                yAngle += dY;

                for (int du = 0; du <= res; du++) {
                    // place vertices, normals and texcoords
                    va[index++] = floatRadius * r * xCoord[du]; // vx
                    va[index++] = floatRadius * y; // vy
                    va[index++] = floatRadius * r * zCoord[du]; // vz

                    va[index++] = r * xCoord[du]; // nx
                    va[index++] = y; // ny
                    va[index++] = r * zCoord[du]; // nz

                    va[index++] = u[du]; // tx
                    va[index++] = tv; // ty
                }
            }

            // build up indices
            int[] indices = new int[(res - 1) * (2 * res + 2)];
            index = 0;
            int v1, v2;
            for (int dv = 0; dv < res - 1; dv++) {
                v1 = dv * (res + 1);
                v2 = (dv + 1) * (res + 1);

                // start off the strip
                indices[index++] = v1++;
                indices[index++] = v2++;
                for (int du = 0; du < res; du++) {
                    indices[index++] = v1++;
                    indices[index++] = v2++;
                }
            }

            this.indices = new VertexBufferObject(new BufferData(indices), mode);
            vertexAttributes = new VertexBufferObject(new BufferData(va), mode);
            vertices = new VertexAttribute(vertexAttributes, 3, 0, 5);
            normals = new VertexAttribute(vertexAttributes, 3, 3, 5);
            texCoords = new VertexAttribute(vertexAttributes, 2, 6, 6);

            bounds = new AxisAlignedBox(new Vector3(-radius, -radius, -radius),
                                        new Vector3(radius, radius, radius));
        }

        @Override
        public PolygonType getPolygonType() {
            return PolygonType.TRIANGLE_STRIP;
        }

        @Override
        public VertexBufferObject getIndices() {
            return indices;
        }

        @Override
        public int getIndexOffset() {
            return 0;
        }

        @Override
        public int getIndexCount() {
            return indices.getData().getLength();
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
