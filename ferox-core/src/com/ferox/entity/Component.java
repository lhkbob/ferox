package com.ferox.entity;

import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.ferox.entity.KeyedLinkedList.Key;

/**
 * <p>
 * Component represents a set of self-consistent state that can be added to an
 * {@link Entity} or {@link Template}. Components added to a Template are used
 * to specify a common configuration for Entities created from the template. The
 * set of components of a ComponentContainer effectively create a composite
 * class type based on all of the components. Components are intended to be data
 * storage objects, so their definition should not contain methods for
 * processing or updating (that is the responsibility of a {@link Controller}).
 * </p>
 * <p>
 * The behavior or purpose of a Component should be well defined, even in
 * isolation. Component definitions should not require an entity or template to
 * have other components added to function correctly. Other components may be
 * needed or desirable to get useful functionality. Often there is the case
 * where a number of components have a common subset of properties, such as
 * transform or bounds. In this case, it is better to have each Component define
 * the transform and their other properties, and another Component define only a
 * transform with a controller that would synchronize the complex Component with
 * the transform.
 * </p>
 * <p>
 * Each Component class gets a {@link TypedId}, which can be looked up with
 * {@link #getTypedId(Class)}, passing in a desired class type. Because the
 * entity-component design pattern does not follow common object-oriented
 * principles, certain rules are followed when handling Component types in a
 * class hierarchy:
 * <ol>
 * <li>Any abstract type extending Component cannot get a TypedId</li>
 * <li>All concrete classes extending Component get separate TypedIds, even if
 * they extend from the same intermediate class below Component.</li>
 * <li>All intermediate classes in a Component type's hierarchy must be abstract
 * or runtime exceptions will be thrown.</li>
 * </ol>
 * As an example, an abstract component could be Light, with concrete subclasses
 * SpotLight and DirectionLight. SpotLight and DirectionLight would be separate
 * component types as determined by TypedId. Light would not have any TypedId
 * and only serves to consolidate property definition among related component
 * types.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class Component {
    // Use a ConcurrentHashMap to perform reads. It is still synchronized completely to do
    // an insert to make sure a type doesn't try to use two different id values.
    private static final ConcurrentHashMap<Class<? extends Component>, TypedId<? extends Component>> typeMap = new ConcurrentHashMap<Class<? extends Component>, TypedId<? extends Component>>();
    private static int idSeq = 0;
    
    private final TypedId<? extends Component> id;
    private final AtomicReference<ComponentContainer> owner;
    
    // This is guarded by the lock of the current owner
    private Key<Component> indexKey;

    /**
     * Create a Component, which looks up the appropriate TypedId via
     * {@link #getTypedId(Class)}.
     * 
     * @throws IllegalComponentHierarchyException if getTypedId fails
     */
    public Component() {
        id = getTypedId(getClass());
        owner = new AtomicReference<ComponentContainer>();
        indexKey = null;
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

    /**
     * Get the current ComponentContainer that owns this Component. The
     * ComponentContainer will either be an Entity or a Template. If it is an
     * Entity, it may be part of an EntitySystem, in which case this Component
     * can be iterated over via {@link EntitySystem#iterator(TypedId)}. If null
     * is returned, the Component is not owned by any container. A Component
     * cannot be added to another container if it owned; it must be removed
     * manually first.
     * 
     * @return The current owner
     */
    public ComponentContainer getOwner() {
        return owner.get();
    }

    /**
     * Notify this Component's owner of a change that should update the owner's
     * version for this Component's type. The new version is returned. It is
     * recommended that subclasses automatically call this method and return the
     * version in their exposes setter methods. If the Component has no owner,
     * -1 is returned.
     * 
     * @return The new version
     */
    public int notifyChange() {
        ComponentContainer owner = this.owner.get();
        return (owner != null ? owner.notifyChange(id.getId()) : -1);
    }

    /**
     * Attempt to declare ownership of this Component for newOwner. This returns
     * false if another ComponentContainer gets ownership first, in which case
     * the ComponentContainer cannot add the Component to its data structures.
     * This should only be called when the lock for newOwner is held. If the new
     * owner is in an EntitySystem, this Component will be attached to the
     * system's component indices. This returns false if any container owns the
     * component, including if newOwner already owns it.
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
        
        // Do a look up first without locking to avoid the synchronized lock and expensive
        // error checking.  If we found one, we know it passed validation the first time, otherwise
        // we'll validate it before creating a new TypedId.
        TypedId<T> id = (TypedId<T>) typeMap.get(type);
        if (id != null)
            return id; // Found an existing id
        
        if (!Component.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Type must be a subclass of Component: " + type);
        
        // Validate the class hierarchy such that this class is not abstract, and all parent classes
        // below Component are abstract (which includes TypedComponent).
        if (Modifier.isAbstract(type.getModifiers()))
            throw new IllegalArgumentException("Component class type cannot be abstract: " + type);
        Class<? super T> parent = type.getSuperclass();
        while(!Component.class.equals(parent)) {
            if (Modifier.isAbstract(parent.getModifiers()))
                throw new IllegalComponentHierarchyException(type, parent);
            parent = parent.getSuperclass();
        }

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
