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

    private static class ObjectDataStore extends AbstractIndexedDataStore {
        private Object[] array;
        
        public ObjectDataStore(int elementSize, Object[] array) {
            super(elementSize);
            this.array = array;
        }
        
        @Override
        protected Object createArray(int arraySize) {
            return new Object[arraySize];
        }

        @Override
        protected void setArray(Object array) {
            this.array = (Object[]) array;
        }

        @Override
        protected Object getArray() {
            return array;
        }

        @Override
        protected int getArrayLength(Object array) {
            return ((Object[]) array).length;
        }
    }
}
