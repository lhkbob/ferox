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
package com.ferox.renderer.impl.jogl;

import com.ferox.entity2.Entity;
import com.ferox.entity2.EntitySystem;
import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.logic.InputState;
import com.ferox.input.logic.KeyTypedCondition;
import com.ferox.input.logic.Trigger;
import com.ferox.math.Color3f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Frustum;
import com.ferox.math.bounds.Octree;
import com.ferox.math.bounds.Octree.Strategy;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.OnscreenSurfaceOptions.AntiAliasMode;
import com.ferox.renderer.OnscreenSurfaceOptions.PixelFormat;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.ThreadQueueManager;
import com.ferox.renderer.pass.FrameStatistics;
import com.ferox.scene.Billboarded.Axis;
import com.ferox.scene.controller.ffp.FixedFunctionRenderController;
import com.ferox.scene.controller.ffp.ShadowMapFrustumController;
import com.ferox.util.geom.*;
import com.ferox.util.geom.Geometry.CompileType;
import com.ferox.util.geom.text.CharacterSet;
import com.ferox.util.geom.text.Text;
import com.ferox.util.geom.text.TextRenderPass;
import com.ferox.util.input.FreeLookCameraInputManager;
import com.ferox.util.texture.loader.TextureLoader;

import java.awt.*;
import java.io.File;

public class FixedFunctionSceneCompositorTest {
    private static final CompileType COMPILE_TYPE = CompileType.RESIDENT_STATIC;
    private static final int BOUNDS = 70;
    private static final int NUM_SHAPES = 10000;

    private static final CharacterSet DEFAULT_CHARSET = new CharacterSet(
            Font.decode("FranklinGothic-Medium 36"), true, true);

    static {
        //        System.out.println("Available fonts:");
        //        for (Font f: GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
        //            System.out.println("\t" + f.getFontName());
        //        }
        System.out.println("Chosen font: " + DEFAULT_CHARSET.getFont().getFontName());
        System.out.println("Character set dimensions: " + DEFAULT_CHARSET.getTexture().getWidth() +
                           " " + DEFAULT_CHARSET.getTexture().getHeight());
    }

