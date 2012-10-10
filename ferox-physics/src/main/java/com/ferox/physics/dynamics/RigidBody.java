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
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.Unmanaged;
import com.lhkbob.entreri.property.DoubleProperty;

public class RigidBody extends ComponentData<RigidBody> {
    public static final TypeId<RigidBody> ID = TypeId.get(RigidBody.class);

    private DoubleProperty inverseMass;

    private Matrix3Property inertiaTensorWorldInverse;

    private Vector3Property velocity;
    private Vector3Property angularVelocity;

    private Vector3Property totalForce;
    private Vector3Property totalTorque;

    // This got pretty ugly pretty fast
    @Unmanaged
    private final Vector3 velocityCache = new Vector3();
    @Unmanaged
    private final Vector3 angularVelocityCache = new Vector3();
    @Unmanaged
    private final Vector3 forceCache = new Vector3();
    @Unmanaged
    private final Vector3 torqueCache = new Vector3();
    @Unmanaged
    private final Matrix3 tensorCache = new Matrix3();

    @Unmanaged
    private final Vector3 temp = new Vector3();

    private RigidBody() {}

    public @Const
    Matrix3 getInertiaTensorInverse() {
        return tensorCache;
    }

    public RigidBody setInertiaTensorInverse(@Const Matrix3 tensorInverse) {
        tensorCache.set(tensorInverse);
        inertiaTensorWorldInverse.set(tensorInverse, getIndex());
        return this;
    }

    public RigidBody addForce(@Const Vector3 force, @Const Vector3 relPos) {
        forceCache.add(force);
        totalForce.set(forceCache, getIndex());

        if (relPos != null) {
            torqueCache.add(temp.cross(relPos, force));
            totalTorque.set(torqueCache, getIndex());
        }

        return this;
    }

    public RigidBody addImpulse(@Const Vector3 impulse, @Const Vector3 relPos) {
        velocityCache.addScaled(getInverseMass(), impulse);
        velocity.set(velocityCache, getIndex());

        if (relPos != null) {
            temp.cross(relPos, impulse);
            temp.mul(tensorCache, temp);
            angularVelocityCache.add(temp);

            angularVelocity.set(angularVelocityCache, getIndex());
        }

        return this;
    }

    public RigidBody setForce(@Const Vector3 f) {
        forceCache.set(f);
        totalForce.set(f, getIndex());
        return this;
    }

    public RigidBody setTorque(@Const Vector3 t) {
        torqueCache.set(t);
        totalTorque.set(t, getIndex());
        return this;
    }

    public RigidBody setVelocity(@Const Vector3 vel) {
        velocityCache.set(vel);
        velocity.set(vel, getIndex());

        return this;
    }

    public RigidBody setAngularVelocity(@Const Vector3 angVel) {
        angularVelocityCache.set(angVel);
        angularVelocity.set(angVel, getIndex());

        return this;
    }

    public RigidBody clearForces() {
        forceCache.set(0.0, 0.0, 0.0);
        torqueCache.set(0.0, 0.0, 0.0);

        totalForce.set(forceCache, getIndex());
        totalTorque.set(torqueCache, getIndex());

        return this;
    }

    public RigidBody setMass(double mass) {
        if (mass <= 0.0) {
            throw new IllegalArgumentException("Mass must be positive");
        }
        inverseMass.set(1.0 / mass, getIndex());
        return this;
    }

    public double getMass() {
        return 1.0 / inverseMass.get(getIndex());
    }

    public double getInverseMass() {
        return inverseMass.get(getIndex());
    }

    public @Const
    Vector3 getVelocity() {
        return velocityCache;
    }

    public @Const
    Vector3 getAngularVelocity() {
        return angularVelocityCache;
    }

    public @Const
    Vector3 getTotalForce() {
        return forceCache;
    }

    public @Const
    Vector3 getTotalTorque() {
        return torqueCache;
    }

    @Override
    protected void onSet(int index) {
        // FIXME this might be a performance bottleneck if we don't need to
        // access every single vector object when processing a given rigid body
        angularVelocity.get(index, angularVelocityCache);
        velocity.get(index, velocityCache);
        inertiaTensorWorldInverse.get(index, tensorCache);
        totalForce.get(index, forceCache);
        totalTorque.get(index, torqueCache);
    }
}
