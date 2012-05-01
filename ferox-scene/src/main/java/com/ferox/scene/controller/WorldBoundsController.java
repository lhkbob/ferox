package com.ferox.scene.controller;

import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.SimpleController;

public class WorldBoundsController extends SimpleController {
    private Renderable renderable;
    private Transform transform;
    
    private ComponentIterator it;
    
    @Override
    public void process(double dt) {
        AxisAlignedBox worldBounds = new AxisAlignedBox();
        
        it.reset();
        while(it.next()) {
            worldBounds.transform(renderable.getLocalBounds(), transform.getMatrix());
            renderable.setWorldBounds(worldBounds);
        }
    }
    
    @Override
    public void init(EntitySystem system) {
        super.init(system);
        renderable = system.createDataInstance(Renderable.ID);
        transform = system.createDataInstance(Transform.ID);
        it = new ComponentIterator(system).addRequired(renderable)
                                          .addRequired(transform);
    }
    
    @Override
    public void destroy() {
        renderable = null;
        transform = null;
        it = null;
        super.destroy();
    }
}
