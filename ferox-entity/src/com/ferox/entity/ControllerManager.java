package com.ferox.entity;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerManager {
    public static enum Phase {
        PREPROCESS, PROCESS, POSTPROCESS, ALL
    }

    private float fixedDelta;
    private final List<Controller> controllers;
    
    // This is a concurrent map so that parallel controllers can access it efficiently
    // - the rest of the class is assumed to be single-threaded
    private final ConcurrentHashMap<Class<? extends Annotation>, Object> controllerData;
    private final ConcurrentHashMap<Object, Object> privateData;

    private final EntitySystem system;
    
    public ControllerManager(EntitySystem system) {
        if (system == null)
            throw new NullPointerException("EntitySystem cannot be null");
        
        this.system = system;
        controllerData = new ConcurrentHashMap<Class<? extends Annotation>, Object>();
        privateData = new ConcurrentHashMap<Object, Object>();
        controllers = new ArrayList<Controller>();
    }
    
    /**
     * Return the controller data that has been mapped to the given annotation
     * <tt>key</tt>. This will return if there has been no assigned data. This
     * can be used to store arbitrary data that must be shared between related
     * controllers.
     * 
     * @param key The annotation key
     * @return The object previously mapped to the annotation with
     *         {@link #setControllerData(Class, Object)}
     * @throws NullPointerException if key is null
     */
    public Object getControllerData(Class<? extends Annotation> key) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        return controllerData.get(key);
    }

    /**
     * Map <tt>value</tt> to the given annotation <tt>key</tt> so that future
     * calls to {@link #getControllerData(Class)} with the same key will return
     * the new value. If the value is null, any previous mapping is removed.
     * 
     * @param key The annotation key
     * @param value The new value to store
     * @throws NullPointerException if key is null
     */
    public void setControllerData(Class<? extends Annotation> key, Object value) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        if (value == null)
            controllerData.remove(key);
        else
            controllerData.put(key, value);
    }
    
    public Object getPrivateData(Object key) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        return privateData.get(key);
    }
    
    public void setPrivateData(Object key, Object value) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        if (value == null)
            privateData.remove(key);
        else
            privateData.put(key, value);
    }
    
    public void addController(Controller controller) {
        if (controller == null)
            throw new NullPointerException("Controller cannot be null");
        if (controllers.contains(controller))
            throw new IllegalStateException("Controller already registered with this manager");
        controllers.add(controller);
    }
    
    public void removeController(Controller controller) {
        if (controller == null)
            throw new NullPointerException("Controller cannot be null");
        controllers.remove(controller);
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
        
        switch(phase) {
        case PREPROCESS:
            doPreProcess(dt); break;
        case PROCESS:
            doProcess(dt); break;
        case POSTPROCESS:
            doPostProcess(dt); break;
        case ALL:
            // Perform all stages in one go
            doPreProcess(dt);
            doProcess(dt);
            doPostProcess(dt);
            break;
        }
    }
    
    private void doPreProcess(float dt) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).preProcess(system, dt);
    }
    
    private void doProcess(float dt) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).process(system, dt);
    }
    
    private void doPostProcess(float dt) {
        for (int i = 0; i < controllers.size(); i++)
            controllers.get(i).postProcess(system, dt);
    }
}
