package com.ferox.physics.collision;


public interface CollisionManager {
    public void add(Collidable collidable);
    
    public void remove(Collidable collidable);

    public void processCollisions(CollisionCallback callback);
    
    public void register(CollisionAlgorithm algorithm);
    
    public void unregister(CollisionAlgorithm algorithm);
    
    public void unregister(Class<? extends CollisionAlgorithm> type);
}
