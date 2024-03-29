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
import com.ferox.math.bounds.SpatialIndex;
import com.ferox.math.entreri.BoundsResult;
import com.ferox.scene.Renderable;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.Collections;
import java.util.Set;

@ParallelAware(readOnlyComponents = {Renderable.class}, modifiedComponents = {}, entitySetModified = false)
public class BuildVisibilityIndexTask implements Task {
    private final SpatialIndex<Entity> index;

    // could be local scope but we can save GC work
    private Renderable renderable;
    private ComponentIterator iterator;

    public BuildVisibilityIndexTask(SpatialIndex<Entity> index) {
        this.index = index;
    }

    public void report(BoundsResult result) {
        if (result.getBoundedType().equals(Renderable.class)) {
            index.setExtent(result.getBounds());
        }
    }

    @Override
    public void reset(EntitySystem system) {
        if (iterator == null) {
            iterator = system.fastIterator();
            renderable = iterator.addRequired(Renderable.class);
        }

        index.clear(true);
        iterator.reset();
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        Profiler.push("build-visibility-index");

        AxisAlignedBox bounds = new AxisAlignedBox();
        while (iterator.next()) {
            index.add(renderable.getEntity(), renderable.getWorldBounds(bounds));
        }

        // send the built index to everyone listened
        job.report(new SpatialIndexResult(index));

        Profiler.pop();
        return null;
    }
}
