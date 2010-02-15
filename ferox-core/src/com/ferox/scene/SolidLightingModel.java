package com.ferox.scene;

import com.ferox.math.Color4f;
import com.ferox.util.entity.AbstractComponent;
import com.ferox.util.entity.Description;
import com.ferox.util.entity.NonIndexable;

@NonIndexable
@Description("Renderable entities are unlit using a solid color")
public final class SolidLightingModel extends AbstractComponent<SolidLightingModel> {
	private final Color4f color;
	
	public SolidLightingModel(Color4f color) {
		super(SolidLightingModel.class);
		this.color = new Color4f();
		setColor(color);
	}
	
	public Color4f getColor() {
		return color;
	}
	
	public void setColor(Color4f color) {
		if (color == null)
			throw new NullPointerException("Color cannot be null");
		this.color.set(color);
	}
}
