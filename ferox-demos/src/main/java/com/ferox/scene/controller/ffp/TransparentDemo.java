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

import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.logic.Action;
import com.ferox.input.logic.InputManager;
import com.ferox.input.logic.InputState;
import com.ferox.input.logic.Predicates;
import com.ferox.math.AxisAlignedBox;
import com.ferox.math.ColorRGB;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.QuadTree;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.impl.lwjgl.LwjglFramework;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.BlinnPhongMaterial;
import com.ferox.scene.Camera;
import com.ferox.scene.DiffuseColor;
import com.ferox.scene.PointLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.ferox.scene.Transparent;
import com.ferox.scene.controller.BuildVisibilityIndexTask;
import com.ferox.scene.controller.ComputeCameraFrustumTask;
import com.ferox.scene.controller.ComputePVSTask;
import com.ferox.scene.controller.UpdateWorldBoundsTask;
import com.ferox.scene.controller.light.ComputeLightGroupTask;
import com.ferox.scene.controller.light.ComputeShadowFrustumTask;
import com.ferox.util.ApplicationStub;
import com.ferox.util.geom.Geometry;
import com.ferox.util.geom.Teapot;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;

public class TransparentDemo extends ApplicationStub {
    protected static final int BOUNDS = 50;
    protected static final double BOX_SIZE = 1.5;
    protected static final int NUM_X = 1;
    protected static final int NUM_Y = 1;
    protected static final int NUM_Z = 1;
    protected static final double GAP_X = 0;
    protected static final double GAP_Y = 0;
    protected static final double GAP_Z = 0;

    protected static final AxisAlignedBox worldBounds = new AxisAlignedBox(new Vector3(-2 * BOUNDS - 1,
                                                                                       -2 * BOUNDS - 1,
                                                                                       -2 * BOUNDS - 1),
                                                                           new Vector3(2 * BOUNDS + 1,
                                                                                       2 * BOUNDS + 1,
                                                                                       2 * BOUNDS + 1));

    // positive half-circle
    private static final double MAX_THETA = Math.PI;
    private static final double MIN_THETA = 0;

    // positive octant
    private static final double MAX_PHI = Math.PI / 2.0;
    private static final double MIN_PHI = Math.PI / 12.0;

    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = BOUNDS;

    private static final double ANGLE_RATE = Math.PI / 4.0;
    private static final double ZOOM_RATE = 10.0;

    private Entity camera;

    private double theta; // angle of rotation around global y-axis
    private double phi; // angle of rotation from xz plane
    private double zoom; // distance from origin

    protected final EntitySystem system;
    private Job renderJob;

    public TransparentDemo() {
        super(LwjglFramework.create());
        system = new EntitySystem();
    }

