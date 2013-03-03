package com.ferox.anim;

import com.ferox.scene.Transform;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BoneLinkTask implements Task, ParallelAware {
    private static final Set<Class<? extends ComponentData<?>>> COMPONENTS;

    static {
        Set<Class<? extends ComponentData<?>>> set = new HashSet<Class<? extends ComponentData<?>>>();
        set.add(BoneLink.class);
        set.add(Transform.class);
        COMPONENTS = Collections.unmodifiableSet(set);
    }

    private BoneLink boneLink;
    private Transform transform;

    private ComponentIterator iterator;

    @Override
    public Task process(EntitySystem system, Job job) {
        while (iterator.next()) {
            transform.setMatrix(boneLink.getLinkedBone().getGlobalBoneTransform());
        }

        return null;
    }

    @Override
    public void reset(EntitySystem system) {
        if (boneLink == null) {
            boneLink = system.createDataInstance(BoneLink.class);
            transform = system.createDataInstance(Transform.class);

            iterator = new ComponentIterator(system);
            iterator.addRequired(boneLink);
            iterator.addRequired(transform);
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
