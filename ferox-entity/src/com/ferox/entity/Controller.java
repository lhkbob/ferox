package com.ferox.entity;

public interface Controller {
    public void preProcess(EntitySystem system, float dt);
    
    public void process(EntitySystem system, float dt);
    
    public void postProcess(EntitySystem system, float dt);
}
