package com.ferox.scene.controller.light;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.bounds.QuadTree;
import com.ferox.math.bounds.QueryCallback;
import com.ferox.math.bounds.SpatialIndex;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.Camera;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.InfluenceRegion;
import com.ferox.scene.Influences;
import com.ferox.scene.Light;
import com.ferox.scene.PointLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.SpotLight;
import com.ferox.scene.Transform;
import com.ferox.scene.controller.PVSResult;
import com.ferox.util.Bag;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.Result;
import com.lhkbob.entreri.SimpleController;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.property.IntProperty;

public class LightGroupController extends SimpleController {
    // read-only so threadsafe
    private static final Matrix4 DEFAULT_MAT = new Matrix4().setIdentity();
    private static final AxisAlignedBox DEFAULT_AABB = new AxisAlignedBox();
    
    private SpatialIndex<LightSource> lightIndex;
    
    private IntProperty assignments;
    private List<Bag<Entity>> allVisibleSets;
    
    
    public LightGroupController(@Const AxisAlignedBox worldBounds) {
        this.lightIndex = new QuadTree<LightSource>(worldBounds, 1);
    }
    
    private <T extends Light<T>> void processLights(TypeId<T> id, LightInfluence.Factory<T> factory, 
                                                    List<LightSource> globalLights,
                                                    List<LightSource> allLights) {
        Transform transform = getEntitySystem().createDataInstance(Transform.ID);
        T light = getEntitySystem().createDataInstance(id);
        Influences influenceSet = getEntitySystem().createDataInstance(Influences.ID);
        InfluenceRegion region = getEntitySystem().createDataInstance(InfluenceRegion.ID);
        
        ComponentIterator dlt = new ComponentIterator(getEntitySystem())
            .addRequired(light)
            .addOptional(transform)
            .addOptional(influenceSet)
            .addOptional(region);
        while(dlt.next()) {
            Matrix4 t = (transform.isEnabled() ? transform.getMatrix() : DEFAULT_MAT);
            AxisAlignedBox bounds = null;
            boolean invertBounds = false;
            
            if (region.isEnabled()) {
                bounds = new AxisAlignedBox().transform(region.getBounds(), t);
                invertBounds = region.isNegated();
            }
            
            LightSource l = new LightSource(allLights.size(), light.getComponent(), factory.create(light, t), 
                                            (influenceSet.isEnabled() ? influenceSet.getInfluencedSet() : null),
                                            bounds, invertBounds);
            
            if (bounds != null && !invertBounds) {
                // this light is not a globally influencing light so add it to the index
                if (!lightIndex.add(l, bounds)) {
                    // fallback if we're out of bounds
                    globalLights.add(l);
                }
            } else {
                globalLights.add(l);
            }
            
            // always add it to the full list
            allLights.add(l);
        }
    }
    
    private void queryGlobalLights(Entity e, LightCallback callback, List<LightSource> globalLights) {
        // accumulate globally influencing lights into bit set
        int numGlobalLights = globalLights.size();
        for (int i = 0; i < numGlobalLights; i++) {
            LightSource light = globalLights.get(i);
            
            // check influence region of light
            if (light.bounds != null) {
                if (light.invertBounds) {
                    // skip light if entity is contained entirely in light bounds
                    if (light.bounds.contains(callback.entityBounds))
                        continue;
                } else {
                    // skip light if entity does not intersect light bounds
                    if (!light.bounds.intersects(callback.entityBounds))
                        continue;
                }
            }
            
            // check influence set of light
            if (light.validEntities != null 
                && !light.validEntities.contains(e)) {
                continue;
            }
            
            // final check
            if (light.influence.influences(callback.entityBounds)) {
                // passed the last check, so the entity is influence by the ith light
                callback.lights.set(i);
            }
        }
    }
    
