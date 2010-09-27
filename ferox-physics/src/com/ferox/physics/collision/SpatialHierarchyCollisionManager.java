package com.ferox.physics.collision;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ferox.math.bounds.IntersectionCallback;
import com.ferox.math.bounds.Octree;
import com.ferox.math.bounds.Octree.Strategy;
import com.ferox.math.bounds.SpatialHierarchy;

public class SpatialHierarchyCollisionManager implements CollisionManager {
    private final SpatialHierarchy<Collidable> hierarchy;
    private final Map<Collidable, Key> hierarchyKeys;
    
    private volatile CollisionHandler handler;
    
    public SpatialHierarchyCollisionManager() {
        this(new Octree<Collidable>(Strategy.STATIC));
    }
    
    public SpatialHierarchyCollisionManager(SpatialHierarchy<Collidable> hierarchy) {
        this(hierarchy, new DefaultCollisionHandler());
    }
    
    public SpatialHierarchyCollisionManager(SpatialHierarchy<Collidable> hierarchy, CollisionHandler handler) {
        if (hierarchy == null)
            throw new NullPointerException("SpatialHierarchy cannot be null");
        this.hierarchy = hierarchy;
        hierarchyKeys = new HashMap<Collidable, Key>();
        
        setCollisionHandler(handler);
    }
    
    @Override
    public CollisionHandler getCollisionHandler() {
        return handler;
    }

    @Override
    public void setCollisionHandler(CollisionHandler handler) {
        if (handler == null)
            throw new NullPointerException("CollisionHandler cannot be null");
        this.handler = handler;
    }
    
    @Override
    public void add(Collidable collidable) {
        synchronized(hierarchy) {
            if (hierarchyKeys.containsKey(collidable))
                return; // don't re-add
            
            // don't add to the hierarchy yet, that's done during getClosestPairs()
            hierarchyKeys.put(collidable, new Key());
        }
    }

    @Override
    public void remove(Collidable collidable) {
        synchronized(hierarchy) {
            Key key = hierarchyKeys.remove(collidable);
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
            Key key;
            Collidable shape;
            for (Entry<Collidable, Key> c: hierarchyKeys.entrySet()) {
                shape = c.getKey();
                key = c.getValue();
                
                // compute world bounds
                if (key.key == null)
                    key.key = hierarchy.add(shape, shape.getWorldBounds());
                else
                    hierarchy.update(shape, shape.getWorldBounds(), key.key);
            }
            
            hierarchy.query(new CollisionIntersectionCallback(callback));
        }
    }
    
    private static class Key {
        // we only have this wrapper so we can track when something
        // is in the hierarchy or not
        Object key = null;
    }
    
    private class CollisionIntersectionCallback implements IntersectionCallback<Collidable> {
        private final CollisionCallback delegate;
        
        public CollisionIntersectionCallback(CollisionCallback callback) {
            delegate = callback;
        }
        
        @Override
        public void process(Collidable item1, Collidable item2) {
            delegate.process(item1, item2, handler);
        }
    }
}
