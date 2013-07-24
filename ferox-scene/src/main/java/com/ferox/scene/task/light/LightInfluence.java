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
import com.ferox.math.Vector3;
import com.ferox.math.bounds.Frustum;

public class LightInfluence {
    private final Frustum lightFrustum;
    private final double falloffSquared;
    private final Vector3 lightPosition;

    private final Vector3 objectPos; // cached local instance

    public LightInfluence(double falloff, double cutoffAngle, @Const Matrix4 lightTransform) {
        if (falloff >= 0) {
            falloffSquared = falloff * falloff;
        } else {
            falloffSquared = -1;
        }
        lightPosition = new Vector3(lightTransform.m03, lightTransform.m13, lightTransform.m23);

        if (cutoffAngle >= 0 && cutoffAngle <= 90) {
            // spotlight
            double zfar = (falloff < 0 ? Double.MAX_VALUE : falloff);
            lightFrustum = new Frustum(60.0, 1.0, 0.1, 1.0);
            lightFrustum.setPerspective(cutoffAngle, 1.0, 0.00001, zfar);
            lightFrustum.setOrientation(lightTransform);
        } else {
            lightFrustum = null;
        }

        objectPos = new Vector3();
    }

    public boolean influences(@Const AxisAlignedBox bounds) {
        if (lightFrustum != null) {
            // spot light
            if (lightFrustum.intersects(bounds, null) == Frustum.FrustumIntersection.OUTSIDE) {
                return false;
            }
        }

        if (falloffSquared >= 0) {
            objectPos.add(bounds.min, bounds.max).scale(0.5); // center of aabb
            // updated to direction vector from light to center
            objectPos.sub(lightPosition);
            // compute near extent in place
            objectPos.nearExtent(bounds, objectPos);

            // make sure the bounds intersects with the bounding sphere centered
            // on the light with a radius equal to the falloff distance

            // distance between lightPos and objectPos is distance of light
            // to closest point on the entity bounds
            return objectPos.distanceSquared(lightPosition) <= falloffSquared;
        } else {
            // if no falloff and we've passed the optional frustum check then we always influence the entity
            return true;
        }
    }
}
