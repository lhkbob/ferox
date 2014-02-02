package com.ferox.renderer.geom;

import java.util.Map;

/**
 *
 */
class TriangleStripIterator extends AbstractTriangleIterator {
    private final int[] indices;
    private final int offset;
    private final int count;

    private int currentIndex;
    private boolean ccw;

    public TriangleStripIterator(int[] indices, int offset, int count, Map<String, Attribute> attrs) {
        super(attrs);
        this.indices = indices;
        this.offset = offset;
        this.count = count;

        reset();
    }

    @Override
    protected void configureBuilder(Builder b) {
        super.configureBuilder(b);
        b.fromStripElements(indices, offset, count);
    }

    @Override
    public boolean next() {
        currentIndex++;
        ccw = !ccw; // flip winding after each triangle
        return currentIndex < count;
    }

    @Override
    public void reset() {
        currentIndex = 1; // first call to next() puts it at 2 so we read the first texture
        ccw = false; // first call to next switches it to true
    }

    @Override
    protected int getVertexIndex(int p) {
        int rel;
        if (p == 2) {
            // last vertex of triangle, so that's always the current pointer
            rel = 0;
        } else if (ccw) {
            // first vertex is 2 back, second vertex is 1 back
            rel = p - 2;
        } else {
            // first vertex is 1 back, second vertex is 2 back
            rel = -p - 1;
        }
        if (indices == null) {
            return offset + currentIndex + rel;
        } else {
            return indices[offset + currentIndex + rel];
        }
    }

    @Override
    protected void ensureAvailable(int p) {
        if (p < 0 || p > 2) {
            throw new IndexOutOfBoundsException("Triangle index must be 0, 1, or 2. Not: " + p);
        }
        if (currentIndex <= 1) {
            throw new IllegalStateException("Must call next() first");
        }
        if (currentIndex >= count) {
            throw new IllegalStateException("No more triangles");
        }
    }
}
