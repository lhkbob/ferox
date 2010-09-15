package com.ferox.physics.collision;

import com.ferox.util.Bag;

public interface CollisionManager {
    public void add(Collidable collidable);
    
    public void remove(Collidable collidable);
    
    // FIXME: do we instead want to have collision listeners? that could veto/approve of the
    // collision? could they be at the manager level, or the collidable level?
    // - could use this to report collisions to the collision handler?
    // - or could be used to have sfx, etc. for scripted collision response
    // - or just expose this bag so that others can look up collisions as desired
    // - or use a callback system like with the spatial hierarchy?
    public Bag<ClosestPair> getClosestPairs(Bag<ClosestPair> results);
    
    public void register(CollisionAlgorithm algorithm);
    
    public void unregister(CollisionAlgorithm algorithm);
    
    public void unregister(Class<? extends CollisionAlgorithm> type);
}
