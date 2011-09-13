package com.ferox.entity2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

final class ComponentIndex<T extends Component> {
    
    private int[] entityIndexToComponentIndex;
    
    private int[] componentIndexToEntityIndex;
    private Component[] components;
    
    private final IndexedDataStore[] propertyStores;
    private final TypedId<T> type;
    private final EntitySystem system;
    
    private final Comparator<Component> entityIndexComparator;

    // FIXME: do we need this?
    private final List<Integer> removedIndices;
    
    private int componentInsert;
    
    public ComponentIndex(EntitySystem system, TypedId<T> type) {
        this.type = type;
        this.system = system;
        propertyStores = new IndexedDataStore[type.getPropertyCount()];
        
        entityIndexToComponentIndex = new int[1]; // holds default 0 value in 0th index
        componentIndexToEntityIndex = new int[1]; // holds default 0 value in 0th index
        components = new Component[1]; // holds default null value in 0th index
        
        componentInsert = 1;
        
        removedIndices = new ArrayList<Integer>();
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
    
    @SuppressWarnings("unchecked")
    private void expandComponentIndex(int numComponents) {
        if (numComponents < components.length)
            return;

        int size = (int) (numComponents * 1.5f) + 1;
        
        // Expand the indexed data stores for the properties
        for (int i = 0; i < propertyStores.length; i++) {
            if (propertyStores[i] != null) {
                IndexedDataStore newData = propertyStores[i].create(size);
                propertyStores[i].copy(0, propertyStores[i].size(), newData, 0);
                propertyStores[i] = newData;
            }
        }
        
        // Expand the canonical component array
        components = Arrays.copyOf(components, size);
        
        // Assign new indexed data stores over to each component's properties
        Property[] props = new Property[propertyStores.length];
        for (int i = 0; i < components.length; i++) {
            if (components[i] != null) {
                type.getProperties((T) components[i], props);
                for (int j = 0; j < props.length; j++) {
                    props[j].setDataStore(propertyStores[j]);
                }
            }
        }
        
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
    
    public boolean removeComponent(int entityIndex, T match) {
        int componentIndex = entityIndexToComponentIndex[entityIndex];

        if (match != null && match.index != componentIndex)
            return false;
        
        // This code works even if componentIndex is 0
        Component oldComponent = components[componentIndex];

        components[componentIndex] = null;
        entityIndexToComponentIndex[entityIndex] = 0; // entity does not have component
        componentIndexToEntityIndex[componentIndex] = 0; // component does not have entity
        
        // Make all removed component instances point to the 0th index
        if (oldComponent != null) {
            oldComponent.index = 0;
            removedIndices.add(componentIndex);
        }
        
        if (match != null)
            match.index = 0;
        
        return oldComponent != null;
    }
    
    public void index() {
        // FIXME: index() is responsible for packing and sorting the component
        // data in the order of entity ids
        
        // so that makes me think we should sort the entity ids first, although
        // this is always in-order, we just need to repack it.
        
        // Then we need to process all component indices for the sorting
        
        // How will we go about this though? I can't just do a sort using Arrays.sort
        // because I have to move all of the data along with it
        
        // Maybe use a quicksort solution similar to what's in Bag
        // I would need to move components[], and componentIndexToEntityIndex[] over
        // and update the indices in entityIndexToComponentIndex to match
        // finally also copy all data of the indexed data store as appropriate.
        
        // Is there a way of doing a single sort on one of the arrays and
        // then doing an O(n) update.  Probably, if I sort component[] and
        // then read its index value to figure out its original value
        // 
        
        // I could not do the O(n) updates in place, however, because I would
        // start overwriting other data that hasn't been shifted yet.
        
        // Is it too much to just reallocate every time an index is done?
        // Also, the indexed data copy will be slow, although it might be even
        // slower to try to identify regions of shifting.
        
        
        // First sort the components[] array. This will re-order
        // the components to be in the same order as their entities,
        // and will pack gaps as well.
        Arrays.sort(components, 1, componentInsert + 1, entityIndexComparator);
        
        // FIXME must use notebook to see if there is a way to work out inplace
        // update without doing an overwrite of some 'needed' chunk of array
        // If not, make comp-index keep track of num removes and adds, and if enough
        // happen, then index() does something, otherwise its a no-op.
        
        // Similarly, with when entities are force-indexed, must pass forcing
        // through to the comp-index.
        
        // Let's see how this ordering issue works.  In the components[] array,
        // everyone could potentially be moved left or right.  They might be
        // shifted left to fill a gap, or shifted right to make room for a component
        // that needs re-ordering, and shifted left to become sorted, which sucks
        // as far as prediction goes
        
        // now what can we do?
        // - maybe an O(n) swap instead, stealing the swap approach from quick sort?
        //   but I don't know how we could look up the target component and update its
        //   index so that it knew where to grab data
        //
        // - iterating from the back of the sorted components[] array offers some potential
        //   since I know any swap will come from an unprocessed part of the list, assuming
        //   that we update the needed swaps as swaps go
        
        // - It really feels like a re-allocate will be faster, although maybe an O(nlogn) swap
        //   isn't so bad.  I should experiment with the cost of the quicksort swap option
        //   since it will offer me the best memory usage.
        
        // - Alternatively, I can just have two data stores that get swapped back and forth
        //   every index.  This would duplicate the amount of storage needed for the entity system,though
        //   but would remove the need to do allocations every index.
        
        // Although, quicksort might not be the best sort since most of the data will be 
        // fully sorted, so I want to pick a sort that works well with that.  Bubble? Insertion?
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
                propertyStores[i] = origData.create(components.length);
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
            removeComponent(componentIndexToEntityIndex[index], null);
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
            
            removeComponent(entityIndex, null);
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
