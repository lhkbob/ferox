package com.ferox.entity;

import java.util.List;

public class ControllerManager {
    public static enum Phase {
        PREPROCESS, PROCESS, POSTPROCESS, ALL
    }

    private float fixedDelta;
    private final List<Controller> controllers;
    
    public ControllerManager() {
        
    }
    
    public void addController(Controller controller) {
        
    }
    
    public void removeController(Controller controller) {
        
    }
    
    public void process() {
        process(fixedDelta);
    }
    
    public void process(float dt) {
        process(Phase.ALL, dt);
    }
    
    public void process(Phase phase, float dt) {
        if (phase == null)
            throw new NullPointerException("Phase cannot be null");
        
        if (phase == Phase.ALL) {
            // special handling for ALL
            
        } else {
            
        }
    }
    
}
