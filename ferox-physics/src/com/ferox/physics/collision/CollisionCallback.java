package com.ferox.physics.collision;

public interface CollisionCallback {
    public void process(Collidable objA, Collidable objB, CollisionAlgorithm<Shape, Shape> algorithm);
}
