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

import com.lhkbob.entreri.property.BooleanProperty;
import com.lhkbob.entreri.property.BooleanProperty.DefaultBoolean;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;

/**
 * <p>
 * SpotLight is a light that shines light in a cone along a specific direction,
 * with the origin of the light (or apex of the cone) located at a specific
 * position. The size of the cone can be configured with a cutoff angle that
 * describes how wide or narrow the cone is.
 * </p>
 * <p>
 * A SpotLight should be combined with a {@link Transform} component to specify
 * its position and direction. The direction is stored in the 3rd column of the
 * 4x4 affine matrix. If there is no transform component, the direction vector
 * defaults to the positive z axis.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class SpotLight extends AbstractPlacedLight<SpotLight> {
    @DefaultDouble(30.0)
    private DoubleProperty cutoffAngle;

    @DefaultBoolean(false)
    private BooleanProperty shadowCaster;

    private SpotLight() {}

    /**
     * @return True if this spotlight should cast shadows, defaults to false
     */
    public boolean isShadowCaster() {
        return shadowCaster.get(getIndex());
    }

    /**
     * Set whether or not this spotlight should cast shadows.
     * 
     * @param castsShadow True if this light is a shadow caster
     * @return This component
     */
    public SpotLight setShadowCaster(boolean castsShadow) {
        shadowCaster.set(castsShadow, getIndex());
        updateVersion();
        return this;
    }

    /**
     * Return the cutoff angle, in degrees, representing the maximum angle light
     * will spread from the {@link #getDirection() direction vector}. This
     * creates a cone of light that is fat or thin depending on if the angle is
     * large or small.
     * 
     * @return The cutoff angle in degrees, will be in [0, 90]
     */
    public double getCutoffAngle() {
        return cutoffAngle.get(getIndex());
    }

    /**
     * Set the cutoff angle for this SpotLight. The cutoff angle is the maximum
     * angle, in degrees, from the {@link #getDirection() direction vector} that
     * will be affected by the light. Thus an angle value of 90 would create a
     * half-space that is lit.
     * 
     * @param angle The new cutoff angle, in [0, 90]
     * @return This light for chaining purposes
     * @throws IllegalArgumentException if angle is not between 0 and 90
     */
    public SpotLight setCutoffAngle(double angle) {
        if (angle < 0 || angle > 90) {
            throw new IllegalArgumentException("Illegal cutoff angle, must be in [0, 90], not: " + angle);
        }
        cutoffAngle.set(angle, getIndex());
        updateVersion();
        return this;
    }
}
