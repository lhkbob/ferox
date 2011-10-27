package com.ferox.math.bounds;

import com.ferox.entity2.Entity;
import com.ferox.math.bounds.Frustum.FrustumIntersection;
import com.ferox.scene.controller.SceneHierarchyController;

/**
 * <p>
 * A SpatialHierarchy partitions the three dimensions of a space to provide
 * efficient spatial queries. These queries are generally of the form: find all
 * objects that are within a certain region. For SpatialHierarchy, a region can
 * either be a {@link Frustum} or an {@link AxisAlignedBox}. It is assumed that
 * each region exists within the same space as the SpatialHierachy.
 * </p>
 * <p>
 * Implementations of SpatialHierarchy are required to allow dynamic updates to
 * the objects within the hierarchy. This is often much faster than removing and
 * re-adding an object because much of the initial work that was done to place
 * the object initially will remain the same. This is because objects generally
 * have good spatial and temporal locality. To facilitate this, when an object
 * is added to the SpatialHierarchy, it has a key returned which contains the
 * implementation dependent information needed to quickly update an object's
 * location within the hierarchy.
 * </p>
 * <p>
 * It is the programmer's responsibility to maintain the mapping between an
 * object and its key. An object that is added a second time (instead of
 * updating it) will then be in the hierarchy twice. If using the {@link Entity}
 * based scene package, a {@link SceneHierarchyController} handles all of this
 * logic.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The class type of the objects contained within the hierarchy
 */
public interface SpatialHierarchy<T> {
    /**
     * <p>
     * Add <tt>item</tt> to this SpatialHierarchy using the given
     * <tt>bounds</tt> to represent the extents of the item. It is assumed that
     * the item has not already been added. If this is the case,
     * {@link #update(Object, AxisAlignedBox, Object)} should be used instead.
     * On a successful add, a non-null key is returned. This key must be used in
     * {@link #update(Object, AxisAlignedBox, Object)} and
     * {@link #remove(Object, Object)} when modifying <tt>item</tt>.
     * </p>
     * <p>
     * Some implementations of SpatialHierarchy may have constraints on the
     * extents that they may grow to. If <tt>bounds</tt> is unable to fit within
     * the hierarchy, a null key is returned and the item was not added to the
     * hierarchy.
     * </p>
     * 
     * @param item The item to add
     * @param bounds The extends of <tt>item</tt>
     * @return A key object representing where the item was placed in the
     *         hierarchy, or null on a failure
     * @throws NullPointerException if item is null
     */
    public Object add(T item, AxisAlignedBox bounds);

    /**
     * Notify this hierarchy that the given <tt>item</tt> has had its extents
     * changed to <tt>bounds</tt>. The <tt>key</tt> given is assumed to be the
     * key returned from a previous {@link #add(Object, AxisAlignedBox)} and
     * that the item is still within the hierarchy. Often this can be much
     * faster than removing and then re-adding the item, although this performs
     * the equivalent actions.
     * 
     * @param item The item that is to be updated
     * @param bounds The new bounds for <tt>item</tt>
     * @param key The key previously returned by add() for this item
     * @throws NullPointerException if item or key are null
     * @throws IllegalArgumentException if the given key is invalid, or if item
     *             isn't in the hierarchy
     */
    public void update(T item, AxisAlignedBox bounds, Object key);

    /**
     * Remove <tt>item</tt> from this hierarchy so that the given item can no
     * longer be returned in queries to the SpatialHierarchy. The given
     * <tt>key</tt> must be the key provided by a previous call to
     * {@link #add(Object, AxisAlignedBox)} and the given item must still be
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
     * Query this SpatialHierarchy for all previously added items that have
     * their provided bounds {@link AxisAlignedBox#intersects(AxisAlignedBox)
     * intersecting} with <tt>volume</tt>. Additionally, any item that was added
     * with a null bounds will always be considered as intersecting the query
     * volume.
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
    public void query(AxisAlignedBox volume, QueryCallback<T> callback);

    /**
     * <p>
     * Query this SpatialHierarchy for all previously added items that have
     * their provided bounds
     * {@link AxisAlignedBox#intersects(Frustum, PlaneState) intersecting} with
     * <tt>frustum</tt>. An item's bounds intersects with the Frustum if its
     * FrustumIntersection is not {@link FrustumIntersection#OUTSIDE}.
     * Additionally, any item that was added with a null bounds will always be
     * considered as intersecting the query volume.
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
     * Query this SpatialHierarchy for all pairs of intersecting items that have
     * been added to this hierarchy. Intersections are determined by
     * {@link AxisAlignedBox#intersects(AxisAlignedBox)}, between the bounds
     * provided with the items when they were added or updated.
     * </p>
     * <p>
     * A pair of items is considered unique, and any combination of two items
     * that intersect will not be passed to the callback more than once. Unlike
     * the other queries, items with null bounds are ignored when determining
     * the number of intersecting pairs, because the performance hit is much
     * worse in this O(N^2) problem compared to the O(N) problems for
     * visibility.
     * </p>
     * 
     * @param callback The IntersectionCallback to run on each pair that is
     *            intersecting
     * @throws NullPointerException if callback is null
     */
    public void query(IntersectionCallback<T> callback);
}
