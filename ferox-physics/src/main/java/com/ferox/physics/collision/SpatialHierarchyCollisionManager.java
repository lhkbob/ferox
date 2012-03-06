package com.ferox.physics.collision;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ferox.math.bounds.IntersectionCallback;
import com.ferox.math.bounds.Octree;
import com.ferox.math.bounds.Octree.Strategy;
import com.ferox.math.bounds.SpatialIndex;

/**
 * SpatialHierarchyCollisionManager is a CollisionManager implementation
 * that relies on the {@link SpatialIndex} implementations to determine
 * the set of potentially intersecting objects.
 * @author Michael Ludwig
 *
 */
public class SpatialHierarchyCollisionManager implements CollisionManager {
    private final SpatialIndex<Collidable> hierarchy;
    private final Map<Collidable, Key> hierarchyKeys;
    
    private volatile CollisionAlgorithmProvider handler;

    /**
     * Create a new SpatialHierarchyCollisionManager that uses a static
     * {@link Octree} with the default initial tree depth. It initially uses a
     * DefaultCollisionAlgorithmProvider.
     */
    public SpatialHierarchyCollisionManager() {
        this(new Octree<Collidable>(Strategy.STATIC));
    }

    /**
     * Create a new SpatialHierarchyCollisionManager that uses the provided
     * SpatialIndex. It initially uses a DefaultCollisionAlgorithmProvider.
     * The manager assumes ownership over the hierarchy, and the hierarchy
     * should not be used by the caller anymore (it should also be empty when
     * passed to this constructor).
     * 
     * @param hierarchy The SpatialIndex instance to use
     * @throws NullPointerException if hierarchy is null
     */
    public SpatialHierarchyCollisionManager(SpatialIndex<Collidable> hierarchy) {
        this(hierarchy, new DefaultCollisionAlgorithmProvider());
    }

    /**
     * Create a new SpatialHierarchyCollisionManager with the provided
     * SpatialIndex and CollisionAlgorithmProvider. The manager assumes
     * ownership over the hierarchy, and the hierarchy should not be used by the
     * caller anymore (it should also be empty when passed to this constructor).
     * 
     * @param hierarchy The SpatialIndex instance to use
     * @param handler The CollisionAlgorithProvider to use
     * @throws NullPointerException if hierarchy or handler are null
     */
    public SpatialHierarchyCollisionManager(SpatialIndex<Collidable> hierarchy, CollisionAlgorithmProvider handler) {
        if (hierarchy == null)
            throw new NullPointerException("SpatialIndex cannot be null");
        this.hierarchy = hierarchy;
        hierarchyKeys = new HashMap<Collidable, Key>();
        
        setCollisionHandler(handler);
    }
    
    @Override
    public CollisionAlgorithmProvider getCollisionHandler() {
        return handler;
    }

    @Override
    public void setCollisionHandler(CollisionAlgorithmProvider handler) {
        if (handler == null)
            throw new NullPointerException("CollisionAlgorithmProvider cannot be null");
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
