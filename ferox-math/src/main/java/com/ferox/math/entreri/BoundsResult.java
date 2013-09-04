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
package com.ferox.math.entreri;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.task.Result;

/**
 * BoundsResult is a result that can be used to report the union'ed bounds of a specific type of component. A
 * component's final bounds might be determined by a combination of components, so the primary component
 * involved in the behavior should be used with this result. A common use case of this result is to easily
 * resize a {@link com.ferox.math.bounds.SpatialIndex}.
 *
 * @author Michael Ludwig
 */
public class BoundsResult extends Result {
    private final AxisAlignedBox bounds;
    private final Class<? extends Component> boundsOverType;

    /**
     * Create a new result for the bounded type {@code boundsOverType} and the bounds. The bounds are copied
     * so future modification to the passed instance does not reflect the result.
     *
     * @param boundsOverType The bounded type
     * @param bounds         The union'ed bounds of components in the system of the specified type
     *
     * @throws NullPointerException if arguments are null
     */
    public BoundsResult(Class<? extends Component> boundsOverType, @Const AxisAlignedBox bounds) {
        if (boundsOverType == null) {
            throw new NullPointerException("Component type cannot be null");
        }
        this.bounds = new AxisAlignedBox(bounds);
        this.boundsOverType = boundsOverType;
    }

    /**
     * @return Get the reported bounds
     */
    @Const
    public AxisAlignedBox getBounds() {
        return bounds;
    }

    /**
     * @return Get the component type bounded by {@link #getBounds()}
     */
    public Class<? extends Component> getBoundedType() {
        return boundsOverType;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
