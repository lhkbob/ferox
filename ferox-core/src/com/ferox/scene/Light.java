package com.ferox.scene;

import com.ferox.math.Color4f;
import com.ferox.util.entity.Component;

public class Light extends Component {
	private static final String DESCR = "Adds light to rendered entities in a scene";
	
	private final Color4f color;
	private boolean castsShadow;
	// FIXME: add intensity, or other form of light descriptor that modifies final colors sent to opengl?
	
	public Light(Color4f color, boolean castsShadow) {
		super(DESCR);
		this.color = new Color4f();
		
		setColor(color);
		setShadowCaster(castsShadow);
	}
	
	public void setColor(Color4f color) {
		if (color == null)
			throw new NullPointerException("Color cannot be null");
		this.color.set(color);
	}
	
	public Color4f getColor() {
		return color;
	}
	
	public void setShadowCaster(boolean castsShadow) {
		this.castsShadow = castsShadow;
	}
	
	public boolean isShadowCaster() {
		return castsShadow;
	}
}
