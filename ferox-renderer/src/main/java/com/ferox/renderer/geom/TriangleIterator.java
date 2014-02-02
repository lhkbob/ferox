package com.ferox.renderer.geom;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.renderer.Renderer;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public interface TriangleIterator {
    @Const
    public Vector3 getVertex(int p);

    @Const
    public Vector3 getNormal(int p);

    @Const
    public Vector4 getTangent(int p);

    public double getTextureCoordinateU(int p);

    public double getTextureCoordinateV(int p);

    public void setVertex(int p, @Const Vector3 v);

    public void setNormal(int p, @Const Vector3 n);

    public void setTangent(int p, @Const Vector4 t);

    public void setTextureCoordinate(int p, double u, double v);

    public int getAttributeIndex(String attrName, int p);

    public Vector3 getAttribute(String attrName, int p, Vector3 v);

    public Vector4 getAttribute(String attrName, int p, Vector4 v);

    public void setAttribute(String attrName, int p, @Const Vector3 v);

    public void setAttribute(String attrName, int p, @Const Vector4 v);

    public boolean next();

    public void reset();

    public static final class Builder {
        private final Map<String, AbstractTriangleIterator.Attribute> attrs;
        private int[] indices;
        private int offset;
        private int count;
        private Renderer.PolygonType type;

        private int maxVertexSize;

        private Builder() {
            attrs = new HashMap<>();
        }

        public Builder vertices(float[] data) {
            return vertices(data, 0, 0);
        }

        public Builder vertices(float[] data, int offset, int stride) {
            return attrImpl(AbstractTriangleIterator.ATTR_VERTEX, data, offset, 3, stride);
        }

        public Builder normals(float[] data) {
            return normals(data, 0, 0);
        }

        public Builder normals(float[] data, int offset, int stride) {
            return attrImpl(AbstractTriangleIterator.ATTR_NORMAL, data, offset, 3, stride);
        }

        public Builder tangents(float[] data) {
            return tangents(data, 0, 0);
        }

        public Builder tangents(float[] data, int offset, int stride) {
            return attrImpl(AbstractTriangleIterator.ATTR_TANGENT, data, offset, 4, stride);
        }

        public Builder textureCoordinates(float[] data) {
            return textureCoordinates(data, 0, 0);
        }

        public Builder textureCoordinates(float[] data, int offset, int stride) {
            return attrImpl(AbstractTriangleIterator.ATTR_TEX_COORD, data, offset, 2, stride);
        }

        public Builder attribute(String name, float[] data, int elementSize) {
            return attribute(name, data, 0, elementSize, 0);
        }

        public Builder attribute(String name, float[] data, int offset, int elementSize, int stride) {
            if (name.equals(AbstractTriangleIterator.ATTR_VERTEX) ||
                name.equals(AbstractTriangleIterator.ATTR_NORMAL) ||
                name.equals(AbstractTriangleIterator.ATTR_TANGENT) ||
                name.equals(AbstractTriangleIterator.ATTR_TEX_COORD)) {
                throw new IllegalArgumentException("Reserved attribute name: " + name);
            }

            return attrImpl(name, data, offset, elementSize, stride);
        }

        public float[] createAttribute(String name, int elementSize) {
            float[] a = new float[maxVertexSize * elementSize];
            attribute(name, a, elementSize);
            return a;
        }

        Builder attrImpl(String name, float[] data, int offset, int elementSize, int stride) {
            attrs.put(name, new AbstractTriangleIterator.Attribute(data, offset, elementSize, stride));
            maxVertexSize = Math.max((data.length - offset) / (elementSize + stride), maxVertexSize);
            return this;
        }

        public Builder fromArray(int offset, int count) {
            return fromElements(null, offset, count);
        }

        public Builder fromElements(int[] indices, int offset, int count) {
            this.indices = indices;
            this.offset = offset;
            this.count = count;
            type = Renderer.PolygonType.TRIANGLES;
            return this;
        }

        public Builder fromStripArray(int offset, int count) {
            return fromStripElements(null, offset, count);
        }

        public Builder fromStripElements(int[] indices, int offset, int count) {
            this.indices = indices;
            this.offset = offset;
            this.count = count;
            type = Renderer.PolygonType.TRIANGLE_STRIP;
            return this;
        }

        public Builder set(TriangleIterator ti) {
            ((AbstractTriangleIterator) ti).configureBuilder(this);
            return this;
        }

        public TriangleIterator build() {
            if (type == Renderer.PolygonType.TRIANGLE_STRIP) {
                return new TriangleStripIterator(indices, offset, count, attrs);
            } else {
                return new TriangleSoupIterator(indices, offset, count, attrs);
            }
        }

        public static Builder newBuilder() {
            return new Builder();
        }
    }
}
