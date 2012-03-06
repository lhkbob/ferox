package com.ferox.scene.controller;

import com.ferox.math.Matrix4f;
import com.ferox.math.bounds.Octree;
import com.ferox.renderer.Framework;
import com.ferox.renderer.Surface;
import com.ferox.scene.Camera;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.ferox.util.geom.Box;
import com.googlecode.entreri.Entity;
import com.googlecode.entreri.EntitySystem;

public class HierarchySpeedTest {
    public static void main(String[] args) {
        EntitySystem system = new EntitySystem();
        Box b = new Box(2f);
        
        Entity camera = system.addEntity();
        camera.add(Transform.ID);
        camera.add(Camera.ID, new Surface() {
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
        });
        
        for (int i = 0; i < 11111; i++) {
            Entity e = system.addEntity();
            e.add(Renderable.ID, b.getVertices()).setLocalBounds(b.getBounds());
            e.add(Transform.ID).setMatrix(new Matrix4f(1f, 0f, 0f, (float) Math.random() * 20 - 10, 
                                                       0f, 1f, 0f, (float) Math.random() * 20 - 10, 
                                                       0f, 0f, 1f, (float) Math.random() * 20 - 10,
                                                       0f, 0f, 0f, 1f));
        }
        
        system.getControllerManager().setData(RenderableController.RENDERABLE_HIERARCHY, 
                                              new Octree<Renderable>(30f, 6));
        
        system.getControllerManager().addController(new CameraController());
        system.getControllerManager().addController(new RenderableController());
        system.getControllerManager().addController(new VisibilityController());
        
        int numRuns = 1000;
        for (int i = 0; i < numRuns; i++) {
            system.getControllerManager().process();
        }
        
        CameraController.time = 0;
        RenderableController.time = 0;
        RenderableController.world = 0;
        RenderableController.update = 0;
        VisibilityController.time = 0;
        
        long now = System.currentTimeMillis();
        for (int i = 0; i < numRuns; i++) {
            system.getControllerManager().process();
        }
        
        long total = System.currentTimeMillis() - now;
        print("total", total, numRuns);
        print("camera", CameraController.time/ 1000000, numRuns);
        print("renderable", RenderableController.time / 1000000, numRuns);
        print("renderable-world", RenderableController.world / 1000000, numRuns);
        print("renderable-update", RenderableController.update / 1000000, numRuns);

        print("visibility", VisibilityController.time / 1000000, numRuns);
    }
    
    private static void print(String label, long total, int numRuns) {
        float avg = total / (float) numRuns;
        System.out.println(label + " - total time: " + total + ", avg: " + avg);
    }
}
