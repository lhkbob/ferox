package com.ferox.scene;

import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Framework;
import com.ferox.util.entity.EntitySystem;

public interface SceneCompositor {
	public Framework getFramework();
	
	public EntitySystem getEntitySystem();
	
	public FrameStatistics render(boolean queueOnly);
}
