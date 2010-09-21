package com.ferox.physics.dynamics;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.CollisionManager;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.SpatialHierarchyCollisionManager;
import com.ferox.util.Bag;

public class PhysicsWorld implements CollisionManager {
    // FIXME: handle duplicate bodies
    private final Bag<RigidBody> bodies;
    private final CollisionManager delegate;

    private final Vector3f gravity;

    private final Bag<ClosestPair> collisions;
    private final Bag<Constraint> constraints;
    private final Bag<Constraint> singleStepConstraints;
    

    public PhysicsWorld() {
        this(new SpatialHierarchyCollisionManager());
    }

    public PhysicsWorld(CollisionManager delegate) {
        if (delegate == null)
            throw new NullPointerException("CollisionManager cannot be null");
        this.delegate = delegate;
        
        gravity = new Vector3f(0f, -9.82f, 0f);
        bodies = new Bag<RigidBody>();
        constraints = new Bag<Constraint>();
        singleStepConstraints = new Bag<Constraint>();
        collisions = new Bag<ClosestPair>();
    }

    public ReadOnlyVector3f getGravity() {
        return gravity;
    }

    public void setGravity(ReadOnlyVector3f gravity) {
        if (gravity == null)
            throw new NullPointerException("Gravity cannot be null");
        this.gravity.set(gravity);
    }

    public void add(Constraint constraint) {
        throw new UnsupportedOperationException();
    }

    public void remove(Constraint constraint) {
        throw new UnsupportedOperationException();
    }

    public void step(float dt) {
        // FIXME use time step interpolation to maintain a fixed time step rate like in bullet
        long now = System.currentTimeMillis();
        RigidBody b;
        Vector3f gravForce = new Vector3f();
        int ct = bodies.size();
        for (int i = 0; i < ct; i++) {
            b = bodies.get(i);
            
            // add gravity force to each body
            if (b.getExplicitGravity() != null)
                b.getExplicitGravity().scale(b.getMass(), gravForce);
            else
               gravity.scale(b.getMass(), gravForce);
            
            b.addForce(gravity, null); // gravity is a central force, applies no torque
            
            
            // predict unconstrained motion based on current velocities
            b.predictMotion(dt);
        }
        System.out.println("Predicted integration: " + (System.currentTimeMillis() - now));
        
        now = System.currentTimeMillis();
        collisions.clear();
        getClosestPairs(collisions);
        System.out.println("Collide time: " + (System.currentTimeMillis() - now) + " " + collisions.size());
        // FIXME: respond to collisions
        
        now = System.currentTimeMillis();
        for (int i = 0; i < ct; i++) {
            // recomput next frame position after constraint solving
            // and store it in the world transform
            b = bodies.get(i);
            b.predictMotion(dt);
            b.setWorldTransform(b.getPredictedTransform());
        }
        
        System.out.println("Advance step time: " + (System.currentTimeMillis() - now));
        
        // TODO algorithm
        // split into a number of substeps to do a fixed timestep simulation
        // for each substep:
        // 1. calculate velocity of kinematic objects controlled by user
        // 2. predict ideal motion based on gravity and current velocities
        // 3. calculate collisions
        // 4. solve all constraints in system, including collisions from #3,
        // split into islands
        // 5. update object positions for standard objects based on current
        // velocities
        // 6. clear accumulated forces on object
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
    public Bag<ClosestPair> getClosestPairs(Bag<ClosestPair> results) {
        return delegate.getClosestPairs(results);
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
}
