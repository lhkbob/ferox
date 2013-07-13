package com.ferox.anim;

import com.ferox.math.Matrix4;
import com.ferox.scene.Transform;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoneTransformTask implements Task, ParallelAware {
    private static final Set<Class<? extends ComponentData<?>>> COMPONENTS;

    static {
        Set<Class<? extends ComponentData<?>>> set = new HashSet<Class<? extends ComponentData<?>>>();
        set.add(Skeleton.class);
        set.add(Transform.class);
        COMPONENTS = Collections.unmodifiableSet(set);
    }

    private Skeleton skeleton;
    private Transform transform;

    private ComponentIterator iterator;

    @Override
    public Task process(EntitySystem system, Job job) {
        Matrix4 m = new Matrix4();
        while (iterator.next()) {
            Bone root = skeleton.getRootBone();
            m.mul(transform.getMatrix(), root.getRelativeBoneTransform());
            root.setGlobalBoneTransform(m);

            updateChildren(root, m);
        }

        return null;
    }

    private void updateChildren(Bone parent, Matrix4 store) {
        List<Bone> children = skeleton.getChildren(parent);

        if (children != null) {
            Bone child;
            for (int i = 0; i < children.size(); i++) {
                child = children.get(i);
                store.mul(parent.getGlobalBoneTransform(), child.getRelativeBoneTransform());
                child.setGlobalBoneTransform(store);

                updateChildren(child, store);
            }
        }
    }

    @Override
    public void reset(EntitySystem system) {
        if (skeleton == null) {
            skeleton = system.createDataInstance(Skeleton.class);
            transform = system.createDataInstance(Transform.class);

            iterator = new ComponentIterator(system);
            iterator.addRequired(skeleton);
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
