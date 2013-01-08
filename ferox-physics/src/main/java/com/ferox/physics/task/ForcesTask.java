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
package com.ferox.physics.task;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.ExplicitEulerIntegrator;
import com.ferox.physics.dynamics.Gravity;
import com.ferox.physics.dynamics.Integrator;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.ElapsedTimeResult;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

// FIXME this task should be split into 3 pieces:
// 1. inertia tensor computer (which must be done before any force can be added to a body)
// 2. gravity force applier
// 3. force integrator
// Although see note in SpatialIndexCollisionController. This is the main place
// that benefits from merging the loop -> maybe I can artificially support
// auto-merging by calling this the PrepTask that takes preparers that operate
// on single RigidBody/CollisionBody entities. These can be split into 
// composable tasks but only iterated through once in this task.
//
// The Motion and ConstraintSolving tasks could be similarly combined, although
// part of the constraint solving task is global... I bet it doesn't make a 
// big performance dent so let's just keep it in mind but design for maximum
// cleanliness right now.
public class ForcesTask implements Task, ParallelAware {
    private static final Set<Class<? extends ComponentData<?>>> COMPONENTS;
    static {
        Set<Class<? extends ComponentData<?>>> types = new HashSet<Class<? extends ComponentData<?>>>();
        types.add(CollisionBody.class);
        types.add(RigidBody.class);
        types.add(Gravity.class);
        COMPONENTS = Collections.unmodifiableSet(types);
    }

    private final Integrator integrator;
    private final Vector3 defaultGravity;

    private double dt;

    // cached instances that could be local to process()
    private RigidBody rigidBody;
    private CollisionBody colBody;
    private Gravity gravity;
    private ComponentIterator iterator;

    private final Vector3 inertia = new Vector3();
    private final Vector3 force = new Vector3();
    private final Matrix3 rotation = new Matrix3();

    public ForcesTask() {
        this(new Vector3(0, -9.8, 0));
    }

    public ForcesTask(@Const Vector3 gravity) {
        this(gravity, new ExplicitEulerIntegrator());
    }

    public ForcesTask(@Const Vector3 gravity, Integrator integrator) {
        if (integrator == null) {
            throw new NullPointerException("Integrator cannot be null");
        }
        defaultGravity = gravity.clone();
        this.integrator = integrator;
    }

    public void report(ElapsedTimeResult dt) {
        this.dt = dt.getTimeDelta();
    }

    @Override
    public void reset(EntitySystem system) {
        if (rigidBody == null) {
            rigidBody = system.createDataInstance(RigidBody.class);
            colBody = system.createDataInstance(CollisionBody.class);
            gravity = system.createDataInstance(Gravity.class);

            iterator = new ComponentIterator(system).addRequired(rigidBody)
                                                    .addRequired(colBody)
                                                    .addOptional(gravity);
        }

        iterator.reset();
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        Profiler.push("apply-forces");
        while (iterator.next()) {
            Matrix4 transform = colBody.getTransform();

            // compute the body's new inertia tensor
            Matrix3 tensor = rigidBody.getInertiaTensorInverse();

            colBody.getShape().getInertiaTensor(rigidBody.getMass(), inertia);
            inertia.set(1.0 / inertia.x, 1.0 / inertia.y, 1.0 / inertia.z);

            rotation.setUpper(transform);
            tensor.mulDiagonal(rotation, inertia).mulTransposeRight(rotation);
            rigidBody.setInertiaTensorInverse(tensor);

            // add gravity force
            if (gravity.isEnabled()) {
                force.scale(gravity.getGravity(), rigidBody.getMass());
            } else {
                force.scale(defaultGravity, rigidBody.getMass());
            }

            rigidBody.addForce(force, null);

            // integrate and apply forces to the body's velocity
            Vector3 lv = rigidBody.getVelocity();
            integrator.integrateLinearAcceleration(force.scale(rigidBody.getTotalForce(),
                                                               rigidBody.getInverseMass()),
                                                   dt, lv);
            rigidBody.setVelocity(lv);

            Vector3 la = rigidBody.getAngularVelocity();
            integrator.integrateAngularAcceleration(force.mul(tensor,
                                                              rigidBody.getTotalTorque()),
                                                    dt, la);
            rigidBody.setAngularVelocity(la);

            // reset forces
            rigidBody.clearForces();
        }
        Profiler.pop();
        return null;
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
