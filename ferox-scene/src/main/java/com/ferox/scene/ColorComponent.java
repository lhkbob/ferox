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

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.entreri.ColorRGBProperty;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.SharedInstance;
import com.lhkbob.entreri.Unmanaged;

/**
 * <p/>
 * ColorComponent is an abstract Component type for components that provide a color used
 * in describing a material to render an Entity with. This makes it semantically different
 * from the abstract {@link Light}, which also only stores a color. In addition to using
 * ColorComponent subclasses to describe an Entity's material, there will often be similar
 * Components extending from {@link TextureMap} that function the same, but can act on a
 * per-pixel basis.
 *
 * @param <T> The concrete type of ColorComponent
 *
 * @author Michael Ludwig
 */
public abstract class ColorComponent<T extends ColorComponent<T>>
        extends ComponentData<T> {
    private ColorRGBProperty color;

    @Unmanaged
    private final ColorRGB cache = new ColorRGB();

    protected ColorComponent() {
    }

    /**
     * Return the color stored by this component. This color will be used for different
     * purposes depending on the concrete type of ColorComponent. The returned color is a
     * cached instance shared within the component's EntitySystem, so it should be cloned
     * before accessing another component of this type.
     *
     * @return This component's color
     */
    @Const
    @SharedInstance
    public final ColorRGB getColor() {
        color.get(getIndex(), cache);
        return cache;
    }

    /**
     * Set the color of this component by copying <tt>color</tt> into this components
     * color object.
     *
     * @param color The new color values
     *
     * @return This component for chaining purposes
     *
     * @throws NullPointerException if color is null
     */
    @SuppressWarnings("unchecked")
    public final T setColor(@Const ColorRGB color) {
        if (color == null) {
            throw new NullPointerException("Color cannot be null");
        }
        this.color.set(color, getIndex());
        updateVersion();
        return (T) this;
    }
}
