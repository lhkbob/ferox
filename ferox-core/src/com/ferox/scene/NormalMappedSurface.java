package com.ferox.scene;

import com.ferox.resource.TextureImage;
import com.ferox.util.entity.AbstractComponent;

public final class NormalMappedSurface extends AbstractComponent<NormalMappedSurface> {
	private TextureImage normalMap;
	
	public NormalMappedSurface(TextureImage normalMap) {
		super(NormalMappedSurface.class);
		setNormalMap(normalMap);
	}
	
	public void setNormalMap(TextureImage normalMap) {
		if (normalMap == null)
			throw new NullPointerException("Normal map must be non-null");
		if (normalMap.getFormat().getNumComponents() != 3)
			throw new IllegalArgumentException("Normal map must use a texture format with 3 components, not: " 
											   + normalMap.getFormat());
		this.normalMap = normalMap;
	}
	
	public TextureImage getNormalMap() {
		return normalMap;
	}
}
