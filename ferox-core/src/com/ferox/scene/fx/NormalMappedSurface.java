package com.ferox.scene.fx;

import com.ferox.resource.TextureImage;
import com.ferox.util.entity.Component;

public class NormalMappedSurface extends Component {
	private static final String DESCR = "Adds image based normal mapping to rendered Entities";
	
	private TextureImage normalMap;
	
	public NormalMappedSurface(TextureImage normalMap) {
		super(DESCR, false);
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
