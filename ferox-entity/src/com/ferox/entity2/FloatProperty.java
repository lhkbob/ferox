package com.ferox.entity2;

public final class FloatProperty implements Property {
    private FloatDataStore store;
    
    public FloatProperty(int elementSize) {
        store = new FloatDataStore(elementSize, new float[elementSize]);
    }
    
    public float[] getIndexedData() {
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
        if (!(store instanceof FloatDataStore))
            throw new IllegalArgumentException("Store not compatible with FloatProperty, wrong type: " + store.getClass());
        
        FloatDataStore newStore = (FloatDataStore) store;
        if (newStore.elementSize != this.store.elementSize)
            throw new IllegalArgumentException("Store not compatible with FloatProperty, wrong element size: " + newStore.elementSize);
        
        this.store = newStore;
    }

    private static class FloatDataStore implements IndexedDataStore {
        private final int elementSize;
        private final float[] array;
        
        public FloatDataStore(int elementSize, float[] array) {
            this.elementSize = elementSize;
            this.array = array;
        }
        
        @Override
        public IndexedDataStore create(int size) {
            return new FloatDataStore(elementSize, new float[elementSize * size]);
        }

        @Override
        public int size() {
            return array.length / elementSize;
        }

        @Override
        public void copy(int srcOffset, int len, IndexedDataStore dest, int destOffset) {
            if (dest == null)
                throw new NullPointerException("Destination store cannot be null");
            if (!(dest instanceof FloatDataStore))
                throw new IllegalArgumentException("Destination store not compatible with this store, wrong type: " + dest.getClass());
            
            
            FloatDataStore dstStore = (FloatDataStore) dest;
            if (dstStore.elementSize != elementSize)
                throw new IllegalArgumentException("Destination store not compatible with this store, wrong element size: " + dstStore.elementSize);
            
            System.arraycopy(array, srcOffset * elementSize, dstStore.array, destOffset * elementSize, len * elementSize);
        }
    }
}
