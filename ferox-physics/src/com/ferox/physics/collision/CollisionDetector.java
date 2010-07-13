package com.ferox.physics.collision;


public interface CollisionDetector {
    public CollisionInfo getCollision(Collidable objA, Collidable objB);
}
