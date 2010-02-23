package com.ferox.scene;

import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;
import com.ferox.util.entity.AbstractComponent;
import com.ferox.util.entity.Description;

@Description("SpotLight acts as a positional light with a cone or sphere of influence")
public class SpotLight extends AbstractComponent<SpotLight> {
	private final Vector3f direction;
	private final Vector3f position;
	
	private final Color4f color;
	
	private float cutoffAngle;

	public SpotLight(Color4f color, Vector3f direction) {
		super(SpotLight.class);
		
		this.direction = new Vector3f();
		this.position = new Vector3f();
		this.color = new Color4f();
		
		setDirection(direction);
		setColor(color);
	}
	
	public Vector3f getPosition() {
		return position;
	}
	
	public void setPosition(Vector3f position) {
		if (position == null)
			throw new NullPointerException("Position can't be null");
		this.position.set(position);
	}
	
	public float getCutoffAngle() {
		return cutoffAngle;
	}
	
	public void setCutoffAngle(float angle) {
		if ((angle < 0 || angle > 90) && angle != 180f)
			throw new IllegalArgumentException("Illegal cutoff angle, must be in [0, 90] or 180, not: " + angle);
		cutoffAngle = angle;
	}
	
	public Color4f getColor() {
		return color;
	}
	
	public void setColor(Color4f color) {
		if (color == null)
			throw new NullPointerException("Color cannot be null");
		this.color.set(color);
	}
	
	public Vector3f getDirection() {
		return direction;
	}
	
	public void setDirection(Vector3f direction) {
		if (direction == null)
			throw new NullPointerException("Direction cannot be null");
		this.direction.set(direction);
	}
}
