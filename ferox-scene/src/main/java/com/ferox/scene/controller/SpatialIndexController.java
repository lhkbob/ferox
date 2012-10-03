package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.math.bounds.SpatialIndex;
import com.ferox.scene.Renderable;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.SimpleController;

public class SpatialIndexController extends SimpleController {
    private SpatialIndex<Entity> index;

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
        Renderable r;
        Iterator<Renderable> it = getEntitySystem().iterator(Renderable.ID);
        while (it.hasNext()) {
            r = it.next();
            index.add(r.getEntity(), r.getWorldBounds());
        }

        // send the built index to everyone listened
        getEntitySystem().getControllerManager().report(new SpatialIndexResult(index));
    }

    @Override
    public void destroy() {
        index.clear();
        super.destroy();
    }
}
