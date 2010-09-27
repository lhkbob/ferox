package com.ferox.physics.collision;

import com.ferox.physics.collision.algorithm.ClosestPair;
import com.ferox.physics.collision.algorithm.CollisionAlgorithm;

public interface CollisionHandler {
    public ClosestPair getClosestPair(Collidable objA, Collidable objB);
    
    public void register(CollisionAlgorithm<?, ?> algorithm);
    
    public void unregister(Class<? extends CollisionAlgorithm<?, ?>> algorithmType);
}
