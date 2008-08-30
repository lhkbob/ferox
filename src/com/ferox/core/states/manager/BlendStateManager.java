package com.ferox.core.states.manager;

import com.ferox.core.states.StateAtom;
import com.ferox.core.states.atoms.BlendState;

public class BlendStateManager extends UniqueStateManager<BlendState> {
	public BlendStateManager() {
		super();
	}
	
	public BlendStateManager(BlendState state) {
		super(state);
	}

	@Override
	public Class<? extends StateAtom> getAtomType() {
		return BlendState.class;
	}
}
