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
package com.ferox.scene.controller.ffp;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.ColorRGB;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.QuadTree;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.geom.Box;
import com.ferox.renderer.geom.Geometry;
import com.ferox.renderer.geom.Sphere;
import com.ferox.renderer.geom.Teapot;
import com.ferox.scene.*;
import com.ferox.scene.task.BuildVisibilityIndexTask;
import com.ferox.scene.task.ComputeCameraFrustumTask;
import com.ferox.scene.task.ComputePVSTask;
import com.ferox.scene.task.UpdateWorldBoundsTask;
import com.ferox.scene.task.ffp.FixedFunctionRenderTask;
import com.ferox.scene.task.light.ComputeLightGroupTask;
import com.ferox.scene.task.light.ComputeShadowFrustumTask;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SimpleTest {
    public static final double BOUNDS = 200;

    public static void main(String[] args) {
        Framework framework = Framework.Factory.create();
        OnscreenSurface surface = framework
                .createSurface(new OnscreenSurfaceOptions().windowed(800, 600).fixedSize());
        //        surface.setVSyncEnabled(true);

        EntitySystem system = EntitySystem.Factory.create();

        Entity camera = system.addEntity();
        camera.add(Transform.class)
              .setMatrix(new Matrix4().set(-1, 0, 0, 0, 0, 1, 0, 0, 0, 0, -1, .9 * BOUNDS, 0, 0, 0, 1));
        camera.add(Camera.class).setSurface(surface).setZDistances(0.1, 1200).setFieldOfView(75);

        Geometry b1 = Sphere.create(framework, 2f, 16);
        Geometry b2 = Box.create(framework, 2f);
        Geometry b3 = Teapot.create(framework, 1f);

        ColorRGB c1 = new ColorRGB(Math.random() + 0.2, Math.random() + 0.2, Math.random() + 0.2);
        ColorRGB c2 = new ColorRGB(Math.random() + 0.2, Math.random() + 0.2, Math.random() + 0.2);
        ColorRGB c3 = new ColorRGB(Math.random() + 0.2, Math.random() + 0.2, Math.random() + 0.2);

        int totalpolys = 0;
        for (int i = 0; i < 10000; i++) {
            Geometry b;
            double choice = Math.random();
            if (choice < .35) {
                b = b1;
            } else if (choice < .9) {
                b = b2;
            } else {
                b = b3;
            }

            ColorRGB c;
            choice = Math.random();
            if (choice < .33) {
                c = c1;
            } else if (choice < .67) {
                c = c2;
            } else {
                c = c3;
            }

            int polycount = b.getPolygonType().getPolygonCount(b.getIndexCount() - b.getIndexOffset());

            Entity e = system.addEntity();
            e.add(Renderable.class).setGeometry(b);
            e.add(LambertianDiffuseModel.class).setColor(c);
            e.add(Transform.class).setMatrix(new Matrix4()
                                                     .set(1, 0, 0, Math.random() * BOUNDS - BOUNDS / 2, 0, 1,
                                                          0, Math.random() * BOUNDS - BOUNDS / 2, 0, 0, 1,
                                                          Math.random() * BOUNDS - BOUNDS / 2, 0, 0, 0, 1));
            e.add(Animation.class);
            totalpolys += polycount;
        }

        System.out.println("Approximate total polygons / frame: " + totalpolys);

        for (int i = 0; i < 5; i++) {
            double falloff = 100.0 + Math.random() * 40;

            Entity light = system.addEntity();
            light.add(Light.class).setFalloffDistance(falloff).setCutoffAngle(180.0)
                 .setColor(new ColorRGB(Math.random(), Math.random(), Math.random()));

            light.add(Transform.class).setMatrix(new Matrix4()
                                                         .set(1, 0, 0, Math.random() * BOUNDS - BOUNDS / 2, 0,
                                                              1, 0, Math.random() * BOUNDS - BOUNDS / 2, 0, 0,
                                                              1, Math.random() * BOUNDS - BOUNDS / 2, 0, 0, 0,
                                                              1));
        }
        system.addEntity().add(AmbientLight.class).setColor(new ColorRGB(0.2, 0.2, 0.2));

        Entity inf = system.addEntity();
        inf.add(Light.class).setColor(new ColorRGB(1, 1, 1)).setCutoffAngle(Double.NaN)
           .setShadowCaster(false);
        inf.add(Transform.class).setMatrix(new Matrix4().lookAt(new Vector3(),
                                                                new Vector3(.3 * BOUNDS, .3 * BOUNDS,
                                                                            .3 * BOUNDS),
                                                                new Vector3(0, 1, 0)));

        AxisAlignedBox worldBounds = new AxisAlignedBox(
                new Vector3(-1.5 * BOUNDS / 2, -1.5 * BOUNDS / 2, -1.5 * BOUNDS / 2),
                new Vector3(1.5 * BOUNDS / 2, 1.5 * BOUNDS / 2, 1.5 * BOUNDS / 2));

        Job renderJob = system.getScheduler()
                              .createJob("render", Timers.measuredDelta(), new AnimationController(),
                                         new UpdateWorldBoundsTask(), new ComputeCameraFrustumTask(),
                                         new ComputeShadowFrustumTask(),
                                         new BuildVisibilityIndexTask(new QuadTree<Entity>(worldBounds, 6)),
                                         new ComputePVSTask(), new ComputeLightGroupTask(),
                                         new FixedFunctionRenderTask(framework, 1024, false));

        long now = System.nanoTime();
        int numRuns = 0;
        try {
            while (System.nanoTime() - now < 20000000000L) {
                system.getScheduler().runOnCurrentThread(renderJob);
                framework.flush(surface);
                numRuns++;
            }
        } finally {
            long total = System.nanoTime() - now;

            framework.destroy();

            System.out.println("***** TIMING *****");
            print("total", total, numRuns);
            Profiler.getDataSnapshot().print(System.out);

            System.out.println("***** MEMORY *****");
            Runtime r = Runtime.getRuntime();
            printMemory("total", r.totalMemory());
            printMemory("used", r.totalMemory() - r.freeMemory());
        }
    }

    private static void printMemory(String label, long bytes) {
        System.out.printf("%s: %.2f MB\n", label, (bytes / (1024.0 * 1024.0)));
    }

    private static void print(String label, long total, int numRuns) {
        float millis = total / 1e6f;
        float avg = millis / numRuns;
        System.out.printf("%s - total time: %.2f, avg: %.2f\n", label, millis, avg);
    }

    public static class AnimationController implements Task, ParallelAware {
        public static double SPEED = 4;

        private double dt;

        public void report(ElapsedTimeResult dt) {
            this.dt = dt.getTimeDelta();
        }

        @Override
        public void reset(EntitySystem system) {
        }

        @Override
        public Task process(EntitySystem system, Job job) {
            ComponentIterator it = system.fastIterator();
            Transform t = it.addRequired(Transform.class);
            Animation anim = it.addRequired(Animation.class);

            while (it.next()) {
                Vector3 d = anim.getDirection();

                if (anim.getDirection().lengthSquared() > 0.00001) {
                    // have a direction, assume its normalized
                    Matrix4 m = t.getMatrix();

                    m.m03 += SPEED * dt * d.x;
                    m.m13 += SPEED * dt * d.y;
                    m.m23 += SPEED * dt * d.z;
                    t.setMatrix(m);
                }

                double newLifetime = anim.getLifetime() - dt;
                if (newLifetime <= 0) {
                    // change direction and lifetime
                    newLifetime = Math.random() * 5 + .2;

                    if (Math.random() > .8) {
                        // no movement
                        d.x = 0;
                        d.y = 0;
                        d.z = 0;
                    } else {
                        d.x = Math.random() - 0.5;
                        d.y = Math.random() - 0.5;
                        d.z = Math.random() - 0.5;

                        if (d.lengthSquared() > 0.0001) {
                            d.normalize();
                        }
                    }

                    anim.setDirection(d);
                }
                anim.setLifetime(newLifetime);
            }

            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<Class<? extends Component>> getAccessedComponents() {
            return new HashSet<>(Arrays.asList(Animation.class, Transform.class));
        }

        @Override
        public boolean isEntitySetModified() {
            return false;
        }
    }

}
