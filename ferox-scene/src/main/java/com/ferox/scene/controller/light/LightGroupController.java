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
import com.ferox.math.Matrix4;
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
    
    private IntProperty assignments;
    private List<Bag<Entity>> allVisibleSets;
    
    @SuppressWarnings("rawtypes")
    private final Map<TypeId, LightInfluence> lightInfluences;
    
    @SuppressWarnings("rawtypes")
    public LightGroupController() {
        lightInfluences = new HashMap<TypeId, LightInfluence>();
        
        // register default algorithms for known light types
        registerInfluence(AmbientLight.ID, new AmbientLightInfluence());
        registerInfluence(DirectionLight.ID, new DirectionLightInfluence());
        registerInfluence(PointLight.ID, new PointLightInfluence());
        registerInfluence(SpotLight.ID, new SpotLightInfluence());
    }
    
    public <T extends Light<T>> void registerInfluence(TypeId<T> type, LightInfluence<T> algorithm) {
        if (type == null || algorithm == null)
            throw new NullPointerException("Arguments cannot be null");
        lightInfluences.put(type, algorithm);
    }
    
    /*
     * This method makes excessive use of raw types, but it is safe given that
     * TypeIds are used correctly. Java's type inference is just not good enough
     * to keep track of it.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void process(double dt) {
        // collect all lights into a list for later
        Map<TypeId, Light> lightAccessors = new HashMap<TypeId, Light>();
        List<Component> allLights = new ArrayList<Component>();
        for (TypeId lightType: lightInfluences.keySet()) {
            Light light = getEntitySystem().createDataInstance(lightType);
            ComponentIterator it = new ComponentIterator(getEntitySystem());
            it.addRequired(light);
            
            while(it.next()) {
                // FIXME: is it faster to add an Iterator over Component<?> if
                // that's what we really need
                allLights.add(light.getComponent());
            }
            lightAccessors.put(lightType, light);
        }
        
        int numLights = allLights.size();
        int groupId = 0;
        Map<BitSet, Integer> groups = new HashMap<BitSet, Integer>();
        
        // instances reused for each iteration
        Influences influenceSet = getEntitySystem().createDataInstance(Influences.ID);
        InfluenceRegion influenceAABB = getEntitySystem().createDataInstance(InfluenceRegion.ID);
        Transform transform = getEntitySystem().createDataInstance(Transform.ID);
        Renderable renderable = getEntitySystem().createDataInstance(Renderable.ID);
        BitSet entityLights = new BitSet(numLights);
        
        // process every visible entity
        for (Bag<Entity> pvs: allVisibleSets) {
            for (Entity entity: pvs) {
                // get matrix and bounds for entity
                AxisAlignedBox bounds = (entity.get(renderable) ? renderable.getWorldBounds() : DEFAULT_AABB);
                
                // check if we've already processed this entity in another pvs
                if (assignments.get(renderable.getIndex(), 0) >= 0) {
                    continue;
                }
                
                // reset light influences for this entity
                entityLights.clear();
                // accumulate influencing lights into bit set
                for (int i = 0; i < numLights; i++) {
                    Component light = allLights.get(i);
                    Entity lightEntity = light.getEntity();
                    
                    // check influence region on light
                    if (lightEntity.get(influenceAABB)) {
                        if (influenceAABB.isNegated()) {
                            // skip light if entity is contained entirely in light bounds
                            if (influenceAABB.getBounds().contains(bounds))
                                continue;
                        } else {
                            // skip light if entity does not intersect light bounds
                            if (!influenceAABB.getBounds().intersects(bounds))
                                continue;
                        }
                    }
                    
                    // check influence set of light
                    if (lightEntity.get(influenceSet)) {
                        if (!influenceSet.canInfluence(entity))
                            continue;
                    }
                    
                    // use light influence algorithm for last check
                    Light accessor = lightAccessors.get(light.getTypeId());
                    accessor.set(light);
                    Matrix4 matrix = (lightEntity.get(transform) ? transform.getMatrix() : DEFAULT_MAT);

                    if (lightInfluences.get(light.getTypeId()).influences(accessor, matrix, bounds)) {
                        // passed the last check, so the entity is influence by the ith light
                        entityLights.set(i);
                    }
                }
                
                // light influence bit set is complete for the entity
                Integer lightGroup = groups.get(entityLights);
                if (lightGroup == null) {
                    // new group encountered
                    lightGroup = groupId++;
                    groups.put((BitSet) entityLights.clone(), lightGroup);
                }
                
                // assign group to entity
                assignments.set(lightGroup.intValue(), renderable.getIndex(), 0);
            }
        }
        
        // convert computed groups into LightGroupResult
        Set<Component<? extends Light<?>>>[] finalGroups = new Set[groups.size()];
        for (Entry<BitSet, Integer> group: groups.entrySet()) {
            BitSet groupAsBitSet = group.getKey();
            Set<Component<? extends Light<?>>> lightsInGroup = new HashSet<Component<? extends Light<?>>>();
            for (int i = groupAsBitSet.nextSetBit(0); i >= 0; i = groupAsBitSet.nextSetBit(i+1)) {
                lightsInGroup.add(allLights.get(i));
            }
            
            // store in array
            finalGroups[group.getValue()] = Collections.unmodifiableSet(lightsInGroup);
        }
        
        getEntitySystem().getControllerManager().report(new LightGroupResult(finalGroups, assignments));
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
