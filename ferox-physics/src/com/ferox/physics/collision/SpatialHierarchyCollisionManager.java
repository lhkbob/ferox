package com.ferox.physics.collision;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.ferox.math.Vector3f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.IntersectionCallback;
import com.ferox.math.bounds.Octree;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.util.Bag;

public class SpatialHierarchyCollisionManager extends AbstractCollisionManager {
    private final SpatialHierarchy<Collidable> hierarchy;
    private final Map<Collidable, KeyWithBounds> hierarchyKeys;
    
    public SpatialHierarchyCollisionManager() {
        this(new Octree<Collidable>(new AxisAlignedBox(new Vector3f(-100f, -100f, -100f), new Vector3f(100f, 100f, 100f))));
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
        private final Set<Pair> testedPairs;
        
        private final Pair query;
        
        public ClosestPairIntersectionCallback(Bag<ClosestPair> results) {
            intersections = results;
            testedPairs = new HashSet<Pair>();
            query = new Pair(null, null); // invalid right now
        }
        
        @Override
        public void process(Collidable item1, Collidable item2) {
            if (item1.canCollide(item2)) {
                query.c1 = item1;
                query.c2 = item2;
                
                if (!testedPairs.contains(query)) {
                    // record pair, whether or not the narrowphase finds anything
                    testedPairs.add(new Pair(item1, item2));
                    
                    ClosestPair pair = getClosestPair(item1, item2);
                    if (pair != null)
                        intersections.add(pair);
                }
            }
        }
    }
    
    private static class Pair {
        Collidable c1, c2;
        
        public Pair(Collidable c1, Collidable c2) {
            this.c1 = c1;
            this.c2 = c2;
        }
        
        @Override
        public int hashCode() {
            return this.c1.hashCode() ^ this.c2.hashCode();
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pair))
                return false;
            Pair that = (Pair) o;
            return (that.c1 == c1 && that.c2 == c2) || 
                   (that.c2 == c1 && that.c1 == c2);
        }
    }
}
