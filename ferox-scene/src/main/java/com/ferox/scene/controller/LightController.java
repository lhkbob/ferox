package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.entity2.Controller;
import com.ferox.entity2.Entity;
import com.ferox.entity2.EntitySystem;
import com.ferox.entity2.Parallel;
import com.ferox.entity2.TypedId;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.scene.Light;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.ferox.scene.Visibility;

@Parallel(reads={Transform.class, Renderable.class, Light.class, SpatialHierarchy.class}, 
          writes={LightInfluences.class})
public class LightController extends Controller {
    private final SpatialHierarchy<Entity> hierarchy;
    private final LightInfluenceListener listener;
    
    public LightController(EntitySystem system, SpatialHierarchy<Entity> hierarchy) {
        super(system);
        if (hierarchy == null)
            throw new NullPointerException("SpatialHierarchy cannot be null");
        this.hierarchy = hierarchy;
        listener = new LightInfluenceListener();
        system.addEntityListener(listener);
    }

    @Override
    protected void executeImpl() {
        
    }
    
    private void processSpotLights() {
        
    }
    
    private void processPointLights() {
        
    }
    
    private void processAmbientLights() {
        
    }
    
    private void processDirectionLights() {
        
    }

    @Override
    protected void destroyImpl() {
        Iterator<Visibility> it = getEntitySystem().iterator(Visibility.ID);
        while(it.hasNext()) {
            it.next();
            it.remove();
        }
        getEntitySystem().removeEntityListener(listener);
    }
    
    private static class LightInfluenceListener extends MetaComponentListener<Renderable, LightInfluences> {
        public LightInfluenceListener() {
            super(Renderable.class, LightInfluences.class);
        }

        @Override
        protected void add(Entity e, Renderable component) {
            // do nothing, the light processing will handle it
        }

        @Override
        protected void remove(Entity e, LightInfluences meta) {
            // do nothing else
        }
    }
}
