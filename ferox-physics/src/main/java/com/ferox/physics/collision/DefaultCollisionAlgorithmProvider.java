package com.ferox.physics.collision;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ferox.physics.collision.algorithm.GjkEpaCollisionAlgorithm;
import com.ferox.physics.collision.algorithm.SphereSphereCollisionAlgorithm;
import com.ferox.physics.collision.algorithm.SwappingCollisionAlgorithm;

/**
 * DefaultCollisionAlgorithmProvider is the default implementation of
 * CollisionAlgorithmProvider. It is thread safe and supports efficient lookups
 * of algorithms based on shape type pairs. It caches the algorithm selection so
 * that class hierarchy matching is not required after an algorithm has been
 * found for a pair type. Additionally, it uses the
 * {@link SwappingCollisionAlgorithm} to support extra type pairs.
 * 
 * @author Michael Ludwig
 */
public class DefaultCollisionAlgorithmProvider implements CollisionAlgorithmProvider {
    private final Map<TypePair, CollisionAlgorithm<?, ?>> algorithmCache;
    private final ThreadLocal<TypePair> lookup;
    
    private final List<CollisionAlgorithm<?, ?>> algorithms;
    private final ReentrantReadWriteLock lock;

    /**
     * Create a new DefaultCollisionAlgorithmProvider. It initially has a
     * {@link GjkEpaCollisionAlgorithm} and a
     * {@link SphereSphereCollisionAlgorithm} registered.
     */
    public DefaultCollisionAlgorithmProvider() {
        algorithms = new ArrayList<CollisionAlgorithm<?,?>>();
        lock = new ReentrantReadWriteLock();
        
        algorithmCache = new ConcurrentHashMap<TypePair, CollisionAlgorithm<?,?>>();
        lookup = new ThreadLocal<TypePair>();
        
        register(new GjkEpaCollisionAlgorithm());
        register(new SphereSphereCollisionAlgorithm());
    }

    @Override
    public <A extends Shape, B extends Shape> CollisionAlgorithm<A, B> getAlgorithm(Class<A> shapeA, Class<B> shapeB) {
        if (shapeA == null || shapeB == null)
            throw new NullPointerException("Shape types cannot be null");
        
        lock.readLock().lock();
        try {
            return getAlgorithmImpl(shapeA, shapeB);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @SuppressWarnings("unchecked")
    private <A extends Shape, B extends Shape> CollisionAlgorithm<A, B> getAlgorithmImpl(Class<A> aType, Class<B> bType) {
        // look for type with current order
        TypePair key = lookup.get();
        if (key == null) {
            key = new TypePair(aType, bType);
            lookup.set(key);
        }
        
        key.shapeA = aType;
        key.shapeB = bType;
        
        CollisionAlgorithm<A, B> algo = (CollisionAlgorithm<A, B>) algorithmCache.get(key);
        if (algo != null)
            return algo;
        
        // didn't find original type, look for a swapped type
        key = new TypePair(bType, aType);
        CollisionAlgorithm<B, A> swapped = (CollisionAlgorithm<B, A>) algorithmCache.get(key);
        
        // re-swap key parameters
        key.shapeA = aType;
        key.shapeB = bType;
        
        if (swapped != null) {
            // return a wrapped instance of the algorithm that swaps everything
            // and store it in the cache
            algo = new SwappingCollisionAlgorithm<A, B>(swapped);
            algorithmCache.put(key, algo);
            return algo;
        }
        
        // find best match with current ordering
        algo = getBestMatch(aType, bType);
        if (algo != null) {
            algorithmCache.put(key, algo);
            return algo;
        }
        
        // find best match with swapped ordering
        swapped = getBestMatch(bType, aType);
        if (swapped != null) {
            // store original algorithm so we don't get swaps of swaps later on
            TypePair orig = new TypePair(bType, aType);
            algorithmCache.put(orig, algo);
            
            // store swapped algorithm
            algo = new SwappingCollisionAlgorithm<A, B>(swapped);
            algorithmCache.put(key, algo);
            return algo;
        }
        
        // nothing found
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private <A extends Shape, B extends Shape> CollisionAlgorithm<A, B> getBestMatch(Class<A> aType, Class<B> bType) {
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
                    return (CollisionAlgorithm<A, B>) algo;
                }
                
                if (bestAlgo == null || depth < bestDepth) {
                    // found a better match, store and continue iterating
                    bestAlgo = algo;
                    bestDepth = depth;
                }
            }
        }
        
        return (CollisionAlgorithm<A, B>) bestAlgo;
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
        
        public TypePair(Class<? extends Shape> shapeA, Class<? extends Shape> shapeB) {
            this.shapeA = shapeA;
            this.shapeB = shapeB;
        }
        
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
        
        @Override
        public String toString() {
            return "Pair(" + shapeA.getSimpleName() + ", " + shapeB.getSimpleName() + ")";
        }
    }
}
