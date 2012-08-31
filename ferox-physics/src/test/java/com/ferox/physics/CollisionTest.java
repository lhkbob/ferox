package com.ferox.physics;

import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.logic.InputManager;
import com.ferox.input.logic.InputState;
import com.ferox.input.logic.KeyHeldCondition;
import com.ferox.input.logic.KeyPressedCondition;
import com.ferox.input.logic.Trigger;
import com.ferox.math.AxisAlignedBox;
import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.ferox.math.Quat4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.QuadTree;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.collision.Shape;
import com.ferox.physics.collision.algorithm.GjkEpaCollisionAlgorithm;
import com.ferox.physics.collision.algorithm.GjkEpaCollisionAlgorithm2;
import com.ferox.physics.collision.shape.ConvexShape;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.OnscreenSurfaceOptions.MultiSampling;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.renderer.impl.lwjgl.LwjglFramework;
import com.ferox.resource.BufferData;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.VertexBufferObject.StorageMode;
import com.ferox.scene.AmbientLight;
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
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.SimpleController;

public class CollisionTest {
    public static final double BOUNDS = 50;
    public static final double MARGIN = 0.05;
    
    public static final double ANGLE_RATE = Math.PI / 4;
    public static final double TRANSLATE_RATE = 4;
    
    public static final boolean A_SPHERE = false;
    public static final boolean B_SPHERE = false;
    
    public static final double A_SIZE = 2;
//    public static final double B_SIZE = 100;
    public static final double B_SIZE = 2;
    
