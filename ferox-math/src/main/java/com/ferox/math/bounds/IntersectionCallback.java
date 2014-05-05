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
 * <p/>
 * Callback used by {@link SpatialIndex} to provide all intersecting pairs of items within an index. Use
 * {@link SpatialIndex#query(IntersectionCallback)} to run an intersection query with a callback to process
 * the pairs as desired.
 * <p/>
 * This can be used to simply implement a broadphase collision filter as used in many physics engines.
 *
 * @param <T>
 *
 * @author Michael Ludwig
 */
public interface IntersectionCallback<T> {
    /**
     * <p/>
     * Invoked once for each unique pair of intersecting items in the queried SpatialIndex. There is no
     * significance associated with the label 'a' or 'b', it's merely the order stored within the index.
     * <p/>
     * The AxisAlignedBox instances provided may be reused by future invocations of process(), so they should
     * be cloned if their state is needed outside of the scope of the method call.
     *
     * @param a       The first item in the pair
     * @param boundsA The bounds of the first item
     * @param b       The second item in the pair
     * @param boundsB The bounds of the second item
     */
    public void process(T a, @Const AxisAlignedBox boundsA, T b, @Const AxisAlignedBox boundsB);
}
