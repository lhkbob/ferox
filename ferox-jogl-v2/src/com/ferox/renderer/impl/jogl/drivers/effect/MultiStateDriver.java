package com.ferox.renderer.impl.jogl.drivers.effect;

import javax.media.opengl.GLBase;

import com.ferox.effect.Effect;
import com.ferox.renderer.UnsupportedEffectException;
import com.ferox.renderer.impl.EffectDriver;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/**
 * The MultiStateDriver is suitable for state types that support more than one
 * active state. Each of these units have no effective change on the output if
 * the states per unit were somehow re-ordered (e.g. a light on unit 0 has the
 * same effect as if it were on unit 1). Multi-texturing is an example of a type
 * with multiple units, but where the order actually matters, and hence is not a
 * MultiStateDriver. A MultiStateDriver assumes that its type are described by 1
 * type (or subclasses of that class type).
 * 
 * @author Michael Ludwig
 */
public abstract class MultiStateDriver<T extends Effect, G extends GLBase> implements EffectDriver {
	// State of a Unit that is marked as 'empty'
	private static final int EMPTY = 0;

	// State of a Unit that isn't 'empty', but has the same effect
	private static final int SAME = 1;

	// State of a Unit that isn't empty and has a new effect
	private static final int NEW = 2;

	/* Simple class to hold the union of a state and its influence/priority. */
	private static class Unit<T extends Effect> {
		T effect;
		int state;
	}

	private final T[] queue;
	private int queueSize; // may be larger than queue, access is with %

	// stores the last-applied and next effects
	private final Unit<T>[] apply;

	private final T dfltEffect;
	private final Class<? extends Effect> effectType;

	protected final JoglContextManager factory;

	/**
	 * Configure the driver for the given state type and unit count. The default
	 * state may be null or not. maxUnits configures how many simultaneous
	 * states can be active at one point. The algorithms used within assume that
	 * maxUnits will be of a reasonable (e.g. 1-16).
	 */
	@SuppressWarnings("unchecked")
	protected MultiStateDriver(T dfltState, Class<T> stateType, 
							   int maxUnits, JoglContextManager factory) {
		this.factory = factory;
		this.dfltEffect = dfltState;
		this.effectType = stateType;

		this.queue = (T[]) new Effect[maxUnits];
		this.apply = new Unit[maxUnits];

		// Fill the arrays with storage units so we won't be
		// constantly creating them later
		for (int i = 0; i < maxUnits; i++) {
			this.queue[i] = null;

			this.apply[i] = new Unit<T>();
			this.apply[i].state = EMPTY;
		}

		this.queueSize = 0;
	}

	protected abstract void apply(G gl, JoglStateRecord record, int unit, T next);
	
	protected abstract G convert(GLBase base);

	@Override
	public void doApply() {
		int i, j;
		int maxUnits = this.apply.length;

		Unit<T> o;
		T q;
		if (this.queueSize > 0) {
			queueSize = Math.min(queue.length, queueSize);

			// transfer queued states over to apply[]
			for (i = 0; i < this.queueSize; i++) {
				q = this.queue[i];
				// search for existing unit position
				for (j = 0; j < maxUnits; j++) {
					o = this.apply[j];
					if (o.state == EMPTY && q == o.effect) {
						// flag apply position as "no change needed"
						o.state = SAME;

						// reset queue item
						queue[i] = null;
						break;
					}
				}
			}
			// add any remaining queued states to the apply array
			for (i = 0; i < this.queueSize; i++) {
				q = this.queue[i];
				if (q != null)
					// search for an empty unit for this state
					for (j = 0; j < maxUnits; j++) {
						o = this.apply[j];
						// only choose unit that hasn't been updated
						if (o.state == EMPTY) {
							// add new state to the apply array
							o.state = NEW;
							o.effect = q; // will be in [0, 1]

							// reset queue position
							queue[i] = null;
							break;
						}
					}
			}
			this.queueSize = 0; // queue will be empty now
		}

		// apply states with priority >= 0, restore others
		JoglStateRecord record = this.factory.getRecord();
		G gl = convert(this.factory.getGL());
		for (i = 0; i < maxUnits; i++) {
			o = this.apply[i];
			if (o.state != EMPTY) {
				if (o.state != SAME)
					// apply new state
					this.apply(gl, record, i, o.effect);
				// mark it as invalid again
				o.state = EMPTY;
			} else if (o.effect != null) {
				// no more state on unit, so apply the "default"
				this.apply(gl, record, i, this.dfltEffect);
				o.effect = null;
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void queueEffect(Effect state) {
		if (!this.effectType.isInstance(state))
			throw new UnsupportedEffectException("Unsupported effect type: " + state + 
												 ", expected an instance of " + this.effectType);

		queue[queueSize % queue.length] = (T) state;
		queueSize++;
	}

	@Override
	public void reset() {
		// clear the queue, everything >= queueSize should be invalid already
		for (int i = 0; i < Math.min(queueSize, queue.length); i++)
			this.queue[i] = null;
		this.queueSize = 0;
	}
}
