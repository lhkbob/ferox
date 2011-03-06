package com.ferox.entity;

/**
 * ComponentEvent is a sub-type of EntityEvent that represents when a
 * {@link Component} is added or removed from an {@link Entity}. If the
 * Component is added, the ComponentEvent also reports the old Component that
 * was attached to the Entity.
 * 
 * @author Michael Ludwig
 */
public class ComponentEvent extends EntityEvent {
    private final Component mainComponent;
    private final Component auxComponent;

    /**
     * Create a new ComponentEvent for the given Entity in the given
     * EntitySystem with the given primary Component. There is no auxiliary
     * component.
     * 
     * @param entity The entity that was edited
     * @param system The system owning the entity
     * @param comp The primary component that was added or removed
     * @throws NullPointerException if mainComp or entity or system is null
     */
    public ComponentEvent(Entity entity, EntitySystem system, Component comp) {
        this(entity, system, comp, null);
    }
    
    /**
     * Create a new ComponentEvent for the given Entity in the given EntitySystem
     * with the given primary and auxiliary Components. This should only be used
     * for an add event, and the auxiliary component is the old component on the
     * Entity.
     * @param entity The entity that was edited
     * @param system The system owning the entity
     * @param mainComp The primary component that was added
     * @param auxComp The old component of the same type as the primary
     * @throws NullPointerException if mainComp or entity or system is null
     */
    public ComponentEvent(Entity entity, EntitySystem system, Component mainComp, Component auxComp) {
        super(entity, system);
        if (mainComp == null)
            throw new NullPointerException("Primary Component cannot be null");
        mainComponent = mainComp;
        auxComponent = auxComp;
    }

    /**
     * Get the primary component of this ComponentEvent. This is the new
     * Component that was added if this event represents an add, or the
     * Component that was removed from the Entity.
     * 
     * @return The Component that was added or removed, depending on the type of
     *         the ComponentEvent
     */
    public Component getPrimaryComponent() {
        return mainComponent;
    }

    /**
     * Get the auxiliary component of this ComponentEvent. This is only non-null
     * when a Component is added and replaces an already attached Component, at
     * which point the auxiliary component is that old Component. This will be
     * null if the ComponentEvent signals a removal, or if there was no replaced
     * Component during the add.
     * 
     * @return The old Component that was replaced by the primary Component when
     *         it was added to an Entity.
     */
    public Component getAuxiliaryComponent() {
        return auxComponent;
    }
}
