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
package com.ferox.scene.controller;

import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.Frustum;
import com.ferox.scene.Camera;
import com.ferox.scene.Transform;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.SimpleController;

/**
 * CameraController is a controller that synchronizes a {@link Camera}'s Frustum
 * location and orientation with an attached {@link Transform}. When run, all
 * entities with a Camera and Transform will have the Camera's Frustum's
 * orientation equal that stored in the transform.
 * 
 * @author Michael Ludwig
 */
public class CameraController extends SimpleController {
    @Override
    public void process(double dt) {
        Camera camera = getEntitySystem().createDataInstance(Camera.ID);
        Transform transform = getEntitySystem().createDataInstance(Transform.ID);

        ComponentIterator it = new ComponentIterator(getEntitySystem()).addRequired(camera)
                                                                       .addOptional(transform);

        while (it.next()) {
            double aspect = camera.getSurface().getWidth() / (double) camera.getSurface()
                                                                            .getHeight();
            Frustum f = new Frustum(camera.getFieldOfView(),
                                    aspect,
                                    camera.getNearZDistance(),
                                    camera.getFarZDistance());

            if (transform.isEnabled()) {
                Matrix4 m = transform.getMatrix();
                f.setOrientation(new Vector3(m.m03, m.m13, m.m23), new Vector3(m.m02,
                                                                               m.m12,
                                                                               m.m22),
                                 new Vector3(m.m01, m.m11, m.m21));
            }

            getEntitySystem().getControllerManager()
                             .report(new FrustumResult(camera.getComponent(), f));
        }
    }
}
