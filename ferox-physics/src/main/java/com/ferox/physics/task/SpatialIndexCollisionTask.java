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
package com.ferox.physics.task;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.bounds.BoundedSpatialIndex;
import com.ferox.math.bounds.IntersectionCallback;
import com.ferox.math.bounds.SpatialIndex;
import com.ferox.physics.collision.CollisionAlgorithmProvider;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

public class SpatialIndexCollisionTask extends CollisionTask implements ParallelAware {
    private static final Set<Class<? extends ComponentData<?>>> COMPONENTS;
    static {
        Set<Class<? extends ComponentData<?>>> types = new HashSet<Class<? extends ComponentData<?>>>();
        types.add(CollisionBody.class);
        types.add(RigidBody.class);
        COMPONENTS = Collections.unmodifiableSet(types);
    }

    private final SpatialIndex<Entity> index;

    // cached instances that are normally local to process()
    private CollisionBody bodyA;
    private CollisionBody bodyB;

    private ComponentIterator iterator;

    public SpatialIndexCollisionTask(SpatialIndex<Entity> index,
                                           CollisionAlgorithmProvider algorithms) {
        super(algorithms);
        if (index == null) {
            throw new NullPointerException("SpatialIndex cannot be null");
        }
        this.index = index;
    }

    @Override
    public void reset(EntitySystem system) {
        super.reset(system);

        if (bodyA == null) {
            bodyA = system.createDataInstance(CollisionBody.class);
            bodyB = system.createDataInstance(CollisionBody.class);

            iterator = new ComponentIterator(system).addRequired(bodyA);
        }

        index.clear(true);
        iterator.reset();
    }

    // NOTE: with regards to performance and task creation, there are 
    // non-zero costs with iterating through the system: content is pulled
    // from the properties into local instances and then processed (either
    // directly with getters, or the onSet() method). This means that if
    // multiple tasks are executed consecutively but access the same component
    // types, we're doing additional load work.
    //
    // It'd be faster to merge those tasks into a single loop. Is this something
    // I should automate, or something that I should consider when I design my
    // tasks? I think it's too awkward to automate, although it would be cool.
    //
    // 

    @Override
    public Task process(EntitySystem system, Job job) {
        Profiler.push("detect-collisions");

        // if the index is bounded, update its size so everything is processed
        if (index instanceof BoundedSpatialIndex) {
            // FIXME how much does computing the union hurt our performance?
            // FIXME do we want to shrink the extent even when the original extent
            // is large enough? How does that affect query performance?

            // FIXME right now, setTransform() computes updateBounds() for CollisionBody,
            // which isn't the best -> what if we had a task that just computed
            // world bounds (like world bounds task in scene module) and it could
            // report the union as well.

            // Now the only issue is whether we want to duplicate this code, since
            // both sources need the same functionality but they are definitely not
            // sharable.  The only place would be if the math module defined the
            // world bounds result type (we need different tasks because they
            // process things differently anyways).
            Profiler.push("update-index-extent");
            AxisAlignedBox extent = new AxisAlignedBox();
            boolean first = true;
            while (iterator.next()) {
                if (first) {
                    extent.set(bodyA.getWorldBounds());
                    first = false;
                } else {
                    extent.union(bodyA.getWorldBounds());
                }
            }

            // make the extents slightly larger so floating point errors don't
            // cause shapes on the edge to get discarded
            extent.min.scale(1.1);
            extent.max.scale(1.1);
            ((BoundedSpatialIndex<Entity>) index).setExtent(extent);
            iterator.reset();
            Profiler.pop();
        }

        // fill the index with all collision bodies
        Profiler.push("build-index");
        while (iterator.next()) {
            index.add(bodyA.getEntity(), bodyA.getWorldBounds());
        }
        Profiler.pop();

        // query for all intersections
        Profiler.push("find-intersections");
        index.query(new CollisionCallback(bodyA, bodyB));
        Profiler.pop();

        Profiler.push("generate-constraints");
        reportConstraints(job);
        Profiler.pop();

        Profiler.pop();
        return super.process(system, job);
    }

    private class CollisionCallback implements IntersectionCallback<Entity> {
        private final CollisionBody bodyA;
        private final CollisionBody bodyB;

        public CollisionCallback(CollisionBody bodyA, CollisionBody bodyB) {
            this.bodyA = bodyA;
            this.bodyB = bodyB;
        }

        @Override
        public void process(Entity a, AxisAlignedBox boundsA, Entity b,
                            AxisAlignedBox boundsB) {
            // at this point we know the world bounds of a and b intersect, but
            // we need to test for collision against their actual shapes
            a.get(bodyA);
            b.get(bodyB);

            notifyPotentialContact(bodyA, bodyB);
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