package com.ferox.effect;


/**
 * Basic implementation that caches the EffectType returned by a sub-classes Type
 * annotation, so its constructor will fail unless the class is properly
 * annotated.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractEffect implements Effect {
	private final EffectType type;

	public AbstractEffect() {
		type = getClass().getAnnotation(Type.class).value();
	}

	@Override
	public EffectType getType() {
		return type;
	}
}
