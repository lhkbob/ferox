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
