package com.ferox.scene.fx;

import com.ferox.resource.TextureImage;
import com.ferox.util.entity.AbstractComponent;
import com.ferox.util.entity.Description;
import com.ferox.util.entity.NonIndexable;

@NonIndexable
@Description("Adds textureing to rendered Entities")
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
