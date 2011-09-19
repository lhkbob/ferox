package com.ferox.entity2;


public abstract class AbstractIndexedDataStore implements IndexedDataStore {
    protected final int elementSize;
    private Object swap;
    
    public AbstractIndexedDataStore(int elementSize) {
        if (elementSize < 1)
            throw new IllegalArgumentException("Element size must be at least 1");
        this.elementSize = elementSize;
    }
    
    @Override
    public void resize(int size) {
        Object newArray = createArray(size * elementSize);
        Object oldArray = getArray();
        
        System.arraycopy(oldArray, 0, newArray, 0, Math.min(size * elementSize, getArrayLength(oldArray)));
        setArray(newArray);
    }

    @Override
    public int size() {
        return getArrayLength(getArray()) / elementSize;
    }

    @Override
    public void copy(int srcOffset, int len, IndexedDataStore dest, int destOffset) {
        if (dest == null)
            throw new NullPointerException("Destination store cannot be null");
        if (!(getClass().isInstance(dest)))
            throw new IllegalArgumentException("Destination store not compatible with this store, wrong type: " + dest.getClass());
        
        AbstractIndexedDataStore dstStore = (AbstractIndexedDataStore) dest;
        if (dstStore.elementSize != elementSize)
            throw new IllegalArgumentException("Destination store not compatible with this store, wrong element size: " + dstStore.elementSize);
        
        System.arraycopy(getArray(), srcOffset * elementSize, dstStore.getArray(), destOffset * elementSize, len * elementSize);
    }

    @Override
    public void update(Component[] newToOldMap, int from, int to) {
        int swapSize = newToOldMap.length * elementSize;
        
        if (swap == null || getArrayLength(swap) < swapSize)
            swap = createArray(newToOldMap.length * elementSize);
        
        Object oldArray = getArray();
        
        int lastIndex = -1;
        int copyIndexNew = -1;
        int copyIndexOld = -1;
        for (int i = from; i < to; i++) {
            if (newToOldMap[i].index != lastIndex + 1) {
                // we are not in a contiguous section
                if (copyIndexOld >= 0) {
                    // we have to copy over the last section
                    System.arraycopy(oldArray, copyIndexOld * elementSize, swap, copyIndexNew * elementSize, (i - copyIndexNew) * elementSize);
                }
                
                // set the copy indices
                copyIndexNew = i;
                copyIndexOld = newToOldMap[i].index;
            }
            lastIndex = newToOldMap[i].index;
        }
        
        if (copyIndexOld >= 0) {
            // final copy
            System.arraycopy(oldArray, copyIndexOld * elementSize, swap, copyIndexNew * elementSize, (to - copyIndexNew) * elementSize);
        }

        setArray(swap);
        swap = oldArray;
    }

    protected abstract Object createArray(int arraySize);
    
    protected abstract void setArray(Object array);
    
    protected abstract Object getArray();
    
    protected abstract int getArrayLength(Object array);
}
