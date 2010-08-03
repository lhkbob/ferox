package com.ferox.physics.dynamics;

import com.ferox.math.Vector3f;
import com.ferox.physics.collision.BroadphaseManager;
import com.ferox.physics.collision.PairDetector;
import com.ferox.util.Bag;

public class PhysicsWorld {
    private final Bag<RigidBody> bodies;
    
    private final Vector3f gravity;
    
    private final Bag<Constraint> constraints;
    private final Bag<Constraint> singleStepConstraints;
    
    private BroadphaseManager broadphase;
    private PairDetector narrowphase;
    
    public PhysicsWorld() {
        
    }
    
    public PhysicsWorld(BroadphaseManager broadphase, PairDetector narrowphase) {
        
    }
    
    public Vector3f getGravity() {
        
    }
    
    public void setGravity(Vector3f gravity) {
        
    }
    
    public void remove(RigidBody body) {
        
    }
    
    public void add(RigidBody body) {
        
    }
    
    public void add(Constraint constraint) {
        
    }
    
    public void remove(Constraint constraint) {
        
    }
    
    public BroadphaseManager getBroadphaseManager() {
        
    }
    
    public void setBroadphaseManager(BroadphaseManager broadphase) {
        
    }
    
    public PairDetector getCollisionDetector() {
        
    }
    
    public void setCollisionDetector(PairDetector narrowphase) {
        
    }
    
    public void step(float dt) {
        
    }
}
