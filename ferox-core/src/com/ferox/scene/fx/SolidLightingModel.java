package com.ferox.scene.fx;

import com.ferox.math.Color4f;

public class SolidLightingModel extends LightingModel {
	private Color4f color;
	
	public SolidLightingModel(Color4f color, boolean shadowReceiver) {
		super(shadowReceiver);
		setColor(color);
	}
	
	public Color4f getColor() {
		return color;
	}
	
	public void setColor(Color4f color) {
		if (color == null)
			color = new Color4f(.2f, .2f, .2f, 1f);
		this.color = color;
	}
}
