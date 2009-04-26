package com.ferox.effect;

import com.ferox.renderer.RenderDataCache;
import com.ferox.renderer.Renderer;

/**
 * Basic implementation that uses a RenderDataCache to implement
 * get/setRenderData().
 * 
 * @author Michael Ludwig
 * 
 */
public abstract class AbstractEffect implements Effect {
	private final RenderDataCache cache;

	public AbstractEffect() {
		cache = new RenderDataCache();
	}

	@Override
	public Object getRenderData(Renderer renderer) {
		return cache.getRenderData(renderer);
	}

	@Override
	public void setRenderData(Renderer renderer, Object data) {
		cache.setRenderData(renderer, data);
	}
}
