package com.ferox.math.bounds;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;

/**
 * BoundedSpatialIndex is a SpatialIndex that has a maximum extent that all added objects
 * must fit within. It will ignore objects that extend past its extent. This interface
 * provides mechanisms to query and set the size of the extent.
 *
 * @param <T>
 *
 * @author Michael Ludwig
 */
// FIXME should I just make this part of the main spec?
// Must resolve what happens for unbounded spatial indexes?
// Become bounded? Size change ignored?
public interface BoundedSpatialIndex<T> extends SpatialIndex<T> {
    /**
     * @return The current extent of the index
     */
    @Const
    public AxisAlignedBox getExtent();

    /**
     * Set the new extent of the spatial index. This can only be called when the index is
     * empty.
     *
     * @param bounds The new bounding box for the extent of this index
     *
     * @throws IllegalStateException if the index is not empty
     * @throws NullPointerException  if bounds is null
     */
    public void setExtent(@Const AxisAlignedBox bounds);
}
