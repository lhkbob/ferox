package com.ferox.physics.dynamics;

import com.ferox.math.Vector3f;
import com.ferox.physics.collision.CollisionManager;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.util.Bag;

public class PhysicsWorld {
    private final Bag<RigidBody> bodies;
    
    private final Vector3f gravity;
    
    private final Bag<Constraint> constraints;
    private final Bag<Constraint> singleStepConstraints;
    
    private CollisionManager broadphase;
    
    public PhysicsWorld() {
        
    }
    
    public PhysicsWorld(CollisionManager broadphase, CollisionAlgorithm narrowphase) {
        
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
    
    public CollisionManager getBroadphaseManager() {
        
    }
    
    public void setBroadphaseManager(CollisionManager broadphase) {
        
    }
    
    public CollisionAlgorithm getCollisionDetector() {
        
    }
    
    public void setCollisionDetector(CollisionAlgorithm narrowphase) {
        
    }
    
    public void step(float dt) {
        
    }
}