    public static void main(String[] args) throws Exception {
        final Framework framework = new JoglFramework(false, false, true);
        System.out.println("OpenGL: " + framework.getCapabilities().getVendor() + " " +
                           framework.getCapabilities().getVersion());
        ThreadQueueManager organizer = new ThreadQueueManager(framework);

        EntitySystem system = new EntitySystem();
        SpatialHierarchy<Entity> sh = new Octree<Entity>(Strategy.STATIC, BOUNDS, 3);

        AttachmentController c1 = new AttachmentController(system);
        BillboardController c2 = new BillboardController(system);
        SceneController c3 = new SceneController(system, sh);
        LightUpdateController c4 = new LightUpdateController(system);
        ViewNodeController c5 = new ViewNodeController(system, sh);

        // FIXME: make this sizing/placement of shadowmap better and more intuitive
        ShadowMapFrustumController c6 = new ShadowMapFrustumController(system, sh, .075f, 1024);
        FixedFunctionRenderController c7 = new FixedFunctionRenderController(system, organizer, 2048);

        TextRenderPass textPass = new TextRenderPass();
        Text stats = new Text(DEFAULT_CHARSET, CompileType.NONE);
        stats.setScale(.5f);
        textPass.setTextPosition(stats, new Vector3f(0f, 0f, 0f));

        OnscreenSurface surface = buildSurface(framework, system);
        buildScene(system);

        // scene element controlling the viewnode
        Entity camera = system.iterator(Component.getComponentId(Camera.class)).next();
        FreeLookCameraInputManager ioManager = new FreeLookCameraInputManager(surface, camera);
        ioManager.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                framework.destroy();
                System.exit(0);
            }
        }, new KeyTypedCondition(KeyCode.ESCAPE));
        organizer.setSurfaceGroup(surface, "ffp_sct");

        try {
            Runtime r = Runtime.getRuntime();

            long start = System.nanoTime();
            long renderTime = 0;
            int framesCompleted = 0;

            int numFramesPerUpdate = 10;
            while (true) {
                if (surface.isDestroyed()) {
                    break;
                }

                ioManager.process();

                c1.execute();
                c2.execute();
                c3.execute();
                c4.execute();
                c5.execute();
                c6.execute();
                c7.execute();
                organizer.queue(surface, textPass);

                // begin rendering the frame
                organizer.flush("ffp_sct");
                FrameStatistics frameStats = framework.renderAndWait();
                renderTime += frameStats.getRenderTime();
                framesCompleted++;

                if (frameStats.getRenderTime() / 1e6f > 100) {
                    System.out.println("Exceptionally slow frame: " + (frameStats.getRenderTime() / 1e6f));
                }

                if (framesCompleted >= numFramesPerUpdate) {
                    // update statistics
                    long now = System.nanoTime();
                    float fps = numFramesPerUpdate * 1e9f / (now - start);
                    int timeInRender = (int) (renderTime / (1e6f * numFramesPerUpdate));
                    int timeInProcess = (int) ((now - start) / (1e6f * numFramesPerUpdate)) - timeInRender;

                    stats.setText(
                            String.format("FPS: %.2f (%d + %d)\nPolys: %d\nMem: %.2f", fps, timeInProcess,
                                          timeInRender, frameStats.getPolygonCount(),
                                          (r.totalMemory() - r.freeMemory()) / (1024f * 1024f)));

                    // reset counters
                    start = now;
                    framesCompleted = 0;
                    renderTime = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        framework.destroy();
    }

    private static OnscreenSurface buildSurface(Framework framework, EntitySystem system) {
        OnscreenSurfaceOptions options = new OnscreenSurfaceOptions().setPixelFormat(PixelFormat.RGB_24BIT)
                                                                     .setAntiAliasMode(AntiAliasMode.EIGHT_X)
                                                                     .setWidth(800).setHeight(600);
        OnscreenSurface surface = framework.createSurface(options);
        surface.setClearColor(new Color3f(.5f, .5f, .5f, 1f));
        //        surface.setVSyncEnabled(true);
        surface.setTitle(FixedFunctionSceneCompositorTest.class.getSimpleName());

        // camera
        Camera vn = new Camera(surface, 60f, 1f, 3 * BOUNDS);
        Transform el = new Transform();
        el.setTranslation(new Vector3f(0f, 0f, -1f * BOUNDS));

        system.add(new Entity(el, vn));
        return surface;
    }

    private static void buildScene(EntitySystem scene) throws Exception {
        // shapes
        PrimitiveGeometry geom = new Box(2f, COMPILE_TYPE);
        PrimitiveGeometry geom2 = new Sphere(4f, 32, COMPILE_TYPE);
        PrimitiveGeometry geom3 = new Teapot(2f, COMPILE_TYPE);

        AxisAlignedBox bounds1 = new AxisAlignedBox(geom.getVertices().getData());
        AxisAlignedBox bounds2 = new AxisAlignedBox(geom2.getVertices().getData());
        AxisAlignedBox bounds3 = new AxisAlignedBox(geom3.getVertices().getData());

        Renderable toRender = new Renderable();

        Shape shape1 = new Shape(geom);
        Shape shape2 = new Shape(geom2);
        Shape shape3 = new Shape(geom3);

        ShadowCaster sc = new ShadowCaster();
        ShadowReceiver sr = new ShadowReceiver();

        TexturedMaterial texture = new TexturedMaterial(TextureLoader.readTexture(new File("ferox-gl.tga")));
        BlinnPhongMaterial material = new BlinnPhongMaterial(new Color3f(1f, 1f, 1f, .4f),
                                                             new Color3f(.2f, 0f, .1f));
        Transparent trans = new Transparent();

        // grab camera location
        Frustum cam = scene.iterator(Component.getComponentId(Camera.class)).next()
                           .get(Component.getComponentId(Camera.class)).getFrustum();

        for (int i = 0; i < NUM_SHAPES; i++) {
            float x = (float) (Math.random() * BOUNDS - BOUNDS / 2);
            float y = (float) (Math.random() * BOUNDS - BOUNDS / 2);
            float z = (float) (Math.random() * BOUNDS - BOUNDS / 2);

            int choice = 1;//(int) (Math.random() * 6 + 1);

            Transform element = new Transform();
            element.setTranslation(new Vector3f(x, y, z));

            Entity e = new Entity(element, toRender, material, sc, sr);
            if (Math.random() > 1f) {
                Billboarded b = new Billboarded();
                b.setConstraint(Axis.Z, cam.getDirection(), false, true);
                b.setConstraint(Axis.Y, cam.getUp());

                Text text = new Text(DEFAULT_CHARSET, "Hello, World!", COMPILE_TYPE);
                text.setScale(.15f);

                Entity t = new Entity(new Transform(), new Attached(e,
                                                                    new Matrix4f(1f, 0f, 0f, 0f, 0f, 1f, 0f,
                                                                                 text.getTextHeight(), 0f, 0f,
                                                                                 1f, 0f, 0f, 0f, 0f, 1f)),
                                      new Renderable(DrawStyle.SOLID, DrawStyle.SOLID),
                                      new SolidMaterial(new Color3f(1f, 1f, 1f)),
                                      new TexturedMaterial(text.getCharacterSet().getTexture()),
                                      new Shape(text), b, trans);

                scene.add(t);
                e.add(trans);
            }

            switch (choice) {
            case 1:
            case 2:
            case 3:
                e.add(shape1);
                //                e.add(new Shape(new Box(2f, (USE_VBOS ? CompileType.RESIDENT_STATIC : CompileType.NONE))));
                element.setLocalBounds(bounds1);
                break;
            case 4:
            case 5:
                e.add(shape2);
                //                e.add(new Shape(new Sphere(4f, 32, (USE_VBOS ? CompileType.RESIDENT_STATIC : CompileType.NONE))));
                element.setLocalBounds(bounds2);
                break;
            case 6:
                e.add(shape3);
                //                e.add(new Shape(new Teapot(2f, (USE_VBOS ? CompileType.RESIDENT_STATIC : CompileType.NONE))));
                element.setLocalBounds(bounds3);
                break;
            }

            scene.add(e);
        }

        // some walls
        Rectangle backWall = new Rectangle(new Vector3f(0f, 1f, 0f), new Vector3f(0f, 0f, -1f), -BOUNDS,
                                           BOUNDS, -BOUNDS, BOUNDS);
        Transform pos = new Transform();
        pos.setTranslation(new Vector3f(BOUNDS, 0f, 0f));
        pos.setLocalBounds(new AxisAlignedBox(backWall.getVertices().getData()));
        scene.add(new Entity(pos, new Shape(backWall), material, texture,
                             new Renderable(DrawStyle.SOLID, DrawStyle.SOLID), sr));

        Rectangle bottomWall = new Rectangle(new Vector3f(1f, 0f, 0f), new Vector3f(0f, 0f, -1f), -BOUNDS,
                                             BOUNDS, -BOUNDS, BOUNDS);
        pos = new Transform();
        pos.setTranslation(new Vector3f(0f, -BOUNDS, 0f));
        pos.setLocalBounds(new AxisAlignedBox(bottomWall.getVertices().getData()));
        scene.add(new Entity(pos, new Shape(bottomWall), material, texture,
                             new Renderable(DrawStyle.SOLID, DrawStyle.SOLID), sr));

        // ambient light
        scene.add(new Entity(new AmbientLight(new Color3f(.2f, .2f, .2f, 1f))));

        // a point light
        scene.add(
                new Entity(new SpotLight(new Color3f(.5f, .8f, 0f), new Vector3f(BOUNDS / 2f, 0f, BOUNDS))));

        // a directed light, which casts shadows
        scene.add(new Entity(
                new DirectionLight(new Color3f(1f, 1f, 1f), new Vector3f(1f, -1f, -1f).normalize()), sc));
    }
}
