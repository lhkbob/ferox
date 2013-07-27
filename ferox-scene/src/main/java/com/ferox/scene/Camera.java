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

import com.ferox.renderer.Surface;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Requires;
import com.lhkbob.entreri.Validate;
import com.lhkbob.entreri.Within;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;
import com.lhkbob.entreri.property.Named;

/**
 * <p/>
 * Camera is a Component that specifies the viewing settings for a "camera" into the scene of the
 * EntitySystem. It represents a perspective projection and stores the field of view, near and far z planes.
 * It is also attached to a {@link Surface} to actually render into. This surface determines the aspect ratio
 * that must be used when rendering with this camera. Additionally, the camera takes its position and
 * orientation from any transform-providing component attached to the same entity.
 *
 * @author Michael Ludwig
 */
@Requires(Transform.class)
public interface Camera extends Component {
    /**
     * @return The field of view for this Camera, in degrees
     */
    @DefaultDouble(60.0)
    public double getFieldOfView();

    /**
     * Set the field of view for this Camera, in degrees.
     *
     * @param fov The new field of view
     *
     * @return This Camera for chaining purposes
     *
     * @throws IllegalArgumentException if fov is less than 0 or greater than 180
     */
    public Camera setFieldOfView(@Within(min = 0, max = 180) double fov);

    /**
     * @return The distance to the near z plane of the camera
     */
    @DefaultDouble(0.01)
    public double getNearZDistance();

    /**
     * Set the distance to the near and far z planes.
     *
     * @param znear The new near distance
     * @param zfar  The new far distance
     *
     * @return This Camera for chaining purposes
     */
    @Validate(value = "$1 < $2", errorMsg = "znear must be less than zfar")
    public Camera setZDistances(@Named("nearZDistance") @Within(min = Double.MIN_VALUE) double znear,
                                @Named("farZDistance") @Within(min = Double.MIN_VALUE) double zfar);

    /**
     * @return The distance to the far z plane of the camera
     */
    @DefaultDouble(100.0)
    public double getFarZDistance();

    /**
     * Return the Surface that this Camera is linked to.
     *
     * @return The Surface of this Camera
     */
    public Surface getSurface();

    /**
     * Set the current Surface of this Camera.
     *
     * @param surface The new surface
     *
     * @return This camera for chaining purposes
     *
     * @throws NullPointerException if surface is null
     */
    public Camera setSurface(Surface surface);
}
