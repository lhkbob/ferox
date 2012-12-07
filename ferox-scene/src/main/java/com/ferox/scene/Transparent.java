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

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.property.BooleanProperty;
import com.lhkbob.entreri.property.BooleanProperty.DefaultBoolean;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;

/**
 * <p>
 * The Transparent component can be added to an Entity to change the Entity's
 * transparency or opacity. When an Entity is not fully opaque (thus partially
 * transparent), some amount of light reflected or emitted from behind the
 * Entity can travel through the Entity. The details and fidelity of being able
 * to render this depends entirely on the controller implementation handling the
 * rendering of the scene.
 * </p>
 * <p>
 * The simplest way will likely be using linearly interpolated blending based on
 * the opacity of the Entity. It could get as complex as performing subsurface
 * scattering or applying Fresnel effects, although these might require
 * additional Components to properly configure.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Transparent extends ComponentData<Transparent> {
    @DefaultDouble(0.5)
    private DoubleProperty opacity;

    @DefaultBoolean(false)
    private BooleanProperty additive;

    private Transparent() {}

    /**
     * @return True if the transparent object emits light, such as fire might
     */
    public boolean isAdditive() {
        return additive.get(getIndex());
    }

    /**
     * Set whether or not the transparent object emits light, and thus needs to
     * use additive blending. An example would be the particles of fire, which
     * is additive, versus particles of smoke that do not emit light.
     * 
     * @param additive True if object emits light
     * @return This component
     */
    public Transparent setAdditive(boolean additive) {
        this.additive.set(additive, getIndex());
        updateVersion();
        return this;
    }

    /**
     * Return the opacity of the Entity. This is a value between 0 and 1,
     * representing the fraction of light that is blocked by the Entity. A value
     * of 1 means the Entity is fully opaque and a value of 0 means the Entity
     * is completely transparent.
     * 
     * @return The opacity
     */
    public double getOpacity() {
        return opacity.get(getIndex());
    }

    /**
     * Set the opacity of this Transparent component. A value of 1 means the
     * Entity is fully opaque and a value of 0 means the Entity is completely
     * transparent. Intermediate values blend the Entity with its background in
     * different contributions depending on the exact opacity.
     * 
     * @param opacity The opacity, must be in [0, 1]
     * @return This component for chaining purposes
     * @throws IllegalArgumentException if opacity is not between 0 and 1
     */
    public Transparent setOpacity(double opacity) {
        if (opacity < 0f || opacity > 1f) {
            throw new IllegalArgumentException("Opacity must be in [0, 1], not: " + opacity);
        }
        this.opacity.set(opacity, getIndex());
        updateVersion();
        return this;
    }
}
