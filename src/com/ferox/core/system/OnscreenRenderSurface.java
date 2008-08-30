package com.ferox.core.system;

public abstract class OnscreenRenderSurface extends RenderSurface {
	@Override
	public boolean isHeadless() {
		return false;
	}
	
	public abstract Object getRenderSurface();
}
