package com.ferox.entity;

/**
 * IntProperty is an implementation of Property that stores the property data
 * as a number of packed ints for each property.
 * 
 * @author Michael Ludwig
 */
public final class IntProperty implements Property {
    private IntDataStore store;
    
    /**
     * Create a new IntProperty where each property will have
     * <tt>elementSize</tt> array elements together.
     * 
     * @param elementSize The element size of the property
     * @throws IllegalArgumentException if elementSize is less than 1
     */
    public IntProperty(int elementSize) {
        store = new IntDataStore(elementSize, new int[elementSize]);
    }

    /**
     * Return the backing int array of this property's IndexedDataStore. The
     * array may be longer than necessary for the number of components in the
     * system. Data may be looked up for a specific component by scaling the
     * {@link Component#getIndex() component's index} by the element size of the
     * property.
     * 
     * @return The int data for all packed properties that this property has
     *         been packed with
     */
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

    private static class IntDataStore extends AbstractIndexedDataStore<int[]> {
        private int[] array;
        
        public IntDataStore(int elementSize, int[] array) {
            super(elementSize);
            this.array = array;
        }
        
        @Override
        protected int[] createArray(int arraySize) {
            return new int[arraySize];
        }

        @Override
        protected void setArray(int[] array) {
            this.array = array;
        }

        @Override
        protected int[] getArray() {
            return array;
        }

        @Override
        protected int getArrayLength(int[] array) {
            return array.length;
        }
    }
}
