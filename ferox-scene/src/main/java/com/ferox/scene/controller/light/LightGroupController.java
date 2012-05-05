package com.ferox.scene.controller.light;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ferox.scene.Camera;
import com.ferox.scene.Renderable;
import com.ferox.scene.controller.PVSResult;
import com.ferox.util.Bag;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.Result;
import com.lhkbob.entreri.SimpleController;
import com.lhkbob.entreri.property.IntProperty;

public class LightGroupController extends SimpleController {
    // FIXME add a way to register new light influence algorithms
    // FIXME: come up with a good name for light influence algorithms
    private IntProperty assignments;
    private List<Bag<Entity>> allVisibleSets;
    
    @Override
    public void process(double dt) {
        // 1. collect all lights into an array
        // 2. setup up group map and reusable bit set
        //    a. group map is bitset->integer, paired with a list of
        //        set<light> with indices == appr. value in map
        
        for (Bag<Entity> pvs: allVisibleSets) {
            // 3. process all entities in pvs and test for influence
            //    b. clear bit set
            //    a. each light influenced is put into bit set
            // 4. add entity to group, and create new group if bitset was
            //    not seen before
        }
        // 5. list of groups into an array and push the result (is necessary?
        //    maybe we should just update the result defn.)
    }
    
    @Override
    public void preProcess(double dt) {
        allVisibleSets = new ArrayList<Bag<Entity>>();
        Arrays.fill(assignments.getIndexedData(), -1);
    }
    
    @Override
    public void init(EntitySystem system) {
        super.init(system);
        assignments = system.decorate(Renderable.ID, new IntProperty.Factory(1, 0));
    }
    
    @Override
    public void destroy() {
        getEntitySystem().undecorate(Renderable.ID, assignments);
        assignments = null;
        super.destroy();
    }
    
    @Override
    public void report(Result r) {
        if (r instanceof PVSResult) {
            PVSResult pvs = (PVSResult) r;
            if (pvs.getSource().getTypeId() == Camera.ID) {
                // we are only interested in entities that will be rendered
                // to a surface, and not for something like a shadow map
                allVisibleSets.add(pvs.getPotentiallyVisibleSet());
            }
        }
    }
}
