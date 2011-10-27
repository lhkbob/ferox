package com.ferox.scene.controller;

import com.ferox.entity2.Entity;
import com.ferox.math.bounds.Frustum;
import com.ferox.math.bounds.QueryCallback;

/**
 * VisibilityCallback is a simple QueryCallback that updates a
 * {@link Visibility} component attached to an Entity if it exists.
 * 
 * @author Michael Ludwig
 */
public class VisibilityCallback implements QueryCallback<Entity> {
    private final Frustum f;

    /**
     * Create a new VisibilityCallback that set each discovered Entity with a
     * Transform's visibility to true for the given Frustum, <tt>f</tt>.
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
        Visibility v = e.get(Visibility.ID);
        if (v == null) {
            v = new Visibility();
            e.add(v);
        }
        
        v.setVisible(f, true);
    }
}