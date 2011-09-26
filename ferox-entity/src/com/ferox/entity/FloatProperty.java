package com.ferox.entity;

/**
 * FloatProperty is an implementation of Property that stores the property data
 * as a number of packed floats for each property. An example would be a
 * three-dimensional vector, which would have an element size of 3.
 * 
 * @author Michael Ludwig
 */
public final class FloatProperty implements Property {
    private FloatDataStore store;

    /**
     * Create a new FloatProperty where each property will have
     * <tt>elementSize</tt> array elements together.
     * 
     * @param elementSize The element size of the property
     * @throws IllegalArgumentException if elementSize is less than 1
     */
    public FloatProperty(int elementSize) {
        store = new FloatDataStore(elementSize, new float[elementSize]);
    }

    /**
     * Return the backing float array of this property's IndexedDataStore. The
     * array may be longer than necessary for the number of components in the
     * system. Data may be looked up for a specific component by scaling the
     * {@link Component#getIndex() component's index} by the element size of the
     * property.
     * 
     * @return The float data for all packed properties that this property has
     *         been packed with
     */
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

    private static class FloatDataStore extends AbstractIndexedDataStore<float[]> {
        private float[] array;
        
        public FloatDataStore(int elementSize, float[] array) {
            super(elementSize);
            this.array = array;
        }
        
        @Override
        protected float[] createArray(int arraySize) {
            return new float[arraySize];
        }

        @Override
        protected void setArray(float[] array) {
            this.array = array;
        }

        @Override
        protected float[] getArray() {
            return array;
        }

        @Override
        protected int getArrayLength(float[] array) {
            return array.length;
        }
    }
}
