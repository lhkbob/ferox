package com.ferox.renderer.geom;

import java.util.Map;

/**
 *
 */
class TriangleSoupIterator extends AbstractTriangleIterator {
    private final int[] indices;
    private final int offset;
    private final int count;

    private int currentIndex;

    public TriangleSoupIterator(int[] indices, int offset, int count, Map<String, Attribute> attrs) {
        super(attrs);
        this.indices = indices;
        this.offset = offset;
        this.count = count;

        reset();
    }

    @Override
    protected void configureBuilder(Builder b) {
        super.configureBuilder(b);
        b.fromElements(indices, offset, count);
    }

    @Override
    protected void ensureAvailable(int p) {
        if (p < 0 || p > 2) {
            throw new IndexOutOfBoundsException("Triangle index must be 0, 1, or 2. Not: " + p);
        }
        if (currentIndex < 0) {
            throw new IllegalStateException("Must call next() first");
        }
        if (currentIndex >= count) {
            throw new IllegalStateException("No more triangles");
        }
    }

    @Override
    protected int getVertexIndex(int p) {
        if (indices == null) {
            // array access
            return offset + currentIndex + p;
        } else {
            // element access
            return indices[offset + currentIndex + p];
        }
    }

    @Override
    public boolean next() {
        currentIndex += 3;
        return currentIndex < count;
    }

    @Override
    public void reset() {
        currentIndex = -3; // starting value so that first call to next() hits 0
    }
}
