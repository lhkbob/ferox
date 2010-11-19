package com.ferox.physics.entity;

import java.awt.Font;
import java.io.File;

import com.ferox.entity.Component;
import com.ferox.entity.Entity;
import com.ferox.entity.EntitySystem;
import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.logic.InputState;
import com.ferox.input.logic.KeyTypedCondition;
import com.ferox.input.logic.Trigger;
import com.ferox.math.Color4f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Octree;
import com.ferox.math.bounds.Octree.Strategy;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.SpatialHierarchyCollisionManager;
import com.ferox.physics.dynamics.DiscretePhysicsWorld;
import com.ferox.physics.dynamics.PhysicsWorldConfiguration;
import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.OnscreenSurfaceOptions.AntiAliasMode;
import com.ferox.renderer.OnscreenSurfaceOptions.PixelFormat;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.ThreadQueueManager;
import com.ferox.renderer.impl.jogl.FixedFunctionJoglFramework;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.BlinnPhongLightingModel;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.SceneElement;
import com.ferox.scene.ShadowCaster;
import com.ferox.scene.ShadowReceiver;
import com.ferox.scene.Shape;
import com.ferox.scene.SpotLight;
import com.ferox.scene.TexturedMaterial;
import com.ferox.scene.ViewNode;
import com.ferox.scene.controller.AttachmentController;
import com.ferox.scene.controller.BillboardController;
import com.ferox.scene.controller.LightUpdateController;
import com.ferox.scene.controller.SceneController;
import com.ferox.scene.controller.ViewNodeController;
import com.ferox.scene.controller.ffp.FixedFunctionRenderController;
import com.ferox.scene.controller.ffp.ShadowMapFrustumController;
import com.ferox.util.geom.Box;
import com.ferox.util.geom.PrimitiveGeometry;
import com.ferox.util.geom.Rectangle;
import com.ferox.util.geom.Sphere;
import com.ferox.util.geom.text.CharacterSet;
import com.ferox.util.geom.text.Text;
import com.ferox.util.geom.text.TextRenderPass;
import com.ferox.util.input.FreeLookCameraInputManager;
import com.ferox.util.texture.loader.TextureLoader;

public class PhysicsTest {
    private static final CompileType COMPILE_TYPE = CompileType.RESIDENT_STATIC;
    private static final int BOUNDS = 50;
   
    private static final int NUM_X = 10;
    private static final int NUM_Y = 10;
    private static final int NUM_Z = 10;
    private static final float SCALE_X = 2.0f;
    private static final float SCALE_Y = 2.0f;
    private static final float SCALE_Z = 2.0f;
    
    private static final float MARGIN = .05f;

    private static final float PERCENT = 1f;
    private static final float RANDOM = 0f;
    
    private static final float START_POS_X = -5;
    private static final float START_POS_Y = 1 + 2 * MARGIN;
    private static final float START_POS_Z = -3;
    
    private static final CharacterSet DEFAULT_CHARSET = new CharacterSet(Font.decode("FranklinGothic-Medium 24"), true, true);
    
    static {
        System.out.println("Chosen font: " + DEFAULT_CHARSET.getFont().getFontName());
        System.out.println("Character set dimensions: " + DEFAULT_CHARSET.getTexture().getWidth() + " " + DEFAULT_CHARSET.getTexture().getHeight());
    }
    
