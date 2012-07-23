package com.ferox.scene;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.entreri.ColorRGBProperty;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.Unmanaged;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;
import com.lhkbob.entreri.property.ObjectProperty;

/**
 * <p>
 * AtmosphericFog is a Component that can add a visual approximation to fog to a
 * rendered scene. This component models fog by using a density fall-off
 * function and a distance through which the fog will become opaque. This model
 * is compatible with the classic eye-space fog that is usable in a
 * fixed-function OpenGL rendering engine.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class AtmosphericFog extends ComponentData<AtmosphericFog> {
    /**
     * The shared TypedId representing AtmosphericFog.
     */
    public static final TypeId<AtmosphericFog> ID = TypeId.get(AtmosphericFog.class);
    
    /**
     * Falloff represents how the visibility of fog decreases as distance
     * increases. The opacity of the fog, at some distance, can be considered as
     * a floating point number within [0, 1]. When at a distance less than or
     * equal to 0, the opacity is 0. When at a distance greater than or equal to
     * the {@link AtmosphericFog#getOpaqueDistance() opacity distance of the fog}, the
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
    
    private ColorRGBProperty color;
    
    @DefaultDouble(10)
    private DoubleProperty distanceToOpaque;
    
    private ObjectProperty<Falloff> falloff;
    
    @Unmanaged
    private final ColorRGB colorCache = new ColorRGB();

    /**
     * Set the Falloff to use for this fog. See {@link #getFalloff()} and
     * {@link Falloff} for descriptions of what the Falloff accomplishes.
     * 
     * @param falloff The new falloff
     * @return This AtmosphericFog for chaining purposes
     * @throws NullPointerException if falloff is null
     */
    public AtmosphericFog setFalloff(Falloff falloff) {
        if (falloff == null)
            throw new NullPointerException("Falloff cannot be null");
        this.falloff.set(falloff, getIndex());
        return this;
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
        return falloff.get(getIndex());
    }

    /**
     * Set the maximum distance that light can travel through the fog before
     * being completely obscured. Any object that's closer to the edge of the
     * fog, or the viewer if within the fog, will be combined with the
     * {@link #getColor() fog color} based on its distance and the
     * {@link #getFalloff() falloff}.
     * 
     * @param dist The new opaque distance
     * @return This AtmosphericFog for chaining purposes
     * @throws IllegalArgumentException if dist <= 0
     */
    public AtmosphericFog setOpaqueDistance(double dist) {
        if (dist <= 0)
            throw new IllegalArgumentException("Distance must be positive, not: " + dist);
        distanceToOpaque.set(dist, getIndex());
        return this;
    }

    /**
     * Return the distance from the viewer required for an object to be
     * completely obscured by the fog, or equivalently when the fog is fully
     * opaque and lets no light through to the viewer.
     * 
     * @return The opaque distance, this will be above 0
     */
    public double getOpaqueDistance() {
        return distanceToOpaque.get(getIndex());
    }

    /**
     * Copy <tt>color</tt> into this AtmosphericFog's color instance. The color represents
     * the color of the fog when fully opaque.
     * 
     * @param color The new fog color
     * @return The new version, via {@link #notifyChange()}
     * @throws NullPointerException if color is null
     */
    public AtmosphericFog setColor(@Const ColorRGB color) {
        if (color == null)
            throw new NullPointerException("Color cannot be null");
        colorCache.set(color);
        this.color.set(color, getIndex());
        return this;
    }

    /**
     * Return the color of this fog when the fog is fully opaque. If the fog is
     * not opaque, the fog color is blended with whatever happens to be within
     * the fog, based on its depth in the fog.
     * 
     * @return The fog color
     */
    public @Const ColorRGB getColor() {
        return colorCache;
    }
}
