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
package com.ferox.physics.controller;

import java.util.Collections;
import java.util.Set;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.bounds.BoundedSpatialIndex;
import com.ferox.math.bounds.IntersectionCallback;
import com.ferox.math.bounds.SpatialIndex;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.CollisionAlgorithmProvider;
import com.ferox.physics.collision.CollisionBody;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

public class SpatialIndexCollisionController extends CollisionTask implements ParallelAware {
    private final SpatialIndex<Entity> index;
    private final CollisionAlgorithmProvider algorithms;

    // cached instances that are normally local to process()
    private CollisionBody bodyA;
    private CollisionBody bodyB;

    private ComponentIterator iterator;

    public SpatialIndexCollisionController(SpatialIndex<Entity> index,
                                           CollisionAlgorithmProvider algorithms) {
        if (index == null || algorithms == null) {
            throw new NullPointerException("Arguments cannot be null");
        }
        this.index = index;
        this.algorithms = algorithms;
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

    @Override
    public Task process(EntitySystem system, Job job) {
        // if the index is bounded, update its size so everything is processed
        if (index instanceof BoundedSpatialIndex) {
            // FIXME how much does computing the union hurt our performance?
            // FIXME do we want to shrink the extent even when the original extent
            // is large enough? How does that affect query performance?
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
        }

        // fill the index with all collision bodies
        while (iterator.next()) {
            index.add(bodyA.getEntity(), bodyA.getWorldBounds());
        }

        // query for all intersections
        index.query(new CollisionCallback(bodyA, bodyB));

        reportConstraints(job);

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
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void process(Entity a, AxisAlignedBox boundsA, Entity b,
                            AxisAlignedBox boundsB) {
            // at this point we know the world bounds of a and b intersect, but
            // we need to test for collision against their actual shapes
            a.get(bodyA);
            b.get(bodyB);

            CollisionAlgorithm algorithm = algorithms.getAlgorithm(bodyA.getShape()
                                                                        .getClass(),
                                                                   bodyB.getShape()
                                                                        .getClass());

            if (algorithm != null) {
                // have a valid algorithm to test
                ClosestPair pair = algorithm.getClosestPair(bodyA.getShape(),
                                                            bodyA.getTransform(),
                                                            bodyB.getShape(),
                                                            bodyB.getTransform());

                if (pair != null && pair.isIntersecting()) {
                    notifyContact(bodyA, bodyB, pair);
                }
            }
        }
    }

    @Override
    public Set<Class<? extends ComponentData<?>>> getAccessedComponents() {
        return Collections.<Class<? extends ComponentData<?>>> singleton(CollisionBody.class);
    }

    @Override
    public boolean isEntitySetModified() {
        return false;
    }
}
