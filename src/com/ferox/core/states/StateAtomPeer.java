package com.ferox.core.states;

import com.ferox.core.states.StateAtom.StateRecord;

public interface StateAtomPeer {
	public void validateStateAtom(StateAtom atom) throws StateUpdateException;
	public StateRecord initializeStateAtom(StateAtom atom);
	public void updateStateAtom(StateAtom atom, StateRecord record);
	public void cleanupStateAtom(StateRecord record);
	public void applyState(StateAtom prevA, StateRecord prevR, StateAtom nextA, StateRecord nextR);
	public void restoreState(StateAtom cleanA, StateRecord cleanR);
	public void setUnit(StateUnit unit);
	public void prepareManager(StateManager manager, StateManager previous);
	public void disableManager(StateManager manager);
}
