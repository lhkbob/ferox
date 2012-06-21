package com.ferox.scene.controller.ffp;

import java.util.HashMap;
import java.util.Map;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.QuadTree;
import com.ferox.renderer.DisplayMode;
import com.ferox.renderer.DisplayMode.PixelFormat;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.impl.lwjgl.LwjglFramework;
import com.ferox.scene.BlinnPhongMaterial;
import com.ferox.scene.Camera;
import com.ferox.scene.PointLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.ferox.scene.controller.CameraController;
import com.ferox.scene.controller.SpatialIndexController;
import com.ferox.scene.controller.VisibilityController;
import com.ferox.scene.controller.WorldBoundsController;
import com.ferox.scene.controller.light.LightGroupController;
import com.ferox.util.geom.Box;
import com.lhkbob.entreri.Controller;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.TypeId;

public class LWJGLSimpleTest {
    public static void main(String[] args) {
        Framework framework = LwjglFramework.create();
        OnscreenSurface surface = framework.createSurface(new OnscreenSurfaceOptions()
            .setWidth(800)
            .setHeight(600)
//            .setFullscreenMode(new DisplayMode(1440, 900, PixelFormat.RGB_24BIT))
            .setResizable(false));
        surface.setVSyncEnabled(true);
        
        EntitySystem system = new EntitySystem();
        
        Entity camera = system.addEntity();
        camera.add(Transform.ID).getData().setMatrix(new Matrix4(-1, 0, 0, 0, 
                                                                 0, 1, 0, 0,
                                                                 0, 0, -1, 110,
                                                                 0, 0, 0, 1));
        camera.add(Camera.ID).getData().setSurface(surface)
                                       .setZDistances(0.1, 1200)
                                       .setFieldOfView(75);
        
        Box b = new Box(2f);
        for (int i = 0; i < 10000; i++) {
            Entity e = system.addEntity();
            e.add(Renderable.ID).getData().setVertices(b.getVertices())
                                          .setLocalBounds(b.getBounds())
                                          .setArrayIndices(b.getPolygonType(), b.getIndexOffset(), b.getIndexCount());
            e.add(BlinnPhongMaterial.ID).getData().setNormals(b.getNormals());
            e.add(Transform.ID).getData().setMatrix(new Matrix4(1, 0, 0, Math.random() * 200 - 100, 
                                                                0, 1, 0, Math.random() * 200 - 100, 
                                                                0, 0, 1, Math.random() * 200 - 100,
                                                                0, 0, 0, 1));
        }
        Entity light = system.addEntity();
        light.add(PointLight.ID).getData().setFalloffDistance(30f);
        light.add(Transform.ID).getData().setMatrix(new Matrix4(1, 0, 0, Math.random() * 200 - 100, 
                                                                0, 1, 0, Math.random() * 2 - 1, 
                                                                0, 0, 1, Math.random() * 200 - 100,
                                                                0, 0, 0, 1));
        
//        light = system.addEntity();
//        light.add(PointLight.ID).getData().setFalloffDistance(30f);
//        light.add(Transform.ID).getData().setMatrix(new Matrix4(1, 0, 0, Math.random() * 200 - 100, 
//                                                                0, 1, 0, Math.random() * 2 - 1, 
//                                                                0, 0, 1, Math.random() * 200 - 100,
//                                                                0, 0, 0, 1));
        
        Controller boundsUpdate = new WorldBoundsController();
        Controller frustumUpdate = new CameraController();
        Controller indexBuilder = new SpatialIndexController(new QuadTree<Entity>(new AxisAlignedBox(new Vector3(-150, -150, -150), new Vector3(150, 150, 150)), 6));
        Controller pvsComputer = new VisibilityController();
        Controller lights = new LightGroupController();
        Controller render = new FixedFunctionRenderController(framework);
        
        Map<String, Controller> controllers = new HashMap<String, Controller>();
        controllers.put("bounds", boundsUpdate);
        controllers.put("frustum", frustumUpdate);
        controllers.put("index-build", indexBuilder);
        controllers.put("pvs", pvsComputer);
        controllers.put("lights", lights);
        controllers.put("render", render);
        Map<String, Long> times = new HashMap<String, Long>();
        
        system.getControllerManager().addController(boundsUpdate);
        system.getControllerManager().addController(frustumUpdate);
        system.getControllerManager().addController(indexBuilder);
        system.getControllerManager().addController(pvsComputer);
        system.getControllerManager().addController(lights);
        system.getControllerManager().addController(render);

        int numRuns = 1000;
        long now = System.nanoTime();

        try {
            for (int i = 0; i < numRuns; i++) {
                system.getControllerManager().process();
                for (String name: controllers.keySet()) {
                    Long time = times.get(name);
                    if (time == null)
                        time = 0L;
                    time += system.getControllerManager().getExecutionTime(controllers.get(name));
                    times.put(name, time);
                }
            }
        } finally {
            long total = System.nanoTime() - now;
            
            framework.destroy();

            System.out.println("***** TIMING *****");
            print("total", total, numRuns);
            for (String name: controllers.keySet()) {
                print(name, times.get(name), numRuns);
            }
            print("opengl", FixedFunctionRenderController.rendertime, numRuns);
            System.out.println();
            
            System.out.println("***** MEMORY *****");
            Runtime r = Runtime.getRuntime();
            printMemory("total", r.totalMemory());
            printMemory("used", r.totalMemory() - r.freeMemory());
            for (TypeId<?> t: system.getTypes()) {
                printMemory(t.toString(), system.estimateMemory(t));
            }
        }
    }
    
    private static void printMemory(String label, long bytes) {
        System.out.printf("%s: %.2f MB\n", label, (bytes / (1024.0 * 1024.0)));
    }
    
    private static void print(String label, long total, int numRuns) {
        float millis = total / 1e6f;
        float avg = millis / (float) numRuns;
        System.out.printf("%s - total time: %.2f, avg: %.2f\n", label, millis, avg);
    }
}
