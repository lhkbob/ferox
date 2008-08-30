package com.ferox.core.states.manager;

import com.ferox.core.states.StateAtom;
import com.ferox.core.states.atoms.AlphaState;

public class AlphaStateManager extends UniqueStateManager<AlphaState> {
	public AlphaStateManager() {
		super();
	}
	
	public AlphaStateManager(AlphaState state) {
		super(state);
	}

	@Override
	public Class<? extends StateAtom> getAtomType() {
		return AlphaState.class;
	}
}
