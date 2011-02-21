package com.ferox.entity;

public class EntityEvent {
    private final Entity entity;
    private final EntitySystem system;
    
    public EntityEvent(Entity entity, EntitySystem system) {
        if (entity == null || system == null)
            throw new NullPointerException("Arguments cannot be null");
        this.entity = entity;
        this.system = system;
    }
    
    public Entity getEntity() {
        return entity;
    }
    
    public EntitySystem getEntitySystem() {
        return system;
    }
}
