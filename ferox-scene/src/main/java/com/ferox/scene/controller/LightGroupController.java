package com.ferox.scene.controller;

import com.lhkbob.entreri.SimpleController;

public class LightGroupController extends SimpleController {
    // what is the best way to accumulate PVS results?
    // the light-group controller can be optimized in the single frustum case
    // to run through just the PVS.
    //
    // If there are multiple PVS's, then we have to process the union of them.
    // I guess we could just do a reset at the end of the frame so we can
    // easily check if we've encountered an already processed entity
    
    // Find all process'able lights and assign them ids, which will be used
    // to more efficiently store them in a bit set.
    // Then we just do a for-loop over received entities, evaluate them
    // against all available lights (in system, not pvs) and build up a bitset
    // then we assign the id of the bitset or create a new id if the given
    // configuration has never been seen before
    
    // Is it better to run through each light, and then process pvs's?
    // It would reduce the number of Map lookups to get each light's id, but 
    // we couldn't reuse the same BitSet, so I don't think it's worth it.
    
    // What is in the LightGroup result?
    // 1. Index to light-group configuration. This configuration is just a set
    //    of light entities, or should it be components? Probably be components,
    //    since an entity could be multiple lights (Set<Component<? extends Light<?>>>)
    // 2. The IntProperty used to read assigned light configurations to entities
    //    within the PVS
    // Basically, the LightGroup result contains all light groups detected,
    // not single groups at a time.
    
    // Although that brings up an interesting question about how I should design
    // these results. Do I?
    //  1. Store the sets of entities on the components (e.g. getPVS() on Camera)
    //  2. Store them in singleton results (e.g. PVSResult must have all PVS's provided)
    //  3. Have results for each group (e.g. 1 per different PVS, or light group)
    
    // It does not make sense for #1 since shadow mapping lights can't expose
    // the affected entities as easily. It gets even worse with light groups since
    // there is LG component.  We could store influence lists, but then we'd just
    // have to regroup everything.
    
    // #2 works well for light groups since a single controller must analyze everything
    // to determine the proper group identity. It is easy to convert a #2 into a
    // #3 result setup.  But usually if you need a light group result, you'll want to
    // store the set of all so you'd just be recombining them.
    
    // #3 is most convenient for a PVS since multiple PVS's exist even in a single
    // camera system, for reflections and lights, etc. It would be too tedious
    // to force PVS's to be computed into a #2 style result. A #1 result wouldn't be
    // terrible but it makes it hard to do a universal visibility controller.
    
    // I think this just means I need to use the appropriate result model for each
    // situation depending on what's required.
    
    
    // Should I provide a way to specify that a Result is a singleton result in
    // that it should only be supplied once per frame? It doesn't make sense to
    // provide multiple light groups, or spatial indexes. I could have the controller
    // manager verify that.
}
