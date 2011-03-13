package com.ferox.entity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * <p>
 * ComponentContainer is an abstract class that represents a collection of
 * Components. It supports the operations of adding, getting and removing
 * components from containers, and iterating over the components in a container.
 * There are two main subclasses of ComponentContainer. {@link Entity}
 * represents an actor or instance within an {@link EntitySystem} and
 * {@link Template} represents a common configuration of components to easily
 * create multiple related entities.
 * </p>
 * <p>
 * Like the rest of the classes in the entity package, it is thread safe in the
 * sense that multiple threads can operate on it without corrupting its internal
 * data-structures, although the application is still responsible for managing
 * each thread's access to prevent one thread overwriting the changes of
 * another.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class ComponentContainer implements Iterable<Component> {
    private volatile Component[] components;
    private volatile AtomicIntegerArray versions;
    protected final Object lock;

    /**
     * Default constructor that creates a new container with no added
     * components.
     */
    public ComponentContainer() {
        components = new Component[0];
        versions = new AtomicIntegerArray(16); // Versions doesn't shrink, so start with a larger base array
        lock = new Object();
    }

    /**
     * Return an integer representing this ComponentContainer's version for the
     * given type of Component. This version is incremented every time a
     * Component of the given type is added or removed, or when a Component of
     * the given type owned by this Container has its
     * {@link Component#notifyChange()} method invoked.</p>
     * <p>
     * This can be used to process only Entities that have changed.
     * Additionally, this can be checked before accessing the Component on the
     * container.
     * </p>
     * 
     * @param id The Component type
     * @return The current version of the Component type in this container
     * @throws NullPointerException if id is null
     */
    public int getVersion(TypedId<? extends Component> id) {
        if (id == null)
            throw new NullPointerException("TypedId cannot be null");
        
        int index = id.getId();
        AtomicIntegerArray v = versions;
        return  (index < v.length() ? v.get(index) : 0);
    }

    /**
     * <p>
     * Add the given Component to this ComponentContainer. A container can have
     * at most one Component of a given type at a time. If this container
     * already has a Component of the same type as <tt>component</tt>, the old
     * component will be detached from this container and removed.
     * </p>
     * <p>
     * A component can also only be attached to a single container at a time, so
     * this will fail if the component is already owned. False is returned when
     * the add operation failed for any reason. In a multi-threaded scenario, it
     * is possible for two threads to attempt to add the same component to
     * separate containers. In this case one add would succeed and return true,
     * and the other add would fail and return false. If the new component is
     * already attached to this container, nothing is done and true is returned.
     * </p>
     * <p>
     * On a related note, although true might be returned, it does not mean that
     * another thread cannot overwrite this call to add() with a new Component
     * at a later point in time. Care must be given by an application to control
     * access to an container.
     * </p>
     * 
     * @param component The component to add
     * @return True if the add was successful, false otherwise
     * @throws NullPointerException if component is null
     */
    public boolean add(Component component) {
        if (component == null)
            throw new NullPointerException("Component cannot be null");
        int index = component.getTypedId().getId();
        
        synchronized(lock) {
            if (!component.setOwned(this)) {
                // setOwned only succeeds if completely un-owned. Since we hold the container's
                // lock, we can safely compare the component's owner to this container since it's either
                // not this container (and can be or change to anything but this container), or must
                // be owned by this container until after this method releases the lock
                return component.getOwner() == this;
            }
            
            Component[] components = Arrays.copyOf(this.components, Math.max(this.components.length, index + 1));
            Component old = components[index];
            components[index] = component;

            if (old != null)
                old.setUnowned(this);
            this.components = components;
            
            // Must notify a change every time a Component is set on the container
            notifyChange(index);
            return true;
        }
    }

    /**
     * Get the current Component instance of the given type that's attached to
     * this ComponentContainer. Because ComponentContainer can be accessed from
     * multiple threads, it is possible that the returned Component could be
     * subsequently removed by another thread or overwritten by a new Component
     * of the same type. This is dependent on how the application processes
     * entities within an {@link EntitySystem}.
     * 
     * @param <T> The parameterized type of Component being fetched
     * @param id The TypedId representing the given type
     * @return The current Component of type T attached to this container
     * @throws NullPointerException if id is null
     */
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

    /**
     * Remove any Component owned by this container of the given type. The
     * removed component is returned. If null is returned, it means that there
     * was no Component of the given type attached to this container.
     * 
     * @param <T> The parameterized type of the Component being removed
     * @param id The TypedId representing the component type to remove
     * @return The removed Component, or null
     * @throws NullPointerException if id is null
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T remove(TypedId<T> id) {
        if (id == null)
            throw new NullPointerException("TypedId cannot be null");
        return (T) remove(id.getId(), null);
    }

    /**
     * <p>
     * Remove the given Component from this ComponentContainer if-and-only-if
     * the component is currently owned by the container. If the component is
     * not owned, or there is a different component instance of the same type,
     * no remove is performed and false is returned. If true is returned, the
     * Component used to be owned and has been successfully removed.
     * </p>
     * <p>
     * In either case, the Component <tt>c</tt> will not be owned by this
     * container.
     * </p>
     * 
     * @param c The component to remove
     * @return True if the component was owned by this container and is no
     *         longer, false if the component was not owned by this container to
     *         begin with
     * @throws NullPointerException if c is null
     */
    public boolean remove(Component c) {
        if (c == null)
            throw new NullPointerException("Component cannot be null");
        int index = c.getTypedId().getId();
        return remove(index, c) == c;
    }

    /**
     * Internal method to remove the component of the given type. If expected is
     * not null, the remove is only performed if the current component of the
     * type is expected. If expected is null, the remove is always performed
     * (assuming there is a component to remove, of course).
     * 
     * @param index The type, as returned by {@link TypedId#getId()}
     * @param expected The expected Component to remove, or null if any
     *            component can be removed
     * @return The Component that was removed, or null if no component was
     *         removed
     */
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
                    old.setUnowned(this);
                    
                    // Must increment version if a component is removed
                    notifyChange(index);
                }
            }
            
            this.components = components;
            return old;
        }
    }

    /**
     * Increment a change for the given Component type and return it.
     * 
     * @param id The type id whose version is incremented for this Entity
     * @return The new version
     * @throws IndexOutOfBoundsException if id is less than 0
     */
    int notifyChange(int index) {
        AtomicIntegerArray v = versions;
        if (v.length() <= index) {
            // Grow array
            synchronized(lock) {
                v = versions; // Re-read versions within lock in case of a double update
                if (v.length() <= index) {
                    // Note that we perform the atomic write first so that other threads
                    // start using the new reference ASAP. The old array will be added in later
                    versions = new AtomicIntegerArray(index + 1);
                    
                    // Add the original version counters into the new array
                    for (int i = 0; i < v.length(); i++)
                        versions.addAndGet(i, v.get(i));
                }
            }
        }
        
        while(true) {
            int change = v.incrementAndGet(index);
            
            // If at this point, v == versions is true, we know that the
            // above increment will be included in any future copy when 
            // the array needs to grow.
            // If it's not equal, there's a chance the incremented value will
            // be included, but it might also have come in too slow
            if (v == versions)
                return change;
            
            // So to be safe, we update our array reference and try the
            // increment again. This could cause multiple increments for a single
            // "change" but that doesn't cause a problem if users of the version
            // are aware of the possibility and just look for changes in value
            // (instead of specific increment values).
            v = versions;
        }
    }

    /**
     * Called within the lock after a Component has been removed. Does nothing
     * in the base class.
     * 
     * @param removed The removed Component, will not be null
     */
    protected void onRemove(Component removed) {
        // do nothing in base class
    }

    /**
     * Called within the lock after a Component has been added. Does nothing in
     * the base class.
     * 
     * @param added The added Component, will not be null
     * @param old The old Component, may be null
     */
    protected void onAdd(Component added, Component old) {
        // do nothing in base class
    }
    
    @Override
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

    /**
     * <p>
     * Return an iterator over the components of this ComponentContainer. The
     * returned Iterator properly supports concurrent modification, even in
     * multiple threads. It may not return a Component added to the container
     * after the iterator is created, but this depends on the progress within
     * the iterator.
     * </p>
     * <p>
     * The returned iterator will not return removed components unless the
     * "next" component is removed after hasNext() is called but before next()
     * would actually return it. In this situation, the removed component is
     * still returned by the iterator. In this situation, the Component's owner
     * will be null or a different Entity, and invoking remove() on the iterator
     * will do nothing.
     * </p>
     * 
     * @return An iterator over the components within the container
     */
    @Override
    public Iterator<Component> iterator() {
        return new ComponentIterator();
    }

    /**
     * Iterator implementation for iterating through the container's set of
     * components. This supports concurrent modifications and removal of
     * components.
     */
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
        }
        
        @Override
        public boolean hasNext() {
            if (nextValue == null)
                advance();
            return nextValue != null;
        }

        @Override
        public Component next() {
            // Use hasNext() instead of comparing directly against nextValue in
            // case hasNext() wasn't called prior to next()
            if (!hasNext())
                throw new NoSuchElementException();
            
            // Store the current value for later in remove(), so
            // we can make sure to remove the proper Component
            currentValue = nextValue;
            
            // Set nextValue to null so that the next call to hasNext() or
            // next() triggers an advance()
            nextValue = null;
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
            Component next = null;
            Component[] components = ComponentContainer.this.components;
            while(next == null && ++nextIndex < maxLen) {
                next = components[nextIndex];
            }
            
            nextValue = next;
        }
    }
}