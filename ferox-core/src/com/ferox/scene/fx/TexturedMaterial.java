package com.ferox.scene.fx;

public class TexturedMaterial implements Component {
	private TextureUnit primarySurface;
	private TextureUnit decalSurface;
	
	public TexturedMaterial(TextureUnit primary) {
		this(primary, null);
	}
	
	public TexturedMaterial(TextureUnit primary, TextureUnit decal) {
		setPrimaryTexture(primary);
		setDecalTexture(decal);
	}
	
	public void setPrimaryTexture(TextureUnit image) {
		primarySurface = image;
	}
	
	public TextureUnit getPrimaryTexture() {
		return primarySurface;
	}
	
	public void setDecalTexture(TextureUnit image) {
		decalSurface = image;
	}
	
	public TextureUnit getDecalTexture() {
		return decalSurface;
	}

	@Override
	public Class<TexturedMaterial> getType() {
		return TexturedMaterial.class;
	}
}
