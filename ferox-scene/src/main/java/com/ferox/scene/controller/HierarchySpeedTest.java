package com.ferox.scene.controller;

import java.util.HashMap;
import java.util.Map;

import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.QuadTree;
import com.ferox.renderer.Framework;
import com.ferox.renderer.Surface;
import com.ferox.scene.Camera;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.ferox.util.Bag;
import com.ferox.util.geom.Box;
import com.lhkbob.entreri.Controller;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.Result;
import com.lhkbob.entreri.SimpleController;
import com.lhkbob.entreri.TypeId;

public class HierarchySpeedTest {
    public static void main(String[] args) {
        EntitySystem system = new EntitySystem();
        Box b = new Box(2f);
        
        Entity camera = system.addEntity();
        camera.add(Transform.ID).getData().setMatrix(new Matrix4(-1, 0, 0, 0, 
                                                                 0, 1, 0, 0,
                                                                 0, 0, -1, 700,
                                                                 0, 0, 0, 1));
        camera.add(Camera.ID).getData().setSurface(new Surface() {
            @Override
            public boolean isDestroyed() {
                return false;
            }
            
            @Override
            public int getWidth() {
                return 800;
            }
            
            @Override
            public int getHeight() {
                return 800;
            }
            
            @Override
            public Framework getFramework() {
                return null;
            }
            
            @Override
            public void destroy() {
                // do nothing
            }
        }).setZDistances(0.1, 1200).setFieldOfView(75);
        
        for (int i = 0; i < 11111; i++) {
            Entity e = system.addEntity();
            e.add(Renderable.ID).getData().setVertices(b.getVertices()).setLocalBounds(b.getBounds());
            e.add(Transform.ID).getData().setMatrix(new Matrix4(1, 0, 0, Math.random() * 200 - 100, 
                                                                0, 1, 0, Math.random() * 2 - 1, 
                                                                0, 0, 1, Math.random() * 200 - 100,
                                                                0, 0, 0, 1));
        }
        
        Controller boundsUpdate = new WorldBoundsController();
        Controller frustumUpdate = new CameraController();
        Controller indexBuilder = new SpatialIndexController(new QuadTree<Entity>(new AxisAlignedBox(new Vector3(-150, -10, -150), new Vector3(150, 10, 150)), 6));
        Controller pvsComputer = new VisibilityController();
        Controller render = new RenderController();
        
        Map<String, Controller> controllers = new HashMap<String, Controller>();
        controllers.put("bounds", boundsUpdate);
        controllers.put("frustum", frustumUpdate);
        controllers.put("index-build", indexBuilder);
        controllers.put("pvs", pvsComputer);
        controllers.put("render", render);
        Map<String, Long> times = new HashMap<String, Long>();
        
        system.getControllerManager().addController(boundsUpdate);
        system.getControllerManager().addController(frustumUpdate);
        system.getControllerManager().addController(indexBuilder);
        system.getControllerManager().addController(pvsComputer);
        system.getControllerManager().addController(render);

        
        int numRuns = 1000;
        for (int i = 0; i < numRuns; i++) {
            system.getControllerManager().process();
        }
        
        long now = System.currentTimeMillis();
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
        
        long total = System.currentTimeMillis() - now;
        
        System.out.println("***** TIMING *****");
        print("total", total, numRuns);
        for (String name: controllers.keySet()) {
            print(name, times.get(name) / 1000000, numRuns);
        }
        System.out.println();
        
        System.out.println("***** MEMORY *****");
        Runtime r = Runtime.getRuntime();
        printMemory("total", r.totalMemory());
        printMemory("used", r.totalMemory() - r.freeMemory());
        for (TypeId<?> t: system.getTypes()) {
            printMemory(t.toString(), system.estimateMemory(t));
        }
    }
    
    private static void printMemory(String label, long bytes) {
        System.out.printf("%s: %.2f MB\n", label, (bytes / (1024.0 * 1024.0)));
    }
    
    private static void print(String label, long total, int numRuns) {
        float avg = total / (float) numRuns;
        System.out.println(label + " - total time: " + total + ", avg: " + avg);
    }
    
    private static class RenderController extends SimpleController {
        boolean firstProcess = true;
        Bag<Entity> pvs;
        
        @Override
        public void process(double dt) {
            Renderable r = getEntitySystem().createDataInstance(Renderable.ID);
            Transform t = getEntitySystem().createDataInstance(Transform.ID);
           
            int count = 0;
            for (Entity e: pvs) {
                if (e.get(r) && e.get(t)) {
                    count++;
                }
            }
            
            if (firstProcess) {
                System.out.println("count: " + count);
                firstProcess = false;
            }
        }
        @Override
        public void report(Result result) {
            if (result instanceof PVSResult)
                this.pvs = ((PVSResult) result).getPotentiallyVisibleSet();
        }
    }
}
