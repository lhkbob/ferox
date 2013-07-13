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
package com.ferox.math.bounds;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.bounds.Frustum.FrustumIntersection;

import java.util.Arrays;

/**
 * SimpleSpatialIndex is a SpatialIndex that performs no spatial organization. Each query performs a linear
 * scan through the elements within the hierarchy. Inserts, updates and removals are always constant time, and
 * the SimpleSpatialIndex always accepts every element added to it.
 *
 * @param <T> The Class type of elements within this hierarchy
 *
 * @author Michael Ludwig
 */
public class SimpleSpatialIndex<T> implements SpatialIndex<T> {
    private Object[] elements;
    private double[] aabbs;
    private int size;

    /**
     * Create a new SimpleSpatialIndex that is initially empty.
     */
    public SimpleSpatialIndex() {
        elements = new Object[8];
        aabbs = new double[48];
        size = 0;
    }

    @Override
    public boolean add(T item, @Const AxisAlignedBox bounds) {
        int itemIndex = size;
        if (itemIndex == elements.length) {
            // grow items
            int newSize = (int) (itemIndex * 1.5);
            elements = Arrays.copyOf(elements, newSize);
            aabbs = Arrays.copyOf(aabbs, newSize * 6);
        }
        elements[itemIndex] = item;

        bounds.min.get(aabbs, itemIndex * 6);
        bounds.max.get(aabbs, itemIndex * 6 + 3);

        size++;

        // simple index always succeeds in adding an element
        return true;
    }

    @Override
    public boolean remove(T element) {
        int item = -1;
        for (int i = 0; i < size; i++) {
            if (elements[i] == element) {
                item = i;
                break;
            }
        }

        if (item >= 0) {
            if (item < size - 1) {
                int swap = size - 1;
                elements[item] = elements[swap];
                System.arraycopy(aabbs, swap * 6, aabbs, item * 6, 6);

                // must also null the old element index since that won't get
                // iterated over during a non-fast clear anymore
                elements[swap] = null;
            } else {
                elements[item] = null; // to help gc
            }

            size--;
            return true;
        } else {
            // not in the index
            return false;
        }
    }

    private void updateBounds(AxisAlignedBox bounds, int index) {
        int realIndex = index * 6;
        bounds.min.set(aabbs, realIndex);
        bounds.max.set(aabbs, realIndex + 3);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void query(@Const AxisAlignedBox volume, QueryCallback<T> callback) {
        if (volume == null) {
            throw new NullPointerException("Query bound volume cannot be null");
        }
        if (callback == null) {
            throw new NullPointerException("Callback cannot be null");
        }

        AxisAlignedBox itemBounds = new AxisAlignedBox();
        for (int i = 0; i < size; i++) {
            updateBounds(itemBounds, i);
            if (itemBounds.intersects(volume)) {
                callback.process((T) elements[i], itemBounds);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void query(Frustum frustum, QueryCallback<T> callback) {
        if (frustum == null) {
            throw new NullPointerException("Query Frustum cannot be null");
        }
        if (callback == null) {
            throw new NullPointerException("Callback cannot be null");
        }

        AxisAlignedBox itemBounds = new AxisAlignedBox();
        for (int i = 0; i < size; i++) {
            updateBounds(itemBounds, i);
            if (frustum.intersects(itemBounds, null) != FrustumIntersection.OUTSIDE) {
                callback.process((T) elements[i], itemBounds);
            }
        }
    }

    @Override
    public void clear() {
        clear(false);
    }

    @Override
    public void clear(boolean fast) {
        if (!fast) {
            Arrays.fill(elements, null);
        }
        size = 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void query(IntersectionCallback<T> callback) {
        if (callback == null) {
            throw new NullPointerException("Callback cannot be null");
        }

        AxisAlignedBox ba = new AxisAlignedBox();
        AxisAlignedBox bb = new AxisAlignedBox();

        for (int a = 0; a < size; a++) {
            updateBounds(ba, a);
            for (int b = a + 1; b < size; b++) {
                updateBounds(bb, b);

                if (ba.intersects(bb)) {
                    // intersecting pair
                    callback.process((T) elements[a], ba, (T) elements[b], bb);
                }
            }
        }
    }
}
