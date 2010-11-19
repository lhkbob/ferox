package com.ferox.physics.collision;

import com.ferox.physics.collision.algorithm.CollisionAlgorithm;
import com.ferox.physics.collision.shape.Shape;

public interface CollisionHandler {
    public <A extends Shape, B extends Shape> CollisionAlgorithm<A, B> getAlgorithm(Class<A> shapeA, Class<B> shapeB);
    
    public void register(CollisionAlgorithm<?, ?> algorithm);
    
    public void unregister(Class<? extends CollisionAlgorithm<?, ?>> algorithmType);
}
