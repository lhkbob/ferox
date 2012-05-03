package com.ferox.scene.controller;

import com.ferox.math.Const;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.QueryCallback;
import com.ferox.math.bounds.SpatialIndex;
import com.ferox.scene.Renderable;
import com.ferox.util.Bag;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.Result;
import com.lhkbob.entreri.SimpleController;

public class VisibilityController extends SimpleController {
    private Bag<FrustumResult> frustums;
    
    private SpatialIndex<Entity> index;
    
    @Override
    public void process(double dt) {
        if (index != null) {
            for (FrustumResult f: frustums) {
                VisibilityCallback query = new VisibilityCallback(getEntitySystem());
                index.query(f.getFrustum(), query);
                
                // sort the PVS by entity id before reporting it so that
                // iteration over the bag has more optimal cache behavior when
                // accessing entity properties
                query.pvs.sort();
                getEntitySystem().getControllerManager().report(new PVSResult(f.getSource(), f.getFrustum(), query.pvs));
            }
        }
    }
    
    @Override
    public void preProcess(double dt) {
        frustums = new Bag<FrustumResult>();
    }
    
    @Override
    public void postProcess(double dt) {
        index = null;
    }
    
    @Override
    public void report(Result result) {
        if (result instanceof SpatialIndexResult) {
            index = ((SpatialIndexResult) result).getIndex();
        } else if (result instanceof FrustumResult) {
            frustums.add((FrustumResult) result);
        }
    }
    
    private static class VisibilityCallback implements QueryCallback<Entity> {
        private final Renderable renderable;
        
        private final Bag<Entity> pvs;

        /**
         * Create a new VisibilityCallback that set each discovered Entity with a
         * Transform's visibility to true for the given entity, <tt>camera</tt>.
         * 
         * @param camera The Entity that will be flagged as visible
         * @throws NullPointerException if camera is null
         */
        public VisibilityCallback(EntitySystem system) {
            renderable = system.createDataInstance(Renderable.ID);
            pvs = new Bag<Entity>();
        }
        
        @Override
        public void process(Entity r, @Const AxisAlignedBox bounds) {
            // using ComponentData to query existence is faster
            // than pulling in the actual Component
            if (r.get(renderable)) {
                pvs.add(r);
            }
        }
    }
}
