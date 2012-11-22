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
package com.ferox.physics;

import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.logic.Action;
import com.ferox.input.logic.InputManager;
import com.ferox.input.logic.InputState;
import com.ferox.input.logic.Predicates;
import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.QuadTree;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.collision.DefaultCollisionAlgorithmProvider;
import com.ferox.physics.controller.SpatialIndexCollisionController;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.impl.lwjgl.LwjglFramework;
import com.ferox.scene.Camera;
import com.ferox.scene.Transform;
import com.ferox.scene.controller.ComputeCameraFrustumTask;
import com.ferox.scene.controller.BuildVisibilityIndexTask;
import com.ferox.scene.controller.ComputePVSTask;
import com.ferox.scene.controller.UpdateWorldBoundsTask;
import com.ferox.scene.controller.ffp.FixedFunctionRenderController;
import com.ferox.scene.controller.light.ComputeLightGroupTask;
import com.ferox.scene.controller.light.ComputeShadowFrustumTask;
import com.ferox.util.ApplicationStub;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.SimpleController;

public class PhysicsApplicationStub extends ApplicationStub {
    protected static final int BOUNDS = 50;
    protected static final double MARGIN = .05;

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

    private boolean paused;
    private boolean stepOnce;

    private Entity camera;

    private double theta; // angle of rotation around global y-axis
    private double phi; // angle of rotation from xz plane
    private double zoom; // distance from origin

    protected final EntitySystem system;

    public PhysicsApplicationStub() {
        super(LwjglFramework.create());
        system = new EntitySystem();
    }

    @Override
    protected void installInputHandlers(InputManager io) {
        io.on(Predicates.keyPress(KeyCode.SPACE)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                paused = !paused;
            }
        });
        io.on(Predicates.keyPress(KeyCode.P)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                stepOnce = true;
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

        camera.get(Transform.ID).getData()
              .setMatrix(new Matrix4().lookAt(new Vector3(), pos, new Vector3(0, 1, 0)));
    }

    @Override
    protected void init(OnscreenSurface surface) {
        // physics handling
        system.getControllerManager()
              .addController(new com.ferox.physics.controller.ForcesTask());

        //        system.getControllerManager().addController(new SpatialIndexCollisionController(new com.ferox.math.bounds.QuadTree<Entity>(worldBounds, 6), new DefaultCollisionAlgorithmProvider()));
        //        system.getControllerManager().addController(new SpatialIndexCollisionController(new com.ferox.math.bounds.SimpleSpatialIndex<Entity>(), new DefaultCollisionAlgorithmProvider()));
        system.getControllerManager()
              .addController(new SpatialIndexCollisionController(new com.ferox.math.bounds.Octree<Entity>(worldBounds,
                                                                                                          6),
                                                                 new DefaultCollisionAlgorithmProvider()));

        system.getControllerManager()
              .addController(new com.ferox.physics.controller.ConstraintSolvingTask());
        system.getControllerManager()
              .addController(new com.ferox.physics.controller.MotionTask());

        system.getControllerManager().addController(new TransformController());

        // rendering
        system.getControllerManager().addController(new UpdateWorldBoundsTask());
        system.getControllerManager().addController(new ComputeCameraFrustumTask());
        system.getControllerManager().addController(new ComputeShadowFrustumTask());
        system.getControllerManager()
              .addController(new BuildVisibilityIndexTask(new QuadTree<Entity>(worldBounds,
                                                                             6)));
        system.getControllerManager().addController(new ComputePVSTask());
        system.getControllerManager()
              .addController(new ComputeLightGroupTask(worldBounds));
        system.getControllerManager()
              .addController(new FixedFunctionRenderController(surface.getFramework()));

        surface.setVSyncEnabled(true);

        // camera
        theta = (MAX_THETA + MIN_THETA) / 2.0;
        phi = (MAX_PHI + MIN_PHI) / 2.0;
        zoom = (MAX_ZOOM + MIN_ZOOM) / 2.0;

        camera = system.addEntity();
        camera.add(Camera.ID).getData().setSurface(surface).setZDistances(1.0, BOUNDS);
        camera.add(Transform.ID);
        updateCameraOrientation();
    }

    @Override
    protected void renderFrame(OnscreenSurface surface) {
        if (!paused || stepOnce) {
            system.getControllerManager().process(1 / 60.0);
            stepOnce = false;
        }
    }

    private static class TransformController extends SimpleController {
        @Override
        public void process(double dt) {
            CollisionBody cb = getEntitySystem().createDataInstance(CollisionBody.ID);
            Transform t = getEntitySystem().createDataInstance(Transform.ID);

            ComponentIterator it = new ComponentIterator(getEntitySystem());
            it.addRequired(cb);
            it.addRequired(t);

            while (it.next()) {
                t.setMatrix(cb.getTransform());
            }
        }
    }
}
