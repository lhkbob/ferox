package com.ferox.entity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class ComponentContainer implements Iterable<Component> {
    private volatile Component[] components;
    protected final Object lock;
    
    public ComponentContainer() {
        components = new Component[0];
        lock = new Object();
    }
    
    public boolean add(Component component) {
        if (component == null)
            throw new NullPointerException("Component cannot be null");
        int index = component.getTypedId().getId();
        
        synchronized(lock) {
            if (!component.setOwned(this))
                return false;
            
            Component[] components = Arrays.copyOf(this.components, Math.max(this.components.length, index + 1));
            Component old = components[index];
            components[index] = component;

            if (old != null)
                old.setUnowned(null);
            this.components = components;
            return true;
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Component> T get(TypedId<T> id) {
        if (id == null)
            throw new NullPointerException("TypedId cannot be null");
        int index = id.getId();
        
        Component[] c = components;
        if (index < c.length)
            return (T) components[index];
        else
            return null;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Component> T remove(TypedId<T> id) {
        if (id == null)
            throw new NullPointerException("TypedId cannot be null");
        return (T) remove(id.getId(), null);
    }
    
    public boolean remove(Component c) {
        if (c == null)
            throw new NullPointerException("Component cannot be null");
        int index = c.getTypedId().getId();
        return remove(index, c) == c;
    }
    
    private Component remove(int index, Component expected) {
        synchronized(lock) {
            Component old = null;
            Component[] components = this.components;
            if (index < components.length && (expected == null || components[index] == expected)) {
                old = components[index];
                
                if (old != null) {
                    int newLen = (index == components.length - 1 ? index : components.length);
                    components = Arrays.copyOf(this.components, newLen);
                    components[index] = null;
                    old.setUnowned(null);
                }
            }
            
            this.components = components;
            return old;
        }
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(getClass().getSimpleName());
        sb.append(": {");
        
        for (Component c: this)
            sb.append(c);
        
        sb.append("})");
        return sb.toString();
    }
    
    @Override
    public Iterator<Component> iterator() {
        return new ComponentIterator();
    }
    
    private class ComponentIterator implements Iterator<Component> {
        private final int maxLen;
        
        private int nextIndex;
        private Component nextValue;
        
        private Component currentValue;
        
        public ComponentIterator() {
            maxLen = components.length;
            nextIndex = 0;
            nextValue = (maxLen > 0 ? components[0] : null);
            currentValue = null;
            
            if (nextValue == null)
                advance();
        }
        
        @Override
        public boolean hasNext() {
            return nextValue != null;
        }

        @Override
        public Component next() {
            if (nextValue == null)
                throw new NoSuchElementException();
            
            // Store the current value for later in remove(), so
            // we can make sure to remove the proper Component
            currentValue = nextValue;
            advance();
            return currentValue;
        }

        @Override
        public void remove() {
            if (currentValue == null)
                throw new IllegalStateException("Must call next() first");
            
            ComponentContainer.this.remove(currentValue);
            currentValue = null;
        }
        
        private void advance() {
            nextValue = null; // Reset nextValue so we iterate at least once
            Component[] components = ComponentContainer.this.components;
            while(nextValue == null && ++nextIndex < maxLen) {
                nextValue = components[nextIndex];
            }
        }
    }
}
