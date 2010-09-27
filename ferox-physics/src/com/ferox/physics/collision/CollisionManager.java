package com.ferox.physics.collision;

public interface CollisionManager {
    public void add(Collidable collidable);
    
    public void remove(Collidable collidable);

    public void processCollisions(CollisionCallback callback);
    
    public CollisionHandler getCollisionHandler();
    
    public void setCollisionHandler(CollisionHandler handler);
}
