package com.ferox.scene;

import com.ferox.math.Color4f;
import com.ferox.scene.fx.ShadowCaster;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.Indexable;

/**
 * <p>
 * Light is a Component that adds light into a scene of Entities. Its exact
 * behavior when applied to an Entity is dependent on the other components of
 * that entity. The following describes the expected guidelines for a Light's
 * behavior when used by the rendering engine:
 * <ul>
 * <li>If the Light is not combined with a {@link SceneElement}, the light
 * represents ambient light. Every ambient contribution should be summed
 * together to represent the final ambient light.</li>
 * <li>When a Light is combined with a SceneElement, it represents a point light
 * emitting light equally in all directions from the element's location. Only
 * entities within the element's world bounds are influenced by the light.</li>
 * <li>If a Light, SceneElement and {@link DirectedLight} are combined, the
 * light's emissions are constrained to be a directed light. See DirectedLight
 * for more details.</li>
 * <li>If a Light is combined with {@link ShadowCaster}, it is a hint that the
 * light is the source for shadow generation.</li>
 * <li>Additionally, there may be other combinations that are undocumented or
 * unknown at this time. Support is wholly dependent on the rendering engine
 * that processes the scene.</li>
 * </ul>
 * </p>
 * <p>
 * When a Light is combined with a Shape, the Light continues to function as
 * described above. It is not limited by the Shape's geometry (as is the case
 * with {@link Fog}). Instead, it will also be processed independently as a
 * Shape. This can be used to easily render situations such as light bulbs or
 * flashlights.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Indexable
public class Light extends Component {
	private static final String DESCR = "Adds light to rendered entities in a scene";
	
	private final Color4f color;
	private float intensity;

	/**
	 * Create a new Light component using the given color. The initial
	 * intensity is 0.5.
	 * 
	 * @param color The color passed to {@link #setColor(Color4f)}
	 * @throws NullPointerException if color is null
	 */
	public Light(Color4f color) {
		super(DESCR);
		this.color = new Color4f();
		
		setColor(color);
		setIntensity(.5f);
	}
	
	/**
	 * Set the Light's intensity.  The intensity of a light controls
	 * how bright its specular highlights are, and how rapidly its
	 * energy falls off with distance.  A value of 0 signals a very dim
	 * light.  There is no maximum value, which can be used to create very
	 * bright lights within HDR capable scenes. A value of 1 represents
	 * the highest LDR brightness.</p>
	 * <p>The intensity of the a Light may cause its color to appear
	 * darker or brighter than float values in the Color4f instance.
	 * For example a dim white light may appear equivalent to a gray
	 * bright light, excluding the energy falloff.</p>
	 * <p>Because of the nature of current rendering hardware, this
	 * intensity must be translated into approximated depth attenuation
	 * and into a specular exponent.  The exact details of this are
	 * not specified here, but it should follow the pattern that a higher
	 * intensity generates a brighter light.</p>
	 * @param intensity The light's intensity
	 * @throws IllegalArgumentException if intensity < 0
	 */
	public void setIntensity(float intensity) {
		if (intensity < 0f)
			throw new IllegalArgumentException("Intensity must >= 0, not: " + intensity);
		this.intensity = intensity;
	}

	/**
	 * Return the Light's current intensity. See {@link #setIntensity(float)}
	 * for details.
	 * 
	 * @return The light intensity
	 */
	public float getIntensity() {
		return intensity;
	}

	/**
	 * Set the color of this Light. This can be interpreted as ambient, diffuse
	 * and/or specular light depending on how this Light is mixed with other
	 * Components. <tt>color</tt> is copied into the internal Color4f used for
	 * this Light.
	 * 
	 * @param color The new light color
	 * @throws NullPointerException if color is null
	 */
	public void setColor(Color4f color) {
		if (color == null)
			throw new NullPointerException("Color cannot be null");
		this.color.set(color);
	}

	/**
	 * Return the color of this Light. Any modifications to the returned
	 * instance will be reflected in this Light's color.
	 * 
	 * @return The light color
	 */
	public Color4f getColor() {
		return color;
	}
}
