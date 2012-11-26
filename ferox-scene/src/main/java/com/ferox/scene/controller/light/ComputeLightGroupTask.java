/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
import com.ferox.math.bounds.BoundedSpatialIndex;
import com.ferox.math.bounds.QuadTree;
import com.ferox.math.bounds.QueryCallback;
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
import com.ferox.scene.controller.BoundsResult;
import com.ferox.scene.controller.PVSResult;
import com.ferox.util.Bag;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

public class ComputeLightGroupTask implements Task, ParallelAware {
    private static final Set<Class<? extends ComponentData<?>>> COMPONENTS;
    static {
        Set<Class<? extends ComponentData<?>>> types = new HashSet<Class<? extends ComponentData<?>>>();
        types.add(Influences.class);
        types.add(InfluenceRegion.class);
        types.add(AmbientLight.class);
        types.add(DirectionLight.class);
        types.add(SpotLight.class);
        types.add(PointLight.class);
        types.add(Renderable.class);
        types.add(Transform.class);
        COMPONENTS = Collections.unmodifiableSet(types);
    }

    // read-only so threadsafe
    private static final Matrix4 DEFAULT_MAT = new Matrix4().setIdentity();
    private static final AxisAlignedBox DEFAULT_AABB = new AxisAlignedBox();

    private final BoundedSpatialIndex<LightSource> lightIndex;
    private IntProperty assignments;

    // results
    private final List<Bag<Entity>> allVisibleSets;
    private AxisAlignedBox worldBounds;

    // shared local variables for GC performance
    private Transform transform;
    private Influences influenceSet;
    private InfluenceRegion influenceRegion;
    private AmbientLight ambient;
    private DirectionLight direction;
    private SpotLight spot;
    private PointLight point;

    public ComputeLightGroupTask() {
        this.lightIndex = new QuadTree<LightSource>(new AxisAlignedBox(), 2);
        allVisibleSets = new ArrayList<Bag<Entity>>();
    }

    @Override
    public void reset(EntitySystem system) {
        if (assignments == null) {
            assignments = system.decorate(Renderable.class, new IntProperty.Factory(-1));

            transform = system.createDataInstance(Transform.class);
            influenceSet = system.createDataInstance(Influences.class);
            influenceRegion = system.createDataInstance(InfluenceRegion.class);
            ambient = system.createDataInstance(AmbientLight.class);
            direction = system.createDataInstance(DirectionLight.class);
            spot = system.createDataInstance(SpotLight.class);
            point = system.createDataInstance(PointLight.class);
        }

        allVisibleSets.clear();
        worldBounds = null;
        lightIndex.clear(true);
        Arrays.fill(assignments.getIndexedData(), -1);
    }

