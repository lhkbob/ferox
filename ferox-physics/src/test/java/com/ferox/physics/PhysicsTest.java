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
import com.ferox.physics.controller.SpatialIndexCollisionController;
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
import com.ferox.scene.DirectionLight;
import com.ferox.scene.PointLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.ferox.scene.controller.CameraController;
import com.ferox.scene.controller.SpatialIndexController;
import com.ferox.scene.controller.VisibilityController;
import com.ferox.scene.controller.WorldBoundsController;
import com.ferox.scene.controller.ffp.FixedFunctionRenderController;
import com.ferox.scene.controller.light.LightGroupController;
import com.ferox.util.geom.Box;
import com.ferox.util.geom.Geometry;
import com.ferox.util.geom.Sphere;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Controller;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.SimpleController;

public class PhysicsTest {
    private static final StorageMode COMPILE_TYPE = StorageMode.GPU_STATIC;
    private static final int BOUNDS = 100;
   
    private static final int NUM_X = 10;
    private static final int NUM_Y = 10;
    private static final int NUM_Z = 10;
    private static final double SCALE_X = 3.0;
    private static final double SCALE_Y = 2.0;
    private static final double SCALE_Z = 3.0;
    
    private static final double MARGIN = .05;
    private static final double PERCENT = .5;

    private static final double RANDOM = 0;
    
    private static final double START_POS_X = -5;
    private static final double START_POS_Y = 1 + 2 * MARGIN;
    private static final double START_POS_Z = -3;
    
    private static final AxisAlignedBox worldBounds = new AxisAlignedBox(new Vector3(-2 * BOUNDS - 1, -2 * BOUNDS - 1, -2 * BOUNDS - 1), 
                                                                         new Vector3(2 * BOUNDS + 1, 2 * BOUNDS + 1, 2 * BOUNDS + 1));
    
    private static volatile boolean paused = false;
    
