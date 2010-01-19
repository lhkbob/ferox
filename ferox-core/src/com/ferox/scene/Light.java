package com.ferox.scene;

import com.ferox.math.Color4f;
import com.ferox.util.entity.Component;

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
public class Light extends Component {
	private static final String DESCR = "Adds light to rendered entities in a scene";
	
	private final Color4f color;
	private boolean castsShadow;
	// FIXME: add intensity, or other form of light descriptor that modifies final colors sent to opengl?

	/**
	 * Create a new Light component using the given color, and hint to whether
	 * or not it casts shadows.
	 * 
	 * @param color The color passed to {@link #setColor(Color4f)}
	 * @param castsShadow The boolean passed to
	 *            {@link #setShadowCaster(boolean)}
	 * @throws NullPointerException if color is null
	 */
	public Light(Color4f color, boolean castsShadow) {
		super(DESCR);
		this.color = new Color4f();
		
		setColor(color);
		setShadowCaster(castsShadow);
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

	/**
	 * Set whether or not this light casts shadows. This is a hint to the
	 * rendering engine that it should render shadows cast from objects affected
	 * by this light.
	 * 
	 * @param castsShadow True if shadows are cast
	 */
	public void setShadowCaster(boolean castsShadow) {
		this.castsShadow = castsShadow;
	}

	/**
	 * Return true if shadows should be cast by this Light. This is only a hint
	 * and shadows may be unsupported by the rendering engine. However, if this
	 * returns false, then no shadows should be cast from this light.
	 * 
	 * @return True if this light casts shadows
	 */
	public boolean isShadowCaster() {
		return castsShadow;
	}
}
