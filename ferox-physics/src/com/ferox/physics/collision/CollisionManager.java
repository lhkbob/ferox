package com.ferox.physics.collision;


public interface CollisionManager {
    public void add(Collidable collidable);
    
    public void remove(Collidable collidable);

    public void processCollisions(CollisionCallback callback);
    
    public CollisionAlgorithm<Shape, Shape> getGeneralCollisionAlgorithm();
    
    public void setGeneralCollisionAlgorithm(CollisionAlgorithm<Shape, Shape> shape);
}
