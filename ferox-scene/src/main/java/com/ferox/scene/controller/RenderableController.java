package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Octree;
import com.ferox.math.bounds.SpatialIndex;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.googlecode.entreri.AbstractController;
import com.googlecode.entreri.Component;
import com.googlecode.entreri.ControllerManager;
import com.googlecode.entreri.ControllerManager.Key;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.IndexedComponentMap;
import com.googlecode.entreri.property.ObjectProperty;

public class RenderableController extends AbstractController {
    public static final Key<SpatialIndex<Renderable>> RENDERABLE_HIERARCHY = new Key<SpatialIndex<Renderable>>();
    
    private static final Key<ObjectProperty<Object>> PROPERTY_KEY = new Key<ObjectProperty<Object>>();

    private final AxisAlignedBox tempWorldBounds;

    public RenderableController() {
        tempWorldBounds = new AxisAlignedBox();
    }

    public static long time = 0;
    public static long world = 0;
    public static long update = 0;
    @Override
    public void process(EntitySystem system, float dt) {
        time -= System.nanoTime();
        
        SpatialIndex<Renderable> hierarchy = system.getControllerManager().getData(RENDERABLE_HIERARCHY);
        
        ObjectProperty<Object> hierarchyKey = system.getControllerManager().getData(PROPERTY_KEY);

        Iterator<IndexedComponentMap> it = system.iterator(Renderable.ID, Transform.ID);
        while(it.hasNext()) {
            IndexedComponentMap map = it.next();
            Renderable r = map.get(Renderable.ID, 0);
            Transform t = map.get(Transform.ID, 1);
            
//            world -= System.nanoTime();
            r.getLocalBounds(tempWorldBounds).transform(t.getMatrix(), tempWorldBounds);
            r.setWorldBounds(tempWorldBounds);
//            world += System.nanoTime();
            
//            update -= System.nanoTime();
            Object key = hierarchyKey.get(r.getIndex(), 0);
            if (key == null) {
                key = hierarchy.add(r, tempWorldBounds);
                hierarchyKey.set(key, r.getIndex(), 0);
            } else {
                hierarchy.update(r, tempWorldBounds, key);
            }
//            update += System.nanoTime();
            
            r.resetVisibility();
        }
        
        time += System.nanoTime();
    }

    @Override
    public void addedToSystem(EntitySystem system) {
        ObjectProperty<Object> hierarchyKeys = system.decorate(Renderable.ID, ObjectProperty.<Object>factory(1));
        system.getControllerManager().setData(PROPERTY_KEY, hierarchyKeys);
        
        SpatialIndex<Renderable> hierarchy = system.getControllerManager().getData(RENDERABLE_HIERARCHY);
        if (hierarchy == null) {
            hierarchy = new Octree<Renderable>();
            system.getControllerManager().setData(RENDERABLE_HIERARCHY, hierarchy);
        }
    }

    @Override
    public void removedFromSystem(EntitySystem system) {
        system.undecorate(Renderable.ID, system.getControllerManager().getData(PROPERTY_KEY));
        system.getControllerManager().setData(RENDERABLE_HIERARCHY, null);
    }

    @Override
    public void onComponentRemove(Component c) {
        if (c instanceof Renderable) {
            // clean up hierarchy
            ControllerManager cm = c.getEntity().getEntitySystem().getControllerManager();
            
            ObjectProperty<Object> keys = cm.getData(PROPERTY_KEY);
            SpatialIndex<Renderable> hierarchy = cm.getData(RENDERABLE_HIERARCHY);
            
            Object key = keys.get(c.getIndex(), 0);
            if (key != null)
                hierarchy.remove((Renderable) c, key);
        }
    }
}
