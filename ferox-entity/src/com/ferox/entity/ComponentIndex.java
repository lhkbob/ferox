package com.ferox.entity;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * ComponentIndex manages storing all the components of a specific type for an
 * EntitySystem. It also controls the IndexedDataStore's for the type's set of
 * properties. It is package-private because its details are low-level and
 * complex.
 * 
 * @author Michael Ludwig
 * @param <T> The type of component stored by the index
 */
final class ComponentIndex<T extends Component> {
    // These three arrays have a special value of 0 or null stored in the 0th
    // index, which allows us to lookup components or entities when they
    // normally aren't attached.
    private int[] entityIndexToComponentIndex;
    private int[] componentIndexToEntityIndex;
    private Component[] components;
    
    private final IndexedDataStore[] propertyStores;
    private final TypedId<T> type;
    private final EntitySystem system;
    
    private final Comparator<Component> entityIndexComparator;

    private int componentInsert;

    /**
     * Create a ComponentIndex for the given system, that will store Components
     * of the given type.
     * 
     * @param system The owning system
     * @param type The type of component
     * @throws NullPointerException if system or type are null
     */
    public ComponentIndex(EntitySystem system, TypedId<T> type) {
        if (system == null || type == null)
            throw new NullPointerException("Arguments cannot be null");
        
        this.type = type;
        this.system = system;
        propertyStores = new IndexedDataStore[type.getFieldCount()];
        
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

    /**
     * @return An estimate on the number of components in the index, cannot be
     *         less than the true size
     */
    public int getSizeEstimate() {
        return componentInsert + 1;
    }
    
    /**
     * @return The owning EntitySystem
     */
    public EntitySystem getEntitySystem() {
        return system;
    }

    /**
     * Given the index of a Component (e.g. {@link Component#getIndex()}, return
     * the index of an entity within the owning system. The returned entity
     * index can be safely passed to {@link EntitySystem#getEntityByIndex(int)}.
     * 
     * @param componentIndex The component index whose owning entity is fetched
     * @return The index of the entity that has the given component index, or 0
     *         if the component is not attached
     */
    public int getEntityIndex(int componentIndex) {
        return componentIndexToEntityIndex[componentIndex];
    }

    /**
     * Given the index of an entity (e.g. {@link Entity#index}), return the
     * index of the attached component of this ComponentIndex's type. The
     * returned component index can be used in {@link #getComponent(int)} and
     * related methods.
     * 
     * @param entityIndex The entity index to look up
     * @return The index of the attached component, or 0 if the entity does not
     *         have a component of this type attached
     */
    public int getComponentIndex(int entityIndex) {
        return entityIndexToComponentIndex[entityIndex];
    }

    /**
     * Ensure that this ComponentIndex has enough internal space to hold its
     * entity to component mapping for the given number of entities.
     * 
     * @param numEntities The new number of entities
     */
    public void expandEntityIndex(int numEntities) {
        if (entityIndexToComponentIndex.length < numEntities) {
            entityIndexToComponentIndex = Arrays.copyOf(entityIndexToComponentIndex, (int) (numEntities * 1.5f) + 1);
        }
    }
    
    /*
     * As expandEntityIndex() but expands all related component data and arrays
     * to hold the number of components. This doesn't need to be public so its hidden.
     */
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
    
    /**
     * @param entityIndex The entity index whose component is fetched
     * @return The canonical component instance attached to the given entity
     *         index, or null if no component is attached yet
     */
    @SuppressWarnings("unchecked")
    public T getComponent(int entityIndex) {
        return (T) components[entityIndexToComponentIndex[entityIndex]];
    }

    /**
     * Create a new component of this index's type and attach to it the entity
     * at the given entity index. If <tt>fromTemplate</tt> is non-null, the
     * property values from the template should be copied to the values of new
     * component.
     * 
     * @param entityIndex The entity index which the component is attached to
     * @param fromTemplate A template to assign values to the new component, may
     *            be null
     * @return A new component of type T
     */
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
            getProperties(fromTemplate, type.getFields(), templateProps);
            
            for (int i = 0; i < templateProps.length; i++) {
                templateProps[i].getDataStore().copy(fromTemplate.index, 1, propertyStores[i], componentIndex);
            }
        }
        return (T) components[componentIndex];
    }

    /**
     * Detach or remove any component of this index's type from the entity with
     * the given index. True is returned if a component was removed, or false
     * otherwise.
     * 
     * @param entityIndex The entity's index whose component is removed
     * @return True if a component was removed
     */
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

    /**
     * <p>
     * Compact the data of this ComponentIndex to account for removals and
     * additions to the index. This will ensure that all active components are
     * packed into the underlying arrays, and that they will be accessed in the
     * same order as iterating over the entities directly.
     * </p>
     * <p>
     * The map from old to new entity index must be used to properly update the
     * component index's data so that the system is kept in sync.
     * </p>
     * 
     * @param entityOldToNewMap A map from old entity index to new index
     * @param numEntities The number of entities that are in the system
     */
    public void compact(int[] entityOldToNewMap, int numEntities) {
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

    /**
     * @return An iterator over the canonical components in the index. The
     *         iterator's remove() detaches the component from the entity
     */
    public Iterator<T> iterator() {
        return new ComponentIterator();
    }

    /**
     * @return An iterator over the components of the index, but a single
     *         component instance is reused. remove() detaches the current
     *         component from the entity
     */
    public Iterator<T> fastIterator() {
        return new FastComponentIterator();
    }

    /**
     * Create a new instance of T that will take its data from the given index.
     * 
     * @param index The component index to wrap
     * @return The new instance wrapping the data at the given index
     */
    public T newInstance(int index) {
        return newInstance(index, false);
    }

    /*
     * Create a new instance and manage its properties and data stores. If
     * forIter is false, the properties are copied into the data store at the
     * given index.
     */
    private T newInstance(int index, boolean forIter) {
        T cmp;
        
        try {
            // Since the type was generated by Component, we know it has
            // a private/protected constructor for (EntitySystem, int)
            cmp = type.getConstructor().newInstance(system, index);
        } catch(Exception e) {
            throw new RuntimeException("Unable to create new Component instance", e);
        }
        
        Property[] props = new Property[propertyStores.length];
        getProperties(cmp, type.getFields(), props);
        
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
    
    /*
     * Fetch all Property instances for the given instance
     */
    private void getProperties(T instance, List<Field> fields, Property[] propertiesOut) {
        try {
            for (int i = 0; i < fields.size(); i++) {
                propertiesOut[i] = (Property) fields.get(i).get(instance);
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    /*
     * An iterator implementation over the canonical components of the index.
     */
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

    /*
     * An iterator over the components of the system that reuses a single
     * instance for performance.
     */
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
