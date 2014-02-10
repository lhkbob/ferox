package com.ferox.renderer.geom;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.renderer.*;

/**
 *
 */
class SphereImpl implements Geometry {
    // we need a float PI since we're building float vertices
    private static final float PI = (float) Math.PI;

    // Holds vertices, normals, tangents, texture coordinates packed as V3F_N3F_TC2F_T4F
    private final VertexBuffer vertexAttributes;

    private final VertexAttribute vertices;
    private final VertexAttribute normals;
    private final VertexAttribute tangents;
    private final VertexAttribute texCoords;

    private final ElementBuffer indices;

    private final AxisAlignedBox bounds;

    public SphereImpl(Framework framework, double radius, int res) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Invalid radius, must be > 0, not: " + radius);
        }
        if (res < 4) {
            throw new IllegalArgumentException("Invalid resolution, must be > 3, not: " + res);
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

        float[] va = new float[vertexCount * 12]; // 3v + 3n + 2tc + 4t

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

                // calculate ideal tangent vectors
                va[index++] = -zCoord[du];
                va[index++] = 0.0f;
                va[index++] = xCoord[du];
                va[index++] = 1.0f;
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

        if (index != indices.length) {
            throw new RuntimeException("bad length computation");
        }

        this.indices = framework.newElementBuffer().fromUnsigned(indices).build();
        vertexAttributes = framework.newVertexBuffer().from(va).build();
        vertices = new VertexAttribute(vertexAttributes, 3, 0, 9);
        normals = new VertexAttribute(vertexAttributes, 3, 3, 9);
        texCoords = new VertexAttribute(vertexAttributes, 2, 6, 10);
        tangents = new VertexAttribute(vertexAttributes, 4, 8, 8);

        bounds = new AxisAlignedBox(new Vector3(-radius, -radius, -radius),
                                    new Vector3(radius, radius, radius));
    }

    @Override
    public Renderer.PolygonType getPolygonType() {
        return Renderer.PolygonType.TRIANGLE_STRIP;
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
