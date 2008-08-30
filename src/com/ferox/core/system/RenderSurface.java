package com.ferox.core.system;

public abstract class RenderSurface {
	public abstract DisplayOptions getDisplayOptions();
	
	public abstract boolean isHeadless();
	public abstract boolean isLightweight();
}
