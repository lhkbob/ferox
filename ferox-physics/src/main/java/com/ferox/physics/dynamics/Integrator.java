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

/**
 * Integrator implementations are used to perform the numerical computing necessary to integrate accelerations
 * into velocities, and velocities into positions over a time delta. At the moment, only explicit euler
 * integration is implemented in {@link ExplicitEulerIntegrator}.
 *
 * @author Michael Ludwig
 */
public interface Integrator {
    /**
     * Integrate the given acceleration vector {@code a} over the time delta {@code dt}, measured in seconds.
     * The computed velocity should be added to {@code velocity}, which can be assumed not to be null.
     *
     * @param a        The linear acceleration
     * @param dt       The time delta in seconds
     * @param velocity The velocity output vector
     *
     * @throws NullPointerException if a or velocity are null
     */
    public void integrateLinearAcceleration(@Const Vector3 a, double dt, Vector3 velocity);

    /**
     * Integrate the given angular acceleration vector {@code a} over the time delta {@code dt}, measured in
     * seconds. The computed velocity should be added to {@code velocity}, which can be assumed not to be
     * null.
     *
     * @param a               The angular acceleration
     * @param dt              The time delta in seconds
     * @param angularVelocity The angular velocity output vector
     *
     * @throws NullPointerException if a or angularVelocity are null
     */
    public void integrateAngularAcceleration(@Const Vector3 a, double dt, Vector3 angularVelocity);

    /**
     * Integrate the given linear velocity vector {@code v} over the time delta {@code dt}, measured in
     * seconds. The computed delta position should be added to the 'current' position stored in {@code
     * position}, which can be assumed not to be null.
     *
     * @param v        The linear velocity
     * @param dt       The time delta in seconds
     * @param position The position output vector
     *
     * @throws NullPointerException if v or position are null
     */
    public void integrateLinearVelocity(@Const Vector3 v, double dt, Vector3 position);

    /**
     * Integrate the given angular velocity vector {@code a} over the time delta {@code dt}, measured in
     * seconds. The computed delta orientation should be accumulated into the 'current' orientation stored in
     * {@code orientation}, which can be assumed not to be null.
     *
     * @param v           The angular velocity
     * @param dt          The time delta in seconds
     * @param orientation The output orientation matrix
     *
     * @throws NullPointerException if a or velocity are null
     */
    public void integrateAngularVelocity(@Const Vector3 v, double dt, Matrix3 orientation);
}
