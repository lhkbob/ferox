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

import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.Frustum;
import com.ferox.scene.Camera;
import com.ferox.scene.Transform;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * CameraController is a controller that synchronizes a {@link Camera}'s Frustum location and orientation with
 * an attached {@link Transform}. When run, all entities with a Camera and Transform will have the Camera's
 * Frustum's orientation equal that stored in the transform.
 *
 * @author Michael Ludwig
 */
public class ComputeCameraFrustumTask implements Task, ParallelAware {
    private static final Set<Class<? extends Component>> COMPONENTS;

    static {
        Set<Class<? extends Component>> types = new HashSet<Class<? extends Component>>();
        types.add(Camera.class);
        types.add(Transform.class);
        COMPONENTS = Collections.unmodifiableSet(types);
    }

    // could exist in local scope but we can prevent extra garbage this way
    private Camera camera;
    private Transform transform;
    private ComponentIterator iterator;

    @Override
    public void reset(EntitySystem system) {
        if (iterator == null) {
            iterator = system.fastIterator();
            camera = iterator.addRequired(Camera.class);
            transform = iterator.addRequired(Transform.class);
        }

        iterator.reset();
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        Profiler.push("compute-camera-frustum");

        while (iterator.next()) {
            double aspect = camera.getSurface().getWidth() / (double) camera.getSurface().getHeight();
            Frustum f = new Frustum(camera.getFieldOfView(), aspect, camera.getNearZDistance(),
                                    camera.getFarZDistance());

            Matrix4 m = transform.getMatrix();
            f.setOrientation(new Vector3(m.m03, m.m13, m.m23), new Vector3(m.m02, m.m12, m.m22),
                             new Vector3(m.m01, m.m11, m.m21));

            job.report(new FrustumResult(camera.getEntity().get(Camera.class), f));
        }

        Profiler.pop();
        return null;
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