    public static void main(String[] args) throws Exception {
        final Framework framework = LwjglFramework.create();
        System.out.println("OpenGL: " + framework.getCapabilities().getVendor() + " " + framework.getCapabilities().getVersion());
        
        final EntitySystem system = new EntitySystem();
        
        // physics handling
        system.getControllerManager().addController(new com.ferox.physics.controller.ForcesController());

//        system.getControllerManager().addController(new SpatialIndexCollisionController(new com.ferox.math.bounds.QuadTree<Entity>(worldBounds, 6), new DefaultCollisionAlgorithmProvider()));
//        system.getControllerManager().addController(new SpatialIndexCollisionController(new com.ferox.math.bounds.SimpleSpatialIndex<Entity>(), new DefaultCollisionAlgorithmProvider()));
        system.getControllerManager().addController(new SpatialIndexCollisionController(new com.ferox.math.bounds.Octree<Entity>(worldBounds, 6), new DefaultCollisionAlgorithmProvider()));

        system.getControllerManager().addController(new com.ferox.physics.controller.ConstraintSolvingController());
        system.getControllerManager().addController(new com.ferox.physics.controller.MotionController());
        
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
                paused = !paused;
            }
        }, new KeyPressedCondition(KeyCode.SPACE));
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                process(system, 1, 0, false);
            }
        }, new KeyPressedCondition(KeyCode.S));
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                framework.destroy();
            }
        }, new KeyPressedCondition(KeyCode.ESCAPE));
        
        long start = System.currentTimeMillis();
        int  numFrames = 0;
        while(!framework.isDestroyed()) {
            io.process();
            if (!paused) {
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
                                                                     .setHeight(500);
//                                                                     .setMultiSampling(MultiSampling.FOUR_X);
        OnscreenSurface surface = framework.createSurface(options);
//        surface.setVSyncEnabled(true);

        // camera
        Entity camera = system.addEntity();
        camera.add(Camera.ID).getData().setSurface(surface)
                                       .setZDistances(1.0, 6 * BOUNDS);
        camera.add(Transform.ID).getData().setMatrix(new Matrix4(-.707, -.577, -.707, .3 * BOUNDS, 
                                                                 0, .577, 0, .2 * BOUNDS,
                                                                 .707, -.577, -.707,  .3 * BOUNDS,
                                                                 0, 0, 0, 1));
        return surface;
    }
    
    private static void buildScene(EntitySystem scene) throws Exception {
        // shapes
        Geometry box = new Box(2 + 2 * MARGIN, COMPILE_TYPE);
        Geometry sphere = new Sphere(1 + MARGIN, 8, COMPILE_TYPE);
        
        com.ferox.physics.collision.Shape boxShape = new com.ferox.physics.collision.shape.Box(2, 2, 2);
        com.ferox.physics.collision.Shape sphereShape = new com.ferox.physics.collision.shape.Sphere(1);
        boxShape.setMargin(MARGIN);
        sphereShape.setMargin(MARGIN);
        
        double startX = START_POS_X - NUM_X / 2.0;
        double startY = START_POS_Y;
        double startZ = START_POS_Z - NUM_Z / 2.0;
        
        double randXLim = RANDOM * (SCALE_X - 2.0) / 2.0;
        double randYLim = RANDOM * (SCALE_Y - 2.0) / 2.0;
        double randZLim = RANDOM * (SCALE_Z - 2.0) / 2.0;
        
        for (int z = 0; z < NUM_Z; z++) {
            for (int y = 0; y < NUM_Y; y++) {
                for (int x = 0; x < NUM_X; x++) {
                    com.ferox.physics.collision.Shape physShape;
                    Geometry geomShape;
                    ColorRGB color;
                    
//                    if (Math.random() > PERCENT) {
//                    if (z == 0 || z == NUM_Z - 1 || y == 0 || y == NUM_Y - 1 || x == 0 || x == NUM_X - 1) {
                    if (true) {
//                    if (false) {
                        physShape = boxShape;
                        geomShape = box;
                        color = new ColorRGB(.7, .2, .2);
                    } else {
                        physShape = sphereShape;
                        geomShape = sphere;
                        color = new ColorRGB(0, 0, .8);
                    }
                    
                    double rx = (Math.random() * randXLim - randXLim / 2);
                    double ry = (Math.random() * randYLim - randYLim / 2);
                    double rz = (Math.random() * randZLim - randZLim / 2);

                    Entity e = scene.addEntity();
                    e.add(Renderable.ID).getData().setVertices(geomShape.getVertices())
                                                  .setLocalBounds(geomShape.getBounds())
                                                  .setIndices(geomShape.getPolygonType(), geomShape.getIndices(), geomShape.getIndexOffset(), geomShape.getIndexCount());
                    e.add(BlinnPhongMaterial.ID).getData().setNormals(geomShape.getNormals());
                    e.add(DiffuseColor.ID).getData().setColor(color);
                    e.add(Transform.ID);
                    
                    e.add(CollisionBody.ID).getData().setShape(physShape)
                                                     .setTransform(new Matrix4(1, 0, 0, (SCALE_X + 2 * MARGIN) * x + rx + startX,
                                                                               0, 1, 0, (SCALE_Y + 2 * MARGIN + (y > NUM_Y / 2 ? 1 : 0)) * y + ry + startY,
                                                                               0, 0, 1, (SCALE_Z + 2 * MARGIN) * z + rz + startZ,
                                                                               0, 0, 0, 1));
                    e.add(RigidBody.ID).getData().setMass(1.0);
                }
            }
        }
        
        // some walls
        Geometry bottomWall = new Box(BOUNDS);
        Entity wall = scene.addEntity();
        wall.add(Renderable.ID).getData().setVertices(bottomWall.getVertices())
                                         .setLocalBounds(bottomWall.getBounds())
                                         .setIndices(bottomWall.getPolygonType(), bottomWall.getIndices(), bottomWall.getIndexOffset(), bottomWall.getIndexCount());
        wall.add(BlinnPhongMaterial.ID).getData().setNormals(bottomWall.getNormals());
        wall.add(DiffuseColor.ID).getData().setColor(new ColorRGB(0.5, 0.5, 0.5));
        wall.add(Transform.ID);

        wall.add(CollisionBody.ID).getData().setShape(new com.ferox.physics.collision.shape.Box(BOUNDS, BOUNDS, BOUNDS))
                                            .setTransform(new Matrix4(1, 0, 0, 0,
                                                                      0, 1, 0, -BOUNDS / 2,
                                                                      0, 0, 1, 0,
                                                                      0, 0, 0, 1));
        
        // ambient light
        scene.addEntity().add(AmbientLight.ID).getData().setColor(new ColorRGB(0.2, 0.2, 0.2));
        
        // a point light
        Entity point = scene.addEntity();
        point.add(PointLight.ID).getData().setColor(new ColorRGB(0.5, 0.5, 0.5));
        point.add(Transform.ID).getData().setMatrix(new Matrix4(1, 0, 0, BOUNDS / 2,
                                                                0, 1, 0, BOUNDS / 2,
                                                                0, 0, 1, BOUNDS / 2,
                                                                0, 0, 0, 1));
        
        // a directed light, which casts shadows
        Entity inf = scene.addEntity();
        inf.add(DirectionLight.ID).getData().setColor(new ColorRGB(1, 1, 1));
        inf.add(Transform.ID);
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
//                System.out.println(cb.getEntity().getId() + " " + cb.getTransform());
            }
        }
    }
}
