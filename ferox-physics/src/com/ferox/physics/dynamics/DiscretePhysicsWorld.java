package com.ferox.physics.dynamics;

import java.util.HashSet;
import java.util.Set;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.CollisionCallback;
import com.ferox.physics.collision.CollisionHandler;
import com.ferox.physics.collision.CollisionManager;
import com.ferox.physics.collision.algorithm.ClosestPair;
import com.ferox.physics.dynamics.constraint.Constraint;
import com.ferox.physics.dynamics.constraint.ConstraintSolver;
import com.ferox.physics.dynamics.constraint.ContactManifoldCache;
import com.ferox.util.Bag;
import com.ferox.util.ChainedCollection;

public class DiscretePhysicsWorld implements PhysicsWorld {
    // FIXME: thread-safety
    private final Set<Collidable> bodies;
    private final Bag<RigidBody> rigidBodies;

    private final Vector3f gravity;

    private final Set<Constraint> constraints;
    
    private CollisionManager collisionManager;
    private ConstraintSolver constraintSolver;
    private Integrator integrator;

    private final ContactManifoldCache contactCache;
    
    public DiscretePhysicsWorld(PhysicsWorldConfiguration config) {
        if (config == null)
            throw new NullPointerException("Configuration cannot be null");
        
        gravity = new Vector3f(0f, -10f, 0f);
        
        bodies = new HashSet<Collidable>();
        rigidBodies = new Bag<RigidBody>();
        constraints = new HashSet<Constraint>();
        
        contactCache = new ContactManifoldCache();
        
        integrator = config.getIntegrator();
        constraintSolver = config.getConstraintSolver();
        collisionManager = config.getCollisionManager();
    }

    @Override
    public ReadOnlyVector3f getGravity() {
        return gravity;
    }

    @Override
    public void setGravity(ReadOnlyVector3f gravity) {
        if (gravity == null)
            throw new NullPointerException("Gravity cannot be null");
        this.gravity.set(gravity);
    }

    long collideTime = 0;
    long constraintTime = 0;
    long intTime = 0;
    int steps = 0;
    @Override
    public void step(float dt) {
        // FIXME use time step interpolation to maintain a fixed time step rate like in bullet
        intTime -= System.nanoTime();
        RigidBody b;
        Vector3f gravForce = new Vector3f();
        int ct = rigidBodies.size();
        for (int i = 0; i < ct; i++) {
            b = rigidBodies.get(i);
            
            // add gravity force to each body
            if (b.getExplicitGravity() != null)
                b.getExplicitGravity().scale(b.getMass(), gravForce);
            else
                gravity.scale(b.getMass(), gravForce);

            b.addForce(gravForce, null); // gravity is a central force, applies no torque
            b.applyForces(integrator, dt);
        }
        intTime += System.nanoTime();
        
        collideTime -= System.nanoTime();
        contactCache.update();
        collisionManager.processCollisions(new ContactManifoldCallback());
        collideTime += System.nanoTime();

        constraintTime -= System.nanoTime();
        constraintSolver.solve(new ChainedCollection<Constraint>(constraints, contactCache.getContacts()), dt);
        constraintTime += System.nanoTime();
        
        intTime -= System.nanoTime();
        Transform t = new Transform();
        for (int i = 0; i < ct; i++) {
            b = rigidBodies.get(i);
            
            // recompute next frame position after constraint solving
            // and store it in the world transform
            b.applyDeltaImpulse();
            b.predictMotion(integrator, dt, t);
            b.setWorldTransform(t);
        }
        intTime += System.nanoTime();
        
        
        steps++;
        if (steps > 10) {
            System.out.println("integrate: " + (intTime / (steps * 1e6f)) + " collide: " + (collideTime / (steps * 1e6f)) + " constraint: " + (constraintTime / (steps * 1e6f)));
            intTime = 0;
            collideTime = 0;
            constraintTime = 0;
            steps = 0;
        }
    }
    
    @Override
    public void add(Collidable c) {
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
    public void remove(Collidable c) {
        if (c == null)
            throw new NullPointerException("Body cannot be null");
        
        if (bodies.remove(c)) {
            collisionManager.remove(c);
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
        for (Collidable c: bodies) {
            collisionManager.remove(c);
            manager.add(c);
        }
        
        // update reference
        collisionManager = manager;
    }
    
    private class ContactManifoldCallback implements CollisionCallback {
        @Override
        public void process(Collidable objA, Collidable objB, CollisionHandler handler) {
            if (!(objA instanceof RigidBody) && !(objB instanceof RigidBody))
                return; // ignore collisions between only static objects
            ClosestPair pair = handler.getClosestPair(objA, objB);
            if (pair != null && pair.isIntersecting())
                contactCache.addContact(objA, objB, pair);
        }
    }
}
