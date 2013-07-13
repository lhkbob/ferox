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
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Requires;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;
import com.lhkbob.entreri.property.ObjectProperty;

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
public final class Camera extends ComponentData<Camera> {
    private ObjectProperty<Surface> surface;

    @DefaultDouble(60.0)
    private DoubleProperty fov;

    @DefaultDouble(0.01)
    private DoubleProperty znear;

    @DefaultDouble(100.0)
    private DoubleProperty zfar;

    private Camera() {
    }

    /**
     * @return The field of view for this Camera, in degrees
     */
    public double getFieldOfView() {
        return fov.get(getIndex());
    }

    /**
     * Set the field of view for this Camera, in degrees.
     *
     * @param fov The new field of view
     *
     * @return This Camera for chaining purposes
     *
     * @throws IllegalArgumentException if fov is less than 0 or greater than 180
     */
    public Camera setFieldOfView(double fov) {
        if (fov < 0.0 || fov > 180.0) {
            throw new IllegalArgumentException("Field of view must be in [0, 180]: " + fov);
        }
        this.fov.set(fov, getIndex());
        updateVersion();
        return this;
    }

    /**
     * @return The distance to the near z plane of the camera
     */
    public double getNearZDistance() {
        return znear.get(getIndex());
    }

    /**
     * Set the distance to the near and far z planes.
     *
     * @param znear The new near distance
     * @param zfar  The new far distance
     *
     * @return This Camera for chaining purposes
     *
     * @throws IllegalArgumentException if znear is less than or equal to 0, or if zfar is less than znear
     */
    public Camera setZDistances(double znear, double zfar) {
        if (znear <= 0.0) {
            throw new IllegalArgumentException("Near distances must be greater than 0: " + znear);
        }
        if (znear > zfar) {
            throw new IllegalArgumentException("Near distance must be less than far: " + znear + ", " + zfar);
        }
        this.znear.set(znear, getIndex());
        this.zfar.set(zfar, getIndex());
        updateVersion();
        return this;
    }

    /**
     * @return The distance to the far z plane of the camera
     */
    public double getFarZDistance() {
        return zfar.get(getIndex());
    }

    /**
     * Return the Surface that this Camera is linked to.
     *
     * @return The Surface of this Camera
     */
    public Surface getSurface() {
        return surface.get(getIndex());
    }

    /**
     * Set the current Surface of this Camera.
     *
     * @param surface The new surface
     *
     * @return This camera for chaining purposes
     *
     * @throws NullPointerException if surface is null
     */
    public Camera setSurface(Surface surface) {
        this.surface.set(surface, getIndex());
        updateVersion();
        return this;
    }
}
