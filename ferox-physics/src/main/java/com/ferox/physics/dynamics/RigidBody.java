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
package com.ferox.physics.dynamics;

import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.CollisionBody;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Requires;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;
import com.lhkbob.entreri.property.SharedInstance;

@Requires(CollisionBody.class)
public interface RigidBody extends Component {
    @Const
    @SharedInstance
    public Matrix3 getInertiaTensorInverse();

    public RigidBody setInertiaTensorInverse(@Const Matrix3 tensorInverse);

    public RigidBody setForce(@Const Vector3 f);

    public RigidBody setTorque(@Const Vector3 t);

    public RigidBody setVelocity(@Const Vector3 vel);

    public RigidBody setAngularVelocity(@Const Vector3 angVel);

    public RigidBody setMass(double mass);

    @DefaultDouble(1.0)
    public double getMass();

    @Const
    @SharedInstance
    public Vector3 getVelocity();

    @Const
    @SharedInstance
    public Vector3 getAngularVelocity();

    @Const
    @SharedInstance
    public Vector3 getForce();

    @Const
    @SharedInstance
    public Vector3 getTorque();

    public static final class Utils {
        private Utils() {
        }

        public static void addForce(RigidBody body, @Const Vector3 force, @Const Vector3 relPos) {
            Vector3 totalForce = body.getForce();
            totalForce.add(force);
            body.setForce(totalForce);

            if (relPos != null) {
                Vector3 totalTorque = body.getTorque();
                // use the body's total force vector as a temporary store, this is safe because we
                // never use it again and getForce() always refills it
                totalTorque.add(totalForce.cross(relPos, force));
                body.setTorque(totalTorque);
            }
        }

        public static void addImpulse(RigidBody body, @Const Vector3 impulse, @Const Vector3 relPos) {
            Vector3 velocity = body.getVelocity();
            velocity.addScaled(1.0 / body.getMass(), impulse);
            body.setVelocity(velocity);

            if (relPos != null) {
                // use the same reuse trick as in addForce()
                velocity.cross(relPos, impulse);
                velocity.mul(body.getInertiaTensorInverse(), velocity);

                Vector3 angular = body.getAngularVelocity();
                angular.add(velocity);
                body.setAngularVelocity(angular);
            }
        }

        public static void clearForces(RigidBody body) {
            Vector3 force = body.getForce();
            force.set(0, 0, 0);
            body.setForce(force);

            Vector3 torque = body.getTorque();
            torque.set(0, 0, 0);
            body.setTorque(torque);
        }
    }
}
