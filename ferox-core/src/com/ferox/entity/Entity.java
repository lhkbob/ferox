package com.ferox.entity;

import com.ferox.entity.KeyedLinkedList.Key;

/**
 * <p>
 * Entity is a ComponentContainer that represents live objects or entities
 * within an {@link EntitySystem}. Although both Templates and Entities are
 * ComponentContainers, there purposes are very different. A template is used to
 * construct multiple entities that share a common configuration. Entities are
 * used to embody the actors or nodes within a game or simulation. Some examples
 * of entities could then be players in a multiplayer game, or NPC bots. Other
 * possibilities include particle systems and static geometry.
 * </p>
 * <p>
 * See {@link ComponentContainer} for more information on the design goals of
 * component containers such as Entities and Templates.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Entity extends ComponentContainer {
    private volatile EntitySystem owner;
    private Key<Entity> indexKey;

    /**
     * Create a new Entity that has no attached Components and is not in any
     * EntitySystem.
     */
    public Entity() {
        this((Component[]) null);
    }

    /**
     * Create a new Entity that has the given Components and is not in any
     * EntitySystem. The Components are added to the Entity in the order given,
     * so if multiple Components have the same TypedId, the last will be stored
     * in the Entity. Any null Components or Components already in a container
     * will cause an exception to be thrown.
     * 
     * @param components The initial set of Components
     * @throws NullPointerException if any component is null
     * @throws IllegalStateException if any component is already added to
     *             another container
     */
    public Entity(Component... components) {
        if (components != null && components.length > 0) {
            for (Component c: components)
                add(c);
        }
    }

    /**
     * Get the owning EntitySystem of this Entity. An Entity can only be owned
     * by one EntitySystem at a time. Attempts to add an Entity to another
     * system while it is owned will fail. Entities must be removed from their
     * owner manually first.
     * 
     * @return The EntitySystem that owns this Entity, or null if there is no
     *         current owner
     */
    public EntitySystem getEntitySystem() {
        return owner;
    }

    /**
     * Remove the given Component only if the Component is in the Entity and the
     * Entity is still owned by the given EntitySystem. This is needed in
     * EntitySystem's iterators so that it doesn't remove a Component if the
     * Entity gets removed from underneath it.
     * 
     * @param c The component to remove
     * @param owner The expected EntitySystem owning this entity
     * @return True if the removal was successful
     */
    boolean removeIfOwned(Component c, EntitySystem owner) {
        synchronized(lock) {
            // Hopefully JIT will optimize the inner lock away when
            // remove() is called below.
            if (owner != this.owner)
                return false;
            return remove(c);
        }
    }

    /**
     * Perform all operations necessary to add this Entity to the given
     * EntitySystem. Although the EntitySystem exposes the public interface for
     * adding Entities to itself, this operation requires the Entity to be
     * locked so it is simpler to implement here.
     * 
     * @param newOwner The new owner
     * @return True if successfully added to the system, or false if the Entity
     *         is already owned
     * @throws IllegalStateException if the Entity is already owned, including
     *             if newOwner is the current owner
     * @throws NullPointerException if newOwner is null
     */
    boolean addToEntitySystem(EntitySystem newOwner) {
        synchronized(lock) {
            if (owner != null) {
                // This Entity is owned by a system already. If it's a different system from
                // newOwner the add will fail. If it's already newOwner, we do nothing and
                // return true to be nice.
                return owner == newOwner;
            }
            
            indexKey = newOwner.entityList.add(this);
            owner = newOwner;

            // Update component indices, too. We are properly within the Entity's lock,
            // so this should be a safe operation.
            for (Component c: this)
                c.attachComponent();
            return true;
        }
    }

    /**
     * Perform all operations necessary to remove this Entity from the given
     * EntitySystem. Like {@link #addToEntitySystem(EntitySystem)}, this is
     * publically defined in EntitySystem but is easier implemented within
     * Entity.
     * 
     * @param expectedOwner The expected current owner, must not be null
     * @return True if successfully removed from the system, or false if it has
     *         already been removed or the system isn't the owner
     * @throws IllegalStateException if expectedOwner isn't the current owner
     * @throws NullPointerException if expectedOwner is null
     */
    boolean removeFromEntitySystem(EntitySystem expectedOwner) {
        synchronized(lock) {
            if (expectedOwner != owner)
                return false;
            
            owner.entityList.remove(indexKey);
            owner = null;
            indexKey = null;
            
            // Detach component indices, too. We are properly within the Entity's lock,
            // so this should be a safe operation.
            for (Component c: this)
                c.detachComponent();
            return true;
        }
    }
}
