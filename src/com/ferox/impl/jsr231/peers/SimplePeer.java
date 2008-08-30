package com.ferox.impl.jsr231.peers;

import javax.media.opengl.GL;

import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateAtomPeer;
import com.ferox.core.states.StateManager;
import com.ferox.core.states.StateUnit;
import com.ferox.core.states.StateAtom.StateRecord;
import com.ferox.impl.jsr231.JOGLRenderContext;

abstract class SimplePeer<S extends StateAtom, R extends StateRecord> implements StateAtomPeer {
	protected JOGLRenderContext context;
	
	public SimplePeer(JOGLRenderContext context) {
		this.context = context;
	}
	
	public void applyState(StateAtom prevA, StateRecord prevR, StateAtom nextA, StateRecord nextR) {
		this.applyState((S)prevA, (R)prevR, (S)nextA, (R)nextR, this.context.getGL());
	}

	public void cleanupStateAtom(StateRecord record) {
		// do nothing
	}

	public void disableManager(StateManager manager) {
		// do nothing
	}

	public StateRecord initializeStateAtom(StateAtom atom) {
		return new NoRecord();
	}

	public void prepareManager(StateManager manager, StateManager previous) {
		// do nothing
	}

	public void restoreState(StateAtom cleanA, StateRecord cleanR) {
		this.restoreState((S)cleanA, (R)cleanR, this.context.getGL());
	}

	public void setUnit(StateUnit unit) {
		// do nothing
	}

	public void updateStateAtom(StateAtom atom, StateRecord record) {
		// do nothing
	}

	protected abstract void applyState(S prevA, R prevR, S nextA, R nextR, GL gl);
	protected abstract void restoreState(S cleanA, R cleanR, GL gl);
}
