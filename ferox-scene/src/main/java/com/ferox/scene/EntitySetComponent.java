package com.ferox.scene;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.property.Factory;
import com.googlecode.entreri.property.IntProperty;
import com.googlecode.entreri.property.ObjectProperty;
import com.googlecode.entreri.property.Parameter;
import com.googlecode.entreri.property.PropertyFactory;

public abstract class EntitySetComponent extends Component {
    private static final int CACHE_SIZE = 9;
    
    @Parameter(type=int.class, value="9")
    private IntProperty firstCache; // 0 = size, 1-9 = up to 8 cached entity ids
    
    @Factory(CloningFactory.class)
    private ObjectProperty<Set<Integer>> secondCache; // entity ids that don't fit in firstCache
    
    protected EntitySetComponent(EntitySystem system, int index) {
        super(system, index);
    }
    
    @Override
    protected void init(Object... initParams) {
        // make sure count says 0, to begin with
        // we don't care about other indices
        firstCache.set(0, getIndex(), 0);
        
        secondCache.set(null, getIndex(), 0);
    }
    
    protected boolean contains(int entityId) {
        int index = getIndex();
        
        int[] ids = firstCache.getIndexedData();
        int size = ids[index * CACHE_SIZE];
        int discoveredIndex = Arrays.binarySearch(ids, index * CACHE_SIZE + 1, index * CACHE_SIZE + size + 1, entityId);
        if (discoveredIndex >= 0)
            return true;
        
        // not found in first cache, check second
        Set<Integer> objCache = secondCache.get(index, 0);
        return (objCache != null ? objCache.contains(entityId) : false);
    }
    
    protected void put(int entityId) {
        int index = getIndex();
        
        int[] ids = firstCache.getIndexedData();
        int size = ids[index * CACHE_SIZE];
        
        int discoveredIndex = Arrays.binarySearch(ids, index * CACHE_SIZE + 1, index * CACHE_SIZE + size + 1, entityId);
        if (discoveredIndex >= 0)
            return; // already found it
        
        if (size < CACHE_SIZE - 1) {
            // have room in the int cache, insert 
            int insertIndex = -discoveredIndex + 1;
            
            // shift over all other entity ids
            for (int i = index * CACHE_SIZE + size + 1; i > insertIndex; i--)
                ids[i] = ids[i - 1];
            ids[insertIndex] = entityId; // store in cache
            ids[index * CACHE_SIZE] = size + 1; // update size count
        } else {
            // must use secondary cache
            Set<Integer> objCache = secondCache.get(index, 0);
            if (objCache == null) {
                objCache = new HashSet<Integer>();
                secondCache.set(objCache, index, 0);
            }
            
            objCache.add(entityId);
        }
    }
    
    protected void remove(int entityId) {
        int index = getIndex();
        
        int[] ids = firstCache.getIndexedData();
        int size = ids[index * CACHE_SIZE];
        
        int discoveredIndex = Arrays.binarySearch(ids, index * CACHE_SIZE + 1, index * CACHE_SIZE + size + 1, entityId);
        if (discoveredIndex >= 0) {
            // found it in first cache, so remove it
            
            // shift over all other entity ids
            for (int i = discoveredIndex; i < index * CACHE_SIZE + size; i++)
                ids[i] = ids[i + 1];
            ids[index * CACHE_SIZE] = size - 1; // update size count
        } else {
            // check secondary cache
            Set<Integer> objCache = secondCache.get(index, 0);
            if (objCache != null)
                objCache.remove(entityId);
        }
    }
    
    protected void clear() {
        int index = getIndex();

        // reset first cache count to 0, and null the second cache
        firstCache.getIndexedData()[index * CACHE_SIZE] = 0;
        secondCache.set(null, index, 0);
    }

    /**
     * A PropertyFactory with copying semantics for the visibility set.
     */
    // FIXME: figure out a way to make this private
    public static class CloningFactory implements PropertyFactory<ObjectProperty<Set<Integer>>> {
        @Override
        public ObjectProperty<Set<Integer>> create() {
            return new ObjectProperty<Set<Integer>>(1);
        }

        @Override
        public void setValue(ObjectProperty<Set<Integer>> property, int index) {
            property.set(null, index, 0);
        }

        @Override
        public void clone(ObjectProperty<Set<Integer>> src, int srcIndex,
                          ObjectProperty<Set<Integer>> dst, int dstIndex) {
            Set<Integer> original = src.get(srcIndex, 0);
            if (original != null)
                dst.set(new HashSet<Integer>(original), dstIndex, 0);
            else
                dst.set(null, dstIndex, 0);
        }
    }
}
