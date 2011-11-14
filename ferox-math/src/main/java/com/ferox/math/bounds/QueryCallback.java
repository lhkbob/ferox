package com.ferox.math.bounds;

/**
 * QueryCallback is a callback that can be passed into a SpatialHierarchy when
 * querying the hierarchy with a frustum or box query.
 * 
 * @author Michael Ludwig
 * @param <T> The item type processed by the callback, and stored in the
 *            hierarchy
 */
public interface QueryCallback<T> {
    /**
     * <p>
     * Invoked by a SpatialHierarchy when its
     * {@link SpatialHierarchy#query(ReadOnlyAxisAlignedBox, QueryCallback)} or
     * {@link SpatialHierarchy#query(Frustum, QueryCallback)} method is called,
     * for each item satisfying the query.
     * </p>
     * <p>
     * Item query satisfaction is determined by the bounds the items were last
     * updated with, or their original bounds used when adding to the hierarchy,
     * if they were never updated.
     * </p>
     * 
     * @param item The item passing the query
     */
    public void process(T item);
}
