package com.ferox.core.states.manager;

import com.ferox.core.states.StateAtom;
import com.ferox.core.states.atoms.StencilState;

public class StencilStateManager extends UniqueStateManager<StencilState> {
	
	public StencilStateManager() {
		this(null);
	}
	
	public StencilStateManager(StencilState state) {
		super(state);
	}
	
	@Override
	public Class<? extends StateAtom> getAtomType() {
		return StencilState.class;
	}
}
