package com.ferox.scene.controller;

import com.ferox.entity.Entity;
import com.ferox.math.bounds.Frustum;
import com.ferox.math.bounds.QueryCallback;
import com.ferox.scene.SceneElement;

/**
 * VisibilityCallback is a simple QueryCallback that marks SceneElements as
 * visible to a Frustum. Ideally, the Frustum should be the one that is used in
 * the actual query.
 * 
 * @author Michael Ludwig
 */
public class VisibilityCallback implements QueryCallback<Entity> {
    private final Frustum f;

    /**
     * Create a new VisibilityCallback that set each discovered Entity with a
     * SceneElement's visibility to true for the given Frustum, <tt>f</tt>.
     * 
     * @param f The Frustum that will be flagged as visible
     * @throws NullPointerException if f is null
     */
    public VisibilityCallback(Frustum f) {
        if (f == null)
            throw new NullPointerException("Frustum cannot be null");
        this.f = f;
    }
    
    @Override
    public void process(Entity e) {
        SceneElement se = e.get(ViewNodeController.SE_ID);
        if (se != null)
            se.setVisible(f, true);
    }
}