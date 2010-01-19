package com.ferox.scene;

import com.ferox.math.Vector3f;
import com.ferox.util.entity.Component;

public class DirectedLight extends Component {
	private static final String DESCR = "Modifies behavior of Light by adding direction to the light source";
	
	private final Vector3f direction;
	private float cutoffAngle;

	public DirectedLight(Vector3f direction) {
		this(direction, -1f);
	}
	
	public DirectedLight(Vector3f direction, float cutoffAngle) {
		super(DESCR);
		
		this.direction = new Vector3f();
		setDirection(direction);
		setCutoffAngle(cutoffAngle);
	}
	
	public void setDirection(Vector3f dir) {
		if (dir == null)
			throw new NullPointerException("Direction cannot be null");
		direction.set(dir);
	}
	
	public Vector3f getDirection() {
		return direction;
	}
	
	public void setCutoffAngle(float cutoff) {
		if (cutoff > 90f)
			throw new IllegalArgumentException("Cutoff angle must be less than 90 degrees, not: " + cutoff);
		cutoffAngle = cutoff;
	}
	
	public float getCutoffAngle() {
		return cutoffAngle;
	}
}
