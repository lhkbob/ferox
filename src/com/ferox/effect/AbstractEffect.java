package com.ferox.effect;

import com.ferox.effect.EffectType.Type;
import com.ferox.renderer.RenderDataCache;
import com.ferox.renderer.Renderer;

/**
 * Basic implementation that uses a RenderDataCache to implement
 * get/setRenderData(). It caches a Type[] returned by a sub-classes EffectType
 * annotation, so its constructor will fail unless the class is properly
 * annotated.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractEffect implements Effect {
	private final RenderDataCache cache;
	private final Type[] types;

	public AbstractEffect() {
		cache = new RenderDataCache();
		types = getClass().getAnnotation(EffectType.class).value();
	}

	@Override
	public Type[] getTypes() {
		return types;
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
