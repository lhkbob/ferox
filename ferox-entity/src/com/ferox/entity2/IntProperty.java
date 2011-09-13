package com.ferox.entity2;

public final class IntProperty implements Property {
    private IntDataStore store;
    
    public IntProperty(int elementSize) {
        store = new IntDataStore(elementSize, new int[elementSize]);
    }
    
    public int[] getIndexedData() {
        return store.array;
    }
    
    @Override
    public IndexedDataStore getDataStore() {
        return store;
    }

    @Override
    public void setDataStore(IndexedDataStore store) {
        if (store == null)
            throw new NullPointerException("Store cannot be null");
        if (!(store instanceof IntDataStore))
            throw new IllegalArgumentException("Store not compatible with IntProperty, wrong type: " + store.getClass());
        
        IntDataStore newStore = (IntDataStore) store;
        if (newStore.elementSize != this.store.elementSize)
            throw new IllegalArgumentException("Store not compatible with IntProperty, wrong element size: " + newStore.elementSize);
        
        this.store = newStore;
    }

    private static class IntDataStore implements IndexedDataStore {
        private final int elementSize;
        private final int[] array;
        
        public IntDataStore(int elementSize, int[] array) {
            this.elementSize = elementSize;
            this.array = array;
        }
        
        @Override
        public IndexedDataStore create(int size) {
            return new IntDataStore(elementSize, new int[elementSize * size]);
        }

        @Override
        public int size() {
            return array.length / elementSize;
        }

        @Override
        public void copy(int srcOffset, int len, IndexedDataStore dest, int destOffset) {
            if (dest == null)
                throw new NullPointerException("Destination store cannot be null");
            if (!(dest instanceof IntDataStore))
                throw new IllegalArgumentException("Destination store not compatible with this store, wrong type: " + dest.getClass());
            
            
            IntDataStore dstStore = (IntDataStore) dest;
            if (dstStore.elementSize != elementSize)
                throw new IllegalArgumentException("Destination store not compatible with this store, wrong element size: " + dstStore.elementSize);
            
            System.arraycopy(array, srcOffset * elementSize, dstStore.array, destOffset * elementSize, len * elementSize);
        }
    }
}
