package com.ferox.scene.fx;

import com.ferox.resource.TextureImage;

public class TextureUnit {
	private TextureImage image;
	private int unit;

	public TextureUnit(TextureImage image, int unit) {
		setTexture(image);
		setUnit(unit);
	}
	
	public TextureImage getTexture() {
		return image;
	}
	
	public int getUnit() {
		return unit;
	}
	
	public void setTexture(TextureImage image) {
		if (image == null)
			throw new NullPointerException("TextureImage cannot be null");
		this.image = image;
	}
	
	public void setUnit(int unit) {
		if (unit < 0)
			throw new IllegalArgumentException("Unit must be >= 0, not: " + unit);
		this.unit = unit;
	}
}
