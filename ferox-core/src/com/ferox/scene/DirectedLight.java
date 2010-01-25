package com.ferox.scene;

import com.ferox.math.Vector3f;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.Indexable;

/**
 * <p>
 * DirectedLight is a component that customizes the behavior of Entities that
 * are also {@link Light Lights}. An Entity that is a DirectedLight without also
 * being a Light is meaningless and should be ignored by Controllers which
 * process the lights within an EntitySystem. A DirectedLight can be used to
 * create two forms of light besides the ambient and point lights available with
 * a plane Light component:
 * <ul>
 * <li>An infinite directional light - This shines light as if it were
 * infinitely far away, so every point has the light hit it from the same
 * direction.</li>
 * <li>A spotlight - This shines light in a cone or spotlight along its
 * direction. The light is constrained by the half-angle of cone's apex.</li>
 * </ul>
 * </p>
 * <p>
 * The form a DirectedLight takes is configured based on its cutoff-angle. When
 * this angle is negative, the light becomes an infinite directional light since
 * the cone formed is meaningless. A positive angle causes the DirectedLight to
 * act as a spotlight. The spotlight configuration requires the Entity to also
 * be a SceneElement since it requires a finite location from which to shine. If
 * it is not a SceneElement, the DirectedLight acts as an infinite directional
 * light regardless of the cutoff angle.
 * </p>
 * <p>
 * When a DirectedLight is a SceneElement, its direction is further transformed
 * by the SceneElement so in effect, the direction returned by
 * {@link #getDirection()} represents a local direction. The SceneElement's
 * bounds limits the entities influenced just as with normal Lights. An infinite
 * directional light that is also a SceneElement is still useful because of
 * these two additional behaviors.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Indexable
public class DirectedLight extends Component {
	private static final String DESCR = "Modifies behavior of Light by adding direction to the light source";
	
	private final Vector3f direction;
	private float cutoffAngle;

	/**
	 * Create a DirectedLight component that has its initial direction match
	 * <tt>direction</tt>. The light uses a cutoff angle of -1, and is thus an
	 * infinite direction light.
	 * 
	 * @param direction The initial light direction
	 * @throws NullPointerException if direction is null
	 */
	public DirectedLight(Vector3f direction) {
		this(direction, -1f);
	}

	/**
	 * Create a DirectedLight component that uses the given direction and cutoff
	 * angle. These arguments are passed directly to
	 * {@link #setDirection(Vector3f)} and {@link #setCutoffAngle(float)}
	 * respectively.
	 * 
	 * @param direction The initial light direction
	 * @param cutoffAngle The initial cutoff angle, in degrees
	 * @throws NullPointerException if direction is null
	 * @throws IllegalArgumentException if cutoffAngle > 90
	 */
	public DirectedLight(Vector3f direction, float cutoffAngle) {
		super(DESCR);
		
		this.direction = new Vector3f();
		setDirection(direction);
		setCutoffAngle(cutoffAngle);
	}

	/**
	 * Copy <tt>dir</tt> into this DirectedLight's direction. See
	 * {@link #getDirection()} for how the final light direction is computed.
	 * 
	 * @param dir The new light direction
	 * @throws NullPointerException if dir is null
	 */
	public void setDirection(Vector3f dir) {
		if (dir == null)
			throw new NullPointerException("Direction cannot be null");
		direction.set(dir);
	}

	/**
	 * <p>
	 * Return the light direction instance. Any modifications to this instance
	 * will be reflected in future calls to {@link #getDirection()}. The final
	 * light direction depends on whether or not this DirectedLight is also a
	 * SceneElement. If it's not a SceneElement, the direction is only
	 * normalized. With a SceneElement, the direction is normalized and
	 * transformed by the element's Transform. These computations are handled
	 * internally by the rendering engine and will not modify the
	 * DirectedLight's reported direction.
	 * </p>
	 * <p>
	 * Undefined results occur if the returned vector is the zero vector, before
	 * or after transformation.
	 * </p>
	 * 
	 * @return The light direction
	 */
	public Vector3f getDirection() {
		return direction;
	}

	/**
	 * Set the cutoff angle for this DirectedLight. If <tt>cutoff</tt> is
	 * negative, this DirectedLight represents an infinite direction light. When
	 * positive, the light represents a spot light that shines in a cone with
	 * half-angle equal to <tt>cutoff</tt>. Because of this, the cutoff cannot
	 * be above 90 degrees.
	 * 
	 * @param cutoff The cutoff angle, in degrees, or a negative number for
	 *            direction lights
	 * @throws IllegalArgumentException if cutoff is > 90
	 */
	public void setCutoffAngle(float cutoff) {
		if (cutoff > 90f)
			throw new IllegalArgumentException("Cutoff angle cannot be greater than 90 degrees: " + cutoff);
		cutoffAngle = cutoff;
	}

	/**
	 * Return the cutoff angle for this DirectedLight. If the returned value is
	 * negative, the light is an infinite directional light. Otherwise, the
	 * value represents the degree measure of the half-angle for a cone of light
	 * shining along the light's direction. When positive, the DirectedLight
	 * requires a finite location for the apex of the cone. This location is
	 * retrieved from an associated SceneElement; when there is no SceneElement,
	 * the DirectedLight can only function as an infinite directional light and
	 * the cutoff angle is ignored.
	 * 
	 * @return The cutoff angle
	 */
	public float getCutoffAngle() {
		return cutoffAngle;
	}
}
