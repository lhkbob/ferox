package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.math.bounds.SpatialIndex;
import com.ferox.scene.Camera;
import com.ferox.scene.Renderable;
import com.googlecode.entreri.AbstractController;
import com.googlecode.entreri.EntitySystem;

// FIXME: part of me likes being able to look up an entity's visibility on it,
// but in many cases it is nicer to just process a list.
// - is it wrong to maintain both forms of data?
// - not really, especially with the primitive int set -> which should be updated
//    to just use 4 elements and a linear scan instead of a binary search.
// - Perhaps a custom IntSet/Collection for the object-expansion as well.
public class VisibilityController extends AbstractController {
    public static long time = 0;
    @Override
    public void process(EntitySystem system, float dt) {
        time -= System.nanoTime();
        
        SpatialIndex<Renderable> hierarchy = system.getControllerManager().getData(RenderableController.RENDERABLE_HIERARCHY);
        
        Iterator<Camera> it = system.fastIterator(Camera.ID);
        while(it.hasNext()) {
            Camera c = it.next();
            hierarchy.query(c.getFrustum(), new VisibilityCallback(c.getEntity()));
        }
        
        time += System.nanoTime();
    }
}
