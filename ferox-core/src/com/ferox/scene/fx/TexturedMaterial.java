package com.ferox.scene.fx;

import com.ferox.resource.TextureImage;
import com.ferox.util.entity.Component;

public class TexturedMaterial extends Component {
	private static final String DESCR = "Adds textureing to rendered Entities";
	
	private TextureImage primarySurface;
	private TextureImage decalSurface;
	
	public TexturedMaterial(TextureImage primary) {
		this(primary, null);
	}
	
	public TexturedMaterial(TextureImage primary, TextureImage decal) {
		super(DESCR, false);
		
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
