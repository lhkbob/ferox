package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.effect.Effect;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.EffectDriver;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/**
 * A SingleStateDriver can be used by state drivers that only support one type
 * of implementation of a Effect for a given role. The role that it is intended
 * for will be determined by the naming conventions set forth in
 * BasicStateDriverFactory.
 * 
 * @author Michael Ludwig
 */
public abstract class SingleStateDriver<T extends Effect> implements
	EffectDriver {
	private final Class<? extends Effect> stateClass;
	private final T defaultState;
	protected final JoglContextManager factory;

	private T lastAppliedState;
	private T queuedState;

	/**
	 * Subclasses should only expose a public constructor with only a
	 * JoglContextManager as an argument. The given state will be used as the
	 * default when no other state has been enqueued.
	 */
	protected SingleStateDriver(T defaultState, Class<T> type,
		JoglContextManager factory) {
		this.stateClass = type;
		this.defaultState = defaultState;
		this.factory = factory;

		this.lastAppliedState = null;
		this.queuedState = null;
	}

	/**
	 * Make the state changes to the given gl necessary. This will be called
	 * with the queued state, or by the default state supplied in the
	 * constructor.
	 */
	protected abstract void apply(GL gl, JoglStateRecord record, T nextState);

	@Override
	public void doApply() {
		if (this.queuedState != null) {
			if (this.queuedState != this.lastAppliedState)
				this.apply(this.factory.getGL(), this.factory.getRecord(),
					this.queuedState);
		} else if (this.lastAppliedState != null)
			this.apply(this.factory.getGL(), this.factory.getRecord(),
				this.defaultState);
		this.lastAppliedState = this.queuedState;

		this.reset();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void queueEffect(Effect state) {
		if (!this.stateClass.isInstance(state))
			throw new RenderException("Unsupported state type: " + state
				+ ", expected an instance of " + this.stateClass);

		this.queuedState = (T) state;
	}

	@Override
	public void reset() {
		this.queuedState = null;
	}
}
