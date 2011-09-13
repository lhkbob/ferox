package com.ferox.entity2;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Entity implements Iterable<Component> {
    private final EntitySystem system;
    int index;
    
    Entity(EntitySystem system, int index) {
        this.system = system;
        this.index = index;
    }
    
    public EntitySystem getEntitySystem() {
        return system;
    }
    
    public int getId() {
        return system.getEntityId(index);
    }
    
//    public Integer getBoxedId() {
        // optimization worth it?
//    }
    
    public <T extends Component> T get(TypedId<T> componentId) {
        ComponentIndex<T> ci = system.getIndex(componentId);
        return ci.getComponent(index);
    }
    
    public <T extends Component> T add(TypedId<T> componentId) {
        ComponentIndex<T> ci = system.getIndex(componentId);
        return ci.addComponent(index, null);
    }
    
    public <T extends Component> boolean remove(TypedId<T> componentId) {
        ComponentIndex<T> ci = system.getIndex(componentId);
        return ci.removeComponent(index, null);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Component> boolean remove(T component) {
        ComponentIndex<T> ci = (ComponentIndex<T>) system.getIndex(component.getTypedId());
        return ci.removeComponent(index, component);
    }

    @Override
    public Iterator<Component> iterator() {
        return new ComponentIterator(system, index);
    }
    
    private static class ComponentIterator implements Iterator<Component> {
        private final int entityIndex;
        private final Iterator<ComponentIndex<?>> indices;
        
        private ComponentIndex<?> currentIndex;
        private ComponentIndex<?> nextIndex;
        
        public ComponentIterator(EntitySystem system, int entityIndex) {
            this.entityIndex = entityIndex;
            indices = system.iterateComponentIndices();
        }
        
        @Override
        public boolean hasNext() {
            if (nextIndex == null)
                advance();
            return nextIndex != null;
        }

        @Override
        public Component next() {
            if (!hasNext())
                throw new NoSuchElementException();
            
            currentIndex = nextIndex;
            nextIndex = null;
            return currentIndex.getComponent(entityIndex);
        }

        @Override
        public void remove() {
            if (currentIndex == null)
                throw new IllegalStateException("Must call next first");
            
            if (currentIndex.removeComponent(entityIndex, null))
                currentIndex = null; // so next call to remove() fails
            else
                throw new IllegalStateException("Already removed");
        }
        
        private void advance() {
            while(indices.hasNext()) {
                nextIndex = indices.next();
                if (nextIndex.getComponentIndex(entityIndex) != 0)
                    break;
                else
                    nextIndex = null; // must set to null if this was last element
            }
        }
    }
}
