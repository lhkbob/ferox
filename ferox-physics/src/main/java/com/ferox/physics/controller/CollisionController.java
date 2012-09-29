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
        getEntitySystem().getControllerManager().report(new ConstraintResult(contactGroup));
        getEntitySystem().getControllerManager().report(new ConstraintResult(frictionGroup));
    }


    protected void notifyContact(CollisionBody bodyA, CollisionBody bodyB, ClosestPair contact) {
        manifolds.addContact(bodyA, bodyB, contact);
    }
}
