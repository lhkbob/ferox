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
package com.ferox.scene.task.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.bounds.Frustum;
import com.ferox.math.bounds.Frustum.FrustumIntersection;
import com.ferox.scene.SpotLight;

public class SpotLightInfluence implements LightInfluence {
    private final Frustum lightFrustum;

    public SpotLightInfluence(@Const Matrix4 lightTransform, double falloff, double cutoffAngle) {
        // construct a Frustum that approximates the cone of the light
        double zfar = (falloff < 0 ? Double.MAX_VALUE : falloff);
        lightFrustum = new Frustum(60.0, 1.0, 0.1, 1.0);
        lightFrustum.setPerspective(cutoffAngle, 1.0, 0.00001, zfar);
        lightFrustum.setOrientation(lightTransform);
    }

    @Override
    public boolean influences(@Const AxisAlignedBox bounds) {
        return lightFrustum.intersects(bounds, null) != FrustumIntersection.OUTSIDE;
    }

    public static LightInfluence.Factory<SpotLight> factory() {
        return new LightInfluence.Factory<SpotLight>() {
            @Override
            public LightInfluence create(SpotLight light, Matrix4 lightTransform) {
                return new SpotLightInfluence(lightTransform, light.getFalloffDistance(),
                                              light.getCutoffAngle());
            }
        };
    }
}
