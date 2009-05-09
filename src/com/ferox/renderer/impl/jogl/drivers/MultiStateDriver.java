package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.effect.Effect;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.EffectDriver;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/**
 * The MultiStateDriver is suitable for state types that support more than one
 * active state. Each of these units have no effective change on the output if
 * the states per unit were somehow re-ordered (e.g. a light on unit 0 has the
 * same effect as if it were on unit 1). Multi-texturing is an example of a type
 * with multiple units, but where the order actually matters, and hence is not a
 * MultiStateDriver.
 * 
 * A MultiStateDriver assumes that its type are described by 1 type (or
 * subclasses of that class type).
 * 
 * @author Michael Ludwig
 * 
 */
public abstract class MultiStateDriver<T extends Effect> implements EffectDriver {
	/* Simple class to hold the union of a state and its influence/priority. */
	private static class Unit<T extends Effect> {
		T state;
		float priority;
	}

	// Priority of a Unit that is marked as 'empty'
	private static final float INVALID_PRIORTY = -1f;

	// Priority of a Unit that isn't 'empty', but has the same unit on it
	private static final float NOCHNGE_PRIORITY = -2f;

	private final Unit<T>[] queue; // array, maintained in decreasing order by
	// priority
	private int queueSize;

	private final Unit<T>[] apply; // stores the last-applied and next states
	// (last-applied has priority < 0).

	private final T dfltState;
	protected final JoglContextManager factory;
	private final Class<? extends Effect> stateType;

	/**
	 * Configure the driver for the given state type and unit count. The default
	 * state may be null or not. maxUnits configures how many simultaneous
	 * states can be active at one point. The algorithms used within assume that
	 * maxUnits will be of a reasonable (e.g. 1-16).
	 */
	@SuppressWarnings("unchecked")
	protected MultiStateDriver(T dfltState, Class<T> stateType, int maxUnits,
			JoglContextManager factory) {
		this.factory = factory;
		this.dfltState = dfltState;
		this.stateType = stateType;

		this.queue = new Unit[maxUnits];
		this.apply = new Unit[maxUnits];

		// Fill the arrays with storage units so we won't be
		// constantly creating them later
		for (int i = 0; i < maxUnits; i++) {
			this.queue[i] = new Unit<T>();
			this.queue[i].priority = INVALID_PRIORTY;
			this.apply[i] = new Unit<T>();
			this.apply[i].priority = INVALID_PRIORTY;
		}

		this.queueSize = 0;
	}

	protected abstract void apply(GL gl, JoglStateRecord record, int unit,
			T next);

	@Override
	public void doApply() {
		int i;
		int j;
		int maxUnits = this.apply.length;

		Unit<T> q, o;
		if (this.queueSize > 0) {
			// transfer queued states over to apply[]
			for (i = 0; i < this.queueSize; i++) {
				q = this.queue[i];
				// search for existing unit position
				for (j = 0; j < maxUnits; j++) {
					o = this.apply[j];
					if (o.priority == INVALID_PRIORTY && q.state == o.state) {
						// flag apply position as "no change needed"
						o.priority = NOCHNGE_PRIORITY;
						// reset queue position
						q.state = null;
						q.priority = INVALID_PRIORTY;
						break;
					}
				}
			}
			// add any remaining queued states to the apply array
			for (i = 0; i < this.queueSize; i++) {
				q = this.queue[i];
				if (q.state != null)
					// search for an empty unit for this state
					for (j = 0; j < maxUnits; j++) {
						o = this.apply[j];
						if (o.priority == INVALID_PRIORTY) { // only choose unit
							// that hasn't
							// been updated
							// yet
							// add new state to the apply array
							o.state = q.state;
							o.priority = q.priority; // will be in [0, 1]

							// reset queue position
							q.state = null;
							q.priority = INVALID_PRIORTY;
							break;
						}
					}
			}
			this.queueSize = 0; // queue will be empty now
		}

		// apply states with priority >= 0, restore others
		JoglStateRecord record = this.factory.getRecord();
		GL gl = this.factory.getGL();
		for (i = 0; i < maxUnits; i++) {
			o = this.apply[i];
			if (o.priority != INVALID_PRIORTY) {
				if (o.priority != NOCHNGE_PRIORITY)
					this.apply(gl, record, i, o.state); // apply new state
				// else ... no change on unit, so just mark it as invalid again
				o.priority = INVALID_PRIORTY;
			} else if (o.state != null) {
				// no more state on unit, so apply the "default"
				this.apply(gl, record, i, this.dfltState);
				o.state = null;
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void queueEffect(Effect state)
			throws RenderException {
		if (!this.stateType.isInstance(state))
			throw new RenderException("Unsupported state type: " + state
					+ ", expected an instance of " + this.stateType);

		Unit<T> u;
		for (int i = 0; i < this.queueSize; i++) {
			u = this.queue[i];
			if (influence >= u.priority) {
				// found the position, shift everything over
				this.queueSize = Math
						.min(this.queueSize + 1, this.queue.length);
				for (int j = this.queueSize - 1; j >= i + 1; j--) {
					this.queue[j].state = this.queue[j - 1].state;
					this.queue[j].priority = this.queue[j - 1].priority;
				}

				// insert new state
				u.state = (T) state;
				u.priority = influence;
				return; // we've queued it successfully
			}
		}

		// if we've gotten here, we couldn't find an insert position
		if (this.queueSize < this.queue.length) {
			// we have room to add at the end
			this.queue[this.queueSize].state = (T) state;
			this.queue[this.queueSize].priority = influence;
			this.queueSize++;
		} // else ignore the state, it doesn't have enough influence
	}

	@Override
	public void reset() {
		// clear the queue, everything >= queueSize should be invalid already
		for (int i = 0; i < this.queueSize; i++) {
			this.queue[i].state = null;
			this.queue[i].priority = INVALID_PRIORTY;
		}
		this.queueSize = 0;
	}
}
