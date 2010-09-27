package com.ferox.physics.dynamics;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.CollisionCallback;
import com.ferox.physics.collision.CollisionManager;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.SpatialHierarchyCollisionManager;
import com.ferox.util.Bag;

public class PhysicsWorld implements CollisionManager {
    // FIXME: handle duplicate bodies
    private final Bag<RigidBody> bodies;
    private final CollisionManager delegate;

    private final Vector3f gravity;

    private final Bag<LinearNormalConstraint> linearNormalConstraints;
    private final Bag<LinearNormalConstraint> singleStepConstraints;
    
    private final ConstraintSolver constraintSolver;
    

    public PhysicsWorld() {
        this(new SpatialHierarchyCollisionManager());
    }
    
    public PhysicsWorld(CollisionManager delegate) {
        this(delegate, new SequentialImpulseConstraintSolver());
    }

    public PhysicsWorld(CollisionManager delegate, ConstraintSolver solver) {
        if (delegate == null)
            throw new NullPointerException("CollisionManager cannot be null");
        if (solver == null)
            throw new NullPointerException("ConstraintSolver cannot be null");
        this.delegate = delegate;
        constraintSolver = solver;
        
        gravity = new Vector3f(0f, -9.82f, 0f);
        bodies = new Bag<RigidBody>();
        linearNormalConstraints = new Bag<LinearNormalConstraint>();
        singleStepConstraints = new Bag<LinearNormalConstraint>();
    }

    public ReadOnlyVector3f getGravity() {
        return gravity;
    }

    public void setGravity(ReadOnlyVector3f gravity) {
        if (gravity == null)
            throw new NullPointerException("Gravity cannot be null");
        this.gravity.set(gravity);
    }

    public void add(LinearNormalConstraint linearNormalConstraint) {
        throw new UnsupportedOperationException();
    }

    public void remove(LinearNormalConstraint linearNormalConstraint) {
        throw new UnsupportedOperationException();
    }

    public void step(float dt) {
        // FIXME use time step interpolation to maintain a fixed time step rate like in bullet
        RigidBody b;
        Vector3f gravForce = new Vector3f();
        int ct = bodies.size();
        for (int i = 0; i < ct; i++) {
            b = bodies.get(i);
            
            if (!b.isKinematic()) { // && b.isActive()
                // add gravity force to each body
                if (b.getExplicitGravity() != null)
                    b.getExplicitGravity().scale(b.getMass(), gravForce);
                else
                    gravity.scale(b.getMass(), gravForce);

                b.addForce(gravity, null); // gravity is a central force, applies no torque
                b.addForce(new Vector3f((float) Math.random() * 2 - 1, (float) Math.random() * 2 - 1, (float) Math.random() * 2 - 1),
                           new Vector3f((float) Math.random() * 2 - 1, (float) Math.random() * 2 - 1, (float) Math.random() * 2 - 1));
            }
            
            // predict unconstrained motion based on current velocities
            b.predictMotion(dt);
        }
        
        linearNormalConstraints.clear(true);
        processCollisions(new ContactManifoldCallback());
        constraintSolver.solve(linearNormalConstraints);
        
        for (int i = 0; i < ct; i++) {
            // recompute next frame position after constraint solving
            // and store it in the world transform
            b = bodies.get(i);
            
            if (!b.isKinematic()) { // && b.isActive()
                b.applyDeltaImpulse();
                b.predictMotion(dt);
                b.setWorldTransform(b.getPredictedTransform());
            }
        }
    }
    
    @Override
    public void add(Collidable collidable) {
        delegate.add(collidable);
        if (collidable instanceof RigidBody) {
            // track rigid bodies separately
            bodies.add((RigidBody) collidable);
        }
    }

    @Override
    public void remove(Collidable collidable) {
        delegate.remove(collidable);
        if (collidable instanceof RigidBody) {
            // have to also remove it here
            bodies.remove(collidable);
        }
    }

    @Override
    public void processCollisions(CollisionCallback callback) {
        delegate.processCollisions(callback);
    }

    @Override
    public void register(CollisionAlgorithm algorithm) {
        delegate.register(algorithm);
    }

    @Override
    public void unregister(CollisionAlgorithm algorithm) {
        delegate.unregister(algorithm);
    }

    @Override
    public void unregister(Class<? extends CollisionAlgorithm> type) {
        delegate.unregister(type);
    }
    
    private class ContactManifoldCallback implements CollisionCallback {

        @Override
        public void process(Collidable objA, Collidable objB, CollisionAlgorithm algo) {
            // we really only care about collisions between rigid bodies,
            // collisions with just collidables are static objects and should be ignored
            if (objA instanceof RigidBody && objB instanceof RigidBody)
                process((RigidBody) objA, (RigidBody) objB, algo);
            else if (objA instanceof RigidBody)
                process((RigidBody) objA, objB, algo);
            else if (objB instanceof RigidBody)
                process((RigidBody) objB, objA, algo);
            // else collision between 2 static objects, so ignore them
        }
        
        private void process(RigidBody b1, RigidBody b2, CollisionAlgorithm algo) {
            // handle collision between two moving rigid bodies
            // FIXME: check whether or not rigid bodies are both inactive
            // otherwise wake up bodies
            
            ClosestPair p = algo.getClosestPair(b1, b2);
            if (p == null || !p.isIntersecting())
                return;
        }
        
        private void process(RigidBody b1, Collidable b2, CollisionAlgorithm algo) {
            // handle collision between a rigid body and a static object
            // FIXME: if b1 is inactive, return
            
            ClosestPair p = algo.getClosestPair(b1, b2);
            if (p == null || !p.isIntersecting())
                return;
        }
    }
}
