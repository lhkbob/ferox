package com.ferox.core.system;


public abstract class HeadlessRenderSurface extends RenderSurface {

	@Override
	public boolean isHeadless() {
		return true;
	}
	
	@Override
	public boolean isLightweight() {
		return false;
	}
}
