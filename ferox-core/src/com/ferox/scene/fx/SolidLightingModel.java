package com.ferox.scene.fx;

import com.ferox.math.Color4f;
import com.ferox.util.entity.Component;

public class SolidLightingModel extends Component {
	private static final String DESCR = "Renderable entities are unlit using a solid color";
	
	private final Color4f color;
	
	public SolidLightingModel(Color4f color) {
		super(DESCR, false);
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
