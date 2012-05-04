package com.ferox.math.bounds;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;

/**
 * QueryCallback is a callback that can be passed into a SpatialIndex when
 * querying the hierarchy with a frustum or box query.
 * 
 * @author Michael Ludwig
 * @param <T> The item type processed by the callback, and stored in the
 *            hierarchy
 */
public interface QueryCallback<T> {
    /**
     * <p>
     * Invoked by a SpatialIndex when its
     * {@link SpatialIndex#query(AxisAlignedBox, QueryCallback)} or
     * {@link SpatialIndex#query(Frustum, QueryCallback)} method is called, for
     * each item satisfying the query.
     * </p>
     * <p>
     * Item query satisfaction is determined by the bounds the items were last
     * updated with, or their original bounds used when adding to the hierarchy,
     * if they were never updated. The bounds of the item are provided to the
     * callback, although the instance should not be held onto as the
     * SpatialIndex may re-use the instance for the next item.
     * </p>
     * <p>
     * Similarly, the bounds should not be modified.
     * </p>
     * 
     * @param item The item passing the query
     * @param bounds The bounds the given item had in the index
     */
    public void process(T item, @Const AxisAlignedBox bounds);
}
