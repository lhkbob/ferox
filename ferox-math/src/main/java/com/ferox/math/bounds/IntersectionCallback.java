package com.ferox.math.bounds;

/**
 * IntersectionCallback is a callback that can be passed into a SpatialHierarchy
 * to discover all intersecting pairs of items within the hierarchy.
 * 
 * @author Michael Ludwig
 * @param <T> The item type processed by the callback, and stored in the
 *            hierarchy
 */
public interface IntersectionCallback<T> {
    /**
     * <p>
     * Invoked by a SpatialHierarchy when its
     * {@link SpatialHierarchy#query(IntersectionCallback)} method is called,
     * for each intersecting pair. The order of the items is not important, a
     * SpatialHierarchy will only call process() once for each unique pair of
     * items within it.
     * </p>
     * <p>
     * Item intersection is determined by the bounds the items were last updated
     * with, or their original bounds used when adding to the hierarchy, if they
     * were never updated.
     * </p>
     * 
     * @param item1 The first item in the intersection
     * @param item2 The second item in the intersection
     */
    public void process(T item1, T item2);
}
