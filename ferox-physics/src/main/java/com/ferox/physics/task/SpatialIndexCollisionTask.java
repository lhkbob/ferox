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

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.bounds.IntersectionCallback;
import com.ferox.math.bounds.QuadTree;
import com.ferox.math.bounds.SpatialIndex;
import com.ferox.math.entreri.BoundsResult;
import com.ferox.physics.collision.CollisionAlgorithmProvider;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.collision.DefaultCollisionAlgorithmProvider;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SpatialIndexCollisionTask is a collision task that implements the broadphase by relying on the {@link
 * SpatialIndex#query(com.ferox.math.bounds.IntersectionCallback)} method. Thus, the performance of this
 * broadphase is dependent on the type of spatial index provided.
 *
 * @author Michael Ludwig
 */
@ParallelAware(readOnlyComponents = {RigidBody.class, CollisionBody.class}, modifiedComponents = {}, entitySetModified = false)
public class SpatialIndexCollisionTask extends CollisionTask {
    private final SpatialIndex<Entity> index;

    // cached instances that are normally local to process()
    private CollisionBody body;
    private ComponentIterator iterator;

    /**
     * Create a new SpatialIndexCollisionTask that uses a default collision algorithm provider and a quadtree
     * with 3 levels.
     */
    public SpatialIndexCollisionTask() {
        this(new QuadTree<Entity>(new AxisAlignedBox(), 3), new DefaultCollisionAlgorithmProvider());
    }

    /**
     * Create a new SpatialIndexCollisionTask that uses the given algorithm provider and spatial index
     *
     * @param index      The spatial index that is used
     * @param algorithms The algorithm provider
     *
     * @throws NullPointerException if index or algorithms are null
     */
    public SpatialIndexCollisionTask(SpatialIndex<Entity> index, CollisionAlgorithmProvider algorithms) {
        super(algorithms);
        if (index == null) {
            throw new NullPointerException("SpatialIndex cannot be null");
        }
        this.index = index;
    }

    public void report(BoundsResult result) {
        if (result.getBoundedType().equals(CollisionBody.class)) {
            index.setExtent(result.getBounds());
        }
    }

    @Override
    public void reset(EntitySystem system) {
        super.reset(system);

        if (iterator == null) {
            iterator = system.fastIterator();
            body = iterator.addRequired(CollisionBody.class);
        }

        index.clear(true);
        iterator.reset();
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        Profiler.push("detect-collisions");

        // fill the index with all collision bodies
        Profiler.push("build-index");
        AxisAlignedBox aabb = new AxisAlignedBox();
        while (iterator.next()) {
            index.add(body.getEntity(), body.getWorldBounds(aabb));
        }
        Profiler.pop();

        // query for all intersections
        Profiler.push("find-intersections");
        index.query(new CollisionCallback());
        Profiler.pop();

        Profiler.push("generate-constraints");
        reportConstraints(job);
        Profiler.pop();

        Profiler.pop();
        return super.process(system, job);
    }

    private class CollisionCallback implements IntersectionCallback<Entity> {
        @Override
        public void process(Entity a, AxisAlignedBox boundsA, Entity b, AxisAlignedBox boundsB) {
            // at this point we know the world bounds of a and b intersect, but
            // we need to test for collision against their actual shapes
            notifyPotentialContact(a.get(CollisionBody.class), b.get(CollisionBody.class));
        }
    }
}
