package com.ferox.scene;

import com.ferox.entity2.Component;
import com.ferox.entity2.Template;
import com.ferox.entity2.TypedComponent;
import com.ferox.entity2.TypedId;
import com.ferox.math.Color3f;

/**
 * <p>
 * Fog is a Component that can add a visual approximation to fog to a rendered
 * scene. This component models fog by using a density fall-off function and a
 * distance through which the fog will become opaque. This model is compatible
 * with the classic eye-space fog that is usable in a fixed-function OpenGL
 * rendering engine and can be extended by more Component types to provide more
 * advanced fogs, such as spatialized or shaped fogs.
 * </p>
 * 
 * @author Michael Ludwig
 */
// FIXME: Rename AtmosphericFog
public final class Fog extends TypedComponent<Fog> {
    /**
     * The shared TypedId representing Fog.
     */
    public static final TypedId<Fog> ID = Component.getTypedId(Fog.class);
    
    /**
     * Falloff represents how the visibility of fog decreases as distance
     * increases. The opacity of the fog, at some distance, can be considered as
     * a floating point number within [0, 1]. When at a distance less than or
     * equal to 0, the opacity is 0. When at a distance greater than or equal to
     * the {@link Fog#getOpaqueDistance() opacity distance of the fog}, the
     * opacity is 1. The Falloff enum represents how the opacity value changes
     * between 0 and 1.
     */
    public static enum Falloff {
        /**
         * The opacity increases linearly from 0 to 1 as the distance increases
         * from 0 to the opaque distance.
         */
        LINEAR,
        /**
         * The opacity increases exponentially from 0 to 1 as the distance
         * increases from 0 to the opaque distance. The exact nature of this
         * equation is determined by the rendering engine, but the fog should
         * resemble a long tail of low opacity followed with a rapid increase to
         * fully opaque.
         */
        EXPONENTIAL
    }
    
    private final Color3f color;
    private float distanceToOpaque;
    private Falloff falloff;

    /**
     * Create a new Fog component with the given color. The fog is configured to
     * use {@link Falloff#LINEAR linear} falloff and a distance to opacity of 10
     * units.
     * 
     * @param color The initial color
     * @throws NullPointerException if color is null
     */
    public Fog(Color3f color) {
        this(color, Falloff.LINEAR);
    }

    /**
     * Create a new Fog component that uses the given color and falloff. The
     * distance to opacity is set to 10, in whatever units that are relevant to
     * the containing scene.
     * 
     * @param color The initial color
     * @param falloff The initial falloff
     * @throws NullPointerException if color or falloff are null
     */
    public Fog(Color3f color, Falloff falloff) {
        this(color, falloff, 10f);
    }

    /**
     * Create a new Fog component that uses the given color, falloff and
     * distance to full opacity. These are the arguments passed to
     * {@link #setColor(ColorRGB)}, {@link #setFalloff(Falloff)} and
     * {@link #setOpaqueDistance(float)}, respectively.
     * 
     * @param color The initial color
     * @param falloff The initial falloff
     * @param distanceToOpaque The initial opacity distance
     * @throws NullPointerException if color or falloff are null
     * @throws IllegalArgumentException if distanceToOpaque is negative
     */
    public Fog(Color3f color, Falloff falloff, float distanceToOpaque) {
        super(null, false);
        
        this.color = new Color3f();
        setColor(color);
        setFalloff(falloff);
        setOpaqueDistance(distanceToOpaque);
    }

    /**
     * Create a Fog that is a clone of <tt>clone</tt>, for use with a
     * {@link Template}.
     * 
     * @param clone The Fog to clone
     * @throws NullPointerException if clone is null
     */
    public Fog(Fog clone) {
        super(clone, true);
        
        color = new Color3f(clone.color);
        falloff = clone.falloff;
        distanceToOpaque = clone.distanceToOpaque;
    }

    /**
     * Set the Falloff to use for this fog. See {@link #getFalloff()} and
     * {@link Falloff} for descriptions of what the Falloff accomplishes.
     * 
     * @param falloff The new falloff
     * @return The new version, via {@link #notifyChange()}
     * @throws NullPointerException if falloff is null
     */
    public int setFalloff(Falloff falloff) {
        if (falloff == null)
            throw new NullPointerException("Falloff cannot be null");
        this.falloff = falloff;
        return notifyChange();
    }

    /**
     * Return the Falloff equation to use that determines how an object's color
     * and the fog color are combined when the object is in front of the viewer
     * but closer than the opaque distance. This also represents how the density
     * of the fog changes as distance increases.
     * 
     * @return The Falloff of this fog
     */
    public Falloff getFalloff() {
        return falloff;
    }

    /**
     * Set the maximum distance that light can travel through the fog before
     * being completely obscured. Any object that's closer to the edge of the
     * fog, or the viewer if within the fog, will be combined with the
     * {@link #getColor() fog color} based on its distance and the
     * {@link #getFalloff() falloff}.
     * 
     * @param dist The new opaque distance
     * @return The new version, via {@link #notifyChange()}
     * @throws IllegalArgumentException if dist <= 0
     */
    public int setOpaqueDistance(float dist) {
        if (dist <= 0f)
            throw new IllegalArgumentException("Distance must be positive, not: " + dist);
        distanceToOpaque = dist;
        return notifyChange();
    }

    /**
     * Return the distance from the viewer required for an object to be
     * completely obscured by the fog, or equivalently when the fog is fully
     * opaque and lets no light through to the viewer.
     * 
     * @return The opaque distance, this will be above 0
     */
    public float getOpaqueDistance() {
        return distanceToOpaque;
    }

    /**
     * Copy <tt>color</tt> into this Fog's color instance. The color represents
     * the color of the fog when fully opaque.
     * 
     * @param color The new fog color
     * @return The new version, via {@link #notifyChange()}
     * @throws NullPointerException if color is null
     */
    public int setColor(Color3f color) {
        if (color == null)
            throw new NullPointerException("Color cannot be null");
        this.color.set(color);
        return notifyChange();
    }

    /**
     * Return the color of this fog when the fog is fully opaque. If the fog is
     * not opaque, the fog color is blended with whatever happens to be within
     * the fog, based on its depth in the fog.
     * 
     * @return The fog color
     */
    public Color3f getColor() {
        return color;
    }
}
