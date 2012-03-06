package com.ferox.math.bounds;

/**
 * IntersectionCallback is a callback that can be passed into a SpatialIndex
 * to discover all intersecting pairs of items within the hierarchy.
 * 
 * @author Michael Ludwig
 * @param <T> The item type processed by the callback, and stored in the
 *            hierarchy
 */
public interface IntersectionCallback<T> {
    /**
     * <p>
     * Invoked by a SpatialIndex when its
     * {@link SpatialIndex#query(IntersectionCallback)} method is called,
     * for each intersecting pair. The order of the items is not important, a
     * SpatialIndex will only call process() once for each unique pair of
     * items within it.
     * </p>
     * <p>
     * Item intersection is determined by the bounds the items were last updated
     * with, or their original bounds used when adding to the hierarchy, if they
     * were never updated. These bounds are provided to the callback.
     * </p>
     * 
     * @param itemA The first item in the intersection
     * @param boundsA The first item's bounds
     * @param itemB The second item in the intersection
     * @param boundsB The second item's bounds
     */
    public void process(T itemA, ReadOnlyAxisAlignedBox boundsA, T itemB, ReadOnlyAxisAlignedBox boundsB);
}
