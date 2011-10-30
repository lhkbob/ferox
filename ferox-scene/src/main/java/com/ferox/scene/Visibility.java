package com.ferox.scene;

import java.util.HashMap;

import com.ferox.entity2.Component;
import com.ferox.entity2.Controller;
import com.ferox.entity2.MetaComponent;
import com.ferox.entity2.Template;
import com.ferox.entity2.TypedComponent;
import com.ferox.entity2.TypedId;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Frustum;
import com.ferox.scene.controller.VisibilityController;

/**
 * Visibility is a Component type that tracks the visibility of Entities to
 * Frustums. The most common use is using this with a {@link Renderable} for
 * frustum culling purposes. The {@link VisibilityController} automatically
 * associates a Visibility component with an Entity that has a Renderable
 * component.
 * 
 * @author Michael Ludwig
 */
@MetaComponent
public final class Visibility extends TypedComponent<Visibility> {
    /**
     * The shared TypedId representing Visibility.
     */
    public static final TypedId<Visibility> ID = Component.getTypedId(Visibility.class);
    
    private static final Object VALUE = new Object();
    
    private final HashMap<Frustum, Object> visibility;
    
    /**
     * Create a new Visibility component with an empty visibility set.
     */
    public Visibility() {
        super(null, false);
        visibility = new HashMap<Frustum, Object>();
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
        visibility = new HashMap<Frustum, Object>(clone.visibility);
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
    
    
    /**
     * Return the local bounds of the Renderable. This will always return the
     * same instance, and the instance will be updated based on any calls to
     * {@link #setLocalBounds(AxisAlignedBox)}.
     * 
     * @return The local bounds
     */
    public AxisAlignedBox getLocalBounds() {
        // FIXME: update signature when we have read-only aabb's
        return localBounds;
    }

    /**
     * Set the local bounds of this Renderable. The bounds should contain the
     * entire Geometry of this Renderable, including any modifications dynamic
     * animation might cause. If a Visibility component is attached to an
     * entity, the local bounds can be used in frustum-culling.
     * 
     * @param bounds The new local bounds of the Renderable
     * @return The new version of the component, via {@link #notifyChange()}
     * @throws NullPointerException if bounds is null
     */
    public int setLocalBounds(AxisAlignedBox bounds) {
        localBounds.set(bounds);
        return notifyChange();
    }
}
