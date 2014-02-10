package com.ferox.renderer.geom;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Vector3;
import com.ferox.renderer.*;

/**
 *
 */
class CylinderImpl implements Geometry {
    // we need a float PI since we're building float vertices
    private static final float PI = (float) Math.PI;

    // Holds vertices, normals, texture coordinates packed as V3F_N3F_TC2F_T4F
    private final VertexBuffer vertexAttributes;

    private final VertexAttribute vertices;
    private final VertexAttribute normals;
    private final VertexAttribute texCoords;
    private final VertexAttribute tangents;

    private final ElementBuffer indices;

    private final AxisAlignedBox bounds;

    public CylinderImpl(Framework framework, @Const Vector3 axis, @Const Vector3 origin, double radius,
                        double height, int res) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Invalid radius, must be > 0, not: " + radius);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Invalid height, must be > 0, not: " + height);
        }
        if (res < 4) {
            throw new IllegalArgumentException("Invalid resolution, must be > 3, not: " + res);
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

        float[] va = new float[vertexCount * 12]; // 3v + 3n + 2tc + 4t
        int[] indices = new int[12 * res]; // 4 sections of res tris

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

            // calculate ideal normalized tangent vector
            va[vi++] = (float) (zCoord[i] / radius);
            va[vi++] = 0.0f;
            va[vi++] = (float) (-xCoord[i] / radius);
            va[vi++] = 1.0f;

            // center
            va[vi++] = 0; // vx
            va[vi++] = (float) (.5 * height); // vy
            va[vi++] = 0; // vz

            va[vi++] = 0; // nx
            va[vi++] = 1; // ny
            va[vi++] = 0; // nz

            va[vi++] = uCoord[i]; // tx
            va[vi++] = 1; // ty

            // calculate ideal normalized tangent vector
            va[vi++] = (float) (zCoord[i] / radius);
            va[vi++] = 0.0f;
            va[vi++] = (float) (-xCoord[i] / radius);
            va[vi++] = 1.0f;

            if (i != res) {
                // form triangle with proper winding
                indices[ii++] = i * 2;
                indices[ii++] = i * 2 + 1;
                indices[ii++] = i * 2 + 2;
            }
        }

        // second cap
        int offset = vi / 12;
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

            // calculate ideal normalized tangent vector
            va[vi++] = (float) (zCoord[i] / radius);
            va[vi++] = 0.0f;
            va[vi++] = (float) (-xCoord[i] / radius);
            va[vi++] = 1.0f;

            // center
            va[vi++] = 0; // vx
            va[vi++] = (float) (-.5 * height); // vy
            va[vi++] = 0; // vz

            va[vi++] = 0; // nx
            va[vi++] = -1; // ny
            va[vi++] = 0; // nz

            va[vi++] = uCoord[i]; // tx
            va[vi++] = 0; // ty

            // calculate ideal normalized tangent vector
            va[vi++] = (float) (zCoord[i] / radius);
            va[vi++] = 0.0f;
            va[vi++] = (float) (-xCoord[i] / radius);
            va[vi++] = 1.0f;

            if (i != res) {
                // form a triangle with proper winding
                indices[ii++] = offset + i * 2;
                indices[ii++] = offset + i * 2 + 2;
                indices[ii++] = offset + i * 2 + 1;
            }
        }

        // tube
        offset = vi / 12;
        for (int i = 0; i <= res; i++) {
            // place two vertices in panel
            va[vi++] = xCoord[i];
            va[vi++] = (float) (-.5 * height);
            va[vi++] = zCoord[i];

            va[vi++] = xCoord[i];
            va[vi++] = 0;
            va[vi++] = zCoord[i];

            va[vi++] = uCoord[i];
            va[vi++] = 1;

            // calculate ideal normalized tangent vector
            va[vi++] = (float) (zCoord[i] / radius);
            va[vi++] = 0.0f;
            va[vi++] = (float) (-xCoord[i] / radius);
            va[vi++] = 1.0f;

            va[vi++] = xCoord[i];
            va[vi++] = (float) (.5 * height);
            va[vi++] = zCoord[i];

            va[vi++] = xCoord[i];
            va[vi++] = 0;
            va[vi++] = zCoord[i];

            va[vi++] = uCoord[i];
            va[vi++] = 0;

            // calculate ideal normalized tangent vector
            va[vi++] = (float) (zCoord[i] / radius);
            va[vi++] = 0.0f;
            va[vi++] = (float) (-xCoord[i] / radius);
            va[vi++] = 1.0f;

            if (i != res) {
                // form two triangles with proper winding
                indices[ii++] = offset + i * 2;
                indices[ii++] = offset + i * 2 + 1;
                indices[ii++] = offset + i * 2 + 2;

                indices[ii++] = offset + i * 2 + 2;
                indices[ii++] = offset + i * 2 + 1;
                indices[ii++] = offset + i * 2 + 3;
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

        for (int i = 0; i < va.length; i += 12) {
            // vertex
            u.set(va, i);
            u.mul(m, u).add(origin);
            u.get(va, i);

            // normal
            u.set(va, i + 3);
            u.mul(u, n);
            u.get(va, i + 3);

            // tangent
            u.set(va, i + 8);
            u.mul(u, n);
            u.get(va, i + 8);
        }

        this.indices = framework.newElementBuffer().fromUnsigned(indices).build();
        vertexAttributes = framework.newVertexBuffer().from(va).build();
        vertices = new VertexAttribute(vertexAttributes, 3, 0, 9);
        normals = new VertexAttribute(vertexAttributes, 3, 3, 9);
        texCoords = new VertexAttribute(vertexAttributes, 2, 6, 10);
        tangents = new VertexAttribute(vertexAttributes, 4, 8, 8);

        bounds = new AxisAlignedBox(new Vector3(-radius, -height, -radius),
                                    new Vector3(radius, height, radius));
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
        return indices.getLength();
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