    public static void main(String[] args) throws Exception {
        final Framework framework = new FixedFunctionJoglFramework(false);
        System.out.println("OpenGL: " + framework.getCapabilities().getVendor() + " " + framework.getCapabilities().getVersion());
        ThreadQueueManager organizer = new ThreadQueueManager(framework);
        
        final EntitySystem system = new EntitySystem();
        SpatialHierarchy<Entity> sh = new Octree<Entity>(Strategy.STATIC, BOUNDS, 3);
        
        PhysicsWorldConfiguration config = new PhysicsWorldConfiguration().setCollisionManager(new SpatialHierarchyCollisionManager(new Octree<Collidable>(Strategy.STATIC, BOUNDS, 3)));
        
        AttachmentController c1 = new AttachmentController(system);
        BillboardController c2 = new BillboardController(system);
        PhysicsController c3 = new PhysicsController(system, new DiscretePhysicsWorld(config));
        SceneController c4 = new SceneController(system, sh);
        LightUpdateController c5 = new LightUpdateController(system);
        ViewNodeController c6 = new ViewNodeController(system, sh);
        
        // FIXME: make this sizing/placement of shadowmap better and more intuitive
        ShadowMapFrustumController c7 = new ShadowMapFrustumController(system, sh, .075f, 1024);
        FixedFunctionRenderController c8 = new FixedFunctionRenderController(system, organizer, 2048);
        
        TextRenderPass textPass = new TextRenderPass();
        Text stats = new Text(DEFAULT_CHARSET, CompileType.NONE);
        stats.setScale(.5f);
        textPass.setTextPosition(stats, new Vector3f(0f, 0f, 0f));
        
        OnscreenSurface surface = buildSurface(framework, system);
        buildScene(system);
        
        // scene element controlling the viewnode
        final Entity camera = system.iterator(Component.getComponentId(ViewNode.class)).next();
        FreeLookCameraInputManager ioManager = new FreeLookCameraInputManager(surface, camera);
        ioManager.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                framework.destroy();
                System.exit(0);
            }}, new KeyTypedCondition(KeyCode.ESCAPE));
        ioManager.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                SceneElement se = new SceneElement();
                Transform camT = camera.get(Component.getComponentId(SceneElement.class)).getTransform();
                se.getTransform().set(camT);
                camT.getRow(2).getAsVector3f().scaleAdd(30f, se.getTransform().getTranslation(), se.getTransform().getTranslation());
                
                Renderable toRender = new Renderable();
                ShadowCaster sc = new ShadowCaster();
                ShadowReceiver sr = new ShadowReceiver();
                
                BlinnPhongLightingModel material = new BlinnPhongLightingModel(new Color4f(.5f, .5f, .5f, .4f), new Color4f(.2f, 0f, .1f));

                
                Entity e = new Entity(se, toRender, sc, sr, material, new Shape(new Box(4f + MARGIN)));
                e.add(new PhysicsBody(new com.ferox.physics.collision.shape.Box(4f, 4f, 4f), 10f));
                system.add(e);
            }}, new KeyTypedCondition(KeyCode.B));
        organizer.setSurfaceGroup(surface, "ffp_sct");
        
        try {
            Runtime r = Runtime.getRuntime();
            
            long start = System.nanoTime();
            long renderTime = 0;
            int framesCompleted = 0;
            int i = 0;
            int numFramesPerUpdate = 4;
            while(true) {
                if (surface.isDestroyed())
                    break;
                
//                System.out.println("step " + i++);
                ioManager.process();
                
                c1.process();
                c2.process();
                c3.process();
                c4.process();
                c5.process();
                c6.process();
                c7.process();
                c8.process();
                organizer.queue(surface, textPass);
                
                // begin rendering the frame
                organizer.flush("ffp_sct");
                FrameStatistics frameStats = framework.renderAndWait();
                renderTime += frameStats.getRenderTime();
                framesCompleted++;
//                Thread.sleep(100);
                
                if (frameStats.getRenderTime() / 1e6f > 100)
                    System.out.println("Exceptionally slow frame: " + (frameStats.getRenderTime() / 1e6f));
                
                if (framesCompleted >= numFramesPerUpdate) {
                    // update statistics
                    long now = System.nanoTime();
                    float fps = numFramesPerUpdate * 1e9f / (now - start);
                    int timeInRender = (int) (renderTime / (1e6f * numFramesPerUpdate));
                    int timeInProcess = (int) ((now - start) / (1e6f * numFramesPerUpdate)) - timeInRender;
                    
                    stats.setText(String.format("FPS: %.2f (%d + %d)\nPolys: %d\nMem: %.2f\nPhysics: %.2f", 
                                                fps, timeInProcess, timeInRender, frameStats.getPolygonCount(),
                                                (r.totalMemory() - r.freeMemory()) / (1024f * 1024f),
                                                c3.getAverageNanos() / 1e6f));
                    
                    // reset counters
                    start = now;
                    framesCompleted = 0;
                    renderTime = 0;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        framework.destroy();
    }
    
    private static OnscreenSurface buildSurface(Framework framework, EntitySystem system) {
        OnscreenSurfaceOptions options = new OnscreenSurfaceOptions().setPixelFormat(PixelFormat.RGB_24BIT)
                                                                     .setAntiAliasMode(AntiAliasMode.EIGHT_X)
                                                                     .setWidth(800)
                                                                     .setHeight(600);
        OnscreenSurface surface = framework.createSurface(options);
        surface.setClearColor(new Color4f(.5f, .5f, .5f, 1f));
        surface.setVSyncEnabled(true);
        surface.setTitle(PhysicsTest.class.getSimpleName());

        // camera
        ViewNode vn = new ViewNode(surface, 60f, 1f, 6 * BOUNDS);
        SceneElement el = new SceneElement();
        el.setTranslation(new Vector3f(0f, BOUNDS / 2f, -1f * BOUNDS));
        
        system.add(new Entity(el, vn));
        return surface;
    }
    
    private static void buildScene(EntitySystem scene) throws Exception {
        // shapes
        PrimitiveGeometry box = new Box(2 + 2 * MARGIN, COMPILE_TYPE);
        AxisAlignedBox boxBounds = new AxisAlignedBox(box.getVertices().getData());
        PrimitiveGeometry sphere = new Sphere(1 + MARGIN, 16, COMPILE_TYPE);
        AxisAlignedBox sphereBounds = new AxisAlignedBox(sphere.getVertices().getData());
        
        Shape boxElem = new Shape(box);
        Shape sphereElem = new Shape(sphere);
        
        com.ferox.physics.collision.shape.Shape boxShape = new com.ferox.physics.collision.shape.Box(2f, 2f, 2f);
        com.ferox.physics.collision.shape.Shape sphereShape = new com.ferox.physics.collision.shape.Sphere(1f);
        boxShape.setMargin(MARGIN);
        sphereShape.setMargin(MARGIN);
        
        Renderable toRender = new Renderable();
        ShadowCaster sc = new ShadowCaster();
        ShadowReceiver sr = new ShadowReceiver();
        
        TexturedMaterial texture = new TexturedMaterial(TextureLoader.readTexture(new File("ferox-gl.tga")));
        BlinnPhongLightingModel material = new BlinnPhongLightingModel(new Color4f(1f, 1f, 1f, .4f), new Color4f(.2f, 0f, .1f));
        
        float startX = START_POS_X - NUM_X / 2;
        float startY = START_POS_Y;
        float startZ = START_POS_Z - NUM_Z / 2;
        
        float randXLim = RANDOM * (SCALE_X - 2f) / 2f;
        float randYLim = RANDOM * (SCALE_Y - 2f) / 2f;
        float randZLim = RANDOM * (SCALE_Z - 2f) / 2f;
        
        for (int z = 0; z < NUM_Z; z++) {
            for (int y = 0; y < NUM_Y; y++) {
                for (int x = 0; x < NUM_X; x++) {
                    com.ferox.physics.collision.shape.Shape physShape;
                    AxisAlignedBox bounds;
                    Shape geomShape;
                    
                    if (Math.random() > PERCENT) {
                        physShape = sphereShape;
                        bounds = sphereBounds;
                        geomShape = sphereElem;
                    } else {
                        physShape = boxShape;
                        bounds = boxBounds;
                        geomShape = boxElem;
                    }
                    
                    SceneElement element = new SceneElement();
                    
                    float rx = (float) (Math.random() * randXLim - randXLim / 2f);
                    float ry = (float) (Math.random() * randYLim - randYLim / 2f);
                    float rz = (float) (Math.random() * randZLim - randZLim / 2f);

                    element.setTranslation(new Vector3f((SCALE_X + 2 * MARGIN) * x + rx + startX, 
                                                        (SCALE_Y + 2 * MARGIN) * y + ry + startY, 
                                                        (SCALE_Z + 2 * MARGIN) * z + rz + startZ));
                    element.setLocalBounds(bounds);
                    Entity e = new Entity(element, toRender, material, sc, sr, geomShape,
                                          new PhysicsBody(physShape, 1f));
                    scene.add(e);
                }
            }
        }
        
        // some walls
        SceneElement pos = new SceneElement();
        Rectangle bottomWall = new Rectangle(new Vector3f(1f, 0f, 0f), new Vector3f(0f, 0f, -1f), -BOUNDS, BOUNDS, -BOUNDS, BOUNDS);
        pos.setTranslation(new Vector3f(0f, 0f, 0f));
        pos.setLocalBounds(new AxisAlignedBox(bottomWall.getVertices().getData()));
        
        scene.add(new Entity(pos, new Shape(bottomWall), material, texture, new Renderable(DrawStyle.SOLID, DrawStyle.SOLID), sr));

        
        // ambient light
        scene.add(new Entity(new AmbientLight(new Color4f(.2f, .2f, .2f, 1f))));
        
        // a point light
        scene.add(new Entity(new SpotLight(new Color4f(.5f, .8f, 0f), 
                                           new Vector3f(BOUNDS / 2f, BOUNDS / 2f, BOUNDS))));
        
        // a directed light, which casts shadows
        scene.add(new Entity(new DirectionLight(new Color4f(1f, 1f, 1f),
                                                new Vector3f(1f, -1f, -1f).normalize()),
                             sc));
    }
}
