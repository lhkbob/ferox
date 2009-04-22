package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.StateDriver;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.state.State;

/** A SingleStateDriver can be used by state drivers that only support
 * one type of implementation of a State for a given role.  The role
 * that it is intended for will be determined by the naming conventions
 * set forth in BasicStateDriverFactory.
 * 
 * @author Michael Ludwig
 *
 */
public abstract class SingleStateDriver<T extends State> implements StateDriver {
	private Class<? extends State> stateClass;
	private T defaultState;
	protected final JoglSurfaceFactory factory;
	
	private T lastAppliedState;
	private T queuedState;
	private float queuedInfluence;
	
	/** Subclasses should only expose a public constructor with only
	 * a JoglSurfaceFactory as an argument. The given state will be
	 * used as the default when no other state has been enqueued. */
	protected SingleStateDriver(T defaultState, Class<T> type, JoglSurfaceFactory factory) {
		this.stateClass = type;
		this.defaultState = defaultState;
		this.factory = factory;
		
		this.lastAppliedState = null;
		this.queuedState = null;
		this.queuedInfluence = -1f;
	}
	
	/** Make the state changes to the given gl necessary. 
	 * This will be called with the queued state, or by the default
	 * state supplied in the constructor. */
	protected abstract void apply(GL gl, JoglStateRecord record, T nextState);
	
	/** Perform actions to satify the requirements of restoreDefaults(). */
	protected abstract void restore(GL gl, JoglStateRecord record);
	
	@Override
	public void restoreDefaults() {
		this.restore(this.factory.getGL(), this.factory.getRecord());
		this.lastAppliedState = null;
		this.reset();
	}
	
	@Override
	public void doApply() {
		if (this.queuedState != null) {
			if (this.queuedState != this.lastAppliedState)
				this.apply(this.factory.getGL(), this.factory.getRecord(), this.queuedState);
		} else if (this.lastAppliedState != null)
			this.apply(this.factory.getGL(), this.factory.getRecord(), this.defaultState);
		this.lastAppliedState = this.queuedState;
		
		this.reset();
	}

	@Override
	public void queueAppearanceState(State state) throws RenderException {
		// will only update queuedState if no influence atom was applied before
		this.queueInfluenceState(state, 0f);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void queueInfluenceState(State state, float influence) throws RenderException {
		if (!this.stateClass.isInstance(state))
			throw new RenderException("Unsupported state type: " + state + ", expected an instance of " + this.stateClass);
		
		if (influence >= this.queuedInfluence) {
			this.queuedState = (T) state;
			this.queuedInfluence = influence;
		}
	}

	@Override
	public void reset() {
		this.queuedState = null;
		this.queuedInfluence = -1f;
	}
}
