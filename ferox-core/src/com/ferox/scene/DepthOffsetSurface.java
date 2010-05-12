package com.ferox.scene;

import com.ferox.resource.TextureImage;
import com.ferox.entity.AbstractComponent;

public final class DepthOffsetSurface extends AbstractComponent<DepthOffsetSurface> {
	private TextureImage depthMap;
	
	public DepthOffsetSurface(TextureImage depthmap) {
		super(DepthOffsetSurface.class);
		setDepthMap(depthMap);
	}
	
	public void setDepthMap(TextureImage depthMap) {
		if (depthMap == null)
			throw new NullPointerException("Cannot specify a null depth map");
		if (depthMap.getFormat().getNumComponents() != 1)
			throw new IllegalArgumentException("Cannot specify a depth map that has more than one component: " 
											   + depthMap.getFormat());
		this.depthMap = depthMap;
	}
	
	public TextureImage getDepthMap() {
		return depthMap;
	}
}
