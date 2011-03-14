package com.ferox.scene;

import com.ferox.entity.Component;
import com.ferox.entity.Template;
import com.ferox.entity.TypedComponent;
import com.ferox.entity.TypedId;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.util.geom.Geometry;

/**
 * Renderable is a Component that enables an Entity to be rendered. It provides
 * a {@link Geometry} containing the vertex buffer information needed to render
 * the Entity and {@link DrawStyle DrawStyles} determining how each polygon is
 * rendered. To enable frustum-culling, the Renderable also stores an
 * axis-aligned bounding box that contains the geometry.</p>
 * <p>
 * The Renderable should be combined with a {@link Transform} to place the
 * Entity within a rendered scene. Many additional Components in this package
 * can be used to describe the materials, shaders and textures used to color and
 * light the rendered Entity.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Renderable extends TypedComponent<Renderable> {
    /**
     * The shared TypedId representing Renderable.
     */
    public static final TypedId<Renderable> ID = Component.getTypedId(Renderable.class);
    
    private DrawStyle frontStyle;
    private DrawStyle backStyle;

    private Geometry geometry;

    private final AxisAlignedBox localBounds;

    /**
     * Create a Renderable that renders the Geometry, <tt>g</tt>, and renders
     * only front facing polygons. It will use the provided bounds as the
     * initial local bounds.
     * 
     * @param g The Geometry to render
     * @param bounds The local bounds surrounding g
     * @throws NullPointerException if g or bounds are null
     */
    public Renderable(Geometry g, AxisAlignedBox bounds) {
        this(g, bounds, DrawStyle.SOLID, DrawStyle.NONE);
    }

    /**
     * Create a Renderable that renders the Geometry, <tt>g</tt>, and uses the
     * given DrawStyles for front and back facing polygons. It will use the
     * provided bounds as the initial local bounds.
     * 
     * @param g The Geometry to render
     * @param bounds The local bounds surrounding g
     * @param front The DrawStyle for front facing polygons
     * @param back The DrawStyle for back facing polygons
     * @throws NullPointerException if any arguments are null
     */
    public Renderable(Geometry g, AxisAlignedBox bounds, DrawStyle front, DrawStyle back) {
        super(null, false);
        localBounds = new AxisAlignedBox();

        setGeometry(g);
        setLocalBounds(bounds);
        setDrawStyleFront(front);
        setDrawStyleBack(back);
    }

    /**
     * Create a Renderable that is a clone of <tt>clone</tt>, for use with a
     * {@link Template}.
     * 
     * @param clone The Renderable to clone
     * @throws NullPointerException if clone is null
     */
    public Renderable(Renderable clone) {
        super(clone, true);
        
        localBounds = new AxisAlignedBox(clone.localBounds);
        geometry = clone.geometry; 
        frontStyle = clone.frontStyle;
        backStyle = clone.backStyle;
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

    /**
     * Assign a new Geometry instance to this Renderable.
     * 
     * @param geom The Geometry to use
     * @return The new version of the Renderable
     * @throws NullPointerException if geom is null
     */
    public int setGeometry(Geometry geom) {
        if (geom == null)
            throw new NullPointerException("Geometry cannot be null");
        geometry = geom;
        return notifyChange();
    }

    /**
     * @return The Geometry that is rendered
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * Set the DrawStyle used when rendering front-facing polygons of this
     * Renderable.
     * 
     * @param front The front-facing DrawStyle
     * @return The new version of the Renderable
     * @throws NullPointerException if front is null
     */
    public int setDrawStyleFront(DrawStyle front) {
        if (front == null)
            throw new NullPointerException("DrawStyle cannot be null");
        frontStyle = front;
        return notifyChange();
    }

    /**
     * @return The DrawStyle used for front-facing polygons
     */
    public DrawStyle getDrawStyleFront() {
        return frontStyle;
    }

    /**
     * Set the DrawStyle used when rendering back-facing polygons of this
     * Renderable.
     * 
     * @param back The back-facing DrawStyle
     * @return The new version of the Renderable
     * @throws NullPointerException if back is null
     */
    public int setDrawStyleBack(DrawStyle back) {
        if (back == null)
            throw new NullPointerException("DrawStyle cannot be null");
        backStyle = back;
        return notifyChange();
    }

    /**
     * @return The DrawStyle used for back-facing polygons
     */
    public DrawStyle getDrawStyleBack() {
        return backStyle;
    }
}
