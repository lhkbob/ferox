package com.ferox.core.states.manager;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.renderer.RenderPass;
import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateManager;
import com.ferox.core.util.FeroxException;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public abstract class UniqueStateManager<S extends StateAtom> extends StateManager {
	private S state;
	
	public UniqueStateManager() {
		this(null);
	}
	
	public UniqueStateManager(S state) {
		super();
		this.setStateAtom(state);
	}
	
	public S getStateAtom() {
		return this.state;
	}
	
	public void setStateAtom(S atom) {
		this.state = atom;
		this.invalidateAssociatedStateTrees();
	}
	
	@Override
	protected void applyStateAtoms(StateManager previous, RenderManager manager, RenderPass pass) {
		S prev = null;
		UniqueStateManager<S> pM = (UniqueStateManager<S>)previous;
		if (previous != null)
			prev = pM.state;
		if (previous == this || prev == this.state)
			return;
		if (this.state != null) {
			manager.getRenderContext().getStateAtomPeer(this.state).prepareManager(this, previous);
			pass.applyState(manager, this.state, this.state.getAtomType(), NullUnit.get());
		} else if (prev != null) {
			pM.restoreStateAtoms(manager, pass);
		}
	}
	
	public StateManager merge(StateManager manager) throws FeroxException {
		UniqueStateManager<S> pM = (UniqueStateManager<S>)manager;
		
		switch(this.getMergeMode()) {
		case HIGHER:
			if (pM.state != null)
				return pM;
			return this;
		case LOWER:
			if (this.state == null)
				return pM;
			return this;
		case REPLACE:
			return this;
		default:
			throw new FeroxException("Illegal merge mode set for StateManager");
		}
	}

	@Override
	protected void restoreStateAtoms(RenderManager manager, RenderPass pass) {
		if (this.state != null) {
			pass.applyState(manager, null, this.state.getAtomType(), NullUnit.get());
			manager.getRenderContext().getStateAtomPeer(this.state).disableManager(this);
		}
	}
	
	@Override
	public int getSortingIdentifier() {
		if (this.state == null)
			return 0;
		else
			return this.state.hashCode();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.state = (S)in.getObject("state");
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.setObject("state", this.state);
	}
}
