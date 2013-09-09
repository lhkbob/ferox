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
import com.lhkbob.entreri.Within;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;
import com.lhkbob.entreri.property.SharedInstance;

/**
 * <p/>
 * RigidBody represents an instance of an object in a physics simulation that can move, collide, and be
 * collided with. The RigidBody component is paired with the {@link CollisionBody} and controls the motion
 * behavior of the entity, such as its velocity, mass, etc.
 *
 * @author Michael Ludwig
 */
@Requires(CollisionBody.class)
public interface RigidBody extends Component {
    /**
     * @return Get the inverse of the body's inertia tensor matrix
     */
    @Const
    @SharedInstance
    public Matrix3 getInertiaTensorInverse();

    /**
     * Set the inverse of the body's inertia tensor matrix. This must be computed by a task, which by default
     * is handled in the {@link com.ferox.physics.task.IntegrationTask}.
     *
     * @param tensorInverse The inverse tensor
     *
     * @return This component
     */
    public RigidBody setInertiaTensorInverse(@Const Matrix3 tensorInverse);

    /**
     * Set the linear velocity of this rigid body's center of mass.
     *
     * @param vel The new linear velocity
     *
     * @return This component
     */
    public RigidBody setVelocity(@Const Vector3 vel);

    /**
     * Set the angular velocity of this rigid body. The angular velocity is stored as the axis of rotation and
     * its magnitude is the rate of rotation.
     *
     * @param angVel The new angular velocity
     *
     * @return This component
     */
    public RigidBody setAngularVelocity(@Const Vector3 angVel);

    /**
     * Set the mass of this component. The mass must be positive, and to help stability of a simulation, it
     * must be above 0.0001 'units', which are whatever units you wish the system to be expressed in.
     *
     * @param mass The new mass
     *
     * @return This component
     */
    public RigidBody setMass(@Within(min = 0.0001) double mass);

    /**
     * @return The mass of the rigid body
     */
    @DefaultDouble(1.0)
    public double getMass();

    /**
     * @return The current linear velocity
     */
    @Const
    @SharedInstance
    public Vector3 getVelocity();

    /**
     * @return The current angular velocity
     */
    @Const
    @SharedInstance
    public Vector3 getAngularVelocity();

    /**
     * Utils contains static methods to operate on RigidBodies in a useful manner, but that can't be placed
     * directly in RigidBody because it is an interface.
     */
    public static final class Utils {
        private Utils() {
        }

        /**
         * Apply the given impulse to the rigid body. This will update both the velocity and angular velocity
         * based on the mass of the object. An impulse is a force integrated over a time step. If {@code
         * relPos} is null, it is assumed the impulse is applied to the center of mass.
         *
         * @param body    The rigid body to modify
         * @param impulse The impulse applied to the body
         * @param relPos  The relative position the impulse affects, or null for the center of mass
         */
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
    }
}
