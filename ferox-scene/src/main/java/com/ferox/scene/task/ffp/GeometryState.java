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

    public void set(VertexAttribute vertices, VertexAttribute normals, DrawStyle front, DrawStyle back) {
        this.vertices = vertices;
        this.normals = normals;
        this.front = front;
        this.back = back;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
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