    public static void main(String[] args) throws Exception {
        final Framework framework = LwjglFramework.create();
        System.out.println("OpenGL: " + framework.getCapabilities().getVendor() + " " + framework.getCapabilities().getVersion());
        
        final EntitySystem system = new EntitySystem();
        
        AxisAlignedBox worldBounds = new AxisAlignedBox(new Vector3(-2 * BOUNDS - 1, -2 * BOUNDS - 1, -2 * BOUNDS - 1), 
                                                        new Vector3(2 * BOUNDS + 1, 2 * BOUNDS + 1, 2 * BOUNDS + 1));
        
        // physics handling
        system.getControllerManager().addController(new TransformController());
        
        // rendering
        system.getControllerManager().addController(new WorldBoundsController());
        system.getControllerManager().addController(new CameraController());
        system.getControllerManager().addController(new SpatialIndexController(new QuadTree<Entity>(worldBounds, 6)));
        system.getControllerManager().addController(new VisibilityController());
        system.getControllerManager().addController(new LightGroupController(worldBounds));
        system.getControllerManager().addController(new FixedFunctionRenderController(framework));
        
        OnscreenSurface surface = buildSurface(framework, system);
        InputManager io = new InputManager(surface);

        // build scene
        
        // ambient light
        system.addEntity().add(AmbientLight.ID).getData().setColor(new ColorRGB(0.2, 0.2, 0.2));
        
        // a point light
        Entity point = system.addEntity();
        point.add(PointLight.ID).getData().setColor(new ColorRGB(0.5, 0.5, 0.5));
        point.add(Transform.ID).getData().setMatrix(new Matrix4(1, 0, 0, BOUNDS / 2,
                                                                0, 1, 0, BOUNDS / 2,
                                                                0, 0, 1, BOUNDS / 2,
                                                                0, 0, 0, 1));
        
        // a directed light, which casts shadows
        Entity inf = system.addEntity();
        inf.add(DirectionLight.ID).getData().setColor(new ColorRGB(1, 1, 1));
        inf.add(Transform.ID);
        
        // shapes
        Geometry aGeom = (A_SPHERE ? new Sphere(A_SIZE / 2 + MARGIN, 8) : new Box(A_SIZE + 2 * MARGIN));
        Shape aPhys = (A_SPHERE ? new com.ferox.physics.collision.shape.Sphere(A_SIZE / 2) : new com.ferox.physics.collision.shape.Box(A_SIZE, A_SIZE, A_SIZE));
        aPhys.setMargin(MARGIN);
        Geometry bGeom = (B_SPHERE ? new Sphere(B_SIZE / 2 + MARGIN, 8) : new Box(B_SIZE + 2 * MARGIN));
        Shape bPhys = (B_SPHERE ? new com.ferox.physics.collision.shape.Sphere(B_SIZE / 2) : new com.ferox.physics.collision.shape.Box(B_SIZE, B_SIZE, B_SIZE));
        bPhys.setMargin(MARGIN);
        
        Entity a = system.addEntity();
        a.add(Renderable.ID).getData().setVertices(aGeom.getVertices())
                                      .setLocalBounds(aGeom.getBounds())
                                      .setIndices(aGeom.getPolygonType(), aGeom.getIndices(), aGeom.getIndexOffset(), aGeom.getIndexCount());
        a.add(DiffuseColor.ID).getData().setColor(new ColorRGB(1, .5, .5));
        a.add(Transform.ID);
                    
        a.add(CollisionBody.ID).getData().setShape(aPhys)
                                         .setTransform(new Matrix4(1, 0, 0, 0,
                                                                   0, 1, 0, 5,
                                                                   0, 0, 1, 0,
                                                                   0, 0, 0, 1));
//                                         .setTransform(new Matrix4(0.3014718305476797, 0.10220009472983765, 0.9479820019512202, 7.748553848709842,
//                                                                   0.03161100381232058, 0.9926210063725548, -0.11706529009879056, 1.1190060017643766,
//                                                                   -0.9529509325375618, 0.06525854997736887, 0.29601662424708963, 5.405042647953112,
//                                                                   0.0, 0.0, 0.0, 1.0));

        Entity b = system.addEntity();
        b.add(Renderable.ID).getData().setVertices(bGeom.getVertices())
                                      .setLocalBounds(bGeom.getBounds())
                                      .setIndices(bGeom.getPolygonType(), bGeom.getIndices(), bGeom.getIndexOffset(), bGeom.getIndexCount());
        b.add(DiffuseColor.ID).getData().setColor(new ColorRGB(.5, 1, .5));
        b.add(Transform.ID);

        b.add(CollisionBody.ID).getData().setShape(bPhys)
                                         .setTransform(new Matrix4(1, 0, 0, 0,
                                                                   0, 1, 0, -5,
                                                                   0, 0, 1, 0,
                                                                   0, 0, 0, 1));
//                                         .setTransform(new Matrix4(1, 0, 0, 0,
//                                                                   0, 1, 0, -50,
//                                                                   0, 0, 1, 0,
//                                                                   0, 0, 0, 1));
        
        Entity c = system.addEntity();
        c.add(Renderable.ID).getData().setVertices(new VertexAttribute(new VertexBufferObject(new BufferData(new float[6]), StorageMode.IN_MEMORY), 3))
                                      .setArrayIndices(PolygonType.LINES, 0, 2);
        c.add(DiffuseColor.ID);
        
        final Setup setup = new Setup(a, b, c);
        
        // configure input handling
        
        // rotate about x axis
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                double angle = (next.getTimestamp() - prev.getTimestamp()) / 1000.0 * ANGLE_RATE;
                Quat4 rotation = new Quat4().setAxisAngle(new Vector3(1, 0, 0), angle);
                Matrix4 t = new Matrix4().setIdentity().setUpper(new Matrix3().set(rotation));
                setup.transformActive(t, false);
            }
        }, new KeyHeldCondition(KeyCode.W));
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                double angle = (next.getTimestamp() - prev.getTimestamp()) / 1000.0 * ANGLE_RATE;
                Quat4 rotation = new Quat4().setAxisAngle(new Vector3(1, 0, 0), -angle);
                Matrix4 t = new Matrix4().setIdentity().setUpper(new Matrix3().set(rotation));
                setup.transformActive(t, false);
            }
        }, new KeyHeldCondition(KeyCode.S));
        
        // rotate about y axis
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                double angle = (next.getTimestamp() - prev.getTimestamp()) / 1000.0 * ANGLE_RATE;
                Quat4 rotation = new Quat4().setAxisAngle(new Vector3(0, 1, 0), angle);
                Matrix4 t = new Matrix4().setIdentity().setUpper(new Matrix3().set(rotation));
                setup.transformActive(t, false);
            }
        }, new KeyHeldCondition(KeyCode.A));
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                double angle = (next.getTimestamp() - prev.getTimestamp()) / 1000.0 * ANGLE_RATE;
                Quat4 rotation = new Quat4().setAxisAngle(new Vector3(0, 1, 0), -angle);
                Matrix4 t = new Matrix4().setIdentity().setUpper(new Matrix3().set(rotation));
                setup.transformActive(t, false);
            }
        }, new KeyHeldCondition(KeyCode.D));
        
        // rotate about z
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                double angle = (next.getTimestamp() - prev.getTimestamp()) / 1000.0 * ANGLE_RATE;
                Quat4 rotation = new Quat4().setAxisAngle(new Vector3(0, 0, 1), angle);
                Matrix4 t = new Matrix4().setIdentity().setUpper(new Matrix3().set(rotation));
                setup.transformActive(t, false);
            }
        }, new KeyHeldCondition(KeyCode.E));
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                double angle = (next.getTimestamp() - prev.getTimestamp()) / 1000.0 * ANGLE_RATE;
                Quat4 rotation = new Quat4().setAxisAngle(new Vector3(0, 0, 1), -angle);
                Matrix4 t = new Matrix4().setIdentity().setUpper(new Matrix3().set(rotation));
                setup.transformActive(t, false);
            }
        }, new KeyHeldCondition(KeyCode.Q));
        
        // translate x axis
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                double delta = (next.getTimestamp() - prev.getTimestamp()) / 1000.0 * TRANSLATE_RATE;
                Matrix4 t = new Matrix4().setIdentity();
                t.setCol(3, new Vector4(delta, 0, 0, 1));
                setup.transformActive(t, true);
            }
        }, new KeyHeldCondition(KeyCode.R));
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                double delta = (next.getTimestamp() - prev.getTimestamp()) / 1000.0 * TRANSLATE_RATE;
                Matrix4 t = new Matrix4().setIdentity();
                t.setCol(3, new Vector4(-delta, 0, 0, 1));
                setup.transformActive(t, true);
            }
        }, new KeyHeldCondition(KeyCode.T));
        
        // translate y axis
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                double delta = (next.getTimestamp() - prev.getTimestamp()) / 1000.0 * TRANSLATE_RATE;
                Matrix4 t = new Matrix4().setIdentity();
                t.setCol(3, new Vector4(0, delta, 0, 1));
                setup.transformActive(t, true);
            }
        }, new KeyHeldCondition(KeyCode.F));
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                double delta = (next.getTimestamp() - prev.getTimestamp()) / 1000.0 * TRANSLATE_RATE;
                Matrix4 t = new Matrix4().setIdentity();
                t.setCol(3, new Vector4(0, -delta, 0, 1));
                setup.transformActive(t, true);
            }
        }, new KeyHeldCondition(KeyCode.G));
        
        // translate z axis
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                double delta = (next.getTimestamp() - prev.getTimestamp()) / 1000.0 * TRANSLATE_RATE;
                Matrix4 t = new Matrix4().setIdentity();
                t.setCol(3, new Vector4(0, 0, delta, 1));
                setup.transformActive(t, true);
            }
        }, new KeyHeldCondition(KeyCode.V));
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                double delta = (next.getTimestamp() - prev.getTimestamp()) / 1000.0 * TRANSLATE_RATE;
                Matrix4 t = new Matrix4().setIdentity();
                t.setCol(3, new Vector4(0, 0, -delta, 1));
                setup.transformActive(t, true);
            }
        }, new KeyHeldCondition(KeyCode.B));
        
        // toggle active
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                setup.toggleActive();
            }
        }, new KeyPressedCondition(KeyCode.SPACE));
        
        // toggle algorithm
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                setup.toggleAlgorithm();
            }
        }, new KeyPressedCondition(KeyCode.C));
        
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                framework.destroy();
            }
        }, new KeyPressedCondition(KeyCode.ESCAPE));
        
        // main loop
        setup.updateCollision();
        while(!framework.isDestroyed()) {
            io.process();
            system.getControllerManager().process(1 / 60.0);
            try {
                Thread.sleep(10);
            } catch(InterruptedException e) {
                
            }
        }
    }
    
    private static OnscreenSurface buildSurface(Framework framework, EntitySystem system) {
        OnscreenSurfaceOptions options = new OnscreenSurfaceOptions().setWidth(500)
                                                                     .setHeight(500)
                                                                     .setMultiSampling(MultiSampling.FOUR_X);
        OnscreenSurface surface = framework.createSurface(options);
        surface.setVSyncEnabled(true);

        // camera
        Entity camera = system.addEntity();
        camera.add(Camera.ID).getData().setSurface(surface)
                                       .setZDistances(1.0, 6 * BOUNDS);
        camera.add(Transform.ID).getData().setMatrix(new Matrix4(1, 0, 0, 0, 
                                                                 0, 1, 0, 0,
                                                                 0, 0, -1,  1.5 * BOUNDS,
                                                                 0, 0, 0, 1));
        return surface;
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
    
    private static class Setup {
        private final Entity a;
        private final Entity b;
        
        private final Entity collision;
        
        private Entity active;
        private CollisionAlgorithm<ConvexShape, ConvexShape> algo;
        
        public Setup(Entity a, Entity b, Entity collision) {
            this.a = a;
            this.b = b;
            this.collision = collision;
            
            active = a;
            algo = new GjkEpaCollisionAlgorithm();
            collision.get(Renderable.ID, true).setEnabled(false);
        }
        
        public void transformActive(@Const Matrix4 transform, boolean inFront) {
            CollisionBody active = this.active.get(CollisionBody.ID).getData();
            if (inFront)
                active.setTransform(active.getTransform().mul(transform, active.getTransform()));
            else
                active.setTransform(active.getTransform().mul(active.getTransform(), transform));
            updateCollision();
        }
        
        public void updateCollision() {
            CollisionBody a = this.a.get(CollisionBody.ID).getData();
            CollisionBody b = this.b.get(CollisionBody.ID).getData();
            
            ClosestPair pair = algo.getClosestPair((ConvexShape) a.getShape(), a.getTransform(), (ConvexShape) b.getShape(), b.getTransform());
            
            if (pair == null) {
                // disable rendering
                collision.get(Renderable.ID, true).setEnabled(false);
                System.err.println("No collision");
            } else {
                // enable, and update geometry
                Renderable r = collision.get(Renderable.ID, true).getData();
                r.setEnabled(true);
                
                float[] vs = r.getVertices().getData().getData().getArray();
                pair.getClosestPointOnA().get(vs, 0);
                pair.getClosestPointOnB().get(vs, 3);
                r.getVertices().getData().markDirty(0, 6);
                
                r.setLocalBounds(new AxisAlignedBox(vs, 0, 0, 2));
                
                if (pair.isIntersecting()) {
                    // line is red
                    collision.get(DiffuseColor.ID).getData().setColor(new ColorRGB(1, 0, 0));
                } else {
                    // line is green
                    collision.get(DiffuseColor.ID).getData().setColor(new ColorRGB(0, 1, 0));
                }
                System.out.println(pair);
            }
        }
        
        public void toggleAlgorithm() {
            if (algo instanceof GjkEpaCollisionAlgorithm) {
                algo = new GjkEpaCollisionAlgorithm2();
            } else if (algo instanceof GjkEpaCollisionAlgorithm2) {
                algo = new GjkEpaCollisionAlgorithm();
            }
            System.out.println("Changed algorithm to " + algo.getClass());
            updateCollision();
        }
        
        public void toggleActive() {
            if (active == a)
                active = b;
            else
                active = a;
        }
    }
}
