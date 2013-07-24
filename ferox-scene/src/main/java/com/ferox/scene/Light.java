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
import com.ferox.math.entreri.ColorRGBProperty.DefaultColor;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Requires;
import com.lhkbob.entreri.property.BooleanProperty.DefaultBoolean;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;
import com.lhkbob.entreri.property.SharedInstance;

/**
 * The Light component represents a simple light within a scene.  Depending on its configuration, it can
 * represent a spot light, point light, or direction light. This is controlled by the cutoff angle parameter
 * ({@link #getCutoffAngle()}.
 * <p/>
 * The influence of a light is implicitly controlled by its falloff distance, or by adding an {@link
 * InfluenceRegion} component to the entity.
 * <p/>
 * When the Light represents a point or spot light, its position is determined by the position stored in an
 * associated {@link Transform} component or the origin if there is no transform. For spot and direction
 * lights, the direction of light is along the z-axis of its associated Transform.
 *
 * @author Michael Ludwig
 */
@Requires(Transform.class)
public interface Light extends Component {
    /**
     * @return The color of this Light
     */
    @Const
    @SharedInstance
    @DefaultColor(red = 0.2, green = 0.2, blue = 0.2)
    public ColorRGB getColor();

    /**
     * Set the color of this Light.
     *
     * @param color The new color
     *
     * @return This light for chaining purposes
     */
    public Light setColor(@Const ColorRGB color);

    /**
     * Get the cutoff angle of influence for this light.  The cutoff angle is the angle in degrees swept out
     * from the direction of the light. Values between 0 and 90 degrees create a spotlight with a narrow to
     * wide cone, respectively. The special value of 180 degrees creates a pointlight that shines in every
     * direction from the light's position.
     * <p/>
     * The special value {@link Double#NaN} designates the light as an infinite direction light. Its position
     * is ignored and shines in the direction of its transform.
     *
     * @return The cutoff angle
     */
    @DefaultDouble(180.0)
    public double getCutoffAngle();

    /**
     * Set the cutoff angle for this light. See {@link #getCutoffAngle()} for how this value is interpreted.
     *
     * @param angle The new angle
     *
     * @return The light for chaining purposes
     */
    public Light setCutoffAngle(double angle);

    /**
     * Return the distance to where the light's energy has fallen off to zero and no longer contributes to
     * lighting. If this is negative, then the light has no energy falloff. When enabled, a light's energy
     * falls off according to an inverse square law based on the distance to an object. This effect is ignored
     * for infinite direction lights because there's no inherent distance to the object.
     * <p/>
     * Regardless of light types or how the renderer visualizes the falloff effect, when the distance is
     * positive and objects are outside of that distance they must not be influenced by the light.
     *
     * @return The falloff distance
     */
    @DefaultDouble(-1.0)
    public double getFalloffDistance();

    /**
     * Set the distance to where the light's energy has fallen to zero and no longer contributes to the
     * lighting of a scene. If this is negative, the light has no energy falloff and all lit objects will be
     * lit with the same energy.
     *
     * @param distance The new falloff distance
     *
     * @return This light for chaining purposes
     *
     * @see #getFalloffDistance()
     */
    public Light setFalloffDistance(double distance);

    /**
     * @return True if this light should cast shadows, defaults to false.
     */
    @DefaultBoolean(false)
    public boolean isShadowCaster();

    /**
     * Set whether or not this light should cast shadows.
     *
     * @param castsShadow True if this light is a shadow caster
     *
     * @return This component
     */
    public Light setShadowCaster(boolean castsShadow);
}
