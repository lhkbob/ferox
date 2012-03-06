package com.ferox.scene.controller;

import com.ferox.math.bounds.QueryCallback;
import com.ferox.scene.Renderable;
import com.googlecode.entreri.Entity;

/**
 * VisibilityCallback is a simple QueryCallback that updates a
 * {@link Renderable} component's visibility to a given entity.
 * 
 * @author Michael Ludwig
 */
public class VisibilityCallback implements QueryCallback<Renderable> {
    private final Entity camera;

    /**
     * Create a new VisibilityCallback that set each discovered Entity with a
     * Transform's visibility to true for the given entity, <tt>camera</tt>.
     * 
     * @param camera The Entity that will be flagged as visible
     * @throws NullPointerException if camera is null
     */
    public VisibilityCallback(Entity camera) {
        if (camera == null)
            throw new NullPointerException("Entity cannot be null");
        this.camera = camera;
    }
    
    @Override
    public void process(Renderable r) {
        r.setVisible(camera, true);
    }
}