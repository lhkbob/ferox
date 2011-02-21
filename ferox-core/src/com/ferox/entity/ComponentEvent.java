package com.ferox.entity;

public class ComponentEvent extends EntityEvent {
    private final Component mainComponent;
    private final Component auxComponent;
    
    public ComponentEvent(Entity entity, EntitySystem system, Component comp) {
        this(entity, system, comp, null);
    }
    
    public ComponentEvent(Entity entity, EntitySystem system, Component mainComp, Component auxComp) {
        super(entity, system);
        if (mainComp == null)
            throw new NullPointerException("Primary Component cannot be null");
        mainComponent = mainComp;
        auxComponent = auxComp;
    }
    
    public Component getPrimaryComponent() {
        return mainComponent;
    }
    
    public Component getAuxiliaryComponent() {
        return auxComponent;
    }
}
