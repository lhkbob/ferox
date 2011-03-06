package com.ferox.entity;

/**
 * EntityEvents are generated when an {@link Entity} is added to or removed from
 * an {@link EntitySystem}. The subclass of EntityEvent, {@link ComponentEvent}
 * is generated when a Component is added to or removed from an Entity that is
 * owned by an EntitySystem. EntityListeners may be registered with an
 * EntitySystem to receive these events. Entities must be owned by an
 * EntitySystem with listeners in order to generate events.
 * 
 * @author Michael Ludwig
 */
public class EntityEvent {
    private final Entity entity;
    private final EntitySystem system;

    /**
     * Create a new EntityEvent storing the given Entity and EntitySystem. The
     * type of event can only be determined by the method its passed to for an
     * EntityListener.
     * 
     * @param entity The entity
     * @param system The system
     * @throws NullPointerException if entity or system is null
     */
    public EntityEvent(Entity entity, EntitySystem system) {
        if (entity == null || system == null)
            throw new NullPointerException("Arguments cannot be null");
        this.entity = entity;
        this.system = system;
    }

    /**
     * @return The Entity that is affected by the event (i.e. it was
     *         added/removed to the system, or had its components edited)
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * @return The EntitySystem that spawned the event (i.e. the system that
     *         owns the Entity, or was the owner)
     */
    public EntitySystem getEntitySystem() {
        return system;
    }
}
