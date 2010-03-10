package com.ferox.scene;

import com.ferox.math.Color4f;
import com.ferox.util.entity.AbstractComponent;

/**
 * <p>
 * Fog is a Component that can add a visual approximation to fog to a rendered
 * scene. The way in which Fog affects a scene depends on which Components its
 * combined with on a given Entity:
 * <ul>
 * <li>If the Fog has a {@link SceneElement} represents a region of fog
 * constrained to the SceneElement's location and world bounds.</li>
 * <li>If the Fog has a SceneElement and a {@link Shape}, the fog is further
 * constrained to be within the Shape's geometry at the element's location.</li>
 * <li>When neither of these are present, the Fog is a global fog that is always
 * in effect.</li>
 * </ul>
 * </p>
 * <p>
 * The above description of behavior is the ideal scenario. It may not be
 * possible for certain hardware to render geometry constrained fog properly. It
 * is also difficult to combine multiple Fog's together when they intersect.
 * These situations should usually be resolved by blending the fog colors or
 * choosing the more dominant Fog.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Fog extends AbstractComponent<Fog> {
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
	
	private final Color4f color;
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
	public Fog(Color4f color) {
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
	public Fog(Color4f color, Falloff falloff) {
		this(color, falloff, 10f);
	}

	/**
	 * Create a new Fog component that uses the given color, falloff and
	 * distance to full opacity. These are the arguments passed to
	 * {@link #setColor(Color4f)}, {@link #setFalloff(Falloff)} and
	 * {@link #setOpaqueDistance(float)}, respectively.
	 * 
	 * @param color The initial color
	 * @param falloff The initial falloff
	 * @param distanceToOpaque The initial opacity distance
	 * @throws NullPointerException if color or falloff are null
	 * @throws IllegalArgumentException if distanceToOpaque is negative
	 */
	public Fog(Color4f color, Falloff falloff, float distanceToOpaque) {
		super(Fog.class);
		
		this.color = new Color4f();
		setColor(color);
		setFalloff(falloff);
		setOpaqueDistance(distanceToOpaque);
	}

	/**
	 * Set the Falloff to use for this fog. See {@link #getFalloff()} and
	 * {@link Falloff} for descriptions of what the Falloff accomplishes.
	 * 
	 * @param falloff The new falloff
	 * @throws NullPointerException if falloff is null
	 */
	public void setFalloff(Falloff falloff) {
		if (falloff == null)
			throw new NullPointerException("Falloff cannot be null");
		this.falloff = falloff;
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
	 * @throws IllegalArgumentException if dist <= 0
	 */
	public void setOpaqueDistance(float dist) {
		if (dist <= 0f)
			throw new IllegalArgumentException("Distance must be positive, not: " + dist);
		distanceToOpaque = dist;
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
	 */
	public void setColor(Color4f color) {
		if (color == null)
			throw new NullPointerException("Color cannot be null");
		this.color.set(color);
	}

	/**
	 * Return the color of this fog when the fog is fully opaque. If the fog is
	 * not opaque, the fog color is blended with whatever happens to be within
	 * the fog, based on its depth in the fog.
	 * 
	 * @return The fog color
	 */
	public Color4f getColor() {
		return color;
	}
}
