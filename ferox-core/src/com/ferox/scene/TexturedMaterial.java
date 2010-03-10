package com.ferox.scene;

import com.ferox.resource.TextureImage;
import com.ferox.util.entity.AbstractComponent;

public final class TexturedMaterial extends AbstractComponent<TexturedMaterial> {
	private TextureImage primarySurface;
	private TextureImage decalSurface;
	
	public TexturedMaterial(TextureImage primary) {
		this(primary, null);
	}
	
	public TexturedMaterial(TextureImage primary, TextureImage decal) {
		super(TexturedMaterial.class);
		
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
}
