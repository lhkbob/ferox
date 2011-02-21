package com.ferox.entity;

import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.ferox.entity.KeyedLinkedList.Key;

public class Component {
    // Use a ConcurrentHashMap to perform reads. It is still synchronized completely to do
    // an insert to make sure a type doesn't try to use two different id values.
    private static final ConcurrentHashMap<Class<? extends Component>, TypedId<? extends Component>> typeMap = new ConcurrentHashMap<Class<? extends Component>, TypedId<? extends Component>>();
    private static int idSeq = 0;
    
    private final TypedId<? extends Component> id;
    private final AtomicReference<ComponentContainer> owner;
    
    // This is guarded by the lock of the current owner
    private Key<Component> indexKey;
    
    public Component() {
        id = getTypedId();
        owner = new AtomicReference<ComponentContainer>();
        indexKey = null;
    }
    
    public ComponentContainer getOwner() {
        return owner.get();
    }

    /**
     * Attempt to declare ownership of this Component for newOwner. This returns
     * false if another ComponentContainer gets ownership first, in which case
     * the ComponentContainer cannot add the Component to its data structures.
     * This should only be called when the lock for newOwner is held. If the new
     * owner is in an EntitySystem, this Component will be attached to the
     * system's component indices.
     * 
     * @param newOwner The ComponentContainer that wants to own this Component
     * @return True if the the container now owns the component
     */
    boolean setOwned(ComponentContainer newOwner) {
        if (!this.owner.compareAndSet(null, newOwner))
            return false;
        
        // At this point no one else can take ownership of this Component.
        // Because this is called only within the lock of a ComponentContainer,
        // we do not need to worry about the container un-owning this while
        // setting up its indices into the EntitySystem.
        attachComponent();
        return true;
    }

    /**
     * Attach this Component to the Component indices of its owner's
     * EntitySystem. If the current owner is not an Entity, this does nothing
     * because only Entities can be attached to a system. Similarly, if the
     * Entity isn't in a system, nothing is done (the Entity must then call this
     * method when it is added to a system). This can only be called when the
     * lock for the Component's owner is held.
     * 
     * @throws IllegalStateException if the Component is already attached
     */
    void attachComponent() {
        ComponentContainer o = owner.get();
        if (o instanceof Entity) {
            if (indexKey != null)
                throw new IllegalStateException("Component is already attached to an index");
            
            EntitySystem system = ((Entity) o).getEntitySystem();
            if (system != null) {
                KeyedLinkedList<Component> index = system.getComponentIndex(id);
                indexKey = index.add(this);
            }
        } // else do nothing
    }

    /**
     * Remove ownership from this Component, with the assertion that oldOwner is
     * the current owner. This should only be called inside the lock of oldOwner
     * or critical thread-safety assumptions will be broken. If the old owner
     * was part of an EntitySystem, the Component will be detached from any
     * component indices it was part of.
     * 
     * @param oldOwner The expected current owner
     * @throws IllegalStateException if oldOwner is not the current owner, if
     *             this is thrown data inconsisties or leaks are likely to
     *             occur. Should not happen if this is called within oldOwner's
     *             lock
     */
    void setUnowned(ComponentContainer oldOwner) {
        // Detach any component indices before setting the owner to null.
        // Since we're within the container's lock, we don't have to worry about
        // the owner un-owning this component twice.
        detachComponent();
        
        // However, we set the owner to null at the end to prevent anyone
        // from trying to attach the component before we've detached the indices,
        // since other containers aren't synchronized.
        if (this.owner.compareAndSet(oldOwner, null))
            throw new IllegalStateException("Component already unowned - should never happen!");
    }

    /**
     * Detach this Component from any Component indices of its current owner's
     * EntitySystem. If it has no indices, then nothing happens. This is only
     * safe to call within the owner's lock. It is the responsibility of the
     * owner to call this when it is removed from an EntitySystem.
     * 
     * @throws IllegalStateException if the Component should have an index to
     *             detach from but doesn't
     */
    void detachComponent() {
        ComponentContainer o = owner.get();
        if (o instanceof Entity) {
            EntitySystem system = ((Entity) o).getEntitySystem();
            if (system != null) { 
                if (indexKey == null)
                    throw new IllegalStateException("Component already detached from index");
                KeyedLinkedList<Component> index = system.getComponentIndex(id);
                index.remove(indexKey);
                indexKey = null;
            }
        }
    }
    
    /**
     * <p>
     * Return the unique TypedId associated with this Component's class type.
     * All Components of the same class will return this id, too.
     * </p>
     * <p>
     * It is recommended that implementations override this method to use the
     * proper return type. Component does not perform this cast to avoid
     * parameterizing Component. Do not change the actual returned instance,
     * though.
     * </p>
     * 
     * @return The TypedId of this Component
     */
    public TypedId<? extends Component> getTypedId() {
        return id;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /**
     * Return the unique TypedId instance for the given <tt>type</tt>. If a
     * TypedId hasn't yet been created a new one is instantiated with the next
     * numeric id in the internal id sequence. The new TypedId is stored for
     * later, so that subsequent calls to {@link #getTypedId(Class)} with
     * <tt>type</tt> will return the same instance.
     * {@link Component#Component()} implicitly calls this method when a
     * Component is created.
     * 
     * @param <T> The Component class type
     * @param type The Class whose ComponentId is fetched, which must be a
     *            subclass of Component
     * @return A unique TypedId associated with the given type
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if type is not actually a subclass of
     *             Component, or if it is abstract
     */
    @SuppressWarnings("unchecked")
    public static <T extends Component> TypedId<T> getTypedId(Class<T> type) {
        if (type == null)
            throw new NullPointerException("Type cannot be null");
        if (!Component.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Type must be a subclass of Component: " + type);
        if (Modifier.isAbstract(type.getModifiers()))
            throw new IllegalArgumentException("Component class type cannot be abstract: " + type);

        TypedId<T> id = (TypedId<T>) typeMap.get(type);
        if (id != null)
            return id; // Found an existing id
        
        synchronized(typeMap) {
            // Must create a new id, we lock completely to prevent concurrent getTypedId() on the
            // same type using two different ids.  One would get overridden and its returned TypedId
            // would be invalid.
            // - Double check, though, before creating a new id
            id = (TypedId<T>) typeMap.get(type);
            if (id != null)
                return id; // Someone else put the type after we checked but before we locked
            
            id = new TypedId<T>(type, idSeq++);
            typeMap.put(type, id);
            return id;
        }
    }
}
