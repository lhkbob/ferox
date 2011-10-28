package com.ferox.scene.controller;

import com.ferox.entity2.Component;
import com.ferox.entity2.ComponentEvent;
import com.ferox.entity2.Entity;
import com.ferox.entity2.EntityEvent;
import com.ferox.entity2.EntityListener;
import com.ferox.entity2.TypedId;

public abstract class MetaComponentListener<C extends Component, M extends Component> implements EntityListener {
    private final TypedId<C> componentId;
    private final TypedId<M> metaId;
    
    private final boolean autoAdd;
    private final boolean autoRemove;
    
    public MetaComponentListener(Class<C> componentType, Class<M> metaComponentType) {
        this(componentType, metaComponentType, true, true);
    }
    
    public MetaComponentListener(Class<C> componentType, Class<M> metaComponentType,
                                 boolean autoAdd, boolean autoRemove) {
        componentId = Component.getTypedId(componentType);
        metaId = Component.getTypedId(metaComponentType);
        
        this.autoAdd = autoAdd;
        this.autoRemove = autoRemove;
    }
    
    protected abstract void add(Entity e, C component);
    
    protected abstract void remove(Entity e, M meta);
    
    @Override
    public void onEntityAdd(EntityEvent e) {
        if (autoAdd) {
            C primary = e.getEntity().get(componentId);
            if (primary != null)
                add(e.getEntity(), primary);
        }
    }

    @Override
    public void onEntityRemove(EntityEvent e) {
        if (autoRemove) {
            M meta = e.getEntity().remove(metaId);
            if (meta != null)
                remove(e.getEntity(), meta);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onComponentAdd(ComponentEvent c) {
        if (autoAdd) {
            if (componentId.getType().isInstance(c.getPrimaryComponent())) {
                if (!componentId.getType().isInstance(c.getAuxiliaryComponent())) {
                    // only add a meta component if there was no previous component
                    // of the same type
                    add(c.getEntity(), (C) c.getPrimaryComponent());
                }
            }
        }
    }

    @Override
    public void onComponentRemove(ComponentEvent c) {
        if (autoRemove) {
            if (componentId.getType().isInstance(c.getPrimaryComponent())) {
                M meta = c.getEntity().remove(metaId);
                if (meta != null)
                    remove(c.getEntity(), meta);
            }
        }
    }
}