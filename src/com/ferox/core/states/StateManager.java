package com.ferox.core.states;

import java.util.Iterator;
import java.util.WeakHashMap;

import com.ferox.core.renderer.RenderContext;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.renderer.RenderPass;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public abstract class StateManager implements Chunkable {		
	public static final int NUM_CORE_STATES = 15;
	public static enum MergeMode {
		LOWER, HIGHER, REPLACE
	}
	
	private MergeMode mergeMode;
	private WeakHashMap<StateNode, Boolean> links;
	private int dynamicType;
	
	public StateManager() {
		this.dynamicType = RenderContext.registerStateAtomType(this.getAtomType());
		this.links = new WeakHashMap<StateNode, Boolean>();
		this.mergeMode = MergeMode.LOWER;
	}
	
	public void apply(RenderManager manager, RenderPass pass) {
		StateManager prev = manager.getRenderContext().getActiveStateManager(this.dynamicType);
		
		if (prev != this) {
			manager.getRenderContext().setActiveStateManager(this, this.dynamicType);
			this.applyStateAtoms(prev, manager, pass);
		}
	}
	
	public void restore(RenderManager manager, RenderPass pass) {
		if (manager.getRenderContext().getActiveStateManager(this.dynamicType) == this) {
			this.restoreStateAtoms(manager, pass);
			manager.getRenderContext().setActiveStateManager(null, this.dynamicType);
		}
	}
	
	public void update() {
		// do nothing
	}
	
	public void setMergeMode(MergeMode mode) {
		if (mode == null)
			throw new NullPointerException("Merge mode can't be null");
		this.mergeMode = mode;
	}
	
	public MergeMode getMergeMode() {
		return this.mergeMode;
	}
	
	void addStateNode(StateNode node) {
		this.links.put(node, true);
	}
	
	void removeStateNode(StateNode node) {
		this.links.remove(node);
	}
	
	public void invalidateAssociatedStateTrees() {
		Iterator<StateNode> it = this.links.keySet().iterator();
		while (it.hasNext())
			it.next().invalidate();
	}
	
	public final int getDynamicType() {
		return this.dynamicType;
	}
	
	public void readChunk(InputChunk in) {
		
	}
	
	public void writeChunk(OutputChunk out) {
		
	}
	
	public abstract StateManager merge(StateManager manager);
	protected abstract void applyStateAtoms(StateManager previous, RenderManager manager, RenderPass pass);
	protected abstract void restoreStateAtoms(RenderManager manager, RenderPass pass);
	public abstract Class<? extends StateAtom> getAtomType();
	public abstract int getSortingIdentifier();
}
