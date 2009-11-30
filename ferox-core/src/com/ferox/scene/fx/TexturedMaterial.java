package com.ferox.scene.fx;

import com.ferox.resource.TextureImage;

public class TexturedMaterial implements Component {
	private TextureImage primarySurface;
	private TextureImage decalSurface;
	
	public TexturedMaterial(TextureImage primary) {
		this(primary, null);
	}
	
	public TexturedMaterial(TextureImage primary, TextureImage decal) {
		setPrimaryTexture(primary);
		setDecalTexture(decal);
	}
	
	public void setPrimaryTexture(TextureImage image) {
		primarySurface = image;
	}
	
	public TextureImage getPrimaryTexture() {
		return primarySurface;
	}
	
	public void setDecalTexture(TextureImage image) {
		decalSurface = image;
	}
	
	public TextureImage getDecalTexture() {
		return decalSurface;
	}

	@Override
	public Class<TexturedMaterial> getType() {
		return TexturedMaterial.class;
	}
}
