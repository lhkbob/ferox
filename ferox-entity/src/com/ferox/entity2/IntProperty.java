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

    private static class IntDataStore extends AbstractIndexedDataStore {
        private int[] array;
        
        public IntDataStore(int elementSize, int[] array) {
            super(elementSize);
            this.array = array;
        }
        
        @Override
        protected Object createArray(int arraySize) {
            return new int[arraySize];
        }

        @Override
        protected void setArray(Object array) {
            this.array = (int[]) array;
        }

        @Override
        protected Object getArray() {
            return array;
        }

        @Override
        protected int getArrayLength(Object array) {
            return ((int[]) array).length;
        }
    }
}
