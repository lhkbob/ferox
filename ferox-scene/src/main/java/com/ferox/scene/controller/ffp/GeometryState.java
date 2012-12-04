package com.ferox.scene.controller.ffp;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.resource.VertexAttribute;

public class GeometryState implements State {
    private VertexAttribute vertices;
    private VertexAttribute normals;

    public void set(VertexAttribute vertices, VertexAttribute normals) {
        this.vertices = vertices;
        this.normals = normals;
    }

    public VertexAttribute getVertices() {
        return vertices;
    }

    public VertexAttribute getNormals() {
        return normals;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        r.setVertices(vertices);
        r.setNormals(normals);

        currentNode.visitChildren(effects, access);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + (vertices == null ? 0 : vertices.hashCode());
        hash = 31 * hash + (normals == null ? 0 : normals.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GeometryState)) {
            return false;
        }

        GeometryState ts = (GeometryState) o;
        return nullEquals(ts.normals, normals) && nullEquals(ts.vertices, vertices);
    }

    private static boolean nullEquals(Object a, Object b) {
        return (a == null ? b == null : a.equals(b));
    }
}
