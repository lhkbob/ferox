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
package com.ferox.anim;

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.ElapsedTimeResult;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SkeletonAnimationTask implements Task, ParallelAware {
    private static final Set<Class<? extends ComponentData<?>>> COMPONENTS;

    static {
        Set<Class<? extends ComponentData<?>>> set = new HashSet<Class<? extends ComponentData<?>>>();
        set.add(Skeleton.class);
        set.add(Animated.class);
        COMPONENTS = Collections.unmodifiableSet(set);
    }

    private Skeleton skeleton;
    private Animated animated;
    private ComponentIterator iterator;

    private double dt;

    public void report(ElapsedTimeResult r) {
        dt = r.getTimeDelta();
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        while (iterator.next()) {
            SkeletonAnimation anim = animated.getAnimation();
            double newTime = animated.getCurrentTime() + dt * animated.getTimeScale();
            if (newTime > anim.getAnimationLength()) {
                if (animated.getLoopPlayback()) {
                    newTime = newTime - anim.getAnimationLength();
                } else {
                    newTime = anim.getAnimationLength();
                }
            }

            animated.setCurrentTime(newTime);
            anim.updateSkeleton(skeleton, newTime);
        }

        return null;
    }

    @Override
    public void reset(EntitySystem system) {
        if (skeleton == null) {
            skeleton = system.createDataInstance(Skeleton.class);
            animated = system.createDataInstance(Animated.class);

            iterator = new ComponentIterator(system);
            iterator.addRequired(skeleton);
            iterator.addRequired(animated);
        }

        iterator.reset();
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
