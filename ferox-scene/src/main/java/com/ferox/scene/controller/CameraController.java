package com.ferox.scene.controller;

import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.Frustum;
import com.ferox.scene.Camera;
import com.ferox.scene.Transform;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.SimpleController;

/**
 * CameraController is a controller that synchronizes a {@link Camera}'s Frustum
 * location and orientation with an attached {@link Transform}. When run, all
 * entities with a Camera and Transform will have the Camera's Frustum's
 * orientation equal that stored in the transform.
 * 
 * @author Michael Ludwig
 */
public class CameraController extends SimpleController {
    private Camera camera;
    private Transform transform;
    
    private ComponentIterator it;
    
    @Override
    public void process(double dt) {
        it.reset();
        while(it.next()) {
            double aspect = camera.getSurface().getHeight() / (double) camera.getSurface().getWidth();
            Frustum f = new Frustum(camera.getFieldOfView(), aspect, 
                                    camera.getNearZDistance(), camera.getFarZDistance());
            
            if (transform.isEnabled()) {
                Matrix4 m = transform.getMatrix();
                f.setOrientation(new Vector3(m.m03, m.m13, m.m23), 
                                 new Vector3(m.m02, m.m12, m.m22), 
                                 new Vector3(m.m01, m.m11, m.m21));
            }
            
            getEntitySystem().getControllerManager().report(new FrustumResult(camera.getComponent(), f));
        }
    }
    
    @Override
    public void init(EntitySystem system) {
        super.init(system);
        camera = system.createDataInstance(Camera.ID);
        transform = system.createDataInstance(Transform.ID);
        
        it = new ComponentIterator(system).addRequired(camera)
                                          .addOptional(transform);
    }
    
    @Override
    public void destroy() {
        camera = null;
        transform = null;
        it = null;
        super.destroy();
    }
}
