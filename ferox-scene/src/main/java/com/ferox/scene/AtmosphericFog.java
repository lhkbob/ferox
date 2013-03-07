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
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;
import com.lhkbob.entreri.property.IntProperty;

/**
 * <p/>
 * AtmosphericFog is a Component that can add a visual approximation to fog to a rendered
 * scene. This component models fog by using a density fall-off function and a distance
 * through which the fog will become opaque. This model is compatible with the classic
 * eye-space fog that is usable in a fixed-function OpenGL rendering engine.
 *
 * @author Michael Ludwig
 */
public final class AtmosphericFog extends ComponentData<AtmosphericFog> {
    /**
     * Falloff represents how the visibility of fog decreases as distance increases. The
     * opacity of the fog, at some distance, can be considered as a floating point number
     * within [0, 1]. When at a distance less than or equal to 0, the opacity is 0. When
     * at a distance greater than or equal to the {@link AtmosphericFog#getOpaqueDistance()
     * opacity distance of the fog}, the opacity is 1. The Falloff enum represents how the
     * opacity value changes between 0 and 1.
     */
    public static enum Falloff {
        /**
         * The opacity increases linearly from 0 to 1 as the distance increases from 0 to
         * the opaque distance.
         */
        LINEAR,
        /**
         * The opacity increases exponentially from 0 to 1 as the distance increases from
         * 0 to the opaque distance. The exact nature of this equation is determined by
         * the rendering engine, but the fog should resemble a long tail of low opacity
         * followed with a rapid increase to fully opaque.
         */
        EXPONENTIAL,
        /**
         * The opacity increases exponentially from 0 to 1 as the square of the distance
         * increases from 0 to the squared opaque distance.
         */
        EXPONENTIAL_SQUARED
    }

    private static final Falloff[] VALUES = Falloff.values();

    @DefaultColor(red = 0.5, green = 0.5, blue = 0.5)
    private ColorRGBProperty color;

    @DefaultDouble(10)
    private DoubleProperty distanceToOpaque;

    private IntProperty falloff;

    @Unmanaged
    private final ColorRGB colorCache = new ColorRGB();

    private AtmosphericFog() {
    }

    /**
     * Set the Falloff to use for this fog. See {@link #getFalloff()} and {@link Falloff}
     * for descriptions of what the Falloff accomplishes.
     *
     * @param falloff The new falloff
     *
     * @return This AtmosphericFog for chaining purposes
     *
     * @throws NullPointerException if falloff is null
     */
    public AtmosphericFog setFalloff(Falloff falloff) {
        if (falloff == null) {
            throw new NullPointerException("Falloff cannot be null");
        }
        this.falloff.set(falloff.ordinal(), getIndex());
        updateVersion();
        return this;
    }

    /**
     * Return the Falloff equation to use that determines how an object's color and the
     * fog color are combined when the object is in front of the viewer but closer than
     * the opaque distance. This also represents how the density of the fog changes as
     * distance increases.
     *
     * @return The Falloff of this fog
     */
    public Falloff getFalloff() {
        return VALUES[falloff.get(getIndex())];
    }

    /**
     * Set the maximum distance that light can travel through the fog before being
     * completely obscured. Any object that's closer to the edge of the fog, or the viewer
     * if within the fog, will be combined with the {@link #getColor() fog color} based on
     * its distance and the {@link #getFalloff() falloff}.
     *
     * @param dist The new opaque distance
     *
     * @return This AtmosphericFog for chaining purposes
     *
     * @throws IllegalArgumentException if dist <= 0
     */
    public AtmosphericFog setOpaqueDistance(double dist) {
        if (dist <= 0) {
            throw new IllegalArgumentException("Distance must be positive, not: " + dist);
        }
        distanceToOpaque.set(dist, getIndex());
        updateVersion();
        return this;
    }

    /**
     * Return the distance from the viewer required for an object to be completely
     * obscured by the fog, or equivalently when the fog is fully opaque and lets no light
     * through to the viewer.
     *
     * @return The opaque distance, this will be above 0
     */
    public double getOpaqueDistance() {
        return distanceToOpaque.get(getIndex());
    }

    /**
     * Copy <var>color</var> into this AtmosphericFog's color instance. The color
     * represents the color of the fog when fully opaque.
     *
     * @param color The new fog color
     *
     * @return The new version, via {@link #notifyChange()}
     *
     * @throws NullPointerException if color is null
     */
    public AtmosphericFog setColor(@Const ColorRGB color) {
        if (color == null) {
            throw new NullPointerException("Color cannot be null");
        }
        this.color.set(color, getIndex());
        updateVersion();
        return this;
    }

    /**
     * Return the color of this fog when the fog is fully opaque. If the fog is not
     * opaque, the fog color is blended with whatever happens to be within the fog, based
     * on its depth in the fog.
     *
     * @return The fog color
     */
    @Const
    @SharedInstance
    public ColorRGB getColor() {
        color.get(getIndex(), colorCache);
        return colorCache;
    }
}
