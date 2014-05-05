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
        return t.data[idx];
    }

    @Override
    public double getTextureCoordinateV(int p) {
        ensureAvailable(p);
        Attribute t = attrs.get(ATTR_TEX_COORD);
        if (t == null) {
            throw new IllegalStateException("No texture coordinate attribute defined");
        }
        int idx = t.getDataIndex(getVertexIndex(p));
        return t.data[idx + 1];
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
