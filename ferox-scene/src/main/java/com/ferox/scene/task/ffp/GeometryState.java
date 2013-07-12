package com.ferox.scene.task.ffp;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.VertexAttribute;

public class GeometryState implements State {
    private VertexAttribute vertices;
    private VertexAttribute normals;

    private DrawStyle front;
    private DrawStyle back;

    public void set(VertexAttribute vertices, VertexAttribute normals, DrawStyle front,
                    DrawStyle back) {
        this.vertices = vertices;
        this.normals = normals;
        this.front = front;
        this.back = back;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        r.setDrawStyle(front, back);
        r.setVertices(vertices);
        r.setNormals(normals);

        currentNode.visitChildren(effects, access);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + (vertices == null ? 0 : vertices.hashCode());
        hash = 31 * hash + (normals == null ? 0 : normals.hashCode());
        hash = 31 * hash + front.hashCode();
        hash = 31 * hash + back.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GeometryState)) {
            return false;
        }

        GeometryState ts = (GeometryState) o;
        return nullEquals(ts.normals, normals) && nullEquals(ts.vertices, vertices) &&
               ts.front == front && ts.back == back;
    }

    private static boolean nullEquals(Object a, Object b) {
        return (a == null ? b == null : a.equals(b));
    }
}
