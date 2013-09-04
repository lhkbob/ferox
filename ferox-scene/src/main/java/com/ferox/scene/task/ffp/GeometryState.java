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

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.renderer.*;
import com.ferox.renderer.Renderer.DrawStyle;

import java.util.Arrays;

public class GeometryState implements State {
    private static final Matrix4 OBJ_PLANES = new Matrix4().setIdentity();

    private VertexAttribute vertices;
    private VertexAttribute normals;
    private VertexAttribute texCoords;

    private DrawStyle front;
    private DrawStyle back;

    private ElementBuffer indices;
    private int indexOffset;
    private int indexCount;
    private Renderer.PolygonType polyType;

    private final Matrix4 modelMatrix = new Matrix4();

    public void set(VertexAttribute vertices, VertexAttribute normals, VertexAttribute texCoords,
                    DrawStyle front, DrawStyle back, Renderer.PolygonType polyType, ElementBuffer indices,
                    int offset, int count) {
        this.vertices = vertices;
        this.normals = normals;
        this.texCoords = texCoords;
        this.front = front;
        this.back = back;
        this.polyType = polyType;
        this.indices = indices;
        indexOffset = offset;
        indexCount = count;
    }

    public RenderState newOpaqueRenderState() {
        return new OpaqueRenderState();
    }

    public RenderState newTransparentRenderState() {
        return new TransparentRenderState();
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        r.setVertices(vertices);
        r.setNormals(normals);
        r.setIndices(indices);

        if (texCoords == null) {
            r.setTextureObjectPlanes(FixedFunctionRenderTask.DIFFUSE_TEXTURE_UNIT, OBJ_PLANES);
            r.setTextureCoordinateSource(FixedFunctionRenderTask.DIFFUSE_TEXTURE_UNIT,
                                         FixedFunctionRenderer.TexCoordSource.OBJECT);

            r.setTextureObjectPlanes(FixedFunctionRenderTask.DECAL_TEXTURE_UNIT, OBJ_PLANES);
            r.setTextureCoordinateSource(FixedFunctionRenderTask.DECAL_TEXTURE_UNIT,
                                         FixedFunctionRenderer.TexCoordSource.OBJECT);

            r.setTextureObjectPlanes(FixedFunctionRenderTask.EMISSIVE_TEXTURE_UNIT, OBJ_PLANES);
            r.setTextureCoordinateSource(FixedFunctionRenderTask.EMISSIVE_TEXTURE_UNIT,
                                         FixedFunctionRenderer.TexCoordSource.OBJECT);
        } else {
            r.setTextureCoordinateSource(FixedFunctionRenderTask.DIFFUSE_TEXTURE_UNIT,
                                         FixedFunctionRenderer.TexCoordSource.ATTRIBUTE);
            r.setTextureCoordinates(FixedFunctionRenderTask.DIFFUSE_TEXTURE_UNIT, texCoords);

            r.setTextureCoordinateSource(FixedFunctionRenderTask.DECAL_TEXTURE_UNIT,
                                         FixedFunctionRenderer.TexCoordSource.ATTRIBUTE);
            r.setTextureCoordinates(FixedFunctionRenderTask.DECAL_TEXTURE_UNIT, texCoords);

            r.setTextureCoordinateSource(FixedFunctionRenderTask.EMISSIVE_TEXTURE_UNIT,
                                         FixedFunctionRenderer.TexCoordSource.ATTRIBUTE);
            r.setTextureCoordinates(FixedFunctionRenderTask.EMISSIVE_TEXTURE_UNIT, texCoords);
        }

        currentNode.visitChildren(effects, access);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + (vertices == null ? 0 : vertices.hashCode());
        hash = 31 * hash + (normals == null ? 0 : normals.hashCode());
        hash = 31 * hash + (texCoords == null ? 0 : texCoords.hashCode());
        hash = 31 * hash + front.hashCode();
        hash = 31 * hash + back.hashCode();
        hash = 31 * hash + (indices == null ? 0 : indices.hashCode());
        hash = 31 * hash + indexOffset;
        hash = 31 * hash + indexCount;
        hash = 31 * hash + polyType.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GeometryState)) {
            return false;
        }

        GeometryState ts = (GeometryState) o;
        return nullEquals(ts.normals, normals) && nullEquals(ts.vertices, vertices) &&
               nullEquals(ts.texCoords, texCoords) && nullEquals(ts.indices, indices) &&
               ts.indexCount == indexCount && ts.indexOffset == indexOffset && ts.polyType == polyType &&
               ts.front == front && ts.back == back;
    }

    private static boolean nullEquals(Object a, Object b) {
        return (a == null ? b == null : a.equals(b));
    }

    private abstract class AbstractRenderState implements RenderState {
        // packed objects to render
        protected float[] matrices;
        protected int count;

        public AbstractRenderState() {
            count = 0;
            matrices = new float[16];
        }

        @Override
        public void add(@Const Matrix4 transform) {
            if (count + 16 > matrices.length) {
                // grow array
                matrices = Arrays.copyOf(matrices, matrices.length * 2);
            }

            // use provided matrix
            transform.get(matrices, count, false);

            count += 16;
        }
    }

    private class OpaqueRenderState extends AbstractRenderState {
        @Override
        public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
            FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

            // for opaque rendering, we can do the front and back in a single pass
            r.setDrawStyle(front, back);
            for (int i = 0; i < count; i += 16) {
                // load and multiply the model with the view
                modelMatrix.set(matrices, i, false);
                modelMatrix.mul(effects.getViewMatrix(), modelMatrix);

                r.setModelViewMatrix(modelMatrix);
                r.render(polyType, indexOffset, indexCount);
            }

            // restore modelview matrix for lighting, etc.
            r.setModelViewMatrix(effects.getViewMatrix());
        }
    }

    private class TransparentRenderState extends AbstractRenderState {
        @Override
        public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
            FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

            for (int i = 0; i < count; i += 16) {
                // load and multiply the model with the view
                modelMatrix.set(matrices, i, false);
                modelMatrix.mul(effects.getViewMatrix(), modelMatrix);

                r.setModelViewMatrix(modelMatrix);

                // draw the back first if we have a non-NONE style for it
                if (back != DrawStyle.NONE) {
                    r.setDrawStyle(DrawStyle.NONE, back);
                    r.render(polyType, indexOffset, indexCount);
                }
                // draw the front second if we have a non-NONE style for it
                if (front != DrawStyle.NONE) {
                    r.setDrawStyle(front, DrawStyle.NONE);
                    r.render(polyType, indexOffset, indexCount);
                }
            }

            // restore modelview matrix for lighting, etc.
            r.setModelViewMatrix(effects.getViewMatrix());
        }
    }
}
