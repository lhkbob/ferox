package com.ferox.scene.controller.ffp;

import java.util.Arrays;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.VertexBufferObject;

public class RenderState implements StaticState {
    protected VertexBufferObject indices;
    protected int indexOffset;
    protected int indexCount;
    protected PolygonType polyType;

    protected final Matrix4 modelMatrix = new Matrix4();

    // packed objects to render
    protected float[] matrices;
    protected int count;

    public RenderState() {
        matrices = new float[16];
        count = 0;
    }

    public void set(PolygonType polyType, VertexBufferObject indices, int offset,
                    int count) {
        this.polyType = polyType;
        this.indices = indices;
        indexOffset = offset;
        indexCount = count;
    }

    public PolygonType getPolygonType() {
        return polyType;
    }

    public VertexBufferObject getIndices() {
        return indices;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public int getIndexOffset() {
        return indexOffset;
    }

    public void add(@Const Matrix4 transform) {
        if (count + 16 > matrices.length) {
            // grow array
            matrices = Arrays.copyOf(matrices, matrices.length * 2);
        }

        // use provided matrix
        transform.get(matrices, count, false);

        count += 16;
    }

    public void clear() {
        count = 0;
    }

    public RenderState cloneGeometry() {
        RenderState r = new RenderState();
        r.set(polyType, indices, indexOffset, indexCount);
        return r;
    }

    public TransparentRenderState cloneTransparent() {
        TransparentRenderState r = new TransparentRenderState();
        r.set(polyType, indices, indexOffset, indexCount);
        return r;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        if (indices == null) {
            for (int i = 0; i < count; i += 16) {
                // load and multiply the model with the view
                modelMatrix.set(matrices, i, false);
                modelMatrix.mul(effects.getViewMatrix(), modelMatrix);

                r.setModelViewMatrix(modelMatrix);
                r.render(polyType, indexOffset, indexCount);
            }
        } else {
            for (int i = 0; i < count; i += 16) {
                // load and multiply the model with the view
                modelMatrix.set(matrices, i, false);
                modelMatrix.mul(effects.getViewMatrix(), modelMatrix);

                r.setModelViewMatrix(modelMatrix);
                r.render(polyType, indices, indexOffset, indexCount);
            }
        }

        // restore modelview matrix for lighting, etc.
        r.setModelViewMatrix(effects.getViewMatrix());
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
        if (!(o instanceof RenderState)) {
            return false;
        }
        RenderState r = (RenderState) o;
        return nullEquals(r.indices, indices) && r.indexCount == indexCount && r.indexOffset == indexOffset && r.polyType == polyType;
    }

    private static boolean nullEquals(Object a, Object b) {
        return (a == null ? b == null : a.equals(b));
    }
}
