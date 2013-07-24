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
import com.lhkbob.entreri.Component;
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
        frustums = new Bag<>();
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
                VisibilityCallback query = new VisibilityCallback();
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
        private final Bag<Entity> pvs;

        public VisibilityCallback() {
            pvs = new Bag<>();
        }

        @Override
        public void process(Entity r, @Const AxisAlignedBox bounds) {
            pvs.add(r);
        }
    }

    @Override
    public Set<Class<? extends Component>> getAccessedComponents() {
        return Collections.<Class<? extends Component>>singleton(Renderable.class);
    }

    @Override
    public boolean isEntitySetModified() {
        return false;
    }
}
