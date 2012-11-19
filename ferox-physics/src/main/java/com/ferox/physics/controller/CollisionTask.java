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
import java.util.Set;

import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.ContactManifoldPool;
import com.ferox.physics.dynamics.LinearConstraintPool;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.ElapsedTimeResult;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

public abstract class CollisionTask implements Task {
    private final ContactManifoldPool manifolds;

    private final LinearConstraintPool contactGroup;
    private final LinearConstraintPool frictionGroup;

    protected double dt;

    public CollisionTask() {
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

    protected void notifyContact(CollisionBody bodyA, CollisionBody bodyB,
                                 ClosestPair contact) {
        manifolds.addContact(bodyA, bodyB, contact);
    }

    private class WarmstartTask implements Task, ParallelAware {
        @Override
        public Set<Class<? extends ComponentData<?>>> getAccessedComponents() {
            return Collections.emptySet();
        }

        @Override
        public boolean isEntitySetModified() {
            return false;
        }

        @Override
        public Task process(EntitySystem system, Job job) {
            // read back computed impulses from constraint solving controller
            manifolds.computeWarmstartImpulses(contactGroup, frictionGroup);
            return null;
        }

        @Override
        public void reset(EntitySystem system) {

        }
    }
}
