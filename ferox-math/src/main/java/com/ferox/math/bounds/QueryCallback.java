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

/**
 * QueryCallback is a callback that can be passed into a SpatialIndex when querying the
 * hierarchy with a frustum or box query.
 *
 * @param <T> The item type processed by the callback, and stored in the hierarchy
 *
 * @author Michael Ludwig
 */
public interface QueryCallback<T> {
    /**
     * <p/>
     * Invoked by a SpatialIndex when its {@link SpatialIndex#query(AxisAlignedBox,
     * QueryCallback)} or {@link SpatialIndex#query(Frustum, QueryCallback)} method is
     * called, for each item satisfying the query.
     * <p/>
     * Item query satisfaction is determined by the bounds the items were last updated
     * with, or their original bounds used when adding to the hierarchy, if they were
     * never updated. The bounds of the item are provided to the callback, although the
     * instance should not be held onto as the SpatialIndex may re-use the instance for
     * the next item.
     * <p/>
     * Similarly, the bounds should not be modified.
     *
     * @param item   The item passing the query
     * @param bounds The bounds the given item had in the index
     */
    public void process(T item, @Const AxisAlignedBox bounds);
}
