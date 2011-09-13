package com.ferox.entity2;

public final class ObjectProperty implements Property {
    private ObjectDataStore store;
    
    public ObjectProperty(int elementSize) {
        store = new ObjectDataStore(elementSize, new Object[elementSize]);
    }
    
    public Object[] getIndexedData() {
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
        if (!(store instanceof ObjectDataStore))
            throw new IllegalArgumentException("Store not compatible with ObjectProperty, wrong type: " + store.getClass());
        
        ObjectDataStore newStore = (ObjectDataStore) store;
        if (newStore.elementSize != this.store.elementSize)
            throw new IllegalArgumentException("Store not compatible with ObjectProperty, wrong element size: " + newStore.elementSize);
        
        this.store = newStore;
    }

    private static class ObjectDataStore implements IndexedDataStore {
        private final int elementSize;
        private final Object[] array;
        
        public ObjectDataStore(int elementSize, Object[] array) {
            this.elementSize = elementSize;
            this.array = array;
        }
        
        @Override
        public IndexedDataStore create(int size) {
            return new ObjectDataStore(elementSize, new Object[elementSize * size]);
        }

        @Override
        public int size() {
            return array.length / elementSize;
        }

        @Override
        public void copy(int srcOffset, int len, IndexedDataStore dest, int destOffset) {
            if (dest == null)
                throw new NullPointerException("Destination store cannot be null");
            if (!(dest instanceof ObjectDataStore))
                throw new IllegalArgumentException("Destination store not compatible with this store, wrong type: " + dest.getClass());
            
            
            ObjectDataStore dstStore = (ObjectDataStore) dest;
            if (dstStore.elementSize != elementSize)
                throw new IllegalArgumentException("Destination store not compatible with this store, wrong element size: " + dstStore.elementSize);
            
            System.arraycopy(array, srcOffset * elementSize, dstStore.array, destOffset * elementSize, len * elementSize);
        }
    }
}
