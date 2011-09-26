package com.ferox.entity;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public final class EntitySystem {
    private ComponentIndex<?>[] componentIndices;
    private int[] entityIds; // binary search provides index
    
    private Entity[] entities;
    
    private int entityInsert;
    private int entityIdSeq;
    
    private boolean requireReIndex;
    
    private final ConcurrentHashMap<Class<? extends Annotation>, Object> controllerData;
    
    public EntitySystem() {
        entities = new Entity[1];
        entityIds = new int[1];
        
        componentIndices = new ComponentIndex[0];
        controllerData = new ConcurrentHashMap<Class<? extends Annotation>, Object>();
        
        entityIdSeq = 1; // start at 1, id 0 is reserved for index = 0 
        entityInsert = 1;
    }
    
    public Object getControllerData(Class<? extends Annotation> key) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        return controllerData.get(key);
    }
    
    public void setControllerData(Class<? extends Annotation> key, Object value) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        if (value == null)
            controllerData.remove(key);
        else
            controllerData.put(key, value);
    }
    
    public Iterator<Entity> iterator() {
        return new EntityIterator();
    }
    
    public Iterator<Entity> fastIterator() {
        return new FastEntityIterator();
    }

    public <T extends Component> Iterator<T> iterator(TypedId<T> id) {
        return getIndex(id).iterator();
    }
    
    public <T extends Component> Iterator<T> fastIterator(TypedId<T> id) {
        return getIndex(id).fastIterator();
    }
    
    public Iterator<Component[]> iterator(TypedId<?>... ids) { 
        return bulkIterator(false, ids);
    }
    
    public Iterator<Component[]> fastIterator(TypedId<?>... ids) {
        return bulkIterator(true, ids);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Iterator<Component[]> bulkIterator(boolean fast, TypedId<?>... ids) {
        TypedId[] rawIds = ids;
        
        int minIndex = -1;
        int minSize = Integer.MAX_VALUE;
        
        ComponentIndex index;
        ComponentIndex[] rawIndices = new ComponentIndex[ids.length];
        
        for (int i = 0; i < ids.length; i++) {
            index = getIndex(rawIds[i]);
            if (index.getSizeEstimate() < minSize) {
                minIndex = i;
                minSize = index.getSizeEstimate();
            }
            
            rawIndices[i] = index;
        }

        if (fast)
            return new FastBulkComponentIterator(rawIndices, minIndex);
        else
            return new BulkComponentIterator(rawIndices, minIndex);
    }
    
    public void index() {
        // Pack the data
        int startRemove = -1;
        for (int i = 1; i < entityInsert; i++) {
            if (entityIds[i] == 0) {
                // found an entity to remove
                if (startRemove < 0)
                    startRemove = i;
            } else {
                // found an entity to preserve
                if (startRemove > 0) {
                    // we have a gap from [startRemove, i - 1] that can be compacted
                    System.arraycopy(entityIds, i, entityIds, startRemove, entityInsert - i);
                    System.arraycopy(entities, i, entities, startRemove, entityInsert - i);
                    
                    // update entityInsert
                    entityInsert = entityInsert - i + startRemove;
                    
                    // now reset loop
                    i = startRemove;
                    startRemove = -1;
                }
            }
        }
        
        // Build a map from oldIndex to newIndex and repair entity's index
        int[] oldToNew = new int[entityIds.length];
        for (int i = 1; i < entityInsert; i++) {
                oldToNew[entities[i].index] = i;
                entities[i].index = i;
        }
        
        if (entityInsert < .6f * entities.length) {
            // reduce the size of the entities/ids arrays
            int newSize = (int) (1.2f * entityInsert) + 1;
            entities = Arrays.copyOf(entities, newSize);
            entityIds = Arrays.copyOf(entityIds, newSize);
        }
        
        // Now index and update all ComponentIndices
        for (int i = 0; i < componentIndices.length; i++) {
            if (componentIndices[i] != null)
                componentIndices[i].index(oldToNew, entityInsert);
        }
    }
    
    public Entity getEntity(int entity) {
        if (requireReIndex)
            index();
        
        int index = Arrays.binarySearch(entityIds, 0, entityInsert + 1, entity);
        if (index >= 0)
            return entities[index];
        return null;
    }
    
    public Entity addEntity() {
        return addEntity(null);
    }
    
    public Entity addEntity(Entity template) {
        int entityIndex = entityInsert++;
        if (entityIndex >= entityIds.length) {
            entityIds = Arrays.copyOf(entityIds, (int) (entityIndex * 1.5f) + 1);
            entities = Arrays.copyOf(entities, (int) (entityIndex * 1.5f) + 1);
        }
        
        for (int i = 0; i < componentIndices.length; i++) {
            if (componentIndices[i] != null)
                componentIndices[i].expandEntityIndex(entityIndex + 1);
        }
        
        Entity newEntity = new Entity(this, entityIndex);
        entities[entityIndex] = newEntity;
        entityIds[entityIndex] = entityIdSeq++;
        
        if (template != null) {
            for (Component c: template) {
                addFromTemplate(entityIndex, c.getTypedId(), c);
            }
        }

        return newEntity;
    }
    
    public boolean removeEntity(Entity e) {
        return removeEntity(e.index);
    }
    
    private boolean removeEntity(int index) {
        requireReIndex = true;
        
        // Remove all components from the entity
        for (int i = 0; i < componentIndices.length; i++) {
            if (componentIndices[index] != null)
                componentIndices[index].removeComponent(index);
        }
        
        // clear out id and canonical entity
        Entity old = entities[index];
        entityIds[index] = 0;
        entities[index] = null;
        
        if (old != null) {
            // update its index
            old.index = 0;
            return true;
        } else
            return false;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void registerType(TypedId<?> id) {
        int index = id.getId();
        if (componentIndices.length <= index)
            componentIndices = Arrays.copyOf(componentIndices, index + 1);
        if (componentIndices[index] == null) {
            componentIndices[index] = new ComponentIndex(this, id);
            
            // entities.length might not be the correct number of entities,
            // but we really only need to guarantee that the entity index for the
            // type has at least as many elements as entities.
            componentIndices[index].expandEntityIndex(entities.length);
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T extends Component> void addFromTemplate(int entityIndex, TypedId typedId, Component c) {
        ComponentIndex index = getIndex(typedId);
        index.addComponent(entityIndex, c);
    }
    
    @SuppressWarnings("unchecked")
    <T extends Component> ComponentIndex<T> getIndex(TypedId<T> id) {
        return (ComponentIndex<T>) componentIndices[id.getId()];
    }
    
    Iterator<ComponentIndex<?>> iterateComponentIndices() {
        return new ComponentIndexIterator();
    }
    
    int getEntityId(int entityIndex) {
        return entityIds[entityIndex];
    }
    
    Entity getEntityByIndex(int entityIndex) {
        return entities[entityIndex];
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private class BulkComponentIterator implements Iterator<Component[]> {
        private final ComponentIndex[] indices;
        private final int minIndex;
        private final Iterator<Component> minComponentIterator;
        private final Component[] result;
        
        private boolean hasAdvanced;
        private boolean resultValid;
        
        public BulkComponentIterator(ComponentIndex[] indices, int minIndex) {
            this.indices = indices;
            this.minIndex = minIndex;
            
            minComponentIterator = indices[minIndex].iterator();
            result = new Component[indices.length];
            hasAdvanced = false;
            resultValid = false;
        }
        
        @Override
        public boolean hasNext() {
            if (!hasAdvanced)
                advance();
            return resultValid;
        }

        @Override
        public Component[] next() {
            if (!hasNext())
                throw new NoSuchElementException();
            hasAdvanced = false;
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        private void advance() {
            Component c;
            boolean foundAll;
            int entityIndex;
            
            hasAdvanced = true;
            resultValid = false;
            while(minComponentIterator.hasNext()) {
                foundAll = true;
                c = minComponentIterator.next();
                entityIndex = indices[minIndex].getEntityIndex(c.index);
                
                result[minIndex] = c;
                
                // now look for every other component
                for (int i = 0; i < result.length; i++) {
                    if (i == minIndex)
                        continue;
                    
                    c = indices[i].getComponent(entityIndex);
                    if (c == null) {
                        foundAll = false;
                        break;
                    } else {
                        result[i] = c;
                    }
                }
                
                if (foundAll) {
                    resultValid = true;
                    break;
                }
            }
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private class FastBulkComponentIterator implements Iterator<Component[]> {
        private final ComponentIndex[] indices;
        private final int minIndex;
        private final Iterator<Component> minComponentIterator;
        private final Component[] result;
        
        private boolean hasAdvanced;
        private boolean resultValid;
        
        public FastBulkComponentIterator(ComponentIndex[] indices, int minIndex) {
            this.indices = indices;
            this.minIndex = minIndex;
            
            minComponentIterator = indices[minIndex].fastIterator();
            result = new Component[indices.length];
            hasAdvanced = false;
            resultValid = false;
            
            // now create local instances for the components
            for (int i = 0; i < indices.length; i++) {
                if (i == minIndex)
                    continue;
                result[i] = indices[i].newInstance(0, true);
            }
        }
        
        @Override
        public boolean hasNext() {
            if (!hasAdvanced)
                advance();
            return resultValid;
        }

        @Override
        public Component[] next() {
            if (!hasNext())
                throw new NoSuchElementException();
            hasAdvanced = false;
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        private void advance() {
            Component c;
            boolean foundAll;
            int entityIndex;
            int ci;
            
            hasAdvanced = true;
            resultValid = false;
            while(minComponentIterator.hasNext()) {
                foundAll = true;
                c = minComponentIterator.next();
                entityIndex = indices[minIndex].getEntityIndex(c.index);
                
                // we use the fastIterator()'s returned instance for the min component,
                // so we have to assign it here
                result[minIndex] = c;
                
                // now look for every other component
                for (int i = 0; i < result.length; i++) {
                    if (i == minIndex)
                        continue;
                    
                    ci = indices[i].getComponentIndex(entityIndex);
                    if (ci == 0) {
                        foundAll = false;
                        break;
                    } else {
                        result[i].index = ci;
                    }
                }
                
                if (foundAll) {
                    resultValid = true;
                    break;
                }
            }
        }
    }
    
    private class ComponentIndexIterator implements Iterator<ComponentIndex<?>> {
        private int index;
        private boolean advanced;
        
        public ComponentIndexIterator() {
            index = -1;
            advanced = false;
        }
        
        @Override
        public boolean hasNext() {
            if (!advanced)
                advance();
            return index < componentIndices.length;
        }

        @Override
        public ComponentIndex<?> next() {
            if (!hasNext())
                throw new NoSuchElementException();
            advanced = false;
            return componentIndices[index];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        private void advance() {
            index++;
            while(index < componentIndices.length && componentIndices[index] == null) {
                index++;
            }
            advanced = true;
        }
    }
    
    private class EntityIterator implements Iterator<Entity> {
        private int index;
        private boolean advanced;
        
        public EntityIterator() {
            index = 0;
            advanced = false;
        }
        
        @Override
        public boolean hasNext() {
            if (!advanced)
                advance();
            return index < entities.length;
        }

        @Override
        public Entity next() {
            if (!hasNext())
                throw new NoSuchElementException();
            advanced = false;
            return entities[index];
        }

        @Override
        public void remove() {
            if (advanced || index == 0)
                throw new IllegalStateException("Must call next() before remove()");
            if (entities[index] == null)
                throw new IllegalStateException("Entity already removed");
            removeEntity(index);
        }
        
        private void advance() {
            index++; // always advance at least 1
            while(index < entities.length && entities[index] == null) {
                index++;
            }
            advanced = true;
        }
    }
    
    private class FastEntityIterator implements Iterator<Entity> {
        private final Entity instance;
        
        private int index;
        private boolean advanced;
        
        public FastEntityIterator() {
            instance = new Entity(EntitySystem.this, 0);
            index = 0;
            advanced = false;
        }
        
        @Override
        public boolean hasNext() {
            if (!advanced)
                advance();
            return index < entityIds.length;
        }

        @Override
        public Entity next() {
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
            if (entityIds[index] == 0)
                throw new IllegalStateException("Component already removed");
            removeEntity(index);
        }
        
        private void advance() {
            // Check entityIds so we don't pull in an instance 
            // and we can just iterate along the int[] array. A 0 value implies that
            // the component does not have an attached entity, and has been removed
            
            index++; // always advance
            while(index < entityIds.length && 
                  entityIds[index] == 0) {
                index++;
            }
            advanced = true;
        }
    }
}
