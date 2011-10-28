package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.entity2.Controller;
import com.ferox.entity2.EntitySystem;
import com.ferox.entity2.Parallel;
import com.ferox.math.AffineTransform;
import com.ferox.scene.Camera;
import com.ferox.scene.Transform;

/**
 * CameraController is a controller that synchronizes a {@link Camera}'s Frustum
 * location and orientation with an attached {@link Transform}. When run, all
 * entities with a Camera and Transform will have the Camera's Frustum's
 * orientation equal that stored in the transform.
 * 
 * @author Michael Ludwig
 */
@Parallel(reads=Transform.class, writes=Camera.class)
public class CameraController extends Controller {
    public CameraController(EntitySystem system) {
        super(system);
    }

    @Override
    protected void executeImpl() {
        Iterator<Camera> cameras = getEntitySystem().iterator(Camera.ID);
        while(cameras.hasNext()) {
            Camera c = cameras.next();
            Transform t = c.getOwner().get(Transform.ID);
            
            if (t != null) {
                // push transform onto frustum, don't bother with version
                // checks since there won't be that many cameras in the scene
                // (hopefully), so we don't need the extra overhead
                AffineTransform mat = t.getMatrix();
                c.getFrustum().setOrientation(mat.getTranslation(), 
                                              mat.getUpperMatrix().getCol(2), 
                                              mat.getUpperMatrix().getCol(1));
                c.notifyChange();
                //fixme: how to handle surface dimension changes
            }
        }
    }

    @Override
    protected void destroyImpl() {
        // do nothing
    }
}