    private <T extends Light<T>> void convertToLightSources(T light,
                                                            EntitySystem system,
                                                            LightInfluence.Factory<T> factory,
                                                            List<LightSource> globalLights,
                                                            List<LightSource> allLights) {
        ComponentIterator dlt = new ComponentIterator(system).addRequired(light)
                                                             .addOptional(transform)
                                                             .addOptional(influenceSet)
                                                             .addOptional(influenceRegion);
        while (dlt.next()) {
            // we don't take advantage of some light types requiring a transform,
            // because we process ambient lights with this same code
            Matrix4 t = (transform.isEnabled() ? transform.getMatrix() : DEFAULT_MAT);
            AxisAlignedBox bounds = null;
            boolean invertBounds = false;

            if (influenceRegion.isEnabled()) {
                bounds = new AxisAlignedBox().transform(influenceRegion.getBounds(), t);
                invertBounds = influenceRegion.isNegated();
            }

            LightSource l = new LightSource(allLights.size(),
                                            light.getComponent(),
                                            factory.create(light, t),
                                            (influenceSet.isEnabled() ? influenceSet.getInfluencedSet() : null),
                                            bounds,
                                            invertBounds);

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

    private void queryGlobalLights(Entity e, LightCallback callback,
                                   List<LightSource> globalLights) {
        // accumulate globally influencing lights into bit set
        int numGlobalLights = globalLights.size();
        for (int i = 0; i < numGlobalLights; i++) {
            LightSource light = globalLights.get(i);

            // check influence region of light
            if (light.bounds != null) {
                if (light.invertBounds) {
                    // skip light if entity is contained entirely in light bounds
                    if (light.bounds.contains(callback.entityBounds)) {
                        continue;
                    }
                } else {
                    // skip light if entity does not intersect light bounds
                    if (!light.bounds.intersects(callback.entityBounds)) {
                        continue;
                    }
                }
            }

            // check influence set of light
            if (light.validEntities != null && !light.validEntities.contains(e)) {
                continue;
            }

            // final check
            if (light.influence.influences(callback.entityBounds)) {
                // passed the last check, so the entity is influence by the ith light
                callback.lights.set(light.id);
            }
        }
    }

    /*
     * This method makes excessive use of raw types, but it is safe given that
     * TypeIds are used correctly. Java's type inference is just not good enough
     * to keep track of it.
     */
    @Override
    public Task process(EntitySystem system, Job job) {
        Profiler.push("compute-light-groups");

        lightIndex.setExtent(worldBounds);

        // collect all lights
        Profiler.push("collect-lights");
        List<LightSource> allLights = new ArrayList<LightSource>();
        List<LightSource> globalLights = new ArrayList<LightSource>();
        convertToLightSources(direction, system,
                              GlobalLightInfluence.<DirectionLight> factory(),
                              globalLights, allLights);
        convertToLightSources(ambient, system,
                              GlobalLightInfluence.<AmbientLight> factory(),
                              globalLights, allLights);
        convertToLightSources(spot, system, SpotLightInfluence.factory(), globalLights,
                              allLights);
        convertToLightSources(point, system, PointLightInfluence.factory(), globalLights,
                              allLights);
        Profiler.pop("collect-lights");

        int groupId = 0;
        Map<BitSet, Integer> groups = new HashMap<BitSet, Integer>();

        LightCallback callback = new LightCallback(system, allLights.size());

        // process every visible entity
        Profiler.push("assign-lights");
        for (Bag<Entity> pvs : allVisibleSets) {
            for (Entity entity : pvs) {
                // reset callback for this entity
                callback.set(entity);

                // check if we've already processed this entity in another pvs
                if (assignments.get(callback.renderable.getIndex()) >= 0) {
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
                assignments.set(lightGroup.intValue(), callback.renderable.getIndex());
            }
        }
        Profiler.pop("assign-lights");

        // convert computed groups into LightGroupResult
        Profiler.push("report");
        List<Set<Component<? extends Light<?>>>> finalGroups = new ArrayList<Set<Component<? extends Light<?>>>>(groups.size());
        for (int i = 0; i < groups.size(); i++) {
            // must fill with nulls up to the full size so that the random
            // sets below don't cause index oob exceptions
            finalGroups.add(null);
        }

        for (Entry<BitSet, Integer> group : groups.entrySet()) {
            BitSet groupAsBitSet = group.getKey();
            Set<Component<? extends Light<?>>> lightsInGroup = new HashSet<Component<? extends Light<?>>>();
            for (int i = groupAsBitSet.nextSetBit(0); i >= 0; i = groupAsBitSet.nextSetBit(i + 1)) {
                lightsInGroup.add(allLights.get(i).source);
            }

            // store in array
            finalGroups.set(group.getValue(), Collections.unmodifiableSet(lightsInGroup));
        }

        job.report(new LightGroupResult(finalGroups, assignments));
        Profiler.pop("report");

        Profiler.pop("compute-light-groups");
        return null;
    }

    public void report(PVSResult pvs) {
        if (pvs.getSource().getType().equals(Camera.class)) {
            // we are only interested in entities that will be rendered
            // to a surface, and not for something like a shadow map
            allVisibleSets.add(pvs.getPotentiallyVisibleSet());
        }
    }

    public void report(BoundsResult bounds) {
        worldBounds = bounds.getBounds();
    }

    private static class LightCallback implements QueryCallback<LightSource> {
        final BitSet lights;
        final Renderable renderable;
        AxisAlignedBox entityBounds;

        public LightCallback(EntitySystem system, int numLights) {
            lights = new BitSet(numLights);
            renderable = system.createDataInstance(Renderable.class);
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

    @Override
    public Set<Class<? extends ComponentData<?>>> getAccessedComponents() {
        return COMPONENTS;
    }

    @Override
    public boolean isEntitySetModified() {
        return false;
    }
}
