package com.ferox.scene.fx;

import com.ferox.resource.TextureImage;
import com.ferox.util.entity.Component;

public class DepthOffsetSurface extends Component {
	private static final String DESCR = "Adds surface relief data to an Entity";
	
	private TextureImage depthMap;
	
	public DepthOffsetSurface(TextureImage depthmap) {
		super(DESCR);
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
