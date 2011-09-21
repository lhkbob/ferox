package com.ferox.entity;

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

    private static class FloatDataStore extends AbstractIndexedDataStore {
        private float[] array;
        
        public FloatDataStore(int elementSize, float[] array) {
            super(elementSize);
            this.array = array;
        }
        
        @Override
        protected Object createArray(int arraySize) {
            return new float[arraySize];
        }

        @Override
        protected void setArray(Object array) {
            this.array = (float[]) array;
        }

        @Override
        protected Object getArray() {
            return array;
        }

        @Override
        protected int getArrayLength(Object array) {
            return ((float[]) array).length;
        }
    }
}
