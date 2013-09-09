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
import com.ferox.math.Quat4;
import com.ferox.math.Vector3;

/**
 * ExplicitEulerIntegrator is an integrator implemented using simple Euler integration for the linear
 * acceleration, velocity, and angular acceleration. Damping is applied to the angular velocity to improve
 * system stability. The angular velocity integration is ported from the Bullet's integrator.
 *
 * @author Michael Ludwig
 */
public class ExplicitEulerIntegrator implements Integrator {
    private static final double MAX_ANGULAR_VELOCITY = Math.PI / 2.0;
    private static final double ANGULAR_MOTION_THRESHOLD = Math.PI / 4.0;
    private static final double ANGULAR_VELOCITY_DAMPING = .5;

    private final Vector3 tempv = new Vector3();
    private final Quat4 tempq1 = new Quat4();
    private final Quat4 tempq2 = new Quat4();

    @Override
    public void integrateLinearAcceleration(@Const Vector3 a, double dt, Vector3 velocity) {
        integrateVector(a, dt, velocity);
    }

    @Override
    public void integrateAngularAcceleration(@Const Vector3 a, double dt, Vector3 angularVelocity) {
        integrateVector(a, dt, angularVelocity);
        applyDamping(angularVelocity, dt, ANGULAR_VELOCITY_DAMPING);
    }

    private void applyDamping(Vector3 v, double dt, double damping) {
        v.scale(Math.pow(1.0 - damping, dt));
    }

    @Override
    public void integrateLinearVelocity(@Const Vector3 v, double dt, Vector3 position) {
        integrateVector(v, dt, position);
    }

    @Override
    public void integrateAngularVelocity(@Const Vector3 v, double dt, Matrix3 orientation) {
        // clamp angular velocity
        Vector3 axis = tempv;

        double angvel = v.length();
        if (angvel * dt > MAX_ANGULAR_VELOCITY) {
            // set axis to be linear velocity but with magnitude = MAX / dt
            axis.scale(v, MAX_ANGULAR_VELOCITY / (angvel * dt));
            angvel = MAX_ANGULAR_VELOCITY / dt;
        } else {
            axis.set(v); // don't need to clamp so just set axis to angular velocity
        }

        // angular velocity uses the exponential map method
        // "Practical Parameterization of Rotations Using the Exponential Map", F. Sebastian Grassia

        // limit the angular motion - but update the velocity vector
        double fAngle = angvel;
        if (angvel * dt > ANGULAR_MOTION_THRESHOLD) {
            fAngle = ANGULAR_MOTION_THRESHOLD / dt;
        }

        if (fAngle < .001) {
            // use Taylor's expansions of sync function
            axis.scale((.5 * dt) - (dt * dt * dt) * (.02083333333333 * fAngle * fAngle));
        } else {
            // sync(fAngle) = sin(c * fAngle) / t
            axis.scale(Math.sin(.5 * fAngle * dt) / fAngle);
        }

        Quat4 newRot = tempq1.set(axis.x, axis.y, axis.z, Math.cos(.5 * fAngle * dt));
        Quat4 oldRot = tempq2.set(orientation);
        newRot.mul(oldRot).normalize();

        orientation.set(newRot);
    }

    private void integrateVector(@Const Vector3 deriv, double dt, Vector3 func) {
        if (deriv == null || func == null) {
            throw new NullPointerException("Arguments cannot be null");
        }
        func.addScaled(dt, deriv);
    }
}
