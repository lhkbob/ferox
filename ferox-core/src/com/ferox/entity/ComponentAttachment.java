package com.ferox.entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * <p>
 * ComponentAttachment represents an attachment between a single Entity and a
 * Component. It is the responsibility of the Entity to correctly store the
 * ComponentAttachments needed. ComponentAttachment manages the meta-Components
 * and association with the correct ComponentIndex for the attachment and
 * Entity.
 * </p>
 * <p>
 * ComponentAttachment was designed to be a throw-away object. Unless the
 * Component is changing from one non-null Component to another, any change in
 * an Entity's Component set or ownership calls for a new instance of
 * ComponentAttachment.
 * </p>
 * 
 * @author Michael Ludwig
 */
class ComponentAttachment {
    private final Entity owner;
    
    private Component component; // set to null when attachment is broken
    private Map<ComponentId<?>, Component> metaComponents; // null when empty
    
    private final ComponentIndex index;
    
    // linked-list fields used/managed by ComponentIndex
    ComponentAttachment previous;
    ComponentAttachment next;

    /**
     * Create a new ComponentAttachment that links <tt>owner</tt> to
     * <tt>component</tt>. If the Entity has a non-null EntitySystem at the time
     * this is created, the Entity will be added to the appropriate
     * ComponentIndex of its owner.
     * 
     * @param owner The Entity that owns this attachment
     * @param component The Component that owner will be attached to
     * @throws NullPointerException if either argument is null
     */
    public ComponentAttachment(Entity owner, Component component) {
        if (owner == null)
            throw new NullPointerException("Entity owner cannot be null");
        if (component == null)
            throw new NullPointerException("Component cannot be null");
        
        this.owner = owner;
        this.component = component;
        
        if (owner.getSystem() != null) {
            // initialize component index data
            index = owner.getSystem().getIndex(component);
            index.notifyComponentAdd(this);
        } else
            index = null;
    }

    /**
     * Internal constructor used to create 'clones' of ComponentAttachments when
     * an Entity is added or removed from an EntitySystem but its Component set
     * does not change state.
     * 
     * @param toClone The ComponentAttachment to clone, except for the index
     *            information
     * @param index The ComponentIndex to use
     */
    private ComponentAttachment(ComponentAttachment toClone, ComponentIndex index) {
        owner = toClone.owner;
        component = toClone.component;
        // we can just use the same instance, since this constructor is only used
        // when the old attachment is disappearing
        metaComponents = toClone.metaComponents;
        
        this.index = index;
        if (index != null)
            index.notifyComponentAdd(this);
    }
    
    /**
     * @return The Entity that owns this ComponentAttachment
     */
    public Entity getOwner() {
        return owner;
    }
    
    /**
     * @return The Component that is attached to {@link #getOwner()}
     */
    public Component getComponent() {
        return component;
    }

    /**
     * <p>
     * Update the state of this ComponentAttachment to use the new Component.
     * This should be called if a Component of the same type was previously
     * attached to the Entity. A null value signals that the ComponentAttachment
     * should be broken because the previous component has been removed from the
     * Entity.
     * </p>
     * <p>
     * When null is passed and this ComponentAttachment has index information,
     * the Entity will be removed from the appropriate ComponentIndex of its
     * owning EntitySystem. Additionally, any Component change will reset the
     * meta-Components on this attachment.
     * </p>
     * 
     * @param component The new Component, or null if the Component is no longer
     *            meant to be attached
     */
    public void setComponent(Component component) {
        this.component = component;
        metaComponents = null;
        
        // if this attachment is going away, remove it from the index
        if (index != null && component == null)
            index.notifyComponentRemove(this);
    }

