package com.ferox.math.bounds;

import java.util.Arrays;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.bounds.Frustum.FrustumIntersection;

/**
 * SimpleSpatialIndex is a SpatialIndex that performs no spatial organization.
 * Each query performs a linear scan through the elements within the hierarchy.
 * Inserts, updates and removals are always constant time, and the
 * SimpleSpatialIndex always accepts every element added to it.
 * 
 * @author Michael Ludwig
 * @param <T> The Class type of elements within this hierarchy
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
