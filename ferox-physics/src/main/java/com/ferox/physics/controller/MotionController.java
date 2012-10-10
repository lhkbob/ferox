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
package com.ferox.physics.controller;

import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.ExplicitEulerIntegrator;
import com.ferox.physics.dynamics.Integrator;
import com.ferox.physics.dynamics.RigidBody;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.SimpleController;

public class MotionController extends SimpleController {
    private Integrator integrator;

    public MotionController() {
        setIntegrator(new ExplicitEulerIntegrator());
    }

    public void setIntegrator(Integrator integrator) {
        if (integrator == null) {
            throw new NullPointerException("Integrator can't be null");
        }
        this.integrator = integrator;
    }

    public Integrator getIntegrator() {
        return integrator;
    }

    @Override
    public void process(double dt) {
        Vector3 predictedPosition = new Vector3();
        Matrix3 predictedRotation = new Matrix3();

        RigidBody rb = getEntitySystem().createDataInstance(RigidBody.ID);
        CollisionBody cb = getEntitySystem().createDataInstance(CollisionBody.ID);

        ComponentIterator it = new ComponentIterator(getEntitySystem());
        it.addRequired(rb);
        it.addRequired(cb);

        while (it.next()) {
            Matrix4 transform = cb.getTransform();

            predictedRotation.setUpper(transform);
            predictedPosition.set(transform.m03, transform.m13, transform.m23);

            integrator.integrateLinearVelocity(rb.getVelocity(), dt, predictedPosition);
            integrator.integrateAngularVelocity(rb.getAngularVelocity(), dt,
                                                predictedRotation);

            // push values back into transform
            setTransform(predictedRotation, predictedPosition, transform);

            cb.setTransform(transform);
        }
    }

    private void setTransform(@Const Matrix3 r, @Const Vector3 p, Matrix4 t) {
        t.setUpper(r);

        t.m03 = p.x;
        t.m13 = p.y;
        t.m23 = p.z;

        // ensure this is still an affine transform
        t.m30 = 0.0;
        t.m31 = 0.0;
        t.m32 = 0.0;
        t.m33 = 1.0;
    }
}
