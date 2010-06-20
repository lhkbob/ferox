package com.ferox.scene;

import com.ferox.resource.Texture;
import com.ferox.entity.AbstractComponent;

public final class NormalMappedSurface extends AbstractComponent<NormalMappedSurface> {
	private Texture normalMap;
	
	public NormalMappedSurface(Texture normalMap) {
		super(NormalMappedSurface.class);
		setNormalMap(normalMap);
	}
	
	public void setNormalMap(Texture normalMap) {
		if (normalMap == null)
			throw new NullPointerException("Normal map must be non-null");
		if (normalMap.getFormat().getNumComponents() != 3)
			throw new IllegalArgumentException("Normal map must use a texture format with 3 components, not: " 
											   + normalMap.getFormat());
		this.normalMap = normalMap;
	}
	
	public Texture getNormalMap() {
		return normalMap;
	}
}
