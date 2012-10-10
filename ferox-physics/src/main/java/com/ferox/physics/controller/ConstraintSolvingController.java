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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ferox.math.Vector3;
import com.ferox.math.entreri.Vector3Property;
import com.ferox.physics.dynamics.LinearConstraintPool;
import com.ferox.physics.dynamics.LinearConstraintSolver;
import com.ferox.physics.dynamics.RigidBody;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.Result;
import com.lhkbob.entreri.SimpleController;

public class ConstraintSolvingController extends SimpleController {
    private final LinearConstraintSolver solver;

    private List<LinearConstraintPool> groups;

    private Vector3Property deltaLinearImpulse;
    private Vector3Property deltaAngularImpulse;

    public ConstraintSolvingController() {
        solver = new LinearConstraintSolver();
    }

    public LinearConstraintSolver getSolver() {
        return solver;
    }

    @Override
    public void preProcess(double dt) {
        groups = new ArrayList<LinearConstraintPool>();
    }

    @Override
    public void process(double dt) {
        Vector3 d = new Vector3();

        LinearConstraintPool[] asArray = groups.toArray(new LinearConstraintPool[groups.size()]);
        solver.solve(asArray);

        // now apply all of the delta impulses back to the rigid bodies
        Iterator<RigidBody> it = getEntitySystem().iterator(RigidBody.ID);
        while (it.hasNext()) {
            RigidBody b = it.next();

            // linear velocity
            deltaLinearImpulse.get(b.getIndex(), d);
            b.setVelocity(d.add(b.getVelocity()));

            // angular velocity
            deltaAngularImpulse.get(b.getIndex(), d);
            b.setAngularVelocity(d.add(b.getAngularVelocity()));

            // 0 out delta impulse for next frame
            d.set(0, 0, 0);
            deltaLinearImpulse.set(d, b.getIndex());
            deltaAngularImpulse.set(d, b.getIndex());
        }
    }

    @Override
    public void report(Result r) {
        if (r instanceof ConstraintResult) {
            groups.add(((ConstraintResult) r).getConstraints());
        }
    }

    @Override
    public void init(EntitySystem system) {
        super.init(system);
        deltaLinearImpulse = system.decorate(RigidBody.ID,
                                             new Vector3Property.Factory(new Vector3()));
        deltaAngularImpulse = system.decorate(RigidBody.ID,
                                              new Vector3Property.Factory(new Vector3()));

        solver.setDeltaLinearImpulseProperty(deltaLinearImpulse);
        solver.setDeltaAngularImpulseProperty(deltaAngularImpulse);
    }

    @Override
    public void destroy() {
        getEntitySystem().undecorate(RigidBody.ID, deltaLinearImpulse);
        getEntitySystem().undecorate(RigidBody.ID, deltaAngularImpulse);
        super.destroy();
    }
}
