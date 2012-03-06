package com.ferox.math.bounds;

import com.ferox.math.bounds.Frustum.FrustumIntersection;

/**
 * <p>
 * A SpatialIndex partitions the three dimensions of a space to provide
 * efficient spatial queries. These queries are generally of the form: find all
 * objects that are within a certain region. For SpatialIndex, a region can
 * either be a {@link Frustum} or an {@link ReadOnlyAxisAlignedBox}. It is assumed that
 * each region exists within the same space as the SpatialHierachy.
 * </p>
 * <p>
 * Implementations of SpatialIndex are required to allow dynamic updates to
 * the objects within the hierarchy. This is often much faster than removing and
 * re-adding an object because much of the initial work that was done to place
 * the object initially will remain the same. This is because objects generally
 * have good spatial and temporal locality. To facilitate this, when an object
 * is added to the SpatialIndex, it has a key returned which contains the
 * implementation dependent information needed to quickly update an object's
 * location within the hierarchy.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The class type of the objects contained within the hierarchy
 */
public interface SpatialIndex<T> {
    /**
     * <p>
     * Add <tt>item</tt> to this SpatialIndex using the given <tt>bounds</tt> to
     * represent the extents of the item. It is assumed that the item has not
     * already been added. If this is the case,
     * {@link #update(Object, ReadOnlyAxisAlignedBox, Object)} should be used
     * instead. On a successful add, a non-null key is returned. This key must
     * be used in {@link #update(Object, ReadOnlyAxisAlignedBox, Object)} and
     * {@link #remove(Object, Object)} when modifying <tt>item</tt>.
     * </p>
     * <p>
     * Some implementations of SpatialIndex may have constraints on their
     * spatial dimensions. If <tt>bounds</tt> is unable to fit within these
     * constraints, a null key is returned and the item was not added to the
     * hierarchy.
     * </p>
     * <p>
     * Implementations must copy the provided bounds so that any subsequent
     * changes to the bounds instance to do not contaminate the hierarchy.
     * </p>
     * 
     * @param item The item to add
     * @param bounds The extends of <tt>item</tt>
     * @return A key object representing where the item was placed in the
     *         hierarchy, or null on a failure
     * @throws NullPointerException if item or bounds is null
     */
    public Object add(T item, ReadOnlyAxisAlignedBox bounds);

    /**
     * <p>
     * Notify this hierarchy that the given <tt>item</tt> has had its extents
     * changed to <tt>bounds</tt>. The <tt>key</tt> given is assumed to be the
     * key returned from a previous {@link #add(Object, ReadOnlyAxisAlignedBox)}
     * and that the item is still within the hierarchy. Often this can be much
     * faster than removing and then re-adding the item, although this performs
     * the equivalent actions.
     * </p>
     * <p>
     * If the updated bounds falls outside of any spatial constraints imposed on
     * the index, false is returned and the item is removed from the index.
     * Otherwise true is returned and the item will have been updated to be at
     * the provided bounds.
     * </p>
     * <p>
     * Implementations must copy the provided bounds so that any subsequent
     * changes to the bounds instance to do not contaminate the hierarchy.
     * </p>
     * 
     * @param item The item that is to be updated
     * @param bounds The new bounds for <tt>item</tt>
     * @param key The key previously returned by add() for this item
     * @return True if the update was successful, or false if the item was
     *         removed due to spatial constraints
     * @throws NullPointerException if item or key or bounds are null
     * @throws IllegalArgumentException if the given key is invalid, or if item
     *             isn't in the hierarchy
     */
    public boolean update(T item, ReadOnlyAxisAlignedBox bounds, Object key);

    /**
     * Remove <tt>item</tt> from this hierarchy so that the given item can no
     * longer be returned in queries to the SpatialIndex. The given
     * <tt>key</tt> must be the key provided by a previous call to
     * {@link #add(Object, ReadOnlyAxisAlignedBox)} and the given item must still be
     * within the hierarchy. After removal, the key for an item is invalidated
     * and a new key will be provided if the item is re-added.
     * 
     * @param item The item to remove
     * @param key The key previously returned by add() for this item
     * @throws NullPointerException if item or key are null
     * @throws IllegalArgumentException if the given key is invalid, or if item
     *             isn't within the hierarchy
     */
    public void remove(T item, Object key);

    /**
     * <p>
     * Query this SpatialIndex for all previously added items that have
     * their provided bounds {@link ReadOnlyAxisAlignedBox#intersects(ReadOnlyAxisAlignedBox)
     * intersecting} with <tt>volume</tt>.
     * </p>
     * <p>
     * The provided QueryCallback has its {@link QueryCallback#process(Object)}
     * for each intersecting item. An item will be passed to the callback once
     * per query.
     * </p>
     * 
     * @param volume The volume representing the spatial query
     * @param callback A QueryCallback to run on each item within the query
     * @throws NullPointerException if volume or callback is null
     */
    public void query(ReadOnlyAxisAlignedBox volume, QueryCallback<T> callback);

    /**
     * <p>
     * Query this SpatialIndex for all previously added items that have
     * their provided bounds
     * {@link ReadOnlyAxisAlignedBox#intersects(Frustum, PlaneState) intersecting} with
     * <tt>frustum</tt>. An item's bounds intersects with the Frustum if its
     * FrustumIntersection is not {@link FrustumIntersection#OUTSIDE}.
     * </p>
     * <p>
     * The provided QueryCallback has its {@link QueryCallback#process(Object)}
     * for each intersecting item. An item will be passed to the callback once
     * per query.
     * </p>
     * 
     * @param frustum The frustum representing the spatial query
     * @param callback A QueryCallback to run on each item within the query
     * @throws NullPointerException if frustum or callback is null
     */
    public void query(Frustum f, QueryCallback<T> callback);

    /**
     * <p>
     * Query this SpatialIndex for all pairs of intersecting items that have
     * been added to this hierarchy. Intersections are determined by
     * {@link ReadOnlyAxisAlignedBox#intersects(ReadOnlyAxisAlignedBox)}, between the bounds
     * provided with the items when they were added or updated.
     * </p>
     * <p>
     * A pair of items is considered unique, and any combination of two items
     * that intersect will not be passed to the callback more than once.
     * </p>
     * 
     * @param callback The IntersectionCallback to run on each pair that is
     *            intersecting
     * @throws NullPointerException if callback is null
     */
    public void query(IntersectionCallback<T> callback);
}
