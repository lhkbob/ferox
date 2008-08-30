package com.ferox.core.states.manager;

import com.ferox.core.states.StateAtom;
import com.ferox.core.states.atoms.ZBuffer;

public class ZBufferManager extends UniqueStateManager<ZBuffer> {
	public ZBufferManager() {
		super();
	}
	
	public ZBufferManager(ZBuffer state) {
		super(state);
	}

	@Override
	public Class<? extends StateAtom> getAtomType() {
		return ZBuffer.class;
	}
}
