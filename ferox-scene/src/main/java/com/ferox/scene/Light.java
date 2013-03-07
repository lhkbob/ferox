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
import com.ferox.math.entreri.ColorRGBProperty.DefaultColor;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.SharedInstance;
import com.lhkbob.entreri.Unmanaged;

/**
 * <p/>
 * Light is an abstract class that Components representing lights within a 3D scene should
 * extend from. Light does not enforce any particular rules on how a light is described,
 * except that it has a {@link #getColor() color}. The Light type exists so that the
 * myriad types of lights do not need to repeat this definition.
 * <p/>
 * <p/>
 * Additionally, the colors held by Light components should use the HDR values stored in
 * the returned {@link ReadOnlyColor3f}'s. This is because the color of a light can be
 * ultra-bright, going past the conventional limit of 1 for a color component. Support for
 * the HDR values when rendering is dependent on the rendering framework, however.
 *
 * @param <T> The concrete type of light
 *
 * @author Michael Ludwig
 */
public abstract class Light<T extends Light<T>> extends ComponentData<T> {
    @DefaultColor(red = 0.2, green = 0.2, blue = 0.2)
    private ColorRGBProperty color;

    @Unmanaged
    private final ColorRGB cache = new ColorRGB();

    protected Light() {
    }

    /**
     * Return the color of this Light. The returned ColorRGB instance is reused by this
     * Light instance so it should be cloned before changing which Component is
     * referenced.
     *
     * @return The color of this Light
     */
    @Const
    @SharedInstance
    public final ColorRGB getColor() {
        color.get(getIndex(), cache);
        return cache;
    }

    /**
     * Set the color of this Light. The color values in <var>color</var> are copied into
     * an internal instance, so any future changes to <var>color</var> will not affect
     * this Component.
     *
     * @param color The new color
     *
     * @return This light for chaining purposes
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
