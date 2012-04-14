package com.ferox.scene;

import java.util.Arrays;

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.annot.ElementSize;
import com.lhkbob.entreri.annot.Factory;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.property.ObjectProperty;
import com.lhkbob.entreri.property.PropertyFactory;

public abstract class EntitySetComponent<T extends EntitySetComponent<T>> extends ComponentData<T> {
    private static final int CACHE_SIZE = 6;
    private static final int CACHE_OFFSET = 2;
    private static final int SCALE = CACHE_SIZE + CACHE_OFFSET;
    
    @ElementSize(SCALE)
    private IntProperty firstCache; // 0 = 1st size, 1 = 2nd size, and 6 cached entity ids

    // entity ids that don't fit in firstCache, the length of the array is 
    // greater than or equal to the second cache size at offset 1
    @Factory(CloningFactory.class)
    private ObjectProperty<int[]> secondCache; 
    
    protected EntitySetComponent() { }
    
    protected boolean contains(int entityId) {
        final int[] ids = firstCache.getIndexedData();
        final int index = getIndex() * SCALE;
        
        int size = ids[index];
        if (contains(entityId, ids, index + CACHE_OFFSET, index + size + CACHE_OFFSET))
            return true;
        
        size = ids[index + 1];
        if (size > 0)
            return contains(entityId, secondCache.get(getIndex(), 0), 0, size);
        
        return false;
    }
    
    private boolean contains(int entityId, int[] array, int from, int to) {
        return Arrays.binarySearch(array, from, to, entityId) >= 0;
    }
    
    private int put(int entityId, int[] array, int cacheSize, int from, int to) {
        int maxSize = to - from;
        if (cacheSize == 0 || (cacheSize < maxSize - 1 && entityId > array[from + cacheSize - 1])) {
            // append the entity to the end, since there is room, and it will
            // remain in sorted order.
            // - since the size was 0, or it the entity was strictly greater than
            //   the previously greatest item we know it hasn't been seen before
            array[from + cacheSize] = entityId;
            return -1;
        }
        
        // search for the insert index into the array, to maintain sorted order
        int insertIndex = Arrays.binarySearch(array, from, from + cacheSize, entityId);
        if (insertIndex >= 0) {
            // the entity is already in this array
            return -1;
        }
        insertIndex = -insertIndex + 1; // convert to the actual index it should be
        
        if (insertIndex < to) {
            // the entity belongs within this array
            int evicted = -1;
            int shift = from + cacheSize;
            
            if (cacheSize == maxSize) {
                // in order to insert the new entity, the last entity gets evicted,
                // so we decrement the shift to prevent bleeding into outside indices,
                // and remember the last element to return
                evicted = array[--shift];
            }
            
            // we can safely shift and insert the entity
            for (int i = shift; i > insertIndex; i--)
                array[i] = array[i - 1];
            array[insertIndex] = entityId;

            return evicted;
        } else {
            // the array has no more room, so return the entity id to signal
            // it must be added to another array
            return entityId;
        }
    }
    
    protected void put(int entityId) {
        int[] ids = firstCache.getIndexedData();
        int index = getIndex() * SCALE;
        
        int secondInsert = put(entityId, ids, ids[index], index + CACHE_OFFSET, index + CACHE_OFFSET + CACHE_SIZE);
        if (secondInsert < 0) {
            // entityId was successfully inserted into the first cache,
            // so we have to increment the size
            ids[index]++;
        } else {
            // secondInsert must be added to the second cache, this might
            // be the new entity or an evicted entity
            int[] ids2 = secondCache.get(getIndex(), 0);
            int size = ids[index + 1];
            
            if (ids2 == null || size == ids2.length) {
                // expand the 2nd cache
                int[] newIds2 = new int[size + CACHE_SIZE];
                for (int i = 0; i < size; i++)
                    newIds2[i] = ids[i];
                ids2 = newIds2;
                secondCache.set(newIds2, getIndex(), 0);
            }
            
            if (put(secondInsert, ids2, size, 0, ids2.length) < 0) {
                // entity was added to the second cache, so update that size
                ids[index + 1]++;
            } // otherwise entity was in the 2nd cache already
        }
    }
    
    protected void remove(int entityId) {
        int index = getIndex() * SCALE;
        int[] ids = firstCache.getIndexedData();
        
        int firstSize = ids[index];
        if (remove(entityId, ids, index + CACHE_OFFSET, index + CACHE_OFFSET + firstSize)) {
            // entity was removed from the first cache, so the size must be updated
            // or an element moved from the second to the first if available
            if (ids[index + 1] > 0) {
                // transfer from second to first cache
                int[] second = secondCache.get(getIndex(), 0);
                ids[index + CACHE_OFFSET + CACHE_SIZE - 1] = second[0];
                // shift over remaining values
                for (int i = ids[index + 1] - 1; i > 0; i--)
                    ids[i - 1] = ids[i];
                ids[index + 1]--;
            } else {
                // decrease size of first cache
                ids[index]--;
            }
        } else {
            // entity might be in the second cache
            if (ids[index + 1] > 0) {
                if (remove(entityId, secondCache.get(getIndex(), 0), 0, ids[index + 1]))
                    ids[index + 1]--;
            }
        }
    }
    
    protected boolean remove(int entityId, int[] array, int from, int to) {
        int index = Arrays.binarySearch(array, from, to, entityId);
        if (index >= 0) {
            // found it in this array
            if (index < to - 1) {
                // shift over elements when not removing the last element
                for (int i = index; i < to; i++)
                    array[i] = array[i + 1];
            }
            return true;
        } else {
            // not in this array
            return false;
        }
    }
    
    protected void clear() {
        int index = getIndex() * SCALE;

        // reset cache counts to 0, and null the second cache
        firstCache.getIndexedData()[index] = 0;
        firstCache.getIndexedData()[index + 1] = 0;
        secondCache.set(null, index, 0);
    }

    /**
     * A PropertyFactory with copying semantics for the visibility set.
     */
    private static class CloningFactory implements PropertyFactory<ObjectProperty<int[]>> {
        @Override
        public ObjectProperty<int[]> create() {
            return new ObjectProperty<int[]>(1);
        }

        @Override
        public void setDefaultValue(ObjectProperty<int[]> property, int index) {
            property.set(null, index, 0);
        }

        @Override
        public void clone(ObjectProperty<int[]> src, int srcIndex, ObjectProperty<int[]> dst,
                          int dstIndex) {
            int[] original = src.get(srcIndex, 0);
            if (original != null)
                dst.set(Arrays.copyOf(original, original.length), dstIndex, 0);
            else
                dst.set(null, dstIndex, 0);
        }
    }
}
