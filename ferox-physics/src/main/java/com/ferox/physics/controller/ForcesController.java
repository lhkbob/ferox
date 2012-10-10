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
import com.ferox.physics.dynamics.Gravity;
import com.ferox.physics.dynamics.Integrator;
import com.ferox.physics.dynamics.RigidBody;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.SimpleController;

public class ForcesController extends SimpleController {
    private Integrator integrator;
    private final Vector3 defaultGravity;

    public ForcesController() {
        defaultGravity = new Vector3(0, -10, 0);
        setIntegrator(new ExplicitEulerIntegrator());
    }

    public void setGravity(@Const Vector3 gravity) {
        defaultGravity.set(gravity);
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
        Vector3 inertia = new Vector3();
        Vector3 force = new Vector3();
        Matrix3 rotation = new Matrix3();

        RigidBody rb = getEntitySystem().createDataInstance(RigidBody.ID);
        CollisionBody cb = getEntitySystem().createDataInstance(CollisionBody.ID);
        Gravity g = getEntitySystem().createDataInstance(Gravity.ID);

        ComponentIterator it = new ComponentIterator(getEntitySystem());
        it.addRequired(rb);
        it.addRequired(cb);
        it.addOptional(g);

        while (it.next()) {
            Matrix4 transform = cb.getTransform();

            // compute the body's new inertia tensor
            // FIXME this might not be the best place to do this computation
            Matrix3 tensor = rb.getInertiaTensorInverse();

            cb.getShape().getInertiaTensor(rb.getMass(), inertia);
            inertia.set(1.0 / inertia.x, 1.0 / inertia.y, 1.0 / inertia.z);

            rotation.setUpper(transform);
            tensor.mulDiagonal(rotation, inertia).mulTransposeRight(rotation);
            rb.setInertiaTensorInverse(tensor);

            // add gravity force
            if (g.isEnabled()) {
                force.scale(g.getGravity(), rb.getMass());
            } else {
                force.scale(defaultGravity, rb.getMass());
            }

            rb.addForce(force, null);

            // integrate and apply forces to the body's velocity
            Vector3 lv = rb.getVelocity();
            integrator.integrateLinearAcceleration(force.scale(rb.getTotalForce(),
                                                               rb.getInverseMass()), dt,
                                                   lv);
            rb.setVelocity(lv);

            Vector3 la = rb.getAngularVelocity();
            integrator.integrateAngularAcceleration(force.mul(tensor, rb.getTotalTorque()),
                                                    dt, la);
            rb.setAngularVelocity(la);

            rb.clearForces();
        }
    }
}
