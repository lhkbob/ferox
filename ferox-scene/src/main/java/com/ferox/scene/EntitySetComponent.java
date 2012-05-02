package com.ferox.scene;

import java.util.Arrays;
import java.util.Set;

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Factory;
import com.lhkbob.entreri.PropertyFactory;
import com.lhkbob.entreri.property.ElementSize;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.property.ObjectProperty;

/**
 * EntitySetComponent is an abstract component that provides protected methods
 * to store a set of entities by their ids. Internally it stores the set as a
 * sorted array so that contains queries can be done in O(log n) time with a
 * binary search. Updates and removes are more expensive, but this structure
 * allows small sets (size under 6) to be completely packed into a property and
 * avoids object allocation (unlike {@link Set} implementations).
 * 
 * @author Michael Ludwig
 * @param <T>
 */
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
    
    /**
     * @return The number of unique entities within this set
     */
    protected int sizeInternal() {
        int[] ids = firstCache.getIndexedData();
        int index = getIndex() * SCALE;
        return ids[index] + ids[index + 1];
    }
    
    /**
     * Get the entity id of for the <tt>setIndex</tt>'th element in this set.
     * The index should be between 0 and ({@link #sizeInternal()} - 1). This can
     * be used to iterate over the entity ids within this set. The ids returned
     * will be sorted in ascending order.
     * 
     * @param setIndex The index into the logical set
     * @return The entity id stored at the given index
     * @throws IndexOutOfBoundsException if index is not between 0 and size - 1
     */
    protected int getInternal(int setIndex) {
        if (setIndex < 0)
            throw new IndexOutOfBoundsException("Index cannot be less than 0, but was: " + setIndex);
        
        int[] ids = firstCache.getIndexedData();
        int baseIndex = getIndex() * SCALE;
        
        int firstSize = ids[baseIndex];
        if (setIndex < firstSize) {
            // read value from the first index
            return ids[baseIndex + CACHE_OFFSET + setIndex];
        }
        
        int secondSize = ids[baseIndex + 1];
        int secondSetIndex = setIndex - CACHE_SIZE;
        if (secondSize > 0 && secondSetIndex < secondSize) {
            // second set exists and the index is in range
            return secondCache.get(getIndex(), 0)[secondSetIndex];
        }
        
        // otherwise index is out of bounds
        throw new IndexOutOfBoundsException("Index must be less than " + (firstSize + secondSize) + ", but was: " + setIndex);
    }
    
    /**
     * Return true if the entity with id <tt>entitId</tt> is in this set.
     * 
     * @param entityId The entity in question
     * @return True if the entity was previously added to this set
     */
    protected boolean containsInternal(int entityId) {
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
    
    /*
     * Returns -1 if added successfully.
     * Returns MIN_VALUE if already in the array.
     * Returns a positive value if that value was evicted, or if the input id 
     *    was not added.
     */
    private int add(int entityId, int[] array, int cacheSize, int from, int to) {
        int maxSize = to - from;
        if (cacheSize == 0 || (cacheSize < maxSize - 1 && entityId > array[from + cacheSize - 1])) {
            // append the entity to the end, since there is room, and it will
            // remain in sorted order.
            // - since the size was 0, or the entity was strictly greater than
            //   the previously greatest item we know it hasn't been seen before
            array[from + cacheSize] = entityId;
            return -1;
        }
        
        // search for the insert index into the array, to maintain sorted order
        int insertIndex = Arrays.binarySearch(array, from, from + cacheSize, entityId);
        if (insertIndex >= 0) {
            // the entity is already in this array
            return Integer.MIN_VALUE;
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
    
    /**
     * Add the entity with id <tt>entityId</tt> to this set. If the entity is
     * already within the set, then the set is not modified.
     * 
     * @param entityId
     * @return True if the set was modified
     */
    protected boolean addInternal(int entityId) {
        int[] ids = firstCache.getIndexedData();
        int index = getIndex() * SCALE;
        
        int evicted = add(entityId, ids, ids[index], index + CACHE_OFFSET, index + CACHE_OFFSET + CACHE_SIZE);
        if (evicted < 0) {
            // entityId was successfully inserted into the first cache,
            // or that it is already in the set
            if (evicted == Integer.MIN_VALUE) {
                // already present
                return false;
            } else {
                // added, so increase the size
                ids[index]++;
                return true;
            }
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
            
            evicted = add(evicted, ids2, size, 0, ids2.length);
            if (evicted < 0) {
                if (evicted == Integer.MIN_VALUE) {
                    // already in second set
                    return false;
                } else {
                    // update size
                    ids[index + 1]++;
                    return true;
                }
            } else {
                // should not happen with the second cache so it is sized to
                // always have enough room above
                throw new IllegalStateException("Set corrupted, should not happen");
            }
        }
    }
    
    /**
     * Remove the entity with id <tt>entitId</tt> from this set. This does
     * nothing if the entity was not already in the set.
     * 
     * @param entityId The entity to remove
     * @return True if the set was modified
     */
    protected boolean removeInternal(int entityId) {
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
                // shift over remaining values in second cache
                for (int i = ids[index + 1] - 1; i > 0; i--)
                    second[i - 1] = second[i];
                ids[index + 1]--; // decrease size of second cache
            } else {
                // decrease size of first cache
                ids[index]--;
            }
            
            return true;
        } else {
            // entity might be in the second cache
            if (ids[index + 1] > 0) {
                if (remove(entityId, secondCache.get(getIndex(), 0), 0, ids[index + 1])) {
                    // entity was in the second cache so update the size
                    ids[index + 1]--;
                    return true;
                }
            }
        }
        
        // not removed
        return false;
    }
    
    private boolean remove(int entityId, int[] array, int from, int to) {
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
    
    /**
     * Clear all entities from this set and reset its size back to 0.
     */
    protected void clearInternal() {
        int index = getIndex() * SCALE;

        // reset cache counts to 0, and null the second cache
        firstCache.getIndexedData()[index] = 0;
        firstCache.getIndexedData()[index + 1] = 0;
        secondCache.set(null, getIndex(), 0);
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
