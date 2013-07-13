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

/**
 * <p/>
 * A SpatialIndex partitions the three dimensions of a space to provide efficient spatial queries. These
 * queries are generally of the form: find all objects that are within a certain region. For SpatialIndex, a
 * region can either be a {@link Frustum} or an {@link AxisAlignedBox}. It is assumed that each region exists
 * within the same transform space as the SpatialIndex.
 * <p/>
 * SpatialIndices are intended for collections of higher-level entities, such as a physics object, or entire
 * geometries. It is not meant to store indices of the triangles within a geometry, although the algorithms
 * will likely be very similar triangle/primitive specific indices will likely be much more efficient.
 * <p/>
 * If a SpatialIndex is meant to contain dynamic objects that move from frame-to-frame, the expected workflow
 * is to add every object to the index, process with the index, and then clear it before the next frame. If
 * sporadic updates to objects are needed, you must remove and then re-add the object.
 *
 * @param <T> The class type of the objects contained within the hierarchy
 *
 * @author Michael Ludwig
 */
public interface SpatialIndex<T> {
    /**
     * <p/>
     * Add <var>item</var> to this SpatialIndex using the given <var>bounds</var> to represent the extents of
     * the item. If the item is already in the index, the item may be reported multiple times in queries.
     * <p/>
     * Some implementations of SpatialIndex may have constraints on their spatial dimensions. If
     * <var>bounds</var> is unable to fit within these constraints, a false is returned and the item was not
     * added to the hierarchy.
     * <p/>
     * Implementations must copy the provided bounds so that any subsequent changes to the bounds instance to
     * do not affect the index.
     *
     * @param item   The item to add
     * @param bounds The extends of <var>item</var>
     *
     * @return True if the item was added to the index, false otherwise
     *
     * @throws NullPointerException if item or bounds is null
     */
    public boolean add(T item, @Const AxisAlignedBox bounds);

    /**
     * Remove <var>item</var> from this hierarchy so that the given item will no longer be returned in queries
     * on the SpatialIndex. False is returned if the index was not modified (i.e. the item was not in this
     * collection).
     *
     * @param item The item to remove
     *
     * @return True if the collection was modified
     *
     * @throws NullPointerException if item is null
     */
    public boolean remove(T item);

    /**
     * Empty this SpatialIndex so that it no longer contains any items. If <var>fast</var> is true, the index
     * is not required to remove references to old items. This can be more efficient if the index will be
     * re-filled and the old references will be overwritten. However, this may also prevent items from being
     * garbage collected if they are not overwritten.
     *
     * @param fast True if the index can optimize the clear
     */
    public void clear(boolean fast);

    /**
     * Empty this SpatialIndex so that it no longer contains any items. This is a convenience for
     * <code>clear(false);</code>.
     */
    public void clear();

    /**
     * <p/>
     * Query this SpatialIndex for all previously added items that have their provided bounds {@link
     * AxisAlignedBox#intersects(AxisAlignedBox) intersecting} with <var>volume</var>.
     * <p/>
     * The provided QueryCallback has its {@link QueryCallback#process(Object, AxisAlignedBox)} invoked for
     * each intersecting item. An item will be passed to the callback once per query.
     *
     * @param volume   The volume representing the spatial query
     * @param callback A QueryCallback to run on each item within the query
     *
     * @throws NullPointerException if volume or callback is null
     */
    public void query(@Const AxisAlignedBox volume, QueryCallback<T> callback);

    /**
     * <p/>
     * Query this SpatialIndex for all previously added items that have their provided bounds {@link
     * Frustum#intersects(AxisAlignedBox, PlaneState) intersecting} with <var>frustum</var>. An item's bounds
     * intersects with the Frustum if its FrustumIntersection is not {@link FrustumIntersection#OUTSIDE}.
     * <p/>
     * The provided QueryCallback has its {@link QueryCallback#process(Object, AxisAlignedBox)} invoked for
     * each intersecting item. An item will be passed to the callback once per query.
     *
     * @param f        The frustum representing the spatial query
     * @param callback A QueryCallback to run on each item within the query
     *
     * @throws NullPointerException if frustum or callback is null
     */
    public void query(Frustum f, QueryCallback<T> callback);

    /**
     * <p/>
     * Query this SpatialIndex for all pairs of items intersecting based on their provided bounds. This will
     * invoke the provided callback once for each unique pair of items in the index that intersect according
     * to {@link AxisAlignedBox#intersects(AxisAlignedBox)}.
     * <p/>
     * The order of arguments to the callback is meaningless, the first and second item can be swapped and its
     * the same intersecting pair as far as the above uniqueness clause is concerned.
     *
     * @param callback The callback to run for each pair
     *
     * @throws NullPointerException if callback is null
     */
    public void query(IntersectionCallback<T> callback);
}
