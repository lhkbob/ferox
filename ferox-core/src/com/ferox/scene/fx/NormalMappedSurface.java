package com.ferox.scene.fx;

import com.ferox.resource.TextureImage;

public class NormalMappedSurface implements Component {
	private TextureImage normalMap;
	
	public NormalMappedSurface(TextureImage normalMap) {
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

	@Override
	public final Class<NormalMappedSurface> getType() {
		return NormalMappedSurface.class;
	}
}
