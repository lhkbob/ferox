package com.ferox.scene;

import java.util.HashSet;
import java.util.Set;

import com.ferox.entity.AbstractComponent;
import com.ferox.entity.Controller;
import com.ferox.math.ReadOnlyMatrix3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Transform;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Frustum;

/**
 * <p>
 * SceneElement represents any finite element of a rendered scene. Generally a
 * SceneElement is not useful by itself but can be combined with a number of
 * other Components to represent anything from lights, to fog, to rendered
 * geometry. These other Component types then depend on an Entity also being a
 * SceneElement. It is recommended that each Entity use a unique SceneElement
 * instance, unlike appearance related Components which can possibly be shared
 * across many Entities.
 * </p>
 * <p>
 * SceneElements can be added to an EntitySystem which has a SceneController.
 * The SceneController will organize them in an efficient manor to support
 * visibility and spatial queries.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class SceneElement extends AbstractComponent<SceneElement> {
    private final Transform transform;
    
    private AxisAlignedBox localBounds;
    private AxisAlignedBox worldBounds;

    private final Set<Frustum> visibility;
    
    public SceneElement() {
        super(SceneElement.class);
        transform = new Transform();
        visibility = new HashSet<Frustum>();
    }

    /**
     * Return true if this SceneElement has been flagged as visible to the
     * Frustum <tt>f</tt>. Implementations of {@link Controller} are responsible
     * for assigning this as appropriate. If a SceneElement has no world bounds,
     * the controllers should consider the element "visible".
     * 
     * @param f The Frustum to check visibility
     * @return Whether or not the SceneElement is visible to f
     * @throws NullPointerException if f is null
     */
    public boolean isVisible(Frustum f) {
        if (f == null)
            throw new NullPointerException("Frustum cannot be null");
        return visibility.contains(f);
    }

    /**
     * Set whether or not this SceneElement is considered visible to the Frustum
     * <tt>f</tt>. The method is provided as is to allow for Controller
     * implementations to fine-grain or optimize the actual frustum testing
     * performed (instead of this method invoking
     * {@link AxisAlignedBox#intersects(Frustum, com.ferox.math.bounds.PlaneState)}
     * automatically.
     * 
     * @param f The Frustum whose visibility is assigned
     * @param pv Whether or not the SceneElement is visible to f
     * @throws NullPointerException if f is null
     */
    public void setVisible(Frustum f, boolean pv) {
        if (f == null)
            throw new NullPointerException("Frustum cannot be null");
        if (pv)
            visibility.add(f);
        else
            visibility.remove(f);
    }

    /**
     * Reset the visibility flags of this SceneElement so that is no longer
     * visible to any Frustums. Subsequent calls to
     * {@link #isVisible(Frustum)} will return false until a Frustum
     * has been flagged as visible via
     * {@link #setVisible(Frustum, boolean)}.
     */
    public void resetVisibility() {
        visibility.clear();
    }

    /**
     * Copy the given rotation matrix into this SceneElement's transform matrix.
     * 
     * @param m The new rotation matrix
     * @throws NullPointerException if m is null
     */
    public void setRotation(ReadOnlyMatrix3f m) {
        transform.getRotation().set(m);
    }

    /**
     * Copy the given translation into the 4th column of the 4x4 matrix of this
     * SceneElement's transform matrix.
     * 
     * @param t The new translation
     * @throws NullPointerException if t is null
     */
    public void setTranslation(ReadOnlyVector3f t) {
        transform.getTranslation().set(t);
    }

    /**
     * @return The matrix that represents the 3D position and orientation of
     *         this SceneElement. Changes to the returned instance are visible
     *         in the SceneElement
     */
    public Transform getTransform() {
        return transform;
    }

    /**
     * <p>
     * Set the local bounds that represent the extents of this SceneElement
     * within its local coordinate space. If <tt>bounds</tt> is null, then this
     * element has no bounds and the SceneController will assign a null world
     * bounds. Otherwise the local bounds will be transformed into world space
     * using {@link #getTransform()} to compute the world bounds.
     * </p>
     * <p>
     * This instance is not copied, so any changes to it later will be reflected by the
     * SceneElement.
     * </p>
     * 
     * @param bounds The new local bounds
     */
    public void setLocalBounds(AxisAlignedBox bounds) {
        localBounds = bounds;
    }

    /**
     * <p>
     * Return the local AxisAlignedBox instance used by this SceneElement. The
     * local bound volume is in the local coordinate space of this SceneElement,
     * and {@link #getTransform()} is used to compute the world bounds based
     * using {@link AxisAlignedBox#transform(Transform, AxisAlignedBox)}.
     * </p>
     * <p>
     * The returned instance is not a defensive copy.
     * </p>
     * <p>
     * This may return a null local bounds if the SceneElement is not bounded.
     * In this case, the world bounds should also return null after an update.
     * The returned instance must be different than the instance used to store
     * world bounds.
     * </p>
     * 
     * @return The local bounds
     */
    public AxisAlignedBox getLocalBounds() {
        return localBounds;
    }

    /**
     * Set the AxisAlignedBox instance that represents the computed world bounds,
     * based off of {@link #getTransform()} and {@link #getLocalBounds()}. This
     * should not be called directly, but is intended for use by a
     * SceneController.
     * 
     * @param bounds The new AxisAlignedBox instance stored for world bounds
     */
    public void setWorldBounds(AxisAlignedBox bounds) {
        worldBounds = bounds;
    }

    /**
     * Get the world bounds that was last assigned via
     * {@link #setWorldBounds(AxisAlignedBox)}. The SceneController computes the
     * world bounds based off of the assigned local bounds and the current
     * transform. If the local bounds are null, the world bounds will also be
     * null.
     * 
     * @return The world bounds of this SceneElement
     */
    public AxisAlignedBox getWorldBounds() {
        return worldBounds;
    }
}
