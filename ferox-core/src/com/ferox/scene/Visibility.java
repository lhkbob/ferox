package com.ferox.scene;

import java.util.concurrent.ConcurrentHashMap;

import com.ferox.entity.Component;
import com.ferox.entity.Controller;
import com.ferox.entity.Template;
import com.ferox.entity.TypedComponent;
import com.ferox.entity.TypedId;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Frustum;

/**
 * Visibility is a Component type that tracks the visibility of Entities to
 * Frustums. The most common use is using this with a {@link Renderable} for
 * frustum culling purposes. The {@link VisibilityController} takes care of
 * this. The lack of a Visibility component on an Entity should signal that
 * visibility tracking should be disabled for that Entity.
 * 
 * @author Michael Ludwig
 */
public final class Visibility extends TypedComponent<Visibility> {
    /**
     * The shared TypedId representing Visibility.
     */
    public static final TypedId<Visibility> ID = Component.getTypedId(Visibility.class);
    
    private static final Object VALUE = new Object();
    
    // We'll use a concurrent hash map instead of a simple set because this datastructure
    // is more easily corrupted than a vector or transform, and there shouldn't be a
    // big performance hit.
    private final ConcurrentHashMap<Frustum, Object> visibility;
    
    /**
     * Create a new Visibility component with an empty visibility set.
     */
    public Visibility() {
        super(null, false);
        visibility = new ConcurrentHashMap<Frustum, Object>();
    }

    /**
     * Create a Visibility component that is a clone of <tt>clone</tt> for use
     * with a {@link Template}.
     * 
     * @param clone The Visibility component to clone
     * @throws NullPointerException if clone is null
     */
    public Visibility(Visibility clone) {
        super(clone, true);
        visibility = new ConcurrentHashMap<Frustum, Object>(clone.visibility);
    }
    
    /**
     * Return true if this Entity has been flagged as visible to the
     * Frustum <tt>f</tt>. Implementations of {@link Controller} are responsible
     * for assigning this as appropriate.
     * 
     * @param f The Frustum to check visibility
     * @return Whether or not the Entity is visible to f
     * @throws NullPointerException if f is null
     */
    public boolean isVisible(Frustum f) {
        if (f == null)
            throw new NullPointerException("Frustum cannot be null");
        return visibility.containsKey(f);
    }

    /**
     * Set whether or not this Entity is considered visible to the Frustum
     * <tt>f</tt>. The method is provided so that Controllers can implement
     * their own visibility algorithms, instead of relying solely on
     * {@link AxisAlignedBox#intersects(Frustum, com.ferox.math.bounds.PlaneState)}
     * .
     * 
     * @param f The Frustum whose visibility is assigned
     * @param pv Whether or not the Entity is visible to f
     * @return The new version of the component, via {@link #notifyChange()}
     * @throws NullPointerException if f is null
     */
    public int setVisible(Frustum f, boolean pv) {
        if (f == null)
            throw new NullPointerException("Frustum cannot be null");
        if (pv)
            visibility.put(f, VALUE);
        else
            visibility.remove(f);
        return notifyChange();
    }

    /**
     * Reset the visibility flags so that the Entity is no longer visible to any
     * Frustums. Subsequent calls to {@link #isVisible(Frustum)} will return
     * false until a Frustum has been flagged as visible via
     * {@link #setVisible(Frustum, boolean)}.
     * 
     * @return The new version of the component, via {@link #notifyChange()}
     */
    public int resetVisibility() {
        visibility.clear();
        return notifyChange();
    }
}