    /**
     * Return the meta-Component of the given type that has been associated with
     * this Component attachment. This returns null if there is no attachment.
     * 
     * @param <T> The Component type of the returned meta-Component
     * @param id The ComponentId type of the meta-Component
     * @return The meta-Component of the correct id type, if available, or null
     *         otherwise
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getMetaComponent(ComponentId<T> id) {
        return (metaComponents == null ? null : (T) metaComponents.get(id));
    }

    /**
     * Remove and return the meta-Component that is associated with this
     * Component attachment that has the given id type. If this attachment has
     * no meta-Components or no component of the correct type, then null should
     * be returned.
     * 
     * @param <T> The Component type of the removed meta-Component
     * @param id The ComponentId of the component to remove
     * @return The meta-Component that was previously assigned to this
     *         attachment
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T removeMetaComponent(ComponentId<T> id) {
        if (metaComponents != null) {
            T old = (T) metaComponents.remove(id);
            if (metaComponents.isEmpty())
                metaComponents = null;
            return old;
        } else
            return null;
    }

    /**
     * Add the given Component, <tt>c</tt>, as a meta-Component to be associated
     * with the current Component of this attachment. If
     * {@link #setComponent(Component)} is invoked or
     * {@link #removeMetaComponent(ComponentId)} is called with the appropriate
     * id, the given Component will be removed from this attachment. Otherwise,
     * future calls to {@link #getMetaComponent(ComponentId)} can return
     * <tt>c</tt>.
     * 
     * @param c The new meta-Component to add to this attachment
     */
    public void addMetaComponent(Component c) {
        if (metaComponents == null)
            metaComponents = new HashMap<ComponentId<?>, Component>();
        metaComponents.put(c.getComponentId(), c);
    }
    
    /**
     * @return An immutable set of the current Components stored as
     *         meta-Components for this attachment.
     */
    public Set<Component> getMetaComponents() {
        if (metaComponents == null)
            return Collections.emptySet();
        
        Set<Component> values = new HashSet<Component>(metaComponents.values());
        return Collections.unmodifiableSet(values);
    }

    /**
     * <p>
     * Notify the ComponentAttachment that its owner has just been added to an
     * EntitySystem. This does not modify the state of the ComponentAttachment,
     * but instead returns a new ComponentAttachment that mimics the state of
     * this attachment, except it contains the relevant index information for
     * storing the Entity in its system's ComponentIndex.
     * </p>
     * <p>
     * It is the responsibility of the Entity to call this method as needed,
     * after it will return a non-null EntitySystem from its
     * {@link Entity#getSystem()} method. It is assumed that prior to this
     * call, the ComponentAttachment does not have any index information.
     * </p>
     * 
     * @return A ComponentAttachment meant to replace this attachment, with the
     *         same state information and additional index information for the
     *         new EntitySystem owner
     */
    public ComponentAttachment notifySystemAdd() {
        // we can lookup the index now because owner's system has been set
        // to a non-null EntitySystem if this method is called
        ComponentIndex newIndex = owner.getSystem().getIndex(component);
        return new ComponentAttachment(this, newIndex);
    }

    /**
     * <p>
     * Notify the ComponentAttachment that its owner has been removed from its
     * EntitySystem. This will remove the Entity from the current ComponentIndex
     * and return a new ComponentAttachment that represents the same attachment
     * state, minus said index information.
     * </p>
     * <p>
     * Like {@link #notifySystemAdd()}, Entity must invoke this method at the
     * appropriate time, namely after it has cleared references to its previous
     * owner. It is assumed that prior to this call, the ComponentAttachment has
     * valid index information.
     * </p>
     * 
     * @return A ComponentAttachment meant to replace this attachment, with the
     *         same state information except it maintains no index data
     */
    public ComponentAttachment notifySystemRemove() {
        // if we're being removed from the system, we have a non-null index
        index.notifyComponentRemove(this);
        return new ComponentAttachment(this, null); // don't use any index anymore
    }
    
    @Override
    public String toString() {
        if (metaComponents != null) {
            StringBuilder b = new StringBuilder(component.toString());
            boolean first = true;
            b.append('(');
            for (Entry<ComponentId<?>, Component> e: metaComponents.entrySet()) {
                if (!first) {
                    b.append(',');
                    b.append(' ');
                } else
                    first = false;
                b.append(e.getValue().toString());
            }
            b.append(')');
            
            return b.toString();
        } else
            return component.toString();
    }
}
