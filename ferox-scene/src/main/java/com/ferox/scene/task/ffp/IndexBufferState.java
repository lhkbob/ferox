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
import com.ferox.math.Functions;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.renderer.VertexAttribute;
import com.ferox.renderer.geom.TopologyUtil;
import com.ferox.renderer.geom.VertexBufferObject;
import com.ferox.resource.BufferData;
import com.ferox.resource.UnsignedDataView;
import com.ferox.util.ItemView;
import com.ferox.util.QuickSort;

import java.util.Arrays;

public class IndexBufferState {
    private VertexBufferObject indices;
    private int indexOffset;
    private int indexCount;
    private PolygonType polyType;

    private final Matrix4 modelMatrix = new Matrix4();

    public void set(PolygonType polyType, VertexBufferObject indices, int offset, int count) {
        this.polyType = polyType;
        this.indices = indices;
        indexOffset = offset;
        indexCount = count;
    }

    public RenderState newOpaqueRenderState() {
        return new OpaqueRenderState();
    }

    public RenderState newTransparentRenderState(VertexAttribute vertices,
                                                 VertexBufferObject sharedIndexBuffer) {
        return new TransparentRenderState(sharedIndexBuffer, vertices);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + (indices == null ? 0 : indices.hashCode());
        hash = 31 * hash + indexOffset;
        hash = 31 * hash + indexCount;
        hash = 31 * hash + polyType.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IndexBufferState)) {
            return false;
        }
        IndexBufferState r = (IndexBufferState) o;
        return nullEquals(r.indices, indices) && r.indexCount == indexCount &&
               r.indexOffset == indexOffset && r.polyType == polyType;
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

            r.setIndices(indices);

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
        private final VertexBufferObject sortedIndicesShared;
        private final VertexAttribute vertices;

        public TransparentRenderState(VertexBufferObject sortedIndicesShared, VertexAttribute vertices) {
            this.sortedIndicesShared = sortedIndicesShared;
            this.vertices = vertices;
        }

        @Override
        public void visitNode(StateNode currentNode, AppliedEffects effects, HardwareAccessLayer access) {
            FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

            int inflatedIndexCount = polyType.getPolygonCount(indexCount) * polyType.getPolygonSize();
            PolygonType inflatedType = polyType;
            synchronized (sortedIndicesShared) {
                if (sortedIndicesShared.getData().getLength() < inflatedIndexCount) {
                    sortedIndicesShared.setData(new BufferData(new int[inflatedIndexCount]));
                }
                BufferData sharedData = sortedIndicesShared.getData();

                if (indices == null) {
                    switch (polyType) {
                    case TRIANGLE_STRIP:
                        TopologyUtil.inflateTriangleStripArray(indexOffset, indexCount, sharedData, 0);
                        inflatedType = PolygonType.TRIANGLES;
                        break;
                    default:
                        TopologyUtil.inflateSimpleArray(indexOffset, indexCount, sharedData, 0);
                        break;
                    }
                } else {
                    switch (polyType) {
                    case TRIANGLE_STRIP:
                        TopologyUtil
                                .inflateTriangleStrip(indices.getData(), indexOffset, indexCount, sharedData,
                                                      0);
                        inflatedType = PolygonType.TRIANGLES;
                        break;
                    default:
                        UnsignedDataView source = indices.getData().getUnsignedView();
                        UnsignedDataView target = sharedData.getUnsignedView();

                        for (int i = 0; i < indexCount; i++) {
                            target.set(i, source.get(i + indexOffset));
                        }
                        break;
                    }
                }
            }

            FaceView view = new FaceView(inflatedType, sortedIndicesShared, inflatedIndexCount, vertices,
                                         modelMatrix);
            for (int i = 0; i < count; i += 16) {
                // load and multiply the model with the view
                modelMatrix.set(matrices, i, false);
                modelMatrix.mul(effects.getViewMatrix(), modelMatrix);
                r.setModelViewMatrix(modelMatrix);

                // sort indices within sortedIndicesShared
                synchronized (sortedIndicesShared) {
                    QuickSort.sort(view);
                    sortedIndicesShared.markDirty(0, inflatedIndexCount);
                }

                // clear, update and rebind sorted indices
                r.setIndices(null);
                access.update(sortedIndicesShared);
                r.setIndices(sortedIndicesShared);

                r.render(inflatedType, 0, inflatedIndexCount);
            }

            // restore modelview matrix for lighting, etc.
            r.setModelViewMatrix(effects.getViewMatrix());
        }
    }

    private static final class FaceView implements ItemView {
        private final UnsignedDataView indices;
        private final int count;
        private final PolygonType polyType;

        private final VertexAttribute vertices;
        private final Matrix4 modelview;

        private final Vector3 v;

        // this expects the converted index buffer, so it is offset from 0
        // and contains non-strip polygons only (points, lines, tris, or quads)
        public FaceView(PolygonType type, VertexBufferObject indices, int count, VertexAttribute vertices,
                        @Const Matrix4 modelview) {
            this.indices = indices.getData().getUnsignedView();
            this.count = count;
            this.polyType = type;
            this.vertices = vertices;
            this.modelview = modelview;
            v = new Vector3();
        }

        @Override
        public int hash(int index) {
            float[] vData = vertices.getVBO().getData().getArray();

            // convert polygon index to vertex index
            index *= polyType.getPolygonSize();

            float centroid = 0f;
            for (int i = 0; i < polyType.getPolygonSize(); i++) {
                v.set(vData, vertices.getArrayIndex((int) indices.get(index + i), 0));
                v.transform(modelview);
                centroid += v.z;
            }
            centroid /= polyType.getPolygonSize();

            // centroid now contains average camera-space z for the polygon 
            return Functions.sortableFloatToIntBits(centroid);
        }

        @Override
        public void swap(int srcIndex, int dstIndex) {
            // convert polygon index to vertex index
            srcIndex *= polyType.getPolygonSize();
            dstIndex *= polyType.getPolygonSize();

            // swap the entire polygon
            for (int i = 0; i < polyType.getPolygonSize(); i++) {
                long t = indices.get(srcIndex + i);
                indices.set(srcIndex + i, indices.get(dstIndex + i));
                indices.set(dstIndex + i, t);
            }
        }

        @Override
        public int length() {
            return polyType.getPolygonCount(count);
        }
    }
}
