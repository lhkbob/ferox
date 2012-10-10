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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.QuadTree;
import com.ferox.math.entreri.Vector3Property;
import com.ferox.math.entreri.Vector3Property.DefaultVector3;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.OnscreenSurfaceOptions.MultiSampling;
import com.ferox.renderer.impl.jogl.JoglFramework;
import com.ferox.renderer.impl.lwjgl.LwjglFramework;
import com.ferox.resource.VertexBufferObject.StorageMode;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.BlinnPhongMaterial;
import com.ferox.scene.Camera;
import com.ferox.scene.DiffuseColor;
import com.ferox.scene.InfluenceRegion;
import com.ferox.scene.PointLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.ferox.scene.controller.CameraController;
import com.ferox.scene.controller.SpatialIndexController;
import com.ferox.scene.controller.VisibilityController;
import com.ferox.scene.controller.WorldBoundsController;
import com.ferox.scene.controller.light.LightGroupController;
import com.ferox.util.geom.Box;
import com.ferox.util.geom.Geometry;
import com.ferox.util.geom.Sphere;
import com.ferox.util.geom.Teapot;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Controller;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.SimpleController;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.Unmanaged;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;

public class SimpleTest {
    public static final boolean LWJGL = true;

    public static void main(String[] args) {
        Framework framework = (LWJGL ? LwjglFramework.create() : JoglFramework.create());
        OnscreenSurface surface = framework.createSurface(new OnscreenSurfaceOptions().setWidth(800)
                                                                                      .setHeight(600)
                                                                                      //            .setFullscreenMode(new DisplayMode(1440, 900, PixelFormat.RGB_24BIT))
                                                                                      .setMultiSampling(MultiSampling.FOUR_X)
                                                                                      .setResizable(false));
        surface.setVSyncEnabled(true);

        EntitySystem system = new EntitySystem();

        Entity camera = system.addEntity();
        camera.add(Transform.ID).getData()
              .setMatrix(new Matrix4(-1, 0, 0, 0, 0, 1, 0, 0, 0, 0, -1, 150, 0, 0, 0, 1));
        camera.add(Camera.ID).getData().setSurface(surface).setZDistances(0.1, 1200)
              .setFieldOfView(75);

        Geometry b1 = Sphere.create(2f, 16, StorageMode.GPU_STATIC);
        Geometry b2 = Box.create(2f, StorageMode.GPU_STATIC);
        Geometry b3 = Teapot.create(1f, StorageMode.GPU_STATIC);

        ColorRGB c1 = new ColorRGB(Math.random() + 0.2,
                                   Math.random() + 0.2,
                                   Math.random() + 0.2);
        ColorRGB c2 = new ColorRGB(Math.random() + 0.2,
                                   Math.random() + 0.2,
                                   Math.random() + 0.2);
        ColorRGB c3 = new ColorRGB(Math.random() + 0.2,
                                   Math.random() + 0.2,
                                   Math.random() + 0.2);

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

            int polycount = b.getPolygonType()
                             .getPolygonCount(b.getIndexCount() - b.getIndexOffset());

            Entity e = system.addEntity();
            e.add(Renderable.ID)
             .getData()
             .setVertices(b.getVertices())
             .setLocalBounds(b.getBounds())
             .setIndices(b.getPolygonType(), b.getIndices(), b.getIndexOffset(),
                         b.getIndexCount());
            if (Math.random() < .9) {
                e.add(BlinnPhongMaterial.ID).getData().setNormals(b.getNormals());
            }
            e.add(DiffuseColor.ID).getData().setColor(c);
            e.add(Transform.ID)
             .getData()
             .setMatrix(new Matrix4(1,
                                    0,
                                    0,
                                    Math.random() * 200 - 100,
                                    0,
                                    1,
                                    0,
                                    Math.random() * 200 - 100,
                                    0,
                                    0,
                                    1,
                                    Math.random() * 200 - 100,
                                    0,
                                    0,
                                    0,
                                    1));
            e.add(Animation.ID);
            totalpolys += polycount;
        }

        System.out.println("Approximate total polygons / frame: " + totalpolys);

