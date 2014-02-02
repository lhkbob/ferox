package com.ferox.renderer.geom;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;

import java.util.Map;

/**
 *
 */
abstract class AbstractTriangleIterator implements TriangleIterator {
    public static final String ATTR_VERTEX = "reserved_Vertex";
    public static final String ATTR_NORMAL = "reserved_Normal";
    public static final String ATTR_TANGENT = "reserved_Tangent";
    public static final String ATTR_TEX_COORD = "reserved_TexCoord";

    private final Map<String, Attribute> attrs;

    // one for each vertex of the triangle
    private final Vector3[] vertex;
    private final Vector3[] normal;
    private final Vector4[] tangent;

    public AbstractTriangleIterator(Map<String, Attribute> attrs) {
        this.attrs = attrs;
        vertex = new Vector3[] { new Vector3(), new Vector3(), new Vector3() };
        normal = new Vector3[] { new Vector3(), new Vector3(), new Vector3() };
        tangent = new Vector4[] { new Vector4(), new Vector4(), new Vector4() };
    }

    protected void configureBuilder(Builder b) {
        for (Map.Entry<String, Attribute> e : attrs.entrySet()) {
            Attribute a = e.getValue();
            b.attrImpl(e.getKey(), a.data, a.offset, a.elementSize, a.stride);
        }
    }

    protected abstract int getVertexIndex(int p);

    protected abstract void ensureAvailable(int p);

    @Override
    public Vector3 getVertex(int p) {
        ensureAvailable(p);
        Attribute v = attrs.get(ATTR_VERTEX);
        if (v == null) {
            throw new IllegalStateException("No vertex attribute defined");
        }
        int idx = v.getDataIndex(getVertexIndex(p));
        return vertex[p].set(v.data, idx);
    }

    @Override
    public Vector3 getNormal(int p) {
        ensureAvailable(p);
        Attribute n = attrs.get(ATTR_NORMAL);
        if (n == null) {
            throw new IllegalStateException("No normal attribute defined");
        }
        int idx = n.getDataIndex(getVertexIndex(p));
        return normal[p].set(n.data, idx);
    }

    @Override
    public Vector4 getTangent(int p) {
        ensureAvailable(p);
        Attribute t = attrs.get(ATTR_TANGENT);
        if (t == null) {
            throw new IllegalStateException("No tangent attribute defined");
        }
        int idx = t.getDataIndex(getVertexIndex(p));
        return tangent[p].set(t.data, idx);
    }

    @Override
    public double getTextureCoordinateU(int p) {
        ensureAvailable(p);
        Attribute t = attrs.get(ATTR_TEX_COORD);
        if (t == null) {
            throw new IllegalStateException("No texture coordinate attribute defined");
        }
        int idx = t.getDataIndex(getVertexIndex(p));
        float tc = t.data[idx];
        //        if (tc > 1.0)
        //            return 2.0 - tc;
        //        else
        return tc;
    }

    @Override
    public double getTextureCoordinateV(int p) {
        ensureAvailable(p);
        Attribute t = attrs.get(ATTR_TEX_COORD);
        if (t == null) {
            throw new IllegalStateException("No texture coordinate attribute defined");
        }
        int idx = t.getDataIndex(getVertexIndex(p));
        float tc = t.data[idx + 1];
        //        if (tc > 1.0)
        //            return 2.0 - tc;
        //        else
        return tc;
    }

    @Override
    public void setVertex(int p, @Const Vector3 v) {
        ensureAvailable(p);
        Attribute va = attrs.get(ATTR_VERTEX);
        if (va == null) {
            throw new IllegalStateException("No vertex attribute defined");
        }
        int idx = va.getDataIndex(getVertexIndex(p));
        v.get(va.data, idx);
    }

    @Override
    public void setNormal(int p, @Const Vector3 n) {
        ensureAvailable(p);
        Attribute na = attrs.get(ATTR_NORMAL);
        if (na == null) {
            throw new IllegalStateException("No normal attribute defined");
        }
        int idx = na.getDataIndex(getVertexIndex(p));
        n.get(na.data, idx);
    }

    @Override
    public void setTangent(int p, @Const Vector4 t) {
        ensureAvailable(p);
        Attribute ta = attrs.get(ATTR_TANGENT);
        if (ta == null) {
            throw new IllegalStateException("No tangent attribute defined");
        }
        int idx = ta.getDataIndex(getVertexIndex(p));
        t.get(ta.data, idx);
    }

    @Override
    public void setTextureCoordinate(int p, double u, double v) {
        ensureAvailable(p);
        Attribute t = attrs.get(ATTR_TEX_COORD);
        if (t == null) {
            throw new IllegalStateException("No texture coordinate attribute defined");
        }
        int idx = t.getDataIndex(getVertexIndex(p));
        t.data[idx] = (float) u;
        t.data[idx + 1] = (float) v;
    }

    @Override
    public int getAttributeIndex(String attrName, int p) {
        ensureAvailable(p);
        Attribute a = attrs.get(attrName);
        if (a == null) {
            throw new IllegalStateException("No attribute defined for name " + attrName);
        }
        return a.getDataIndex(getVertexIndex(p));
    }

    @Override
    public Vector3 getAttribute(String attrName, int p, Vector3 v) {
        ensureAvailable(p);
        Attribute a = attrs.get(attrName);
        if (a == null) {
            throw new IllegalStateException("No attribute defined for name " + attrName);
        }
        if (a.elementSize != 3) {
            throw new IllegalStateException("Attribute must have element size of 3");
        }
        if (v == null) {
            v = new Vector3();
        }
        v.set(a.data, a.getDataIndex(getVertexIndex(p)));
        return v;
    }

    @Override
    public Vector4 getAttribute(String attrName, int p, Vector4 v) {
        ensureAvailable(p);
        Attribute a = attrs.get(attrName);
        if (a == null) {
            throw new IllegalStateException("No attribute defined for name " + attrName);
        }
        if (a.elementSize != 4) {
            throw new IllegalStateException("Attribute must have element size of 4");
        }
        if (v == null) {
            v = new Vector4();
        }
        return v.set(a.data, a.getDataIndex(getVertexIndex(p)));
    }

    @Override
    public void setAttribute(String attrName, int p, @Const Vector3 v) {
        ensureAvailable(p);
        Attribute a = attrs.get(attrName);
        if (a == null) {
            throw new IllegalStateException("No attribute defined for name " + attrName);
        }
        if (a.elementSize != 3) {
            throw new IllegalStateException("Attribute must have element size of 3");
        }
        v.get(a.data, a.getDataIndex(getVertexIndex(p)));
    }

    @Override
    public void setAttribute(String attrName, int p, @Const Vector4 v) {
        ensureAvailable(p);
        Attribute a = attrs.get(attrName);
        if (a == null) {
            throw new IllegalStateException("No attribute defined for name " + attrName);
        }
        if (a.elementSize != 4) {
            throw new IllegalStateException("Attribute must have element size of 4");
        }
        v.get(a.data, a.getDataIndex(getVertexIndex(p)));
    }

    public static class Attribute {
        final int offset;
        final int elementSize;
        final int stride;

        final float[] data;

        public Attribute(float[] data, int offset, int elementSize, int stride) {
            this.data = data;
            this.offset = offset;
            this.elementSize = elementSize;
            this.stride = stride;
        }

        private int getDataIndex(int vertexIndex) {
            return offset + (elementSize + stride) * vertexIndex;
        }
    }
}
