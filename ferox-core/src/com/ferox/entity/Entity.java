package com.ferox.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * <p>
 * An Entity represents a collection of {@link Component Components} in a given
 * state. The Entity is dynamic and can have Components added and removed at any
 * time. In many ways, an Entity is a conglomerate object type. An Entity's type
 * or functionality is determined by the exact composition of the Components
 * attached to it. Entities are intentionally data storage objects and, along
 * with their Components, do not provide functionality in themselves. They
 * describe the state of a system and {@link Controller Controllers} are used to
 * perform the logic as needed to move the system from one state to the next.
 * The behaviors produced by such Controllers are often dependent on the
 * relationships between the Components of an Entity. As an example, an Entity
 * representing a bullet collides with an Entity containing a physics Component.
 * This triggers a handler that notices the hit Entity also has a health
 * Component, so hit points are removed. These fall below zero and a Controller
 * that removes "dead" Entities cleans up the Entity.
 * </p>
 * <p>
 * The Entity class is intended to be thread safe although it uses fairly course
 * locking. Retrieving the Components of an Entity are going to be significantly
 * faster and more parallelizable than modifications to an Entity's Component
 * set. The Iterator returned by {@link #iterator()} is also thread safe and
 * will not fail with a {@link ConcurrentModificationException} if the Entity is
 * modified while iterating. It will do its best to return the state of the
 * Entity at the time {@link #iterator()} was invoked but iteration may not
 * reflect the exact state. Additionally, invoking {@link Iterator#remove()} on
 * the returned Iterator will detach the current Component from the iterated
 * Entity, which is a reasonable behavior.
 * </p>
 * 
 * @see Component
 * @see EntitySystem
 * @author Michael Ludwig
 */
public final class Entity implements Iterable<Component> {
    private final List<EntityListener> listeners;
    
    private final Object lock;
    private ComponentAttachment[] components;
    
    private EntitySystem owner;
    private EntityNode ownerNode;
    
    /**
     * Create an Entity that has no attached Components and is not assigned to
     * an {@link EntitySystem}.
     */
    public Entity() {
        this((Component[]) null);
    }

    /**
     * Create an Entity that has the given Components attached to it, but is not
     * assigned to an {@link EntitySystem}. If there are multiple Components
     * with the same ComponentId, it is undefined which Component will be used.
     * 
     * @param components Var-args containing a list of Components to add to the
     *            Entity
     * @throws NullPointerException if any element of <tt>components</tt> is null
     */
    public Entity(Component... components) {
        listeners = new ArrayList<EntityListener>();
        lock = new Object();
        
        this.components = new ComponentAttachment[0];
        owner = null;
        ownerNode = null;
        
        if (components != null) {
            for (int i = 0; i < components.length; i++)
                add(components[i]);
        }
    }

    /**
     * Get the EntitySystem owner of this Entity. If null is returned, the
     * Entity is not a member of any EntitySystem. An Entity becomes owned by
     * invoking {@link EntitySystem#add(Entity)}.
     * 
     * @return The EntitySystem that owns this Entity, or null
     */
    public EntitySystem getSystem() {
        return owner;
    }

    /**
     * Add the given EntityListener to this Entity. Future modifications to this
     * Entity will invoke the appropriate methods on <tt>listener</tt>. If
     * <tt>listener</tt> is already listening on this Entity, no change will
     * occur.
     * 
     * @param listener The EntityListener to add
     * @throws NullPointerException if listener is null
     */
    public void addListener(EntityListener listener) {
        if (listener == null)
            throw new NullPointerException("Listener cannot be null");
        
        synchronized(lock) {
            if (!listeners.contains(listener))
                listeners.add(listener);
        }
    }

    /**
     * Remove the given EntityListener from this Entity. Future modifications to
     * this Entity will no longer invoke the methods on <tt>listener</tt>. If
     * <tt>listener</tt> was never listening on this Entity, this method does
     * nothing.
     * 
     * @param listener The EntityListener to remove
     * @throws NullPointerException if listener is null
     */
    public void removeListener(EntityListener listener) {
        if (listener == null)
            throw new NullPointerException("Listener cannot be null");
        
        synchronized(lock) {
            listeners.remove(listener);
        }
    }

    /**
     * <p>
     * Attach the given Component, <tt>c</tt>, to this Entity. This will replace
     * any previously attached Component of the same type. If <tt>c</tt> is not
     * equal to the current Component of the same type, all meta-Components are
     * cleared for the component type. The following will always be true:
     * 
     * <pre>
     * entity.add(c).get(c.getComponentId()) == c
     * </pre>
     * 
     * </p>
     * <p>
     * If the Entity has an owner, the Entity will be included in the iterators
     * returned from {@link EntitySystem#iterator(ComponentId)} if <tt>c</tt>'s
     * ComponentId is used. Any {@link EntityListener listeners} on this Entity
     * will have their {@link EntityListener#onComponentAdd(Entity, Component)}
     * invoked before <tt>c</tt> overwrites any previously attached Component.
     * </p>
     * 
     * @param c The Component to add
     * @throws NullPointerException if c is null
     */
    public void add(Component c) {
        if (c == null)
            throw new NullPointerException("Component cannot be null");
        int index = c.getComponentId().getId();
        
        synchronized(lock) {
            for (int i = listeners.size() - 1; i >= 0; i--)
                listeners.get(i).onComponentAdd(this, c);
            
            if (index >= components.length)
                components = Arrays.copyOf(components, index + 1);
            
            if (components[index] != null)
                components[index].setComponent(c);
            else
                components[index] = new ComponentAttachment(this, c);
        }
    }

    /**
     * Get the Component instance attached to this Entity with the given
     * ComponentId type. If this Entity has no Component of the given type, then
     * null is returned. When determining if an Entity has the given Component,
     * only Components that are added via {@link #add(Component)} are
     * considered. Meta-Components are not included, and can only be retrieved
     * via {@link #getMeta(Component, ComponentId)}.
     * 
     * @param <T> The Component type to look-up
     * @param id The ComponentId associated with T
     * @return The Component of type T that's been attached to this Entity
     * @throws NullPointerException if id is null
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T get(ComponentId<T> id) {
        if (id == null)
            throw new NullPointerException("ComponentId cannot be null");
        
        int index = id.getId();
        if (index < components.length) {
            ComponentAttachment a = components[index];
            return (a != null ? (T) a.getComponent() : null);
        } else
            return null;
    }

    /**
     * Remove and return the Component with the given id type. Null is returned
     * if this Entity has no Component of the given type (i.e. get(id) returns
     * null). After being removed, subsequent calls to get(id) will return null
     * and the Entity will no longer be reported in iterators from
     * {@link EntitySystem#iterator(ComponentId)} using <tt>id</tt>.
     * Additionally, any meta-Components assigned to the returned Component will
     * be discarded. Even if the Component is re-added, the meta-Components will
     * be lost until they are also re-added.
     * 
     * @param <T> The Component type that is to be removed
     * @param id The ComponentId corresponding to type T
     * @return The Component of type T that was previously attached to this
     *         Entity, or null if no such Component existed
     * @throws NullPointerException if id is null
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T remove(ComponentId<T> id) {
        if (id == null)
            throw new NullPointerException("ComponentId cannot be null");
        
        int index = id.getId();
        synchronized(lock) {
            if (index < components.length && components[index] != null) {
                T old = (T) components[index].getComponent();
                for (int i = listeners.size() - 1; i >= 0; i--)
                    listeners.get(i).onComponentRemove(this, old);
                
                components[index].setComponent(null);
                components[index] = null;
                
                return old;
            } else
                return null;
        }
    }

    /**
     * Remove the specific Component from this Entity. This functions
     * identically to {@link #remove(ComponentId)} using <tt>c</tt>'s
     * ComponentId, except that it is only removed if <tt>c</tt> is currently
     * attached to this Entity. This returns true when <tt>c</tt>, and false
     * when <tt>c</tt> was not removed because it was not attached to this
     * Entity. '==' equality is used when determining if <tt>c</tt> is currently
     * attached.
     * 
     * @param c The Component to remove
     * @return True if <tt>c</tt> was removed
     * @throws NullPointerException if c is null
     */
    public boolean remove(Component c) {
        if (c == null)
            throw new NullPointerException("ComponentId cannot be null");
        
        int index = c.getComponentId().getId();
        synchronized(lock) {
            if (index < components.length && components[index] != null && components[index].getComponent() == c) {
                for (int i = listeners.size() - 1; i >= 0; i--)
                    listeners.get(i).onComponentRemove(this, c);
                
                components[index].setComponent(null);
                components[index] = null;

                return true;
            } else
                return false;
        }
    }
    
    /**
     * <p>
     * Attach the Component, <tt>meta</tt>, to this Entity as a meta-Component
     * associated with <tt>c</tt>. A meta-Component is not considered
     * attached to the Entity when {@link #get(ComponentId)} or
     * {@link EntitySystem#iterator(ComponentId)} is invoked. A Component in an
     * Entity can have multiple meta-Components associated with it, but only one
     * meta-Component for a given type. This functions identically to how an
     * Entity can have multiple Components, but only one for each type.
     * </p>
     * <p>
     * A meta-Component's lifetime is limited to the lifetime of the Component
     * it's associated with. When <tt>attach</tt> is removed from the Entity,
     * all meta-Components on <tt>attach</tt> are removed as well. Because of
     * this, if <tt>attach</tt> is not attached to this Entity at the time
     * {@link #addMeta(Component, Component)} is called, the meta-Component will
     * not be assigned. True is returned when <tt>meta</tt> is correctly
     * associated and false when <tt>attach</tt> is not attached to this Entity.
     * </p>
     * 
     * @param <T> The Component type of the meta-Component
     * @param attach The Component in this Entity that the meta-Component is
     *            associated to
     * @param meta The meta-Component that will be stored on <tt>attach</tt> for
     *            this Entity
     * @return True if the meta-Component is successfully added
     * @throws NullPointerException if attach or meta are null
     */
    public <T extends Component> boolean addMeta(Component attach, T meta) {
        if (attach == null || meta == null)
            throw new NullPointerException("Arguments cannot be null");
        
        int index = attach.getComponentId().getId();
        synchronized(lock) {
            if (index < components.length && components[index] != null && components[index].getComponent() == attach) {
                components[index].addMetaComponent(meta);
                return true;
            } else
                return false;
        }
    }

    /**
     * Get the meta-Component instance that's associated with <tt>attach</tt>
     * and has the specified id (<tt>meta</tt>). If <tt>attach</tt> is not a
     * member of this Entity anymore, or if it has not had any meta-Component of
     * the given type associated with it, null is returned. A non-null Component
     * is returned only if {@link #addMeta(Component, Component)} has been
     * previously invoked with <tt>attach</tt> and a meta-Component with the
     * correct id, <b>and</b> <tt>attach</tt> has not already been removed from
     * this Entity.
     * 
     * @param <T> The Component type of the meta-Component to look-up
     * @param attach The Component which specifies the set of meta-Components to
     *            search within
     * @param meta The ComponentId of the type for the requested meta-Component
     * @return The meta-Component of type T that's been associated with
     *         <tt>attach</tt>
     * @throws NullPointerException if attach or meta are null
     */
    public <T extends Component> T getMeta(Component attach, ComponentId<T> meta) {
        if (attach == null || meta == null)
            throw new NullPointerException("Arguments cannot be null");
        
        int index = attach.getComponentId().getId();
        if (index < components.length) {
            ComponentAttachment c = components[index];
            if (c != null && c.getComponent() == attach)
                return c.getMetaComponent(meta);
        }
        
        return null;
    }
    
    /**
     * Remove and return the meta-Component with id, <tt>meta</tt>, that's
     * associated with <tt>attach</tt>. After a call to this method,
     * {@link #getMeta(Component, ComponentId)} using <tt>attach</tt> and
     * <tt>meta</tt> will return null. If <tt>attach</tt> is not currently
     * attached to this Entity, or if <tt>attach</tt> has no meta-Component of
     * the given id type.
     * 
     * @param <T> The Component type of the meta-Component to remove
     * @param attach The Component whose meta-Component will be removed
     * @param meta The ComponentId matching type T
     * @return The meta-Component of type T that was previously associated with
     *         <tt>attach</tt>, or null if <tt>attach</tt> was not attached to
     *         this Entity or had no meta-Component of type T
     * @throws NullPointerException if attach or meta are null
     */
    public <T extends Component> T removeMeta(Component attach, ComponentId<T> meta) {
        if (attach == null || meta == null)
            throw new NullPointerException("Arguments cannot be null");
        
        int index = attach.getComponentId().getId();
        synchronized(lock) {
            if (index < components.length && components[index] != null && components[index].getComponent() == attach)
                return components[index].removeMetaComponent(meta);
            else
                return null;
        }
    }

    /**
     * Return an unmodifiable set of all meta-Components that are currently
     * attached to the Component, <tt>c</tt> on this Entity. If <tt>c</tt> is
     * not attached to this Entity or it has no meta-Components the empty set is
     * returned.
     * 
     * @param c The Component whose meta-Components are queried for
     * @return The current set of meta-Components on c
     * @throws NullPointerException if c is null
     */
    public Set<Component> getMetaComponents(Component c) {
        if (c == null)
            throw new NullPointerException("Component cannot be null");
        
        int index = c.getComponentId().getId();
        if (index < components.length && components[index] != null)
            return components[index].getMetaComponents();
        else
            return Collections.emptySet();
    }
    
    @Override
    public Iterator<Component> iterator() {
        return new ComponentIterator();
    }

    /**
     * @return The EntityNode that was last assigned to this Entity by
     *         {@link #notifySystemAdd(EntitySystem, EntityNode)}, or null if
     *         this Entity is not owned by an EntitySystem.
     */
    EntityNode getNode() {
        return ownerNode;
    }

    /**
     * <p>
     * Notify the Entity that it has been added to <tt>system</tt> and should
     * store the given EntityNode, which represents this Entity's attachment to
     * the system. This will also invoke the EntityListeners as needed and cause
     * the Entity's attached Components to be added to the ComponentIndex's of
     * the EntitySystem.
     * </p>
     * <p>
     * Although the Entity stores the EntityNode, it does not modify its
     * properties. This is the responsibility of the EntitySystem. This should
     * not be called unless the Entity was previously un-owned.
     * </p>
     * 
     * @param system The EntitySystem which now owns the Entity
     * @param node The EntityNode storing the Entity's position in the system's
     *            linked list storage
     */
    void notifySystemAdd(EntitySystem system, EntityNode node) {
        synchronized(lock) {
            for (int i = listeners.size() - 1; i >= 0; i--)
                listeners.get(i).onSystemAdd(this, system);

            owner = system;
            ownerNode = node;
            
            for (int i = 0; i < components.length; i++) {
                // new ComponentAttachments are returned by notifySystemAdd
                // so that the old attachment can remain unmodified if being
                // used on another thread
                if (components[i] != null)
                    components[i] = components[i].notifySystemAdd();
            }
        }
    }

    /**
     * <p>
     * Notify the Entity that it has been removed from its previous owner. This
     * will set this Entity's owner to null, clear the previously assigned
     * EntityNode and invoke the appropriate EntityListener's. It will also
     * remove the Entity's attached Components from its owner's
     * ComponentIndex's.
     * </p>
     * <p>
     * Like {@link #notifySystemAdd(EntitySystem, EntityNode)}, this should only
     * be called when the Entity is removed from its current owner.
     * </p>
     */
    void notifySystemRemove() {
        synchronized(lock) {
            for (int i = listeners.size() - 1; i >= 0; i--)
                listeners.get(i).onSystemRemove(this, owner);

            owner = null;
            ownerNode = null;
            
            for (int i = 0; i < components.length; i++) {
                // new ComponentAttachments are returned by notifySystemRemove
                // so that the old attachment can remain unmodified if being
                // used on another thread (namely being iterated over)
                if (components[i] != null)
                    components[i] = components[i].notifySystemRemove();
            }
        }
    }
    
    private class ComponentIterator implements Iterator<Component> {
        int index;
        final int maxComponentId;
        
        public ComponentIterator() {
            maxComponentId = components.length - 1;
            index = -1;
        }
        
        @Override
        public boolean hasNext() {
            for (int i = index + 1; i <= maxComponentId; i++) {
                if (components[i] != null)
                    return true;
            }
            return false;
        }

        @Override
        public Component next() {
            for (int i = index + 1; i <= maxComponentId; i++) {
                if (components[i] != null) {
                    index = i;
                    return components[i].getComponent();
                }
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            if (index < 0)
                throw new IllegalStateException("Must call next() before first calling remove()");
            if (components[index] == null)
                throw new IllegalStateException("Already called remove()");
            
            ComponentId<?> id = components[index].getComponent().getComponentId();
            Entity.this.remove(id);
        }
    }
}
