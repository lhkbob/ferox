package com.ferox.core.states.manager;

import com.ferox.core.states.StateAtom;
import com.ferox.core.states.atoms.Material;

public class MaterialManager extends UniqueStateManager<Material> {
	public MaterialManager() {
		super();
	}
	
	public MaterialManager(Material state) {
		super(state);
	}
	
	@Override
	public Class<? extends StateAtom> getAtomType() {
		return Material.class;
	}
}
