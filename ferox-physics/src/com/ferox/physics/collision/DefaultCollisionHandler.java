package com.ferox.physics.collision;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ferox.physics.collision.algorithm.ClosestPair;
import com.ferox.physics.collision.algorithm.CollisionAlgorithm;
import com.ferox.physics.collision.algorithm.SwappingCollisionAlgorithm;
import com.ferox.physics.collision.shape.Shape;

public class DefaultCollisionHandler implements CollisionHandler {
    private final Map<TypePair, CollisionAlgorithm<?, ?>> algorithmCache;
    private final ThreadLocal<TypePair> lookup;
    
    private final List<CollisionAlgorithm<?, ?>> algorithms;
    private final ReentrantReadWriteLock lock;
    
    public DefaultCollisionHandler() {
        algorithms = new ArrayList<CollisionAlgorithm<?,?>>();
        lock = new ReentrantReadWriteLock();
        
        algorithmCache = new ConcurrentHashMap<DefaultCollisionHandler.TypePair, CollisionAlgorithm<?,?>>();
        lookup = new ThreadLocal<TypePair>();
    }
    
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ClosestPair getClosestPair(Collidable objA, Collidable objB) {
        CollisionAlgorithm algo = getAlgorithm(objA, objB);
        if (algo == null)
            return null; // no way to check for collisions
        else
            return algo.getClosestPair(objA.getShape(), objA.getWorldTransform(), 
                                       objB.getShape(), objB.getWorldTransform());
    }
    
    @Override
    public CollisionAlgorithm<?, ?> getAlgorithm(Collidable objA, Collidable objB) {
        if (objA == null || objB == null)
            throw new NullPointerException("Collidables cannot be null");
        
        lock.readLock().lock();
        try {
            TypePair key = lookup.get();
            key.shapeA = objA.getShape().getClass();
            key.shapeB = objB.getShape().getClass();
            
            CollisionAlgorithm<?, ?> algo = algorithmCache.get(key);
            if (algo == null) {
                // update the algorithm cache for this pair
                algo = getAlgorithm(key.shapeA, key.shapeB);
            }
            
            if (algo == null) {
                // if algorithm is still null, we don't support it right now
                return null;
            } else {
                // else we've found it some how so just return it
                return algo;
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private CollisionAlgorithm<?, ?> getAlgorithm(Class<? extends Shape> aType, Class<? extends Shape> bType) {
        // look for a swapped type
        TypePair key = new TypePair();
        key.shapeA = bType;
        key.shapeB = aType;

        CollisionAlgorithm algo = algorithmCache.get(key);
        if (algo != null) {
            // return a wrapped instance of the algorithm that swaps everything
            // and store it in the cache
            algo = new SwappingCollisionAlgorithm(algo);
            algorithmCache.put(key, algo);
            return algo;
        }
        
        // find best match with current ordering
        algo = getBestMatch(aType, bType);
        if (algo != null) {
            key.shapeA = aType;
            key.shapeB = bType;
            
            algorithmCache.put(key, algo);
            return algo;
        }
        
        // find best match with swapped ordering
        algo = getBestMatch(bType, aType);
        if (algo != null) {
            // store swapped algorithm
            algo = new SwappingCollisionAlgorithm(algo);
            key.shapeA = bType;
            key.shapeB = aType;
            
            algorithmCache.put(key, algo);
            return algo;
        }
        
        // nothing found
        return null;
    }
    
    private CollisionAlgorithm<?, ?> getBestMatch(Class<? extends Shape> aType, Class<? extends Shape> bType) {
        int bestDepth = 0;
        CollisionAlgorithm<?, ?> bestAlgo = null;
        
        int ct = algorithms.size();
        for (int i = 0; i < ct; i++) {
            CollisionAlgorithm<?, ?> algo = algorithms.get(i);
            if (algo.getShapeTypeA().isAssignableFrom(aType) && algo.getShapeTypeB().isAssignableFrom(bType)) {
                int depthA = depth(aType, algo.getShapeTypeA());
                int depthB = depth(bType, algo.getShapeTypeB());
                
                int depth = depthA * depthA + depthB * depthB; // this selects for more balanced depths
                if (depth == 0) {
                    // best match possible, return now
                    return algo;
                }
                
                if (bestAlgo == null || depth < bestDepth) {
                    // found a better match, store and continue iterating
                    bestAlgo = algo;
                    bestDepth = depth;
                }
            }
        }
        
        return bestAlgo;
    }
    
    private int depth(Class<?> child, Class<?> parent) {
        int depth = 0;
        while(!child.equals(parent) && parent.isAssignableFrom(child)) {
            // move up one in class hierarchy
            child = child.getSuperclass();
            depth++;
        }
        return depth;
    }

    @Override
    public void register(CollisionAlgorithm<?, ?> algorithm) {
        if (algorithm == null)
            throw new NullPointerException("CollisionAlgorithm cannot be null");
        
        lock.writeLock().lock();
        try {
            int ct = algorithms.size();
            for (int i = 0; i < ct; i++) {
                if (algorithms.get(i).getClass().equals(algorithm.getClass())) {
                    algorithms.remove(i);
                    break;
                }
            }
            
            // insert in front so that new algorithms are searched first
            algorithms.add(0, algorithm);
            algorithmCache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void unregister(Class<? extends CollisionAlgorithm<?, ?>> algorithmType) {
        if (algorithmType == null)
            throw new NullPointerException("CollisionAlgorithm type cannot be null");
        
        lock.writeLock().lock();
        try {
            int ct = algorithms.size();
            for (int i = 0; i < ct; i++) {
                if (algorithmType.equals(algorithms.get(i).getClass())) {
                    algorithms.remove(i);
                    algorithmCache.clear();
                    return;
                }
            }
            
            // else nothing to remove
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // Note that the ordering of the pair matters. The algorithm cache
    // inserts swapped algorithm versions as needed.
    private static class TypePair {
        Class<? extends Shape> shapeA;
        Class<? extends Shape> shapeB;
        
        @Override
        public int hashCode() {
            int result = 17;
            result = 37 * result + shapeA.hashCode();
            result = 37 * result + shapeB.hashCode();
            return result;
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TypePair))
                return false;
            TypePair that = (TypePair) o;
            return (shapeA.equals(that.shapeA) && shapeB.equals(that.shapeB));
        }
    }
}
