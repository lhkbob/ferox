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
import com.ferox.math.bounds.SpatialIndex;
import com.ferox.math.entreri.BoundsResult;
import com.ferox.physics.collision.CollisionAlgorithmProvider;
import com.ferox.physics.collision.CollisionBody;
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

public class SpatialIndexCollisionTask extends CollisionTask implements ParallelAware {
    private static final Set<Class<? extends Component>> COMPONENTS;

    static {
        Set<Class<? extends Component>> types = new HashSet<>();
        types.add(CollisionBody.class);
        // notifyPotentialContact looks up RigidBody so we do have a dependency on that type
        types.add(RigidBody.class);
        COMPONENTS = Collections.unmodifiableSet(types);
    }

    private final SpatialIndex<Entity> index;

    // cached instances that are normally local to process()
    private CollisionBody body;
    private ComponentIterator iterator;

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
        while (iterator.next()) {
            index.add(body.getEntity(), body.getWorldBounds());
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

    @Override
    public Set<Class<? extends Component>> getAccessedComponents() {
        return COMPONENTS;
    }

    @Override
    public boolean isEntitySetModified() {
        return false;
    }
}
