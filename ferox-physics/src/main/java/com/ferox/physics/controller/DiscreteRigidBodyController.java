package com.ferox.physics.controller;

import java.awt.geom.AffineTransform;
import java.util.HashSet;
import java.util.Set;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.CollisionAlgorithmProvider;
import com.ferox.physics.collision.CollisionCallback;
import com.ferox.physics.collision.CollisionManager;
import com.ferox.physics.dynamics.Integrator;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.physics.dynamics.constraint.Constraint;
import com.ferox.physics.dynamics.constraint.ConstraintSolver;
import com.ferox.physics.dynamics.constraint.ContactManifoldCache;
import com.ferox.util.Bag;
import com.ferox.util.ChainedCollection;

public class DiscreteRigidBodyController {
    private final Set<CollisionBody> bodies;
    private final Bag<RigidBody> rigidBodies;

    private final Vector3 gravity;

    private final Set<Constraint> constraints;
    
    private CollisionManager collisionManager;
    private ConstraintSolver constraintSolver;
    private Integrator integrator;

    private final ContactManifoldCache contactCache;
    
    public DiscreteRigidBodyController(PhysicsWorldConfiguration config) {
        if (config == null)
            throw new NullPointerException("Configuration cannot be null");
        
        gravity = new Vector3(0, -10, 0);
        
        bodies = new HashSet<CollisionBody>();
        rigidBodies = new Bag<RigidBody>();
        constraints = new HashSet<Constraint>();
        
        contactCache = new ContactManifoldCache();
        
        integrator = config.getIntegrator();
        constraintSolver = config.getConstraintSolver();
        collisionManager = config.getCollisionManager();
    }

//    public @Const Vector3 getGravity() {
//        return gravity;
//    }
//
//    public void setGravity(@Const Vector3 gravity) {
//        if (gravity == null)
//            throw new NullPointerException("Gravity cannot be null");
//        this.gravity.set(gravity);
//    }

    @Override
    public void step(float dt) {
        // FIXME use time step interpolation to maintain a fixed time step rate like in bullet
        // DONE - ForcesController
//        RigidBody b;
//        Vector3f gravForce = new Vector3f();
//        int ct = rigidBodies.size();
//        for (int i = 0; i < ct; i++) {
//            b = rigidBodies.get(i);
//            
//            // add gravity force to each body
//            if (b.getExplicitGravity() != null)
//                b.getExplicitGravity().scale(b.getMass(), gravForce);
//            else
//                gravity.scale(b.getMass(), gravForce);
//
//            b.addForce(gravForce, null); // gravity is a central force, applies no torque
//            b.applyForces(integrator, dt);
//        }
//        
        collisionManager.processCollisions(new ContactManifoldCallback());
        contactCache.update();
        
        // DONE -> ConstraintSolvingController
//        constraintSolver.solve(new ChainedCollection<Constraint>(constraints, contactCache.getContacts()), dt);
        
        
        // DONE -> MotionController
        // recompute next frame position after constraint solving
        // and store it in the world transform
//        AffineTransform t = new AffineTransform();
//        for (int i = 0; i < ct; i++) {
//            b = rigidBodies.get(i);
//            b.predictMotion(integrator, dt, t);
//            b.setTransform(t);
//        }
    }
    
    @Override
    public void add(CollisionBody c) {
        if (c == null)
            throw new NullPointerException("Body cannot be null");
        
        if (!bodies.contains(c)) {
            bodies.add(c);
            collisionManager.add(c);
            if (c instanceof RigidBody)
                rigidBodies.add((RigidBody) c);
        }
    }

    @Override
    public void remove(CollisionBody c) {
        if (c == null)
            throw new NullPointerException("Body cannot be null");
        
        if (bodies.remove(c)) {
            collisionManager.remove(c);
            contactCache.remove(c);
            if (c instanceof RigidBody)
                rigidBodies.remove(c);
        }
    }

    @Override
    public void add(Constraint c) {
        if (c == null)
            throw new NullPointerException("Constraint cannot be null");
        constraints.add(c);
    }

    @Override
    public void remove(Constraint c) {
        if (c == null)
            throw new NullPointerException("Constraint cannot be null");
        constraints.remove(c);
    }

    @Override
    public Integrator getIntegrator() {
        return integrator;
    }

    @Override
    public void setIntegrator(Integrator integrator) {
        if (integrator == null)
            throw new NullPointerException("Integrator cannot be null");
        this.integrator = integrator;
    }

    @Override
    public ConstraintSolver getConstraintSolver() {
        return constraintSolver;
    }

    @Override
    public void setConstraintSolver(ConstraintSolver solver) {
        if (solver == null)
            throw new NullPointerException("ConstraintSolver cannot be null");
        constraintSolver = solver;
    }

    @Override
    public CollisionManager getCollisionManager() {
        return collisionManager;
    }

    @Override
    public void setCollisionManager(CollisionManager manager) {
        if (manager == null)
            throw new NullPointerException("CollisionManager cannot be null");
        
        // remove bodies from old manager and add them to the new one
        for (CollisionBody c: bodies) {
            collisionManager.remove(c);
            manager.add(c);
        }
        
        // update reference
        collisionManager = manager;
    }
    
    private class ContactManifoldCallback implements CollisionCallback {
        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void process(CollisionBody objA, CollisionBody objB, CollisionAlgorithmProvider handler) {
            if (!(objA instanceof RigidBody) && !(objB instanceof RigidBody))
                return; // ignore collisions between only static objects
            
            CollisionAlgorithm algo = handler.getAlgorithm(objA.getShape().getClass(), objB.getShape().getClass());
            if (algo != null) {
                ClosestPair pair = algo.getClosestPair(objA.getShape(), objA.getTransform(), objB.getShape(), objB.getTransform());
                if (pair != null && pair.isIntersecting())
                    contactCache.addContact(objA, objB, pair);
            }
            
        }
    }
}
