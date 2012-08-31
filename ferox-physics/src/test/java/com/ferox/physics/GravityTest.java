package com.ferox.physics;

import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.logic.InputManager;
import com.ferox.input.logic.InputState;
import com.ferox.input.logic.KeyPressedCondition;
import com.ferox.input.logic.Trigger;
import com.ferox.math.AxisAlignedBox;
import com.ferox.math.ColorRGB;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.QuadTree;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.collision.DefaultCollisionAlgorithmProvider;
import com.ferox.physics.controller.ConstraintSolvingController;
import com.ferox.physics.controller.ForcesController;
import com.ferox.physics.controller.MotionController;
import com.ferox.physics.controller.SpatialIndexCollisionController;
import com.ferox.physics.dynamics.Gravity;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.OnscreenSurfaceOptions.MultiSampling;
import com.ferox.renderer.impl.lwjgl.LwjglFramework;
import com.ferox.resource.VertexBufferObject.StorageMode;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.BlinnPhongMaterial;
import com.ferox.scene.Camera;
import com.ferox.scene.DiffuseColor;
import com.ferox.scene.PointLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.ferox.scene.controller.CameraController;
import com.ferox.scene.controller.SpatialIndexController;
import com.ferox.scene.controller.VisibilityController;
import com.ferox.scene.controller.WorldBoundsController;
import com.ferox.scene.controller.ffp.FixedFunctionRenderController;
import com.ferox.scene.controller.light.LightGroupController;
import com.ferox.util.geom.Geometry;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Controller;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.SimpleController;

public class GravityTest {
    private static final StorageMode COMPILE_TYPE = StorageMode.GPU_STATIC;
    private static final int BOUNDS = 20;
   
    private static final double MARGIN = .05;
    
    private static final boolean STEP_ONLY = false;
    
    private static volatile boolean paused = true;
    
