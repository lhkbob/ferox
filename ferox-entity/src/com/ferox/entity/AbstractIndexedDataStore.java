package com.ferox.entity;

import java.nio.Buffer;
import java.util.List;

/**
 * AbstractIndexedDataStore is an implementation of IndexedDataStore that uses
 * an resizable array to hold the packed property values of the store. It
 * implements the vast majority of the logic needed for an IndexedDataStore, and
 * concrete classes are only required to create and store the arrays.
 * 
 * @author Michael Ludwig
 * @param <A> The type of array backing this data store
 */
public abstract class AbstractIndexedDataStore<A> implements IndexedDataStore {
    protected final int elementSize;
    private A swap;

    /**
     * Create an AbstractIndexedDataStore that will use <var>elementSize</var>
     * array elements per Component in the data store. This does not create a
     * backing array, so concrete classes must allocate an initial array.
     * 
     * @param elementSize The number of array elements per property instance
     * @throws IllegalArgumentException if elementSize is less than 1
     */
    public AbstractIndexedDataStore(int elementSize) {
        if (elementSize < 1)
            throw new IllegalArgumentException("Element size must be at least 1");
        this.elementSize = elementSize;
    }
    
    @Override
    public void resize(int size) {
        // Create a new array
        A newArray = createArray(size * elementSize);
        A oldArray = getArray();
        
        // Copy the contents of the old array to the new
        // and update the assigned reference
        arraycopy(oldArray, 0, newArray, 0, Math.min(size * elementSize, getArrayLength(oldArray)));
        setArray(newArray);
        
        // To prevent the swap from getting too big or when
        // it's not used, just reset it here (allocation is cheap)
        swap = null;
    }

    @Override
    public int size() {
        return getArrayLength(getArray()) / elementSize;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void copy(int srcOffset, int len, IndexedDataStore dest, int destOffset) {
        if (dest == null)
            throw new NullPointerException("Destination store cannot be null");
        if (!(getClass().isInstance(dest)))
            throw new IllegalArgumentException("Destination store not compatible with this store, wrong type: " + dest.getClass());
        
        AbstractIndexedDataStore<A> dstStore = (AbstractIndexedDataStore<A>) dest;
        if (dstStore.elementSize != elementSize)
            throw new IllegalArgumentException("Destination store not compatible with this store, wrong element size: " + dstStore.elementSize);
        
        arraycopy(getArray(), srcOffset * elementSize, dstStore.getArray(), destOffset * elementSize, len * elementSize);
    }

    @Override
    public void update(Component[] newToOldMap, int from, int to) {
        int swapSize = newToOldMap.length * elementSize;
        
        if (swap == null || getArrayLength(swap) < swapSize)
            swap = createArray(newToOldMap.length * elementSize);
        
        A oldArray = getArray();
        
        int lastIndex = -1;
        int copyIndexNew = -1;
        int copyIndexOld = -1;
        for (int i = from; i < to; i++) {
            if (newToOldMap[i].index != lastIndex + 1) {
                // we are not in a contiguous section
                if (copyIndexOld >= 0) {
                    // we have to copy over the last section
                    arraycopy(oldArray, copyIndexOld * elementSize, swap, copyIndexNew * elementSize, (i - copyIndexNew) * elementSize);
                }
                
                // set the copy indices
                copyIndexNew = i;
                copyIndexOld = newToOldMap[i].index;
            }
            lastIndex = newToOldMap[i].index;
        }
        
        if (copyIndexOld >= 0) {
            // final copy
            arraycopy(oldArray, copyIndexOld * elementSize, swap, copyIndexNew * elementSize, (to - copyIndexNew) * elementSize);
        }

        setArray(swap);
        swap = oldArray;
    }

    /**
     * <p>
     * Copy <tt>len</tt> elements of <tt>oldArray</tt> starting at
     * <tt>srcOffset</tt> into <tt>newArray</tt> at <tt>dstOffset</tt>. The
     * default implementation uses
     * {@link System#arraycopy(Object, int, Object, int, int)}, which is
     * suitable unless the backing data types are not primitive Java arrays.
     * </p>
     * <p>
     * This can be overridden if the backing data is some other type, such as a
     * {@link List} or {@link Buffer}, in which case the "array copy" can be
     * simulated in this method.
     * </p>
     * 
     * @param oldArray The source array
     * @param srcOffset The element offset into the source array
     * @param newArray The destination array where data is copied
     * @param dstOffset The element offset into the new array
     * @param len The number of array elements to copy
     */
    protected void arraycopy(A oldArray, int srcOffset, A newArray, int dstOffset, int len) {
        System.arraycopy(oldArray, srcOffset, newArray, dstOffset, len);
    }
    
    /**
     * Create an "array" of whatever type is used by the subclass, ensuring that
     * it has the given array length, <tt>arraySize</tt>.
     * 
     * @param arraySize The array size of the created array
     * @return A new array of a specific type with the given length
     */
    protected abstract A createArray(int arraySize);

    /**
     * Update the data store so that its exposed data (in whatever manner it
     * chooses) uses the array provided. The array object will have been created
     * by {@link #createArray(int)} or returned by {@link #getArray()} so casts
     * are safe.
     * 
     * @param array The new array to expose or use for property data
     */
    protected abstract void setArray(A array);
    
    /**
     * @return The current array instance storing property data
     */
    protected abstract A getArray();

    /**
     * Compute the array length of the given array. The array object will have
     * been created by {@link #createArray(int)} or returned by
     * {@link #getArray()} so casts are safe.
     * 
     * @param array The array whose length is computed
     * @return The array length
     */
    protected abstract int getArrayLength(A array);
}
