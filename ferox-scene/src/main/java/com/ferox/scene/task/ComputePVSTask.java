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
package com.ferox.scene.task;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.bounds.QueryCallback;
import com.ferox.math.bounds.SpatialIndex;
import com.ferox.scene.Renderable;
import com.ferox.util.Bag;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.Collections;
import java.util.Set;

public class ComputePVSTask implements Task, ParallelAware {
    // results
    private final Bag<FrustumResult> frustums;
    private SpatialIndex<Entity> index;

    public ComputePVSTask() {
        frustums = new Bag<FrustumResult>();
    }

    public void report(FrustumResult result) {
        frustums.add(result);
    }

    public void report(SpatialIndexResult result) {
        index = result.getIndex();
    }

    @Override
    public void reset(EntitySystem system) {
        frustums.clear(true);
        index = null;
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        Profiler.push("compute-pvs");

        if (index != null) {
            for (FrustumResult f : frustums) {
                VisibilityCallback query = new VisibilityCallback(system);
                index.query(f.getFrustum(), query);

                // sort the PVS by entity id before reporting it so that
                // iteration over the bag has more optimal cache behavior when
                // accessing entity properties
                query.pvs.sort();
                job.report(new PVSResult(f.getSource(), f.getFrustum(), query.pvs));
            }
        }

        Profiler.pop();
        return null;
    }

    private static class VisibilityCallback implements QueryCallback<Entity> {
        private final Renderable renderable;

        private final Bag<Entity> pvs;

        /**
         * Create a new VisibilityCallback that set each discovered Entity with a
         * Transform's visibility to true for the given entity, <tt>camera</tt>.
         *
         * @param camera The Entity that will be flagged as visible
         *
         * @throws NullPointerException if camera is null
         */
        public VisibilityCallback(EntitySystem system) {
            renderable = system.createDataInstance(Renderable.class);
            pvs = new Bag<Entity>();
        }

        @Override
        public void process(Entity r, @Const AxisAlignedBox bounds) {
            // using ComponentData to query existence is faster
            // than pulling in the actual Component
            if (r.get(renderable)) {
                pvs.add(r);
            }
        }
    }

    @Override
    public Set<Class<? extends ComponentData<?>>> getAccessedComponents() {
        return Collections.<Class<? extends ComponentData<?>>>singleton(Renderable.class);
    }

    @Override
    public boolean isEntitySetModified() {
        return false;
    }
}