        for (int i = 0; i < 20; i++) {
            double falloff = 100.0 + Math.random() * 40;

            Entity light = system.addEntity();
            light.add(PointLight.ID).getData().setFalloffDistance(falloff)
                 .setColor(new ColorRGB(Math.random(), Math.random(), Math.random()));

            if (falloff > 0) {
                light.add(InfluenceRegion.ID)
                     .getData()
                     .setBounds(new AxisAlignedBox(new Vector3(-falloff,
                                                               -falloff,
                                                               -falloff),
                                                   new Vector3(falloff, falloff, falloff)));
            }
            light.add(Transform.ID)
                 .getData()
                 .setMatrix(new Matrix4(1,
                                        0,
                                        0,
                                        Math.random() * 200 - 100,
                                        0,
                                        1,
                                        0,
                                        Math.random() * 200 - 100,
                                        0,
                                        0,
                                        1,
                                        Math.random() * 200 - 100,
                                        0,
                                        0,
                                        0,
                                        1));
        }
        system.addEntity().add(AmbientLight.ID).getData()
              .setColor(new ColorRGB(0.2, 0.2, 0.2));

        AxisAlignedBox worldBounds = new AxisAlignedBox(new Vector3(-150, -150, -150),
                                                        new Vector3(150, 150, 150));
        Controller animator = new AnimationController();
        Controller boundsUpdate = new WorldBoundsController();
        Controller frustumUpdate = new CameraController();
        Controller indexBuilder = new SpatialIndexController(new QuadTree<Entity>(worldBounds,
                                                                                  6));
        Controller pvsComputer = new VisibilityController();
        Controller lights = new LightGroupController(worldBounds);
        Controller render = new FixedFunctionRenderController(framework);

        Map<String, Controller> controllers = new HashMap<String, Controller>();
        controllers.put("anim", animator);
        controllers.put("bounds", boundsUpdate);
        controllers.put("frustum", frustumUpdate);
        controllers.put("index-build", indexBuilder);
        controllers.put("pvs", pvsComputer);
        controllers.put("lights", lights);
        controllers.put("render", render);
        Map<String, Long> times = new HashMap<String, Long>();

        system.getControllerManager().addController(animator);
        system.getControllerManager().addController(boundsUpdate);
        system.getControllerManager().addController(frustumUpdate);
        system.getControllerManager().addController(indexBuilder);
        system.getControllerManager().addController(pvsComputer);
        system.getControllerManager().addController(lights);
        system.getControllerManager().addController(render);

        long now = System.nanoTime();
        int numRuns = 0;
        try {
            while (System.nanoTime() - now < 10000000000L) {
                system.getControllerManager().process();
                framework.flush(surface);
                numRuns++;
                for (String name : controllers.keySet()) {
                    Long time = times.get(name);
                    if (time == null) {
                        time = 0L;
                    }
                    time += system.getControllerManager()
                                  .getExecutionTime(controllers.get(name));
                    times.put(name, time);
                }
            }
        } finally {
            long total = System.nanoTime() - now;

            framework.destroy();

            System.out.println("***** TIMING *****");
            print("total", total, numRuns);
            for (String name : controllers.keySet()) {
                print(name, (times.containsKey(name) ? times.get(name) : 0), numRuns);
            }
            print("blocked", FixedFunctionRenderController.blocktime, numRuns);
            print("opengl", FixedFunctionRenderController.rendertime, numRuns);
            System.out.println();

            System.out.println("***** MEMORY *****");
            Runtime r = Runtime.getRuntime();
            printMemory("total", r.totalMemory());
            printMemory("used", r.totalMemory() - r.freeMemory());
            for (TypeId<?> t : system.getTypes()) {
                printMemory(t.toString(), system.estimateMemory(t));
            }
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

    public static class AnimationController extends SimpleController {
        public static double SPEED = 4;

        @Override
        public void process(double dt) {
            Transform t = getEntitySystem().createDataInstance(Transform.ID);
            Iterator<Animation> it = getEntitySystem().iterator(Animation.ID);
            while (it.hasNext()) {
                Animation anim = it.next();
                Vector3 d = anim.getDirection();

                if (anim.getEntity().get(t) && anim.getDirection().lengthSquared() > 0.00001) {
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
        }
    }

    public static class Animation extends ComponentData<Animation> {
        public static final TypeId<Animation> ID = TypeId.get(Animation.class);

        @DefaultDouble(1)
        private DoubleProperty life;

        @DefaultVector3(x = 0, y = 0, z = 0)
        private Vector3Property direction;

        @Unmanaged
        private final Vector3 cache = new Vector3();

        private Animation() {}

        public double getLifetime() {
            return life.get(getIndex());
        }

        public void setLifetime(double lt) {
            life.set(lt, getIndex());
        }

        public @Const
        Vector3 getDirection() {
            return cache;
        }

        public void setDirection(@Const Vector3 dir) {
            cache.set(dir);
            direction.set(dir, getIndex());
        }

        @Override
        protected void onSet(int index) {
            direction.get(getIndex(), cache);
        }
    }
}
