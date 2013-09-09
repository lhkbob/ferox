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

import com.ferox.math.Vector3;
import com.ferox.math.entreri.Vector3Property;
import com.ferox.physics.dynamics.LinearConstraintPool;
import com.ferox.physics.dynamics.LinearConstraintSolver;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * ConstraintSolvingTask is a task implementation that collects all reported {@link ConstraintResult
 * ConstraintResults} and computes a solution using a {@link LinearConstraintSolver}. It also manages the
 * decorated properties expected by the solver to accumulate delta impulses and properly adds those impulses
 * to the rigid bodies' velocities.
 *
 * @author Michael Ludwig
 */
public class ConstraintSolvingTask implements Task, ParallelAware {
    private final LinearConstraintSolver solver;

    private List<LinearConstraintPool> groups;

    private Vector3Property deltaLinearImpulse;
    private Vector3Property deltaAngularImpulse;

    // instances used locally but instantiated once to save performance
    private RigidBody rigidBody;
    private ComponentIterator iterator;
    private final Vector3 delta = new Vector3();

    public ConstraintSolvingTask() {
        solver = new LinearConstraintSolver();
    }

    @Override
    public void reset(EntitySystem system) {
        if (iterator == null) {
            iterator = system.fastIterator();
            rigidBody = iterator.addRequired(RigidBody.class);

            deltaLinearImpulse = system.decorate(RigidBody.class, new Vector3Property.Factory(new Vector3()));
            deltaAngularImpulse = system
                    .decorate(RigidBody.class, new Vector3Property.Factory(new Vector3()));

            solver.setDeltaLinearImpulseProperty(deltaLinearImpulse);
            solver.setDeltaAngularImpulseProperty(deltaAngularImpulse);
        }

        groups = new ArrayList<>();
        iterator.reset();
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        Profiler.push("constraint-solving-task");

        Profiler.push("solve-constraints");
        LinearConstraintPool[] asArray = groups.toArray(new LinearConstraintPool[groups.size()]);
        solver.solve(asArray);
        Profiler.pop();

        // now apply all of the delta impulses back to the rigid bodies
        Profiler.push("apply-constraints");
        while (iterator.next()) {
            // linear velocity
            deltaLinearImpulse.get(rigidBody.getIndex(), delta);
            rigidBody.setVelocity(delta.add(rigidBody.getVelocity()));

            // angular velocity
            deltaAngularImpulse.get(rigidBody.getIndex(), delta);
            rigidBody.setAngularVelocity(delta.add(rigidBody.getAngularVelocity()));

            // 0 out delta impulse for next frame
            delta.set(0, 0, 0);
            deltaLinearImpulse.set(rigidBody.getIndex(), delta);
            deltaAngularImpulse.set(rigidBody.getIndex(), delta);
        }
        Profiler.pop();

        Profiler.pop();

        return null;
    }

    public void report(ConstraintResult r) {
        groups.add(r.getConstraints());
    }

    @Override
    public Set<Class<? extends Component>> getAccessedComponents() {
        return Collections.<Class<? extends Component>>singleton(RigidBody.class);
    }

    @Override
    public boolean isEntitySetModified() {
        return false;
    }
}
