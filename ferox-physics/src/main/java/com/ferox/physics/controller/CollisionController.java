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

import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.ContactManifoldPool;
import com.ferox.physics.dynamics.LinearConstraintPool;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.SimpleController;

public abstract class CollisionController extends SimpleController {
    private final ContactManifoldPool manifolds;

    private final LinearConstraintPool contactGroup;
    private final LinearConstraintPool frictionGroup;

    public CollisionController() {
        manifolds = new ContactManifoldPool();
        contactGroup = new LinearConstraintPool(null);
        frictionGroup = new LinearConstraintPool(contactGroup);
    }

    public ContactManifoldPool getContactManifoldPool() {
        return manifolds;
    }

    public LinearConstraintPool getContactGroup() {
        return contactGroup;
    }

    public LinearConstraintPool getFrictionGroup() {
        return frictionGroup;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onComponentRemove(Component<?> c) {
        if (c.getTypeId() == CollisionBody.ID) {
            // must remove all contacts connected to this entity from the cache
            manifolds.removeAllContacts((Component<CollisionBody>) c);
        }
    }

    @Override
    public void init(EntitySystem system) {
        super.init(system);
        manifolds.setEntitySystem(system);
    }

    @Override
    public void preProcess(double dt) {
        // reset constraint pools
        contactGroup.clear();
        frictionGroup.clear();
    }

    @Override
    public void postProcess(double dt) {
        // read back computed impulses from constraint solving controller
        manifolds.computeWarmstartImpulses(contactGroup, frictionGroup);
    }

    protected void reportConstraints(double dt) {
        manifolds.generateConstraints(dt, contactGroup, frictionGroup);
        getEntitySystem().getControllerManager()
                         .report(new ConstraintResult(contactGroup));
        getEntitySystem().getControllerManager()
                         .report(new ConstraintResult(frictionGroup));
    }

    protected void notifyContact(CollisionBody bodyA, CollisionBody bodyB,
                                 ClosestPair contact) {
        manifolds.addContact(bodyA, bodyB, contact);
    }
}
