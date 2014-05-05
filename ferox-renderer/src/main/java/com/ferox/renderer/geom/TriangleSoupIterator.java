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
