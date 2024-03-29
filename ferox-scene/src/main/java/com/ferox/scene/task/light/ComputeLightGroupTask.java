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
package com.ferox.scene.task.light;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.bounds.QuadTree;
import com.ferox.math.bounds.QueryCallback;
import com.ferox.math.bounds.SpatialIndex;
import com.ferox.math.entreri.BoundsResult;
import com.ferox.scene.*;
import com.ferox.scene.task.PVSResult;
import com.ferox.util.Bag;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.*;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.*;
import java.util.Map.Entry;

@ParallelAware(readOnlyComponents = {InfluenceRegion.class, Light.class, Transform.class}, modifiedComponents = {Renderable.class}, entitySetModified = false)
public class ComputeLightGroupTask implements Task {
    private final SpatialIndex<LightSource> lightIndex;
    private IntProperty assignments;

    // results
    private final List<Bag<Entity>> allVisibleSets;

    // shared local variables for GC performance
    private ComponentIterator iterator;
    private Transform transform;
    private InfluenceRegion influenceRegion;
    private Light light;

    public ComputeLightGroupTask() {
        this.lightIndex = new QuadTree<>(new AxisAlignedBox(), 2);
        allVisibleSets = new ArrayList<>();
    }

    @Override
    public void reset(EntitySystem system) {
        if (assignments == null) {
            assignments = system.decorate(Renderable.class, new IntProperty(-1, false));

            iterator = system.fastIterator();
            light = iterator.addRequired(Light.class);
            transform = iterator.addRequired(Transform.class);
            influenceRegion = iterator.addOptional(InfluenceRegion.class);
        }

        allVisibleSets.clear();
        lightIndex.clear(true);
        iterator.reset();
        Arrays.fill(assignments.getIndexedData(), -1);
    }

    private void convertToLightSources(List<LightSource> globalLights, List<LightSource> allLights) {
        Matrix4 t = new Matrix4();
        while (iterator.next()) {
            // we don't take advantage of some light types requiring a transform,
            // because we process ambient lights with this same code
            transform.getMatrix(t);
            AxisAlignedBox bounds = null;
            boolean invertBounds = false;

            double falloff = light.getFalloffDistance();
            if (influenceRegion.isAlive()) {
                bounds = influenceRegion.getBounds(new AxisAlignedBox()).transform(t);
                invertBounds = influenceRegion.isNegated();
            } else if (falloff >= 0) {
                // compute bounds from the falloff distance to limit query size
                bounds = new AxisAlignedBox();
                bounds.min.set(t.m03 - falloff, t.m13 - falloff, t.m23 - falloff);
                bounds.max.set(t.m03 + falloff, t.m13 + falloff, t.m23 + falloff);
            }

            LightSource l = new LightSource(allLights.size(), light,
                                            new LightInfluence(falloff, light.getCutoffAngle(), t), bounds,
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

    private void queryGlobalLights(LightCallback callback, List<LightSource> globalLights) {
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

        // collect all lights
        Profiler.push("collect-lights");
        List<LightSource> allLights = new ArrayList<>();
        List<LightSource> globalLights = new ArrayList<>();
        convertToLightSources(globalLights, allLights);
        Profiler.pop();

        int groupId = 0;
        Map<BitSet, Integer> groups = new HashMap<>();

        LightCallback callback = new LightCallback(allLights.size());

        // process every visible entity
        Profiler.push("assign-lights");
        for (Bag<Entity> pvs : allVisibleSets) {
            ComponentIterator pvsIterator = system.fastIterator(pvs);
            Renderable pvsRenderable = pvsIterator.addRequired(Renderable.class);
            while (pvsIterator.next()) {
                // check if we've already processed this entity in another pvs
                if (assignments.get(pvsRenderable.getIndex()) >= 0) {
                    continue;
                }

                // reset callback for this entity
                callback.set(pvsRenderable);

                queryGlobalLights(callback, globalLights);
                lightIndex.query(callback.entityBounds, callback);

                // light influence bit set is completed for the entity
                Integer lightGroup = groups.get(callback.lights);
                if (lightGroup == null) {
                    // new group encountered
                    lightGroup = groupId++;
                    groups.put((BitSet) callback.lights.clone(), lightGroup);
                }

                // assign group to entity
                assignments.set(pvsRenderable.getIndex(), lightGroup);
            }
        }
        Profiler.pop();

        // convert computed groups into LightGroupResult
        Profiler.push("report");
        List<Set<Light>> finalGroups = new ArrayList<>(groups.size());
        for (int i = 0; i < groups.size(); i++) {
            // must fill with nulls up to the full size so that the random
            // sets below don't cause index oob exceptions
            finalGroups.add(null);
        }

        for (Entry<BitSet, Integer> group : groups.entrySet()) {
            BitSet groupAsBitSet = group.getKey();
            Set<Light> lightsInGroup = new HashSet<>();
            for (int i = groupAsBitSet.nextSetBit(0); i >= 0; i = groupAsBitSet.nextSetBit(i + 1)) {
                lightsInGroup.add(allLights.get(i).source);
            }

            // store in array
            finalGroups.set(group.getValue(), Collections.unmodifiableSet(lightsInGroup));
        }

        job.report(new LightGroupResult(finalGroups, assignments));
        Profiler.pop();

        Profiler.pop();
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
        if (bounds.getBoundedType().equals(Renderable.class)) {
            lightIndex.setExtent(bounds.getBounds());
        }
    }

    private static class LightCallback implements QueryCallback<LightSource> {
        final BitSet lights;
        final AxisAlignedBox entityBounds;

        public LightCallback(int numLights) {
            lights = new BitSet(numLights);
            entityBounds = new AxisAlignedBox();
        }

        public void set(Renderable r) {
            r.getWorldBounds(entityBounds);
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
        final Light source;
        final LightInfluence influence;

        // non-null only if InfluenceRegion is present
        final AxisAlignedBox bounds;
        final boolean invertBounds;

        public LightSource(int id, Light source, LightInfluence influence, AxisAlignedBox bounds,
                           boolean invertBounds) {
            this.id = id;
            this.source = source.getEntity().get(Light.class); // get canonical instance
            this.influence = influence;
            this.bounds = bounds;
            this.invertBounds = invertBounds;
        }
    }
}
