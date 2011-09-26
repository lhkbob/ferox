package com.ferox.entity;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class ComponentIndex<T extends Component> {
    private int[] entityIndexToComponentIndex;
    
    private int[] componentIndexToEntityIndex;
    private Component[] components;
    
    private final IndexedDataStore[] propertyStores;
    private final TypedId<T> type;
    private final EntitySystem system;
    
    private final Comparator<Component> entityIndexComparator;

    private int componentInsert;
    
    public ComponentIndex(EntitySystem system, TypedId<T> type) {
        this.type = type;
        this.system = system;
        propertyStores = new IndexedDataStore[type.getPropertyCount()];
        
        entityIndexToComponentIndex = new int[1]; // holds default 0 value in 0th index
        componentIndexToEntityIndex = new int[1]; // holds default 0 value in 0th index
        components = new Component[1]; // holds default null value in 0th index
        
        componentInsert = 1;
        
        entityIndexComparator = new Comparator<Component>() {
            @Override
            public int compare(Component o1, Component o2) {
                if (o1 != null && o2 != null)
                    return componentIndexToEntityIndex[o1.index] - componentIndexToEntityIndex[o2.index];
                else if (o1 != null)
                    return -1; // push null o2 to end of array
                else if (o2 != null)
                    return 1; // push null o1 to end of array
                else
                    return 0; // both null so they are "equal"
            }
        };
    }
    
    public int getSizeEstimate() {
        return componentInsert + 1;
    }
    
    public EntitySystem getEntitySystem() {
        return system;
    }
    
    public int getEntityIndex(int componentIndex) {
        return componentIndexToEntityIndex[componentIndex];
    }
    
    public int getComponentIndex(int entityIndex) {
        return entityIndexToComponentIndex[entityIndex];
    }
    
    public void expandEntityIndex(int numEntities) {
        if (entityIndexToComponentIndex.length < numEntities) {
            entityIndexToComponentIndex = Arrays.copyOf(entityIndexToComponentIndex, (int) (numEntities * 1.5f) + 1);
        }
    }
    
    private void expandComponentIndex(int numComponents) {
        if (numComponents < components.length)
            return;

        int size = (int) (numComponents * 1.5f) + 1;
        
        // Expand the indexed data stores for the properties
        for (int i = 0; i < propertyStores.length; i++) {
            if (propertyStores[i] != null) {
                // Becuase we use resize() here, we don't need to update
                // the IndexedDataStores of the components
                propertyStores[i].resize(size);
            }
        }
        
        // Expand the canonical component array
        components = Arrays.copyOf(components, size);
        
        // Expand the component index
        componentIndexToEntityIndex = Arrays.copyOf(componentIndexToEntityIndex, size);
    }
    
    @SuppressWarnings("unchecked")
    public T getComponent(int entityIndex) {
        return (T) components[entityIndexToComponentIndex[entityIndex]];
    }
    
    
    @SuppressWarnings("unchecked")
    public T addComponent(int entityIndex, T fromTemplate) {
        int componentIndex = entityIndexToComponentIndex[entityIndex];
        if (componentIndex == 0) {
            // no existing component, so we make one, possibly expanding the backing array
            componentIndex = componentInsert++;
            if (componentIndex >= components.length)
                expandComponentIndex(componentIndex + 1);
            
            T instance = newInstance(componentIndex, false);
            components[componentIndex] = instance;
            componentIndexToEntityIndex[componentIndex] = entityIndex;
            entityIndexToComponentIndex[entityIndex] = componentIndex;
        }
        
        if (fromTemplate != null) {
            // Copy values from fromTemplate's properties to the new instances
            Property[] templateProps = new Property[propertyStores.length];
            type.getProperties(fromTemplate, templateProps);
            
            for (int i = 0; i < templateProps.length; i++) {
                templateProps[i].getDataStore().copy(fromTemplate.index, 1, propertyStores[i], componentIndex);
            }
        }
        return (T) components[componentIndex];
    }
    
    public boolean removeComponent(int entityIndex) {
        int componentIndex = entityIndexToComponentIndex[entityIndex];

        // This code works even if componentIndex is 0
        Component oldComponent = components[componentIndex];

        components[componentIndex] = null;
        entityIndexToComponentIndex[entityIndex] = 0; // entity does not have component
        componentIndexToEntityIndex[componentIndex] = 0; // component does not have entity
        
        // Make all removed component instances point to the 0th index
        if (oldComponent != null)
            oldComponent.index = 0;
        
        return oldComponent != null;
    }
    
    public void index(int[] entityOldToNewMap, int numEntities) {
        // First sort the canonical components array
        Arrays.sort(components, 1, componentInsert, entityIndexComparator);
        
        // Update all of the propery stores to match up with the components new positions
        for (int i = 0; i < propertyStores.length; i++) {
            if (propertyStores[i] != null)
                propertyStores[i].update(components, 1, componentInsert);
        }
        
        // Repair the componentToEntityIndex and the component.index values
        componentInsert = 1;
        int[] newComponentIndex = new int[components.length];
        for (int i = 1; i < components.length; i++) {
            if (components[i] != null) {
                newComponentIndex[i] = entityOldToNewMap[componentIndexToEntityIndex[components[i].index]];
                components[i].index = i;
                componentInsert = i + 1;
            }
        }
        componentIndexToEntityIndex = newComponentIndex;
        
        // Possibly compact the component data
        if (componentInsert < .6f * components.length) {
            int newSize = (int) (1.2f * componentInsert) + 1;
            components = Arrays.copyOf(components, newSize);
            componentIndexToEntityIndex = Arrays.copyOf(componentIndexToEntityIndex, newSize);
            for (int i = 0; i < propertyStores.length; i++) {
                if (propertyStores[i] != null)
                    propertyStores[i].resize(newSize);
            }
        }
        
        // Repair entityIndexToComponentIndex - and possible shrink the index
        // based on the number of packed entities
        if (numEntities < .6f * entityIndexToComponentIndex.length)
            entityIndexToComponentIndex = new int[(int) (1.2f * numEntities) + 1];
        else
            Arrays.fill(entityIndexToComponentIndex, 0);
        
        for (int i = 1; i < componentInsert; i++)
            entityIndexToComponentIndex[componentIndexToEntityIndex[i]] = i;
    }
    
    public Iterator<T> iterator() {
        return new ComponentIterator();
    }
    
    public Iterator<T> fastIterator() {
        return new FastComponentIterator();
    }
    
    public T newInstance(int index, boolean forIter) {
        T cmp;
        
        try {
            // Since the type was generated by Component, we know it has
            // a private/protected constructor for (EntitySystem, int)
            cmp = type.getConstructor().newInstance(system, index);
        } catch(Exception e) {
            throw new RuntimeException("Unable to create new Component instance", e);
        }
        
        Property[] props = new Property[propertyStores.length];
        type.getProperties(cmp, props);
        
        for (int i = 0; i < props.length; i++) {
            IndexedDataStore origData = props[i].getDataStore();
            
            if (propertyStores[i] == null) {
                // Must create a new store for the component property,
                // make it large enough to fit the current components array
                origData.resize(components.length);
                propertyStores[i] = origData;
            }
            
            if (!forIter) {
                // Copy values from new component into data store
                origData.copy(0, 1, propertyStores[i], index);
            }
            
            props[i].setDataStore(propertyStores[i]);
        }
        
        return cmp;
    }
    
    private class ComponentIterator implements Iterator<T> {
        private int index;
        private boolean advanced;
        
        public ComponentIterator() {
            index = 0;
            advanced = false;
        }
        
        @Override
        public boolean hasNext() {
            if (!advanced)
                advance();
            return index < components.length;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            if (!hasNext())
                throw new NoSuchElementException();
            advanced = false;
            return (T) components[index];
        }

        @Override
        public void remove() {
            if (advanced || index == 0)
                throw new IllegalStateException("Must call next() before remove()");
            if (components[index] == null)
                throw new IllegalStateException("Component already removed");
            removeComponent(componentIndexToEntityIndex[index]);
        }
        
        private void advance() {
            index++; // always advance at least 1
            while(index < components.length && components[index] == null) {
                index++;
            }
            advanced = true;
        }
    }
    
    private class FastComponentIterator implements Iterator<T> {
        private final T instance;
        
        private int index;
        private boolean advanced;
        
        public FastComponentIterator() {
            instance = newInstance(0, true);
            index = 0;
            advanced = false;
        }
        
        @Override
        public boolean hasNext() {
            if (!advanced)
                advance();
            return index < componentIndexToEntityIndex.length;
        }

        @Override
        public T next() {
            if (!hasNext())
                throw new NoSuchElementException();
            advanced = false;
            instance.index = index;
            return instance;
        }

        @Override
        public void remove() {
            if (advanced || index == 0)
                throw new IllegalStateException("Must call next() before remove()");
            
            int entityIndex = componentIndexToEntityIndex[index];
            if (entityIndex == 0)
                throw new IllegalStateException("Component already removed");
            
            removeComponent(entityIndex);
        }
        
        private void advance() {
            // Check componentIndexToEntityIndex so we don't pull in an instance 
            // and we can just iterate along the int[] array. A 0 value implies that
            // the component does not have an attached entity, and has been removed
            
            index++; // always advance
            while(index < componentIndexToEntityIndex.length && 
                  componentIndexToEntityIndex[index] == 0) {
                index++;
            }
            advanced = true;
        }
    }
}
