package com.ferox.physics;

import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.logic.Action;
import com.ferox.input.logic.InputManager;
import com.ferox.input.logic.InputState;
import com.ferox.input.logic.Predicates;
import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.QuadTree;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.collision.DefaultCollisionAlgorithmProvider;
import com.ferox.physics.controller.SpatialIndexCollisionController;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.impl.lwjgl.LwjglFramework;
import com.ferox.scene.Transform;
import com.ferox.scene.controller.CameraController;
import com.ferox.scene.controller.SpatialIndexController;
import com.ferox.scene.controller.VisibilityController;
import com.ferox.scene.controller.WorldBoundsController;
import com.ferox.scene.controller.ffp.FixedFunctionRenderController;
import com.ferox.scene.controller.light.LightGroupController;
import com.ferox.util.ApplicationStub;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.SimpleController;

public class PhysicsApplicationStub extends ApplicationStub {
    protected static final int BOUNDS = 100;
    protected static final double MARGIN = .05;

    protected static final AxisAlignedBox worldBounds = new AxisAlignedBox(new Vector3(-2 * BOUNDS - 1,
                                                                                       -2 * BOUNDS - 1,
                                                                                       -2 * BOUNDS - 1),
                                                                           new Vector3(2 * BOUNDS + 1,
                                                                                       2 * BOUNDS + 1,
                                                                                       2 * BOUNDS + 1));

    private boolean paused;
    private boolean stepOnce;

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
        io.on(Predicates.keyPress(KeyCode.S)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                stepOnce = true;
            }
        });
    }

    @Override
    protected void init(OnscreenSurface surface) {
        // physics handling
        system.getControllerManager()
              .addController(new com.ferox.physics.controller.ForcesController());

        //        system.getControllerManager().addController(new SpatialIndexCollisionController(new com.ferox.math.bounds.QuadTree<Entity>(worldBounds, 6), new DefaultCollisionAlgorithmProvider()));
        //        system.getControllerManager().addController(new SpatialIndexCollisionController(new com.ferox.math.bounds.SimpleSpatialIndex<Entity>(), new DefaultCollisionAlgorithmProvider()));
        system.getControllerManager()
              .addController(new SpatialIndexCollisionController(new com.ferox.math.bounds.Octree<Entity>(worldBounds,
                                                                                                          6),
                                                                 new DefaultCollisionAlgorithmProvider()));

        system.getControllerManager()
              .addController(new com.ferox.physics.controller.ConstraintSolvingController());
        system.getControllerManager()
              .addController(new com.ferox.physics.controller.MotionController());

        system.getControllerManager().addController(new TransformController());

        // rendering
        system.getControllerManager().addController(new WorldBoundsController());
        system.getControllerManager().addController(new CameraController());
        system.getControllerManager()
              .addController(new SpatialIndexController(new QuadTree<Entity>(worldBounds,
                                                                             6)));
        system.getControllerManager().addController(new VisibilityController());
        system.getControllerManager()
              .addController(new LightGroupController(worldBounds));
        system.getControllerManager()
              .addController(new FixedFunctionRenderController(surface.getFramework()));

        surface.setVSyncEnabled(true);
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
