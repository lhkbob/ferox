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
import com.ferox.math.Matrix3;
import com.ferox.math.Vector3;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.BufferData;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.VertexBufferObject.StorageMode;

/**
 * <p/>
 * Cylinder contains factory methods for creating approximations of an ideal cylinder with
 * a configurable radius and height. The accuracy of the approximation depends on a
 * parameter termed <var>resolution</var>, which represents the number of samples along
 * the circular caps.
 *
 * @author Michael Ludwig
 */
public class Cylinder {
    // we need a float PI since we're building float vertices
    private static final float PI = (float) Math.PI;

    private Cylinder() {
    }

    /**
     * Create a new Cylinder with the given radius and height, a resolution of 8, and a
     * StorageMode of IN_MEMORY. Its axis will be the positive y-axis.
     *
     * @param radius The radius of the cylinder, in local space
     * @param height The height of the cylinder
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if radius <= 0
     */
    public static Geometry create(double radius, double height) {
        return create(radius, height, 8);
    }

    /**
     * Create a new Cylinder with the given radius, height, and resolution. It uses a
     * StorageMode of IN_MEMORY. Its axis will be the positive y-axis.
     *
     * @param radius The radius of the cylinder, in local space
     * @param height The height of the cylinder
     * @param res    The resolution of the cylinder, the higher the value the smoother the
     *               tesselation
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if radius <= 0 or if res < 4
     */
    public static Geometry create(double radius, double height, int res) {
        return create(radius, height, res, StorageMode.IN_MEMORY);
    }

    /**
     * Create a new Cylinder with the given radius, height and StorageMode. It uses a
     * resolution of 8. Its axis will be the positive y-axis.
     *
     * @param radius The radius of the cylinder, in local space
     * @param height The height of the cylinder
     * @param mode   The StorageMode to use
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if radius <= 0
     * @throws NullPointerException     if mode is null
     */
    public static Geometry create(double radius, double height, StorageMode mode) {
        return create(radius, height, 8, mode);
    }

    /**
     * Create a new cylinder with the given radius, height, resolution and StorageMode.
     * Its axis will be the positive y-axis.
     *
     * @param radius The radius of the cylinder, in local space
     * @param height The height of the cylinder
     * @param res    The resolution of the sphere
     * @param mode   The StorageMode to use
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if radius <= 0 or if res < 4
     * @throws NullPointerException     if mode is null
     */
    public static Geometry create(double radius, double height, int res,
                                  StorageMode mode) {
        return create(new Vector3(0, 1, 0), new Vector3(0, 0, 0), radius, height, res,
                      mode);
    }

    /**
     * Create a new cylinder with the given vertical axis, radius, height, resolution and
     * StorageMode.
     *
     * @param axis   The vertical axis of the cylinder
     * @param origin The point this cylinder is centered about
     * @param radius The radius of the cylinder, in local space
     * @param height The height of the cylinder
     * @param res    The resolution of the sphere
     * @param mode   The StorageMode to use
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if radius <= 0 or if res < 4
     * @throws NullPointerException     if mode is null
     */
    public static Geometry create(@Const Vector3 axis, @Const Vector3 origin,
                                  double radius, double height, int res,
                                  StorageMode mode) {
        return new CylinderImpl(axis, origin, radius, height, res, mode);
    }

    private static class CylinderImpl implements Geometry {
        // Holds vertices, normals, texture coordinates packed as V3F_N3F_T2F
        private final VertexBufferObject vertexAttributes;

        private final VertexAttribute vertices;
        private final VertexAttribute normals;
        private final VertexAttribute texCoords;

        private final VertexBufferObject indices;

        private final AxisAlignedBox bounds;

