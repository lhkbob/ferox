package com.ferox.scene.fx;

public class ParallaxNormalMappedSurface extends NormalMappedSurface {
	private TextureUnit depthMap;
	
	public ParallaxNormalMappedSurface(TextureUnit depthmap, TextureUnit normalMap,
									   int tangentSlot, int bitangentSlot) {
		super(normalMap, tangentSlot, bitangentSlot);
		setDepthMap(depthMap);
	}
	
	public void setDepthMap(TextureUnit depthMap) {
		if (depthMap == null)
			throw new NullPointerException("Cannot specify a null depth map");
		if (depthMap.getTexture().getFormat().getNumComponents() != 1)
			throw new IllegalArgumentException("Cannot specify a depth map that has more than one component: " 
											   + depthMap.getTexture().getFormat());
		this.depthMap = depthMap;
	}
	
	public TextureUnit getDepthMap() {
		return depthMap;
	}
}
