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
	// FIXME: add intensity, or other form of light descriptor that modifies final colors sent to opengl?

	/**
	 * Create a new Light component using the given color.
	 * 
	 * @param color The color passed to {@link #setColor(Color4f)}
	 * @throws NullPointerException if color is null
	 */
	public Light(Color4f color) {
		super(DESCR);
		this.color = new Color4f();
		
		setColor(color);
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
