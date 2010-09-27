package com.ferox.physics.collision;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.IntersectionCallback;
import com.ferox.math.bounds.Octree;
import com.ferox.math.bounds.Octree.Strategy;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.physics.collision.algorithm.GeneralCollisionAlgorithm;

public class SpatialHierarchyCollisionManager implements CollisionManager {
    private final SpatialHierarchy<Collidable> hierarchy;
    private final Map<Collidable, KeyWithBounds> hierarchyKeys;
    
    private volatile CollisionAlgorithm<Shape, Shape> algorithm;
    
    public SpatialHierarchyCollisionManager() {
        this(new Octree<Collidable>(Strategy.STATIC));
    }
    
    public SpatialHierarchyCollisionManager(SpatialHierarchy<Collidable> hierarchy) {
        this(hierarchy, new GeneralCollisionAlgorithm());
    }
    
    public SpatialHierarchyCollisionManager(SpatialHierarchy<Collidable> hierarchy, CollisionAlgorithm<Shape, Shape> algorithm) {
        if (hierarchy == null)
            throw new NullPointerException("SpatialHierarchy cannot be null");
        this.hierarchy = hierarchy;
        hierarchyKeys = new HashMap<Collidable, KeyWithBounds>();
        
        setGeneralCollisionAlgorithm(algorithm);
    }
    
    @Override
    public CollisionAlgorithm<Shape, Shape> getGeneralCollisionAlgorithm() {
        return algorithm;
    }

    @Override
    public void setGeneralCollisionAlgorithm(CollisionAlgorithm<Shape, Shape> shape) {
        if (shape == null)
            throw new NullPointerException("General CollisionAlgorithm cannot be null");
        algorithm = shape;
    }
    
    @Override
    public void add(Collidable collidable) {
        synchronized(hierarchy) {
            if (hierarchyKeys.containsKey(collidable))
                return; // don't re-add
            
            // don't add to the hierarchy yet, that's done during getClosestPairs()
            hierarchyKeys.put(collidable, new KeyWithBounds());
        }
    }

    @Override
    public void remove(Collidable collidable) {
        synchronized(hierarchy) {
            KeyWithBounds key = hierarchyKeys.remove(collidable);
            if (key != null && key.key != null)
                hierarchy.remove(collidable, key.key);
        }
    }
    
    @Override
    public void processCollisions(CollisionCallback callback) {
        synchronized(hierarchy) {
            if (callback == null)
                throw new NullPointerException("Callback cannot be null");
            
            // update every known collidable within the hierarchy
            KeyWithBounds key;
            Collidable shape;
            for (Entry<Collidable, KeyWithBounds> c: hierarchyKeys.entrySet()) {
                shape = c.getKey();
                key = c.getValue();
                
                // compute world bounds
                shape.getShape().getBounds().transform(shape.getWorldTransform(), key.bounds);
                if (key.key == null)
                    key.key = hierarchy.add(shape, key.bounds);
                else
                    hierarchy.update(shape, key.bounds, key.key);
            }
            
            hierarchy.query(new CollisionIntersectionCallback(callback));
        }
    }
    
    private static class KeyWithBounds {
        Object key = null;
        final AxisAlignedBox bounds = new AxisAlignedBox();
    }
    
    private class CollisionIntersectionCallback implements IntersectionCallback<Collidable> {
        private final CollisionCallback delegate;
        
        public CollisionIntersectionCallback(CollisionCallback callback) {
            delegate = callback;
        }
        
        @Override
        public void process(Collidable item1, Collidable item2) {
            delegate.process(item1, item2, algorithm);
        }
    }
}
