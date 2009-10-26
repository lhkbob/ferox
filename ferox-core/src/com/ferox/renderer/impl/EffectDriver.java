package com.ferox.renderer.impl;

import com.ferox.shader.Effect;

/**
 * <p>
 * EffectDriver's provide low-level implementations for a specific type of
 * Effect. It is a Framework's responsibility to provide EffectDrivers that
 * confict with one-another.
 * </p>
 * <p>
 * For a given effect type, there will only ever be one driver object for an
 * AbstractFramework. When an AbstractFramework uses an EffectDriver returned by
 * a valid state driver factory, it is guaranteed that the Effect objects passed
 * in as arguments to the methods below will return the expected type in their
 * getType() method.
 * </p>
 * <p>
 * Driver implementations may have some internal limit to the number of
 * simultaneous effect instances active for a given type, and may need to ignore
 * some that are queued before a call to doApply(). Drivers should make all
 * efforts to use effects that will have the most influence.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface EffectDriver {
	/**
	 * <p>
	 * Invoked by an AbstractFramework to queue a effect of this driver's
	 * expected type.
	 * </p>
	 * If multiple calls to queueEffect() are issued, the retention policy
	 * depends on whether or not the type allows for multiple instances. If it
	 * does not, the last queued effect should be used. If multiple effects are
	 * supported, hold as many as possible and, if necessary discard the effects
	 * that were queued earliest.</p>
	 * <p>
	 * The driver should not actually modify low-level graphics, that is the
	 * responsibility of doApply().
	 */
	public void queueEffect(Effect state);

	/**
	 * <p>
	 * Perform the low-level graphics calls necessary to properly apply the
	 * effects that have been queued since the last call to doApply().
	 * </p>
	 * <p>
	 * If no states have been queued since the last call of doApply(), this call
	 * should modify the current state record so that rendered geometry will
	 * appear to be rendered with the default state associated with this
	 * driver's expected Effect type (not necessarily the default state of the
	 * underlying graphics device). This does imply that all modified state must
	 * be restored to their defaults. After a queued Effect has been applied, it
	 * should no longer be considered queued until it is passed into
	 * queueEffect() later on.
	 * </p>
	 * <p>
	 * Drivers can assumed that a low-level context is made current on the
	 * calling thread and that low-level graphics calls are allowed.
	 * </p>
	 */
	public void doApply();

	/**
	 * <p>
	 * Clear any queued states that weren't used by a call to doApply(). This
	 * could be called because of an end of the frame, or because a render atom
	 * is ignored, or to clean-up after an exception was thrown.
	 * </p>
	 * <p>
	 * Graphics state should not be modified, all necessary state changes should
	 * be done when doApply() is called again.
	 * </p>
	 */
	public void reset();
}
