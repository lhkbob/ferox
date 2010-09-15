package com.ferox.physics.collision;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.IntersectionCallback;
import com.ferox.math.bounds.Octree;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.math.bounds.Octree.Strategy;
import com.ferox.util.Bag;

public class SpatialHierarchyCollisionManager extends AbstractCollisionManager {
    private final SpatialHierarchy<Collidable> hierarchy;
    private final Map<Collidable, KeyWithBounds> hierarchyKeys;
    
    public SpatialHierarchyCollisionManager() {
        this(new Octree<Collidable>(Strategy.STATIC));
    }
    
    public SpatialHierarchyCollisionManager(SpatialHierarchy<Collidable> hierarchy) {
        if (hierarchy == null)
            throw new NullPointerException("SpatialHierarchy cannot be null");
        this.hierarchy = hierarchy;
        hierarchyKeys = new HashMap<Collidable, KeyWithBounds>();
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
    public Bag<ClosestPair> getClosestPairs(Bag<ClosestPair> results) {
        synchronized(hierarchy) {
            if (results == null)
                results = new Bag<ClosestPair>();
            
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
            
            hierarchy.query(new ClosestPairIntersectionCallback(results));
            return results;
        }
    }
    
    private static class KeyWithBounds {
        Object key = null;
        final AxisAlignedBox bounds = new AxisAlignedBox();
    }
    
    private class ClosestPairIntersectionCallback implements IntersectionCallback<Collidable> {
        private final Bag<ClosestPair> intersections;
        
        public ClosestPairIntersectionCallback(Bag<ClosestPair> results) {
            intersections = results;
        }
        
        @Override
        public void process(Collidable item1, Collidable item2) {
            ClosestPair pair = getClosestPair(item1, item2);
            if (pair != null)
                intersections.add(pair);
        }
    }
}
