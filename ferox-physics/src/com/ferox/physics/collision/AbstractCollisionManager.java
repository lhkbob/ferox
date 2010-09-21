package com.ferox.physics.collision;

import java.util.ArrayList;
import java.util.List;

import com.ferox.physics.collision.narrow.GjkEpaCollisionAlgorithm;

public abstract class AbstractCollisionManager implements CollisionManager {
    private final List<CollisionAlgorithm> algorithms;
    
    public AbstractCollisionManager() {
        algorithms = new ArrayList<CollisionAlgorithm>();
        register(new GjkEpaCollisionAlgorithm());
    }

    @Override
    public void register(CollisionAlgorithm algorithm) {
        if (algorithm == null)
            throw new NullPointerException("CollisionAlgorithm cannot be null");
        
        Class<? extends CollisionAlgorithm> newType = algorithm.getClass();
        synchronized(algorithms) {
            int ct = algorithms.size();
            for (int i = 0; i < ct; i++) {
                if (algorithms.get(i).getClass().equals(newType)) {
                    // replace algorithm instance of same type instead of
                    // storing duplicates
                    algorithms.set(i, algorithm);
                    return;
                }
            }
            
            // no type found, so add the new algorithm
            algorithms.add(algorithm);
        }
    }
    
    @Override
    public void unregister(Class<? extends CollisionAlgorithm> type) {
        if (type == null)
            throw new NullPointerException("Type cannot be null");
        
        synchronized(algorithms) {
            int ct = algorithms.size();
            for (int i = 0; i < ct; i++) {
                if (algorithms.get(i).getClass().equals(type)) {
                    // found the algorithm of the proper type, 
                    // remove it and then return
                    algorithms.remove(i);
                    return;
                }
            }
        }
    }

    @Override
    public void unregister(CollisionAlgorithm algorithm) {
        if (algorithm == null)
            throw new NullPointerException("CollisionAlgirthm cannot be null");
        
        synchronized(algorithms) {
            algorithms.remove(algorithm);
        }
    }
    
    protected ClosestPair getClosestPair(Collidable objA, Collidable objB) {
        if (!objA.canCollide(objB))
            return null;
        
        synchronized(algorithms) {
            Shape sa = objA.getShape();
            Shape sb = objB.getShape();
            
            ClosestPair pair = null;
            CollisionAlgorithm algo;
            int ct = algorithms.size();
            for (int i = 0; i < ct; i++) {
                algo = algorithms.get(i);
                if (algo.isShapeSupported(sa) && algo.isShapeSupported(sb)) {
                    // this algorithm supports collisions between A and B
                    pair = algo.getClosestPair(objA, objB);
                    if (pair != null)
                        break;
                    // otherwise, maybe another algorithm can figure it out
                }
            }
            
            if (pair != null && pair.isIntersecting())
                return pair;
            return null;
        }
    }
}
