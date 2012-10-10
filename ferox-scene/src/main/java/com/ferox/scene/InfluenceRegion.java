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
package com.ferox.scene;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.entreri.AxisAlignedBoxProperty;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.Unmanaged;
import com.lhkbob.entreri.property.BooleanProperty;

public final class InfluenceRegion extends ComponentData<InfluenceRegion> {
    /**
     * TypeId for InfluenceRegion component type.
     */
    public static final TypeId<InfluenceRegion> ID = TypeId.get(InfluenceRegion.class);

    private AxisAlignedBoxProperty bounds;
    private BooleanProperty negate;

    @Unmanaged
    private final AxisAlignedBox boundsCache = new AxisAlignedBox();

    private InfluenceRegion() {}

    public @Const
    AxisAlignedBox getBounds() {
        return boundsCache;
    }

    public InfluenceRegion setBounds(@Const AxisAlignedBox bounds) {
        if (bounds == null) {
            throw new NullPointerException("Bounds cannot be null");
        }
        boundsCache.set(bounds);
        this.bounds.set(bounds, getIndex());
        return this;
    }

    public boolean isNegated() {
        return negate.get(getIndex());
    }

    public InfluenceRegion setNegated(boolean negate) {
        this.negate.set(negate, getIndex());
        return this;
    }

    @Override
    protected void onSet(int index) {
        bounds.get(index, boundsCache);
    }
}