        public CylinderImpl(@Const Vector3 axis, @Const Vector3 origin, double radius,
                            double height, int res, StorageMode mode) {
            if (radius <= 0) {
                throw new IllegalArgumentException(
                        "Invalid radius, must be > 0, not: " + radius);
            }
            if (height <= 0) {
                throw new IllegalArgumentException(
                        "Invalid height, must be > 0, not: " + height);
            }
            if (res < 4) {
                throw new IllegalArgumentException(
                        "Invalid resolution, must be > 3, not: " + res);
            }
            if (mode == null) {
                throw new NullPointerException("StorageMode cannot be null");
            }

            // compute cache for rings
            float[] xCoord = new float[res + 1];
            float[] zCoord = new float[res + 1];
            float[] uCoord = new float[res + 1];

            float xzAngle = 0;
            float dXZ = 2 * PI / res;
            for (int i = 0; i < res; i++) {
                xCoord[i] = (float) (radius * Math.cos(xzAngle));
                zCoord[i] = (float) (radius * Math.sin(xzAngle));
                uCoord[i] = (float) i / res;
                xzAngle += dXZ;
            }

            // wrap around, but with an updated texture coordinate
            xCoord[res] = xCoord[0];
            zCoord[res] = zCoord[0];
            uCoord[res] = 1;

            // vertices required are two rings with normals facing up/down for the cap,
            // another two rings with normals facing out from the tube, and duplicated
            // center points (for different TCs) in the center of the caps
            int vertexCount = 6 * (res + 1);

            float[] va = new float[vertexCount * 8]; // 3v + 3n + 2tc
            int[] indices = new int[18 * res];

            int vi = 0;
            int ii = 0;

            // first cap
            for (int i = 0; i <= res; i++) {
                // outer point
                va[vi++] = xCoord[i]; // vx
                va[vi++] = (float) (.5 * height); // vy
                va[vi++] = zCoord[i]; // vz

                va[vi++] = 0; // nx
                va[vi++] = 1; // ny
                va[vi++] = 0; // nz

                va[vi++] = uCoord[i]; // tx
                va[vi++] = 1; // ty

                // center
                va[vi++] = 0; // vx
                va[vi++] = (float) (.5 * height); // vy
                va[vi++] = 0; // vz

                va[vi++] = 0; // nx
                va[vi++] = 1; // ny
                va[vi++] = 0; // nz

                va[vi++] = uCoord[i]; // tx
                va[vi++] = 1; // ty

                if (i != res) {
                    // form triangle with proper winding
                    indices[ii++] = i * 2;
                    indices[ii++] = i * 2 + 1;
                    indices[ii++] = i * 2 + 2;
                }
            }

            // second cap
            int offset = vi / 8;
            for (int i = 0; i <= res; i++) {
                // outer point
                va[vi++] = xCoord[i]; // vx
                va[vi++] = (float) (-.5 * height); // vy
                va[vi++] = zCoord[i]; // vz

                va[vi++] = 0; // nx
                va[vi++] = -1; // ny
                va[vi++] = 0; // nz

                va[vi++] = uCoord[i]; // tx
                va[vi++] = 0; // ty

                // center
                va[vi++] = 0; // vx
                va[vi++] = (float) (-.5 * height); // vy
                va[vi++] = 0; // vz

                va[vi++] = 0; // nx
                va[vi++] = -1; // ny
                va[vi++] = 0; // nz

                va[vi++] = uCoord[i]; // tx
                va[vi++] = 0; // ty

                if (i != res) {
                    // form a triangle with proper winding
                    indices[ii++] = offset + i * 2;
                    indices[ii++] = offset + i * 2 + 2;
                    indices[ii++] = offset + i * 2 + 1;
                }
            }

            // tube
            offset = vi / 8;
            for (int i = 0; i <= res; i++) {
                // place two vertices in panel
                va[vi++] = xCoord[i];
                va[vi++] = (float) (.5 * height);
                va[vi++] = zCoord[i];

                va[vi++] = xCoord[i];
                va[vi++] = 0;
                va[vi++] = zCoord[i];

                va[vi++] = uCoord[i];
                va[vi++] = 1;

                va[vi++] = xCoord[i];
                va[vi++] = (float) (-.5 * height);
                va[vi++] = zCoord[i];

                va[vi++] = xCoord[i];
                va[vi++] = 0;
                va[vi++] = zCoord[i];

                va[vi++] = uCoord[i];
                va[vi++] = 0;

                if (i != res) {
                    // form two triangles with proper winding
                    indices[ii++] = offset + i * 2;
                    indices[ii++] = offset + i * 2 + 2; // (i + 1) * 2
                    indices[ii++] = offset + i * 2 + 1;

                    indices[ii++] = offset + i * 2 + 2; // (i + 1) * 2 + 1
                    indices[ii++] = offset + i * 2 + 3;
                    indices[ii++] = offset + i * 2 + 1;
                }
            }

            // now take into account the requested axis
            Vector3 u = new Vector3(1, 0, 0);
            Vector3 v = new Vector3().normalize(axis);
            Matrix3 m = new Matrix3();

            if (Math.abs(v.x - 1.0) < 0.0001) {
                // update it to get the right ortho-normal basis
                u.set(0, -1, 0);
            } else if (Math.abs(v.x + 1.0) < 0.0001) {
                // update it to get the right ortho-normal basis
                u.set(0, 1, 0);
            } else {
                // compute orthogonal x-axis in the direction of (1, 0, 0)
                u.ortho(v).normalize();
            }

            m.setCol(0, u);
            m.setCol(1, v);
            m.setCol(2, u.cross(v).normalize());

            Matrix3 n = new Matrix3(m).inverse();

            for (int i = 0; i < va.length; i += 8) {
                // vertex
                u.set(va, i);
                u.mul(m, u).add(origin);
                u.get(va, i);

                // normal
                u.set(va, i + 3);
                u.mul(u, n);
                u.get(va, i + 3);
            }

            this.indices = new VertexBufferObject(new BufferData(indices), mode);
            vertexAttributes = new VertexBufferObject(new BufferData(va), mode);
            vertices = new VertexAttribute(vertexAttributes, 3, 0, 5);
            normals = new VertexAttribute(vertexAttributes, 3, 3, 5);
            texCoords = new VertexAttribute(vertexAttributes, 2, 6, 6);

            bounds = new AxisAlignedBox(new Vector3(-radius, -height, -radius),
                                        new Vector3(radius, height, radius));
        }

        @Override
        public PolygonType getPolygonType() {
            return PolygonType.TRIANGLES;
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
