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

import com.ferox.math.*;
import com.ferox.math.entreri.BoundsResult;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.ExplicitEulerIntegrator;
import com.ferox.physics.dynamics.Gravity;
import com.ferox.physics.dynamics.Integrator;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.ElapsedTimeResult;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * IntegerationTask is a task to run at the start of the physics job that integrates the linear and angular
 * velocities into the new position and orientation. It then updates all CollisionBodies' world bounds, and
 * all RigidBodies' inertia tensor matrices. After that it applies a configured gravity acceleration. The
 * world bounds of the simulation are reported using a {@link BoundsResult} instance.
 *
 * @author Michael Ludwig
 */
public class IntegrationTask implements Task, ParallelAware {
    private static final Set<Class<? extends Component>> COMPONENTS;

    static {
        Set<Class<? extends Component>> types = new HashSet<>();
        types.add(CollisionBody.class);
        types.add(RigidBody.class);
        types.add(Gravity.class);
        COMPONENTS = Collections.unmodifiableSet(types);
    }

    private final Integrator integrator;
    private final Vector3 defaultGravity;

    private double dt;

    // values that could be local to process(), but don't need to be allocated every frame
    private RigidBody rigidBody;
    private CollisionBody collisionBody;
    private Gravity gravity;
    private ComponentIterator iterator;

    private final Vector3 predictedPosition = new Vector3();
    private final Matrix3 predictedRotation = new Matrix3();
    private final Vector3 inertia = new Vector3();
    private final Vector3 force = new Vector3();
    private final Matrix3 rotation = new Matrix3();

    /**
     * Create a new IntegrationTask that uses the default gravity vector along the y-axis with acceleration
     * equal to Earth's. An explicit Euler integrator is used.
     */
    public IntegrationTask() {
        this(new Vector3(0, -9.8, 0));
    }

    /**
     * Create a new IntegrationTask that uses the given default gravity acceleration vector and an explicit
     * Euler integrator.
     *
     * @param gravity The default gravity (it will be cloned so changes to it do not affect the task)
     *
     * @throws NullPointerException if gravity is null
     */
    public IntegrationTask(@Const Vector3 gravity) {
        this(gravity, new ExplicitEulerIntegrator());
    }

    /**
     * Create a new IntegrationTask that uses the given default gravity and integrator.
     *
     * @param gravity    The default gravity (it will be cloned so changes to it do not affect the task)
     * @param integrator The integrator to use
     *
     * @throws NullPointerException if gravity or integrator are null
     */
    public IntegrationTask(@Const Vector3 gravity, Integrator integrator) {
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
    public Task process(EntitySystem system, Job job) {
        // FIXME if it's weird that this causes 1 frame latency of motion, we can move it into a post-job
        Profiler.push("integrate-motion");
        AxisAlignedBox worldBounds = new AxisAlignedBox();
        AxisAlignedBox union = new AxisAlignedBox();
        boolean firstBounds = true;

        while (iterator.next()) {
            Matrix4 transform = collisionBody.getTransform();

            if (rigidBody.isAlive()) {
                // 1. Integrate velocities accumulated from previous time step
                predictedRotation.setUpper(transform);
                predictedPosition.set(transform.m03, transform.m13, transform.m23);

                integrator.integrateLinearVelocity(rigidBody.getVelocity(), dt, predictedPosition);
                integrator.integrateAngularVelocity(rigidBody.getAngularVelocity(), dt, predictedRotation);

                // push values back into transform
                setTransform(predictedRotation, predictedPosition, transform);
                collisionBody.setTransform(transform);
            }

            // 2. Update world bounds and accumulate scene union
            worldBounds.transform(collisionBody.getShape().getBounds(), transform);
            collisionBody.setWorldBounds(worldBounds);
            if (firstBounds) {
                union.set(worldBounds);
                firstBounds = false;
            } else {
                union.union(worldBounds);
            }

            if (rigidBody.isAlive()) {
                // 3. Compute inertia tensors for bodies
                Matrix3 tensor = rigidBody.getInertiaTensorInverse();

                collisionBody.getShape().getInertiaTensor(rigidBody.getMass(), inertia);
                inertia.set(1.0 / inertia.x, 1.0 / inertia.y, 1.0 / inertia.z);

                rotation.setUpper(transform);
                tensor.mulDiagonal(rotation, inertia).mulTransposeRight(rotation);
                rigidBody.setInertiaTensorInverse(tensor);

                // 4. Compute and apply gravity force
                if (gravity.isAlive()) {
                    force.scale(gravity.getGravity(), rigidBody.getMass());
                } else {
                    force.scale(defaultGravity, rigidBody.getMass());
                }

                Vector3 lv = rigidBody.getVelocity();
                integrator.integrateLinearAcceleration(force.scale(force, 1.0 / rigidBody.getMass()), dt, lv);
                rigidBody.setVelocity(lv);
            }
        }

        job.report(new BoundsResult(CollisionBody.class, union));
        Profiler.pop();

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
    public void reset(EntitySystem system) {
        if (iterator == null) {
            iterator = system.fastIterator();
            collisionBody = iterator.addRequired(CollisionBody.class);
            rigidBody = iterator.addOptional(RigidBody.class);
            gravity = iterator.addOptional(Gravity.class);
        }

        iterator.reset();
    }

    @Override
    public Set<Class<? extends Component>> getAccessedComponents() {
        return COMPONENTS;
    }

    @Override
    public boolean isEntitySetModified() {
        return false;
    }
}
