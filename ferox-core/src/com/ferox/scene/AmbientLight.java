package com.ferox.scene;

import com.ferox.math.Color4f;
import com.ferox.util.entity.AbstractComponent;

public class AmbientLight extends AbstractComponent<AmbientLight>{
	private final Color4f color;
	
	public AmbientLight(Color4f color) {
		super(AmbientLight.class);
		this.color = new Color4f();
		setColor(color);
	}
	
	public void setColor(Color4f color) {
		if (color == null)
			throw new NullPointerException("Color cannot be null");
		this.color.set(color);
	}
	
	public Color4f getColor() {
		return color;
	}
}