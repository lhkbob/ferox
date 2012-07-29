package com.ferox.physics.controller;

import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionBody;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.SimpleController;

public abstract class CollisionController extends SimpleController {
// FIXME implement contact manifold logic so that it can easily be 
    // shared by other collision controllers
    
    @Override
    public void init(EntitySystem system) {
        super.init(system);
    }
    
    @Override
    public void preProcess(double dt) {
        // reset constraint pools
    }
    
    protected void reportConstraints() {
        
    }
    
    
    protected void notifyContact(CollisionBody bodyA, CollisionBody bodyB, ClosestPair contact) {
        
    }
}
