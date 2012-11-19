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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.ExplicitEulerIntegrator;
import com.ferox.physics.dynamics.Integrator;
import com.ferox.physics.dynamics.RigidBody;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.ElapsedTimeResult;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

public class MotionTask implements Task, ParallelAware {
    private static final Set<Class<? extends ComponentData<?>>> COMPONENTS;
    static {
        Set<Class<? extends ComponentData<?>>> types = new HashSet<Class<? extends ComponentData<?>>>();
        types.add(CollisionBody.class);
        types.add(RigidBody.class);
        COMPONENTS = Collections.unmodifiableSet(types);
    }

    private final Integrator integrator;

    private double dt;

    // values that could be local to process(), but don't need to be allocated
    // every frame
    private final Vector3 predictedPosition = new Vector3();
    private final Matrix3 predictedRotation = new Matrix3();

    private RigidBody rigidBody;
    private CollisionBody collisionBody;
    private ComponentIterator iterator;

    public MotionTask() {
        this(new ExplicitEulerIntegrator());
    }

    public MotionTask(Integrator integrator) {
        if (integrator == null) {
            throw new NullPointerException("Integrator can't be null");
        }
        this.integrator = integrator;
    }

    public void report(ElapsedTimeResult dt) {
        this.dt = dt.getTimeDelta();
    }

    @Override
    public void reset(EntitySystem system) {
        if (iterator == null) {
            rigidBody = system.createDataInstance(RigidBody.class);
            collisionBody = system.createDataInstance(CollisionBody.class);
            iterator = new ComponentIterator(system);
        } else {
            iterator.reset();
        }
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        while (iterator.next()) {
            Matrix4 transform = collisionBody.getTransform();

            predictedRotation.setUpper(transform);
            predictedPosition.set(transform.m03, transform.m13, transform.m23);

            integrator.integrateLinearVelocity(rigidBody.getVelocity(), dt,
                                               predictedPosition);
            integrator.integrateAngularVelocity(rigidBody.getAngularVelocity(), dt,
                                                predictedRotation);

            // push values back into transform
            setTransform(predictedRotation, predictedPosition, transform);

            collisionBody.setTransform(transform);
        }

        return null;
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

    @Override
    public Set<Class<? extends ComponentData<?>>> getAccessedComponents() {
        return COMPONENTS;
    }

    @Override
    public boolean isEntitySetModified() {
        return false;
    }
}
