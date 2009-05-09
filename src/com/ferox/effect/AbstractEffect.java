package com.ferox.effect;

import com.ferox.effect.EffectType.Type;

/**
 * Basic implementation that caches the Type returned by a sub-classes EffectType
 * annotation, so its constructor will fail unless the class is properly
 * annotated.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractEffect implements Effect {
	private final Type type;

	public AbstractEffect() {
		type = getClass().getAnnotation(EffectType.class).value();
	}

	@Override
	public Type getType() {
		return type;
	}
}
