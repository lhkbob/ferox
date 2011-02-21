package com.ferox.entity;

public interface EntityListener {
    public void onEntityAdd(EntityEvent e);
    
    public void onEntityRemove(EntityEvent e);
    
    public void onComponentAdd(ComponentEvent c);
    
    public void onComponentRemove(ComponentEvent c);
}
