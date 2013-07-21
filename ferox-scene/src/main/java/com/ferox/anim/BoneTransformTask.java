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
