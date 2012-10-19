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
package com.ferox.scene.controller.light;

import com.ferox.math.Matrix4;
import com.ferox.math.bounds.Frustum;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.SpotLight;
import com.ferox.scene.Transform;
import com.ferox.scene.controller.FrustumResult;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.SimpleController;

public class ShadowFrustumController extends SimpleController {
    private static final Matrix4 DEFAULT_MAT = new Matrix4().setIdentity();

    @Override
    public void process(double dt) {
        // Process DirectionLights
        DirectionLight dl = getEntitySystem().createDataInstance(DirectionLight.ID);
        Transform t = getEntitySystem().createDataInstance(Transform.ID);
        ComponentIterator it = new ComponentIterator(getEntitySystem()).addRequired(dl)
                                                                       .addOptional(t);

        while (it.next()) {
            if (dl.isShadowCaster()) {
                Frustum smFrustum = computeFrustum(dl, t);
                getEntitySystem().getControllerManager()
                                 .report(new FrustumResult(dl.getComponent(), smFrustum));
            }
        }

        // Process SpotLights
        SpotLight sl = getEntitySystem().createDataInstance(SpotLight.ID);
        it = new ComponentIterator(getEntitySystem()).addRequired(sl).addOptional(t);

        while (it.next()) {
            if (sl.isShadowCaster()) {
                Frustum smFrustum = computeFrustum(sl, t);
                getEntitySystem().getControllerManager()
                                 .report(new FrustumResult(sl.getComponent(), smFrustum));
            }
        }
    }

    private Frustum computeFrustum(DirectionLight light, Transform t) {
        Frustum f = new Frustum(true, -15, 15, -15, 15, 0, 50);
        if (t.isEnabled()) {
            f.setOrientation(t.getMatrix());
        } else {
            f.setOrientation(DEFAULT_MAT);
        }
        return f;
    }

    private Frustum computeFrustum(SpotLight light, Transform t) {
        // clamp near and far planes to the falloff distance if possible, 
        // otherwise select a depth range that likely will not cause any problems
        double near = (light.getFalloffDistance() > 0 ? Math.min(.1 * light.getFalloffDistance(),
                                                                 .1) : .1);
        double far = (light.getFalloffDistance() > 0 ? light.getFalloffDistance() : 1000);
        Frustum f = new Frustum(light.getCutoffAngle() * 2, 1.0, near, far);
        if (t.isEnabled()) {
            f.setOrientation(t.getMatrix());
        } else {
            f.setOrientation(DEFAULT_MAT);
        }
        return f;
    }
}
