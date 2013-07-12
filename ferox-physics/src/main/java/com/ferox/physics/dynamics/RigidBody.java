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
import com.ferox.math.entreri.Matrix3Property;
import com.ferox.math.entreri.Vector3Property;
import com.ferox.physics.collision.CollisionBody;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Requires;
import com.lhkbob.entreri.Unmanaged;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;

@Requires(CollisionBody.class)
public interface RigidBody extends Component {
    @Const
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
    public Vector3 getVelocity();

    @Const
    public Vector3 getAngularVelocity();

    @Const
    public Vector3 getTotalForce();

    @Const
    public Vector3 getTotalTorque();

    public static class Utils {
        public RigidBody addForce(@Const Vector3 force, @Const Vector3 relPos) {
            totalForce.set(getTotalForce().add(force), getIndex());

            if (relPos != null) {
                totalTorque.set(getTotalTorque().add(temp.cross(relPos, force)), getIndex());
            }

            return this;
        }

        public RigidBody addImpulse(@Const Vector3 impulse, @Const Vector3 relPos) {
            velocity.set(getVelocity().addScaled(getInverseMass(), impulse), getIndex());

            if (relPos != null) {
                temp.cross(relPos, impulse);
                temp.mul(getInertiaTensorInverse(), temp);

                angularVelocity.set(getAngularVelocity().add(temp), getIndex());
            }

            return this;
        }

        public RigidBody clearForces() {
            forceCache.set(0.0, 0.0, 0.0);
            torqueCache.set(0.0, 0.0, 0.0);

            totalForce.set(forceCache, getIndex());
            totalTorque.set(torqueCache, getIndex());

            return this;
        }
    }
}
