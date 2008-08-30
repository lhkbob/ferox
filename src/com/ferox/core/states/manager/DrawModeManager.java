package com.ferox.core.states.manager;

import com.ferox.core.states.StateAtom;
import com.ferox.core.states.atoms.DrawMode;

public class DrawModeManager extends UniqueStateManager<DrawMode> {
	public DrawModeManager() {
		super();
	}
	
	public DrawModeManager(DrawMode state) {
		super(state);
	}

	@Override
	public Class<? extends StateAtom> getAtomType() {
		return DrawMode.class;
	}
}
