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
package com.ferox.scene.controller.ffp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;
import com.ferox.scene.BlinnPhongMaterial;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;

public final class GeometryGroupFactory implements StateGroupFactory {
    // all groups created by the same GeometryGroupFactory object share these instances,
    // this is safe only in a single-threaded context
    private final Matrix4 modelMatrix;

    private final Renderable geometry;
    private final Transform transform;
    private final BlinnPhongMaterial material;

    private final Geometry access;

    private final Matrix4 viewMatrix;

    public GeometryGroupFactory(EntitySystem system, @Const Matrix4 view) {
        if (system == null) {
            throw new NullPointerException("System cannot be null");
        }

        access = new Geometry();
        modelMatrix = new Matrix4();
        viewMatrix = view;
        geometry = system.createDataInstance(Renderable.ID);
        transform = system.createDataInstance(Transform.ID);
        material = system.createDataInstance(BlinnPhongMaterial.ID);
    }

    @Override
    public StateGroup newGroup() {
        return new GeometryGroup();
    }

    private class GeometryGroup implements StateGroup {
        private final List<StateNode> allNodes;
        private final Map<Geometry, StateNode> index;

        public GeometryGroup() {
            allNodes = new ArrayList<StateNode>();
            index = new HashMap<Geometry, StateNode>();
        }

        @Override
        public StateNode getNode(Entity e) {
            if (!e.get(geometry)) {
                return null;
            }

            VertexAttribute normals = (e.get(material) ? material.getNormals() : null);

            access.set(geometry, normals);
            StateNode node = index.get(access);
            if (node == null) {
                // no child group, this will be the leaf node
                GeometryState state = new GeometryState(geometry, normals);
                node = new StateNode(null, state);
                allNodes.add(node);
                index.put(state.geometry, node);
            }

            return node;
        }

        @Override
        public List<StateNode> getNodes() {
            return allNodes;
        }

        @Override
        public AppliedEffects applyGroupState(FixedFunctionRenderer r,
                                              AppliedEffects effects) {
            return effects;
        }

        @Override
        public void unapplyGroupState(FixedFunctionRenderer r, AppliedEffects effects) {
            // set attributes to null
            r.setVertices(null);
            r.setNormals(null);
            // restore matrix to camera only
            r.setModelViewMatrix(effects.getViewMatrix());
        }
    }

    private class GeometryState implements State {
        private final Geometry geometry; // immutable, do not call set()

        // packed objects to render
        private float[] matrices;
        private int count;

        public GeometryState(Renderable r, VertexAttribute n) {
            geometry = new Geometry(r, n);

            matrices = new float[32]; // 2 instances FIXME go to 1?
            count = 0;
        }

        @Override
        public void add(Entity e) {
            if (count + 16 > matrices.length) {
                // grow array
                matrices = Arrays.copyOf(matrices, matrices.length * 2);
            }

            if (e.get(transform)) {
                // use provided matrix
                transform.getMatrix().get(matrices, count, false);
            } else {
                // use identity
                modelMatrix.setIdentity().get(matrices, count, false);
            }

            count += 16;
        }

        @Override
        public AppliedEffects applyState(FixedFunctionRenderer r, AppliedEffects effects,
                                         int index) {
            r.setVertices(geometry.vertices);
            r.setNormals(geometry.normals);

            if (geometry.indices == null) {
                for (int i = 0; i < count; i += 16) {
                    // load and multiply the model with the view
                    modelMatrix.set(matrices, i, false);
                    modelMatrix.mul(viewMatrix, modelMatrix);

                    r.setModelViewMatrix(modelMatrix);
                    r.render(geometry.polyType, geometry.indexOffset, geometry.indexCount);
                }
            } else {
                for (int i = 0; i < count; i += 16) {
                    // load and multiply the model with the view
                    modelMatrix.set(matrices, i, false);
                    modelMatrix.mul(viewMatrix, modelMatrix);

                    r.setModelViewMatrix(modelMatrix);
                    r.render(geometry.polyType, geometry.indices, geometry.indexOffset,
                             geometry.indexCount);
                }
            }

            return effects;
        }

        @Override
        public void unapplyState(FixedFunctionRenderer r, AppliedEffects effects,
                                 int index) {
            // do nothing
        }
    }

    /*
     * Internal POJO to store the geometry data used for clustering, of which
     * there is a surprisingly large amount.
     */
    private static class Geometry {
        private VertexAttribute vertices;
        private VertexAttribute normals;
        private VertexBufferObject indices;
        private PolygonType polyType;
        private int indexOffset;
        private int indexCount;

        public Geometry() {
            // invalid state until set() is called
        }

        public Geometry(Renderable r, VertexAttribute n) {
            set(r, n);
        }

        public void set(Renderable r, VertexAttribute n) {
            vertices = r.getVertices();
            indices = r.getIndices();
            polyType = r.getPolygonType();
            indexOffset = r.getIndexOffset();
            indexCount = r.getIndexCount();

            normals = n;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Geometry)) {
                return false;
            }
            Geometry g = (Geometry) o;

            // vertices
            if (vertices != g.vertices) {
                // if ref's aren't equal they might still use the same data
                if (vertices.getData() != g.vertices.getData() || vertices.getElementSize() != g.vertices.getElementSize() || vertices.getOffset() != g.vertices.getOffset() || vertices.getStride() != g.vertices.getStride()) {
                    return false;
                }
            }

            // normals
            if (normals != g.normals) {
                // if ref's aren't equal they might still use the same data,
                // but we also have to control for nullability
                if (normals != null && g.normals != null) {
                    // check access pattern
                    if (normals.getData() != g.normals.getData() || normals.getElementSize() != g.normals.getElementSize() || normals.getOffset() != g.normals.getOffset() || normals.getStride() != g.normals.getStride()) {
                        return false;
                    }
                }
            }

            // indices
            if (indices == g.indices) {
                if (indexCount != g.indexCount || indexOffset != g.indexOffset || polyType != g.polyType) {
                    return false;
                }
            } else {
                return false;
            }

            // everything is equal
            return true;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result += 31 * indexCount;
            result += 31 * indexOffset;
            result += 31 * polyType.hashCode();
            result += 31 * vertices.getData().getId();
            if (indices != null) {
                result += 31 * indices.getId();
            }
            if (normals != null) {
                result += 31 * normals.getData().getId();
            }
            return result;
        }
    }
}
