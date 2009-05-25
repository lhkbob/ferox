package com.ferox.effect;

/**
 * <p>
 * EffectSet represents a source of Effect instances that can be used with a
 * RenderAtom. Its interface is very similar to that of Iterator<Effect> with
 * one major conceptual difference: it's intended that EffectSet's are re-usable
 * and resettable.
 * </p>
 * <p>
 * EffectSet implementations must adhere to the requirements of an Effect's
 * EffectType policy for multiple effect instances. If an EffectType requires a
 * single effect, then at most one Effect of that type can be returned from a
 * series of next() calls. It is not required that the effect instances of a
 * multiple effect type be returned consecutively with next().
 * </p>
 * <p>
 * To support an EffectSet being iterated over in both an inner and outer loop,
 * it has a notion of position. This position merely determines which Effect is
 * next, the order in which they are returned is meaningless.
 * </p>
 * <p>
 * The ordering of an EffectSet must be consistent between subsequent
 * modifications of its internal structures. Because of this, it is recommended
 * that iterations over an EffectSet only occur when the effect set will not be
 * modified, and to modify it while iterating.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface EffectSet {
	/**
	 * <p>
	 * Returns the current position of the EffectSet. A set's position
	 * determines the next Effect returned by a call to next(). Returned values
	 * become invalidated after the EffectSet is modified.
	 * </p>
	 * <p>
	 * A set's position should start at 0 after being reset and monotonically
	 * increase after each call to next(). It does not have to increase by 1
	 * each time, but it must not decrease, and can only stay the same if next()
	 * returns null (i.e. no more Effects in an iteration).
	 * </p>
	 * 
	 * @return The current position of of this EffectSet.
	 */
	public int position();

	/**
	 * <p>
	 * Set the EffectSet's current position. This can be used to allow for an
	 * EffectSet being iterated over in nested loops. It is recommended that
	 * position(pos) be used carefully.
	 * </p>
	 * <p>
	 * If pos represents an invalid position, it should have the same effect as
	 * resetting the position to 0.
	 * </p>
	 * 
	 * @param pos The new position
	 */
	public void position(int pos);

	/**
	 * <p>
	 * Retrieve the next Effect from this EffectSet, based on the current
	 * position. The current position of the EffectSet should be updated so that
	 * the next logical Effect will be returned by the next invocation of
	 * next(). If an EffectSet is modified while iterating, the returned values
	 * from next() are not guaranteed to represent the EffectSet's entire effect
	 * state; in this situation a call to reset() is required to begin the
	 * iteration anew.
	 * </p>
	 * <p>
	 * If there are no more Effects to return, then null should be returned to
	 * signal the end of this set's Effects.
	 * </p>
	 * 
	 * @return The next logical Effect from this set, or null if there are no
	 *         more
	 */
	public Effect next();

	/**
	 * Reset the EffectSet's internal position so that the next call to next()
	 * will restart the iteration over this EffectSet's Effect instances. This
	 * should have the same effect as calling position(0).
	 */
	public void reset();
}