    @Override
    protected void installInputHandlers(InputManager io) {
        io.on(Predicates.keyPress(KeyCode.O)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                Profiler.getDataSnapshot().print(System.out);
            }
        });

        // camera controls
        io.on(Predicates.keyHeld(KeyCode.W)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                phi += ANGLE_RATE * ((next.getTimestamp() - prev.getTimestamp()) / 1e9);
                if (phi > MAX_PHI) {
                    phi = MAX_PHI;
                }
                updateCameraOrientation();
            }
        });
        io.on(Predicates.keyHeld(KeyCode.S)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                phi -= ANGLE_RATE * ((next.getTimestamp() - prev.getTimestamp()) / 1e9);
                if (phi < MIN_PHI) {
                    phi = MIN_PHI;
                }
                updateCameraOrientation();
            }
        });
        io.on(Predicates.keyHeld(KeyCode.D)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                theta -= ANGLE_RATE * ((next.getTimestamp() - prev.getTimestamp()) / 1e9);
                if (theta < MIN_THETA) {
                    theta = MIN_THETA;
                }
                updateCameraOrientation();
            }
        });
        io.on(Predicates.keyHeld(KeyCode.A)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                theta += ANGLE_RATE * ((next.getTimestamp() - prev.getTimestamp()) / 1e9);
                if (theta > MAX_THETA) {
                    theta = MAX_THETA;
                }
                updateCameraOrientation();
            }
        });
        io.on(Predicates.keyHeld(KeyCode.X)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                zoom += ZOOM_RATE * ((next.getTimestamp() - prev.getTimestamp()) / 1e9);
                if (zoom > MAX_ZOOM) {
                    zoom = MAX_ZOOM;
                }
                updateCameraOrientation();
            }
        });
        io.on(Predicates.keyHeld(KeyCode.Z)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                zoom -= ZOOM_RATE * ((next.getTimestamp() - prev.getTimestamp()) / 1e9);
                if (zoom < MIN_ZOOM) {
                    zoom = MIN_ZOOM;
                }
                updateCameraOrientation();
            }
        });
    }

    private void updateCameraOrientation() {
        Vector3 pos = new Vector3();
        double r = zoom * Math.cos(phi);
        pos.x = r * Math.cos(theta);
        pos.y = zoom * Math.sin(phi);
        pos.z = r * Math.sin(theta);

        camera.get(Transform.class).getData()
              .setMatrix(new Matrix4().lookAt(new Vector3(), pos, new Vector3(0, 1, 0)));
    }

    @Override
    protected void init(OnscreenSurface surface) {
        // rendering
        renderJob = system.getScheduler()
                          .createJob("render",
                                     new UpdateWorldBoundsTask(),
                                     new ComputeCameraFrustumTask(),
                                     new ComputeShadowFrustumTask(),
                                     new BuildVisibilityIndexTask(new QuadTree<Entity>(worldBounds,
                                                                                       6)),
                                     new ComputePVSTask(),
                                     new ComputeLightGroupTask(),
                                     new FixedFunctionRenderTask(surface.getFramework(),
                                                                 1024,
                                                                 false));

        surface.setVSyncEnabled(true);

        // camera
        theta = (MAX_THETA + MIN_THETA) / 2.0;
        phi = (MAX_PHI + MIN_PHI) / 2.0;
        zoom = (MAX_ZOOM + MIN_ZOOM) / 2.0;

        camera = system.addEntity();
        camera.add(Camera.class).getData().setSurface(surface).setZDistances(1.0, BOUNDS);
        updateCameraOrientation();

        // boxes
        Geometry boxGeom = Teapot.create(BOX_SIZE); //Box.create(BOX_SIZE);
        for (int x = 0; x < NUM_X; x++) {
            for (int y = 0; y < NUM_Y; y++) {
                for (int z = 0; z < NUM_Z; z++) {
                    Vector4 pos = new Vector4();
                    pos.x = (x - NUM_X / 2.0) * (BOX_SIZE + GAP_X);
                    pos.y = (y - NUM_Y / 2.0) * (BOX_SIZE + GAP_Y);
                    pos.z = (z - NUM_Z / 2.0) * (BOX_SIZE + GAP_Z);
                    pos.w = 1;

                    Entity box = system.addEntity();

                    box.add(Renderable.class)
                       .getData()
                       .setVertices(boxGeom.getVertices())
                       .setDrawStyle(DrawStyle.SOLID, DrawStyle.SOLID)
                       .setIndices(boxGeom.getPolygonType(), boxGeom.getIndices(),
                                   boxGeom.getIndexOffset(), boxGeom.getIndexCount())
                       .setLocalBounds(boxGeom.getBounds());
                    box.add(BlinnPhongMaterial.class).getData()
                       .setNormals(boxGeom.getNormals());
                    box.add(DiffuseColor.class).getData()
                    //                       .setColor(new ColorRGB(x / (double) NUM_X,
                    //                                              y / (double) NUM_Y,
                    //                                              z / (double) NUM_Z));
                       .setColor(new ColorRGB(.5, .5, .5));
                    double rand = Math.random();
                    //                    if (rand < .3) {
                    //                        box.add(Transparent.class).getData().setOpacity(.5)
                    //                           .setAdditive(true);
                    //                    } else if (rand < .6) {
                    box.add(Transparent.class).getData().setOpacity(.5)
                       .setAdditive(false);
                    //                    }

                    box.get(Transform.class).getData()
                       .setMatrix(new Matrix4().setIdentity().setCol(3, pos));
                }
            }
        }

        // ambient light
        system.addEntity().add(AmbientLight.class).getData()
              .setColor(new ColorRGB(.2, .2, .2));

        // a point light
        Entity point = system.addEntity();
        point.add(PointLight.class).getData().setColor(new ColorRGB(0.8, 0.8, 0.8));
        point.get(Transform.class)
             .getData()
             .setMatrix(new Matrix4().setIdentity().setCol(3,
                                                           new Vector4(100, 100, 100, 1)));

        // a directed light, which casts shadows
        //        Entity inf = system.addEntity();
        //        inf.add(DirectionLight.class).getData().setColor(new ColorRGB(1, 1, 1))
        //           .setShadowCaster(true);
        //        inf.get(Transform.class)
        //           .getData()
        //           .setMatrix(new Matrix4().lookAt(new Vector3(), new Vector3(15, 15, 15),
        //                                           new Vector3(0, 1, 0)));
    }

    @Override
    protected void renderFrame(OnscreenSurface surface) {
        system.getScheduler().runOnCurrentThread(renderJob);
    }

    public static void main(String[] args) {
        new TransparentDemo().run();
    }
}