    /*
     * This method makes excessive use of raw types, but it is safe given that
     * TypeIds are used correctly. Java's type inference is just not good enough
     * to keep track of it.
     */
    @Override
    public void process(double dt) {
        // collect all lights
        List<LightSource> allLights = new ArrayList<LightSource>();
        List<LightSource> globalLights = new ArrayList<LightSource>();
        processLights(DirectionLight.ID, GlobalLightInfluence.<DirectionLight>factory(), globalLights, allLights);
        processLights(AmbientLight.ID, GlobalLightInfluence.<AmbientLight>factory(), globalLights, allLights);
        processLights(SpotLight.ID, SpotLightInfluence.factory(), globalLights, allLights);
        processLights(PointLight.ID, PointLightInfluence.factory(), globalLights, allLights);
        
        int groupId = 0;
        Map<BitSet, Integer> groups = new HashMap<BitSet, Integer>();
        
        LightCallback callback = new LightCallback(getEntitySystem(), allLights.size());
        
        // process every visible entity
        for (Bag<Entity> pvs: allVisibleSets) {
            for (Entity entity: pvs) {
                // reset callback for this entity
                callback.set(entity);
                
                // check if we've already processed this entity in another pvs
                if (!callback.renderable.isEnabled() 
                    || assignments.get(callback.renderable.getIndex(), 0) >= 0) {
                    continue;
                }
                
                queryGlobalLights(entity, callback, globalLights);
                // FIXME must look into performance cost of this part of the callback,
                // some small evidence suggests that just the re-working to use
                // fewer component fetches is significantly faster, I'm not sure why
                lightIndex.query(callback.entityBounds, callback);
                
                // light influence bit set is complete for the entity
                Integer lightGroup = groups.get(callback.lights);
                if (lightGroup == null) {
                    // new group encountered
                    lightGroup = groupId++;
                    groups.put((BitSet) callback.lights.clone(), lightGroup);
                }
                
                // assign group to entity
                assignments.set(lightGroup.intValue(), callback.renderable.getIndex(), 0);
            }
        }
        
        // convert computed groups into LightGroupResult
        List<Set<Component<? extends Light<?>>>> finalGroups = new ArrayList<Set<Component<? extends Light<?>>>>(groups.size());
        for (int i = 0; i < groups.size(); i++) {
            // must fill with nulls up to the full size so that the random
            // sets below don't cause index oob exceptions
            finalGroups.add(null);
        }
        
        for (Entry<BitSet, Integer> group: groups.entrySet()) {
            BitSet groupAsBitSet = group.getKey();
            Set<Component<? extends Light<?>>> lightsInGroup = new HashSet<Component<? extends Light<?>>>();
            for (int i = groupAsBitSet.nextSetBit(0); i >= 0; i = groupAsBitSet.nextSetBit(i+1)) {
                lightsInGroup.add(allLights.get(i).source);
            }
            
            // store in array
            finalGroups.set(group.getValue(), Collections.unmodifiableSet(lightsInGroup));
        }
        
        getEntitySystem().getControllerManager().report(new LightGroupResult(finalGroups, assignments));
    }
    
    @Override
    public void preProcess(double dt) {
        allVisibleSets = new ArrayList<Bag<Entity>>();
        Arrays.fill(assignments.getIndexedData(), -1);
        lightIndex.clear(true);
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
        lightIndex.clear();
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
    
    private static class LightCallback implements QueryCallback<LightSource> {
        final BitSet lights;
        final Renderable renderable;
        AxisAlignedBox entityBounds;
        
        public LightCallback(EntitySystem system, int numLights) {
            lights = new BitSet(numLights);
            renderable = system.createDataInstance(Renderable.ID);
            entityBounds = DEFAULT_AABB;
        }
        
        public void set(Entity e) {
            if (e.get(renderable)) {
                entityBounds = renderable.getWorldBounds();
            } else {
                entityBounds = DEFAULT_AABB;
            }
            lights.clear();
        }
        
        @Override
        public void process(LightSource item, @Const AxisAlignedBox lightBounds) {
            if (item.influence.influences(entityBounds)) {
                // record light
                lights.set(item.id);
            }
        }
    }
    
    private static class LightSource {
        final int id;
        final Component<? extends Light<?>> source;
        final LightInfluence influence;
        
        // non-null only if Influences is present
        final Set<Entity> validEntities;
        
        // non-null only if InfluenceRegion is present
        final AxisAlignedBox bounds;
        final boolean invertBounds;
        
        public LightSource(int id, Component<? extends Light<?>> source, 
                           LightInfluence influence, Set<Entity> validEntities, 
                           AxisAlignedBox bounds, boolean invertBounds) {
            this.id = id;
            this.source = source;
            this.influence = influence;
            this.validEntities = validEntities;
            this.bounds = bounds;
            this.invertBounds = invertBounds;
        }
    }
}
