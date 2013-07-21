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
 * BoundedSpatialIndex is a SpatialIndex that has a maximum extent that all added objects must fit within. It
 * will ignore objects that extend past its extent. This interface provides mechanisms to query and set the
 * size of the extent.
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
     * Set the new extent of the spatial index. This can only be called when the index is empty.
     *
     * @param bounds The new bounding box for the extent of this index
     *
     * @throws IllegalStateException if the index is not empty
     * @throws NullPointerException  if bounds is null
     */
    public void setExtent(@Const AxisAlignedBox bounds);
}
