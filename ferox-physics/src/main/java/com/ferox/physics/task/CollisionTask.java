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

import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.CollisionAlgorithmProvider;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.ContactManifoldPool;
import com.ferox.physics.dynamics.LinearConstraintPool;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.ElapsedTimeResult;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.Collections;
import java.util.Set;

public abstract class CollisionTask implements Task {
    private final CollisionAlgorithmProvider algorithms;

    private final ContactManifoldPool manifolds;

    private final LinearConstraintPool contactGroup;
    private final LinearConstraintPool frictionGroup;

    protected double dt;

    public CollisionTask(CollisionAlgorithmProvider algorithms) {
        if (algorithms == null) {
            throw new NullPointerException("Algorithm provider cannot be null");
        }

        this.algorithms = algorithms;
        manifolds = new ContactManifoldPool();
        contactGroup = new LinearConstraintPool(null);
        frictionGroup = new LinearConstraintPool(contactGroup);
    }

    public void report(ElapsedTimeResult dt) {
        this.dt = dt.getTimeDelta();
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        return new WarmstartTask();
    }

    @Override
    public void reset(EntitySystem system) {
        if (manifolds.getEntitySystem() != system) {
            manifolds.setEntitySystem(system);
        }

        // reset constraint pools
        contactGroup.clear();
        frictionGroup.clear();
    }

    protected void reportConstraints(Job job) {
        manifolds.generateConstraints(dt, contactGroup, frictionGroup);
        job.report(new ConstraintResult(contactGroup));
        job.report(new ConstraintResult(frictionGroup));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void notifyPotentialContact(CollisionBody bodyA, CollisionBody bodyB) {
        // collisions must have at least one rigid body to act on
        if (bodyA.getEntity().get(RigidBody.class) == null &&
            bodyB.getEntity().get(RigidBody.class) == null) {
            return;
        }

        // get the appropriate algorithm
        CollisionAlgorithm algorithm = algorithms
                .getAlgorithm(bodyA.getShape().getClass(), bodyB.getShape().getClass());

        if (algorithm != null) {
            // compute closest pair between the two shapes
            ClosestPair pair = algorithm
                    .getClosestPair(bodyA.getShape(), bodyA.getTransform(), bodyB.getShape(),
                                    bodyB.getTransform());

            if (pair != null && pair.isIntersecting()) {
                // add to manifold only when there is an intersection
                manifolds.addContact(bodyA, bodyB, pair);
            }
        }
    }

    private class WarmstartTask implements Task, ParallelAware {
        @Override
        public Set<Class<? extends Component>> getAccessedComponents() {
            return Collections.emptySet();
        }

        @Override
        public boolean isEntitySetModified() {
            return false;
        }

        @Override
        public Task process(EntitySystem system, Job job) {
            // read back computed impulses from constraint solving controller
            Profiler.push("warmstart-contacts");
            manifolds.computeWarmstartImpulses(contactGroup, frictionGroup);
            Profiler.pop();
            return null;
        }

        @Override
        public void reset(EntitySystem system) {

        }
    }
}
