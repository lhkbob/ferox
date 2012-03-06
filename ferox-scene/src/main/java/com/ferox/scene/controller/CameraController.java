package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.scene.Camera;
import com.ferox.scene.Transform;
import com.googlecode.entreri.AbstractController;
import com.googlecode.entreri.ControllerManager.Key;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.IndexedComponentMap;
import com.googlecode.entreri.property.AbstractPropertyFactory;
import com.googlecode.entreri.property.FloatProperty;
import com.googlecode.entreri.property.IntProperty;

/**
 * CameraController is a controller that synchronizes a {@link Camera}'s Frustum
 * location and orientation with an attached {@link Transform}. When run, all
 * entities with a Camera and Transform will have the Camera's Frustum's
 * orientation equal that stored in the transform.
 * 
 * @author Michael Ludwig
 */
public class CameraController extends AbstractController {
    // Indexes into the viewport bulk property
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int BOTTOM = 2;
    private static final int TOP = 3;
    
    private static final Key<FloatProperty> SCALE_KEY = new Key<FloatProperty>();
    private static final Key<IntProperty> SIZE_KEY = new Key<IntProperty>();
    private static final Key<IntProperty> SURFACE_KEY = new Key<IntProperty>();
    
    public static long time = 0;
    @Override
    public void process(EntitySystem system, float dt) {
        time -= System.nanoTime();
        
        FloatProperty scale = system.getControllerManager().getData(SCALE_KEY);
        IntProperty size = system.getControllerManager().getData(SIZE_KEY);
        IntProperty surface = system.getControllerManager().getData(SURFACE_KEY);
        
        Iterator<IndexedComponentMap> cameras = system.fastIterator(Camera.ID, Transform.ID);
        while(cameras.hasNext()) {
            IndexedComponentMap map = cameras.next();
            Camera c = map.get(Camera.ID, 0);
            ReadOnlyMatrix4f mat = map.get(Transform.ID, 1).getMatrix();
            
            c.getFrustum().setOrientation(mat.getCol(3).getAsVector3f(), 
                                          mat.getUpperMatrix().getCol(2), 
                                          mat.getUpperMatrix().getCol(1));
            
            int index = c.getIndex();
            float scaleLeft = scale.get(index, LEFT);
            float scaleRight = scale.get(index, RIGHT);
            float scaleBottom = scale.get(index, TOP);
            float scaleTop = scale.get(index, BOTTOM);
            
            boolean viewChanged = size.get(index, LEFT) != c.getViewportLeft() 
                                  || size.get(index, RIGHT) != c.getViewportRight()
                                  || size.get(index, TOP) != c.getViewportTop() 
                                  || size.get(index, BOTTOM) != c.getViewportBottom();
            
            if (scaleLeft < 0 || scaleRight < 0 || scaleBottom < 0 || scaleTop < 0
                || viewChanged) {
                // relative size has changed, or we haven't seen the camera before
                // so compute scale of viewport within surface, and save state
                scale.set(c.getViewportLeft() / (float) c.getSurface().getWidth(), index, LEFT);
                scale.set(c.getViewportRight()  / (float) c.getSurface().getWidth(), index, RIGHT);
                scale.set(c.getViewportBottom() / (float) c.getSurface().getHeight(), index, BOTTOM);
                scale.set(c.getViewportTop() / (float) c.getSurface().getHeight(), index, TOP);
            } else {
                // compute appropriate new size for viewport, from saved scale factor
                // and new surface dimensions
                // - this has better reproduction than estimating scale factor from
                //   surface change ratios and applying to int viewport coordinates
                float left = scaleLeft * c.getSurface().getWidth();
                float right = scaleRight * c.getSurface().getWidth();
                float bottom = scaleBottom * c.getSurface().getHeight();
                float top = scaleTop * c.getSurface().getHeight();
                
                // use floor/ceil to account for minor floating point errors,
                // we expand viewport to prevent gaps between neighboring viewports
                // that would contain dead pixels (would rather have 1/2 pixel
                // overlap).
                c.setViewport((int) Math.floor(left), (int) Math.ceil(right), 
                              (int) Math.floor(bottom), (int) Math.ceil(top));
            }
            
            // these states are saved every time
            surface.set(c.getSurface().getWidth(), index, 0);
            surface.set(c.getSurface().getHeight(), index, 1);
            
            size.set(c.getViewportLeft(), index, LEFT);
            size.set(c.getViewportRight(), index, RIGHT);
            size.set(c.getViewportBottom(), index, BOTTOM);
            size.set(c.getViewportTop(), index, TOP);
        }
        
        time += System.nanoTime();
    }
    
    @Override
    public void addedToSystem(EntitySystem system) {
        FloatProperty scale = system.decorate(Camera.ID, new NegatingFloatFactory());
        IntProperty size = system.decorate(Camera.ID, IntProperty.factory(4));
        IntProperty surface = system.decorate(Camera.ID, IntProperty.factory(2));

        system.getControllerManager().setData(SCALE_KEY, scale);
        system.getControllerManager().setData(SIZE_KEY, size);
        system.getControllerManager().setData(SURFACE_KEY, surface);
    }
    
    @Override
    public void removedFromSystem(EntitySystem system) {
        FloatProperty scale = system.getControllerManager().getData(SCALE_KEY);
        system.getControllerManager().setData(SCALE_KEY, null);
        system.undecorate(Camera.ID, scale);
        
        IntProperty size = system.getControllerManager().getData(SIZE_KEY);
        system.getControllerManager().setData(SIZE_KEY, null);
        system.undecorate(Camera.ID, size);
        
        IntProperty surface = system.getControllerManager().getData(SURFACE_KEY);
        system.getControllerManager().setData(SURFACE_KEY, null);
        system.undecorate(Camera.ID, surface);
    }
    
    private static class NegatingFloatFactory extends AbstractPropertyFactory<FloatProperty> {
        @Override
        public FloatProperty create() {
            return new FloatProperty(4); // left, right, bottom, top
        }
        
        @Override
        public void setValue(FloatProperty p, int index) {
            p.set(-1f, index, 0);
            p.set(-1f, index, 1);
            p.set(-1f, index, 2);
            p.set(-1f, index, 3);
        }
    }
}
