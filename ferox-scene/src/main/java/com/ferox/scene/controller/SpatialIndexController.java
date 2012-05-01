package com.ferox.scene.controller;

import com.ferox.math.bounds.SpatialIndex;
import com.ferox.scene.Renderable;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.SimpleController;

public class SpatialIndexController extends SimpleController {
    private SpatialIndex<Entity> index;
    
    private Renderable renderable;
    private ComponentIterator it;
    
    // FIXME: add a setter, too
    public SpatialIndexController(SpatialIndex<Entity> index) {
        this.index = index;
    }
    
    @Override
    public void preProcess(double dt) {
        index.clear(true);
    }
    
    @Override
    public void process(double dt) {
        it.reset();
        while(it.next()) {
            index.add(renderable.getEntity(), renderable.getWorldBounds());
        }
        
        // send the built index to everyone listened
        getEntitySystem().getControllerManager().report(new SpatialIndexResult(index));
    }
    
    @Override
    public void init(EntitySystem system) {
        super.init(system);
        renderable = system.createDataInstance(Renderable.ID);
        it = new ComponentIterator(system).addRequired(renderable);
    }
    
    @Override
    public void destroy() {
        renderable = null;
        it = null;
        index.clear();
        super.destroy();
    }
}
