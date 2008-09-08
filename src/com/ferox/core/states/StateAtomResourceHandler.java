package com.ferox.core.states;

import java.util.ArrayList;
import com.ferox.core.renderer.RenderManager;

public class StateAtomResourceHandler {
	private ArrayList<StateAtom> updates;
	private ArrayList<StateAtom> cleanups;
	
	public StateAtomResourceHandler() {
		this.updates = new ArrayList<StateAtom>();
		this.cleanups = new ArrayList<StateAtom>();
	}
	
	public void registerUpdate(StateAtom atom) {
		if (!this.updates.contains(atom))
			this.updates.add(atom);
	}
	
	public void unregisterUpdate(StateAtom atom) {
		this.updates.remove(atom);
	}
	
	public void registerDestroy(StateAtom atom) {
		if (!this.cleanups.contains(atom))
			this.cleanups.add(atom);
	}
	
	public void unregisterDestroy(StateAtom atom) {
		this.cleanups.remove(atom);
	}
	
	public void doUpdates(RenderManager manager) {
		int s = this.updates.size();
		for (int i = s - 1; i >= 0; i--)
			this.updates.get(i).update(manager);
		this.updates.clear();
	}
	
	public void doDestroys(RenderManager manager) {
		int s = this.cleanups.size();
		for (int i = s - 1; i >= 0; i--)
			this.cleanups.get(i).destroy(manager);
		this.cleanups.clear();
	}
}
