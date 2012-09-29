package com.ferox.math.bounds;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;

/**
 * <p>
 * Callback used by {@link SpatialIndex} to provide all intersecting pairs of
 * items within an index. Use {@link SpatialIndex#query(IntersectionCallback)}
 * to run an intersection query with a callback to process the pairs as desired.
 * <p>
 * This can be used to simply implement a broadphase collision filter as used in
 * many physics engines.
 * 
 * @author Michael Ludwig
 * @param <T>
 */
public interface IntersectionCallback<T> {
    /**
     * <p>
     * Invoked once for each unique pair of intersecting items in the queried
     * SpatialIndex. There is no import associated with an item being labeled
     * 'a' or 'b', it's merely the order stored within the index.
     * <p>
     * The AxisAlignedBox instances provided may be reused for future
     * invocations of
     * {@link #process(Object, AxisAlignedBox, Object, AxisAlignedBox)}, so they
     * should be cloned if their state is needed outside of the scope of the
     * method call.
     * 
     * @param a The first item in the pair
     * @param boundsA The bounds of the first item
     * @param b The second item in the pair
     * @param boundsB The bounds of the second item
     */
    public void process(T a, @Const AxisAlignedBox boundsA,
                        T b, @Const AxisAlignedBox boundsB);
}
