package com.ferox.physics.collision;


public interface PairDetector {
    public ClosestPair getClosestPair(Collidable objA, Collidable objB);
}