    public static void main(String[] args) throws Exception {
        final Framework framework = LwjglFramework.create();
        System.out.println("OpenGL: " + framework.getCapabilities().getVendor() + " " + framework.getCapabilities().getVersion());
        
        final EntitySystem system = new EntitySystem();
        
        AxisAlignedBox worldBounds = new AxisAlignedBox(new Vector3(-2 * BOUNDS - 1, -2 * BOUNDS - 1, -2 * BOUNDS - 1), 
                                                        new Vector3(2 * BOUNDS + 1, 2 * BOUNDS + 1, 2 * BOUNDS + 1));
        
        // physics handling
        system.getControllerManager().addController(new ForcesController());
//        system.getControllerManager().addController(new SpatialIndexCollisionController(new com.ferox.math.bounds.Octree<Entity>(worldBounds, 6), new DefaultCollisionAlgorithmProvider()));
//        system.getControllerManager().addController(new SpatialIndexCollisionController(new com.ferox.math.bounds.QuadTree<Entity>(worldBounds, 6), new DefaultCollisionAlgorithmProvider()));
        system.getControllerManager().addController(new SpatialIndexCollisionController(new com.ferox.math.bounds.SimpleSpatialIndex<Entity>(), new DefaultCollisionAlgorithmProvider()));

        final MotionController m = new MotionController();
        system.getControllerManager().addController(new ConstraintSolvingController());
        system.getControllerManager().addController(m);
        system.getControllerManager().addController(new TransformController());
        
        // rendering
        system.getControllerManager().addController(new WorldBoundsController());
        system.getControllerManager().addController(new CameraController());
        system.getControllerManager().addController(new SpatialIndexController(new QuadTree<Entity>(worldBounds, 6)));
        system.getControllerManager().addController(new VisibilityController());
        system.getControllerManager().addController(new LightGroupController(worldBounds));
        system.getControllerManager().addController(new FixedFunctionRenderController(framework));
        
        OnscreenSurface surface = buildSurface(framework, system);
        buildScene(system);
        
        InputManager io = new InputManager(surface);
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                if (STEP_ONLY)
                    process(system, 1, 0, true);
                else
                    paused = !paused;
            }
        }, new KeyPressedCondition(KeyCode.SPACE));
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                framework.destroy();
            }
        }, new KeyPressedCondition(KeyCode.ESCAPE));
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                System.out.println("break point");
            }
        }, new KeyPressedCondition(KeyCode.D));
        
        
        long start = System.currentTimeMillis();
        int  numFrames = 0;
        while(!framework.isDestroyed()) {
            io.process();
            if (!paused && !STEP_ONLY) {
                long now = System.currentTimeMillis();
                process(system, numFrames, (now - start) / 1e3, (now - start) > 1000);
                numFrames++;
                if (now - start > 1000) {
                    start = now;
                    numFrames = 0;
                }
            } else {
                start = System.currentTimeMillis();
                numFrames = 0;
                try {
                    Thread.sleep(5);
                } catch(InterruptedException e) { }
            }
        }
    }
    
    private static void process(EntitySystem system, int numFrames, double time, boolean printStats) {
        system.getControllerManager().process(1 / 60.0);
        
        if (printStats) {
            System.out.println("Avg FPS: " + (numFrames / time));
            
            long total = 0;
            for (Controller c: system.getControllerManager().getControllers()) {
                total += system.getControllerManager().getExecutionTime(c);
                System.out.printf(" - %s: %.3f ms\n", c.getClass().getSimpleName(), (system.getControllerManager().getExecutionTime(c) / 1e6));
            }
            System.out.printf(" - total: %.3f ms\n", total / 1e6);
        }
    }
    
    private static OnscreenSurface buildSurface(Framework framework, EntitySystem system) {
        OnscreenSurfaceOptions options = new OnscreenSurfaceOptions().setWidth(500)
                                                                     .setHeight(500)
                                                                     .setMultiSampling(MultiSampling.NONE);
        OnscreenSurface surface = framework.createSurface(options);
        surface.setVSyncEnabled(true);
        surface.setTitle(GravityTest.class.getSimpleName());

        // camera
        Entity camera = system.addEntity();
        camera.add(Camera.ID).getData().setSurface(surface)
                                       .setZDistances(1.0, 6 * BOUNDS);
        camera.add(Transform.ID).getData().setMatrix(new Matrix4(-1, 0, 0, 0, 
                                                                 0, 1, 0, 0,
                                                                 0, 0, -1, .75 * BOUNDS,
                                                                 0, 0, 0, 1));
        return surface;
    }
    
    private static void buildScene(EntitySystem scene) throws Exception {
        // shapes
        Geometry geomShape1 = new com.ferox.util.geom.Box(2 + 2 * MARGIN, COMPILE_TYPE);
        com.ferox.physics.collision.Shape physShape1 = new com.ferox.physics.collision.shape.Box(2, 2, 2);
        
        Geometry geomShape2 = new com.ferox.util.geom.Box(2 + 2 * MARGIN, COMPILE_TYPE);
        com.ferox.physics.collision.Shape physShape2 = new com.ferox.physics.collision.shape.Box(2, 2, 2);

//        Geometry geomShape1 = new com.ferox.util.geom.Sphere(1 + MARGIN, 16, COMPILE_TYPE);
//        com.ferox.physics.collision.Shape physShape1 = new com.ferox.physics.collision.shape.Sphere(1);
        
        physShape1.setMargin(MARGIN);
        physShape2.setMargin(MARGIN);
        
        // falling down entity
        Entity e = scene.addEntity();
        e.add(Renderable.ID).getData().setVertices(geomShape1.getVertices())
                                      .setLocalBounds(geomShape1.getBounds())
                                      .setIndices(geomShape1.getPolygonType(), geomShape1.getIndices(), geomShape1.getIndexOffset(), geomShape1.getIndexCount());
        e.add(BlinnPhongMaterial.ID).getData().setNormals(geomShape1.getNormals());
        e.add(DiffuseColor.ID).getData().setColor(new ColorRGB(1.0, 0.0, 0.0));
        e.add(Transform.ID);
        
        e.add(CollisionBody.ID).getData().setShape(physShape1)
                                         .setTransform(new Matrix4(1, 0, 0, 0,
                                                                   0, 1, 0, BOUNDS / 2,
                                                                   0, 0, 1, 0,
                                                                   0, 0, 0, 1));
        e.add(RigidBody.ID).getData().setMass(1.0);
        e.add(Gravity.ID).getData().setGravity(new Vector3(0, -10, 0));
        
        // falling up entity
        e = scene.addEntity();
        e.add(Renderable.ID).getData().setVertices(geomShape2.getVertices())
                                      .setLocalBounds(geomShape2.getBounds())
                                      .setIndices(geomShape2.getPolygonType(), geomShape2.getIndices(), geomShape2.getIndexOffset(), geomShape2.getIndexCount());
        e.add(BlinnPhongMaterial.ID).getData().setNormals(geomShape2.getNormals());
        e.add(DiffuseColor.ID).getData().setColor(new ColorRGB(0.0, 1.0, 0.0));
        e.add(Transform.ID);
        
        e.add(CollisionBody.ID).getData().setShape(physShape2)
                                         .setTransform(new Matrix4(1, 0, 0, 0,
                                                                   0, 1, 0, -BOUNDS / 2,
                                                                   0, 0, 1, 0,
                                                                   0, 0, 0, 1));
        e.add(RigidBody.ID).getData().setMass(1.0);
        e.add(Gravity.ID).getData().setGravity(new Vector3(0, 10, 0));
        
        // ambient light
        scene.addEntity().add(AmbientLight.ID).getData().setColor(new ColorRGB(0.2, 0.2, 0.2));
        
        // a point light
        Entity point = scene.addEntity();
        point.add(PointLight.ID).getData().setColor(new ColorRGB(0.5, 0.5, 0.5));
        point.add(Transform.ID).getData().setMatrix(new Matrix4(1, 0, 0, BOUNDS / 2,
                                                                0, 1, 0, BOUNDS / 2,
                                                                0, 0, 1, BOUNDS / 2,
                                                                0, 0, 0, 1));
    }
    
    private static class TransformController extends SimpleController {
        @Override
        public void process(double dt) {
            CollisionBody cb = getEntitySystem().createDataInstance(CollisionBody.ID);
            Transform t = getEntitySystem().createDataInstance(Transform.ID);
            
            ComponentIterator it = new ComponentIterator(getEntitySystem());
            it.addRequired(cb);
            it.addRequired(t);
            
            while(it.next()) {
                t.setMatrix(cb.getTransform());
            }
        }
    }
}
