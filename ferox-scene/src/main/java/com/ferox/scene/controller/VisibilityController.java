package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.entity2.Controller;
import com.ferox.entity2.Entity;
import com.ferox.entity2.EntitySystem;
import com.ferox.entity2.Parallel;
import com.ferox.math.bounds.Frustum;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.scene.Camera;
import com.ferox.scene.Renderable;
import com.ferox.scene.Visibility;

@Parallel(reads={Camera.class, SpatialHierarchy.class}, writes=Visibility.class)
public class VisibilityController extends Controller {
    private final SpatialHierarchy<Entity> hierarchy;
    private final VisibilityListener listener;
    
    public VisibilityController(EntitySystem system, SpatialHierarchy<Entity> hierarchy) {
        super(system);
        if (hierarchy == null)
            throw new NullPointerException("SpatialHierarchy cannot be null");
        this.hierarchy = hierarchy;
        listener = new VisibilityListener();
        system.addEntityListener(listener);
    }

    @Override
    protected void executeImpl() {
        Iterator<Visibility> vs = getEntitySystem().iterator(Visibility.ID);
        while(vs.hasNext()) { 
            vs.next().resetVisibility();
        }
        
        Iterator<Camera> it = getEntitySystem().iterator(Camera.ID);
        while(it.hasNext()) {
            Frustum f = it.next().getFrustum();
            hierarchy.query(f, new VisibilityCallback(f));
        }
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
    
    private static class VisibilityListener extends MetaComponentListener<Renderable, Visibility> {
        public VisibilityListener() {
            super(Renderable.class, Visibility.class);
        }

        @Override
        protected void add(Entity e, Renderable component) {
            // do nothing, the VisibilityCallback will handle it
        }

        @Override
        protected void remove(Entity e, Visibility meta) {
            // do nothing else since we already have removed the visibility component
        }
    }
}
