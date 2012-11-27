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
package com.ferox.scene.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ferox.math.AxisAlignedBox;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

public class UpdateWorldBoundsTask implements Task, ParallelAware {
    private static final Set<Class<? extends ComponentData<?>>> COMPONENTS;
    static {
        Set<Class<? extends ComponentData<?>>> types = new HashSet<Class<? extends ComponentData<?>>>();
        types.add(Renderable.class);
        types.add(Transform.class);
        COMPONENTS = Collections.unmodifiableSet(types);
    }

    // cached local instances
    private Renderable renderable;
    private Transform transform;
    private ComponentIterator iterator;

    @Override
    public void reset(EntitySystem system) {
        if (renderable == null) {
            renderable = system.createDataInstance(Renderable.class);
            transform = system.createDataInstance(Transform.class);
            iterator = new ComponentIterator(system).addRequired(renderable)
                                                    .addRequired(transform);
        }

        iterator.reset();
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        Profiler.push("update-world-bounds");

        AxisAlignedBox worldBounds = new AxisAlignedBox();
        AxisAlignedBox sceneBounds = new AxisAlignedBox();
        boolean first = true;

        while (iterator.next()) {
            worldBounds.transform(renderable.getLocalBounds(), transform.getMatrix());
            renderable.setWorldBounds(worldBounds);

            if (first) {
                sceneBounds.set(worldBounds);
                first = false;
            } else {
                sceneBounds.union(worldBounds);
            }
        }

        job.report(new BoundsResult(sceneBounds));

        Profiler.pop();
        return null;
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
