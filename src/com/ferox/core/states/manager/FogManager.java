package com.ferox.core.states.manager;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.renderer.RenderPass;
import com.ferox.core.scene.states.Fog;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateManager;
import com.ferox.core.util.FeroxException;

public class FogManager extends StateManager {

	@Override
	protected void applyStateAtoms(StateManager previous, RenderManager manager, RenderPass pass) {
		manager.getRenderContext().getStateAtomPeer(Fog.class).prepareManager(this, previous);
	}

	@Override
	public Class<? extends StateAtom> getAtomType() {
		return Fog.class;
	}

	@Override
	public int getSortingIdentifier() {
		return Fog.class.hashCode();
	}

	@Override
	public StateManager merge(StateManager manager) throws FeroxException {
		FogManager man = (FogManager)manager;
		
		switch(this.getMergeMode()) {
		case HIGHER: 
			return man;
		case LOWER: 
			return this;
		case REPLACE:
			return this;
		default:
			throw new FeroxException("Illegal merge mode set in light manager");		
		}
	}

	@Override
	protected void restoreStateAtoms(RenderManager manager, RenderPass pass) {
		manager.getRenderContext().getStateAtomPeer(Fog.class).disableManager(this);
	}

}
