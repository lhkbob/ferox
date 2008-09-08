package com.ferox.core.states;

import java.util.ArrayList;

import com.ferox.core.renderer.RenderContext;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public abstract class StateAtom implements Chunkable {	
	
	public static abstract class StateRecord {
		private int validContextVersion;
	}
	
	private StateRecord[] boundResults = null;
	private int dynamicType = -1;
	
	public StateAtom() { }
	
	public final int getDynamicType() {
		if (this.dynamicType < 0)
			this.dynamicType = RenderContext.registerStateAtomType(this.getAtomType());
		return this.dynamicType;
	}
	
	public StateRecord getStateRecord(RenderManager manager) {
		if (this.boundResults == null || manager.getManagerID() >= this.boundResults.length)
			return null;
		else
			return this.boundResults[manager.getManagerID()];
	}
	
	public void applyState(RenderManager manager, StateUnit unit) throws IllegalArgumentException {
		if (!this.isValidUnit(unit))
			throw new IllegalArgumentException("Invalid unit");
		this.applyState(manager, manager.getRenderContext().getStateAtomPeer(this), unit);
	}
	
	private void applyState(RenderManager manager, StateAtomPeer peer, StateUnit unit) {
		RenderContext c = manager.getRenderContext();
		StateAtom prev = c.getActiveStateAtom(this.dynamicType, unit.ordinal());
		if (prev != this) {
			StateRecord rec = this.getStateRecord(manager);
			if (rec == null || rec.validContextVersion < c.getContextVersion()) {
				this.update(manager);
				rec = this.getStateRecord(manager);
			}
			
			if (rec == null) {
				if (prev != null)
					prev.restoreState(manager, unit);
			} else {
				peer.setUnit(unit);
				if (prev == null) 
					peer.applyState(null, null, this, rec);
				else 
					peer.applyState(prev, prev.boundResults[manager.getManagerID()], this, rec);
				c.setActiveStateAtom(this, this.dynamicType, unit.ordinal());
			}
		}
	}
	
	public void restoreState(RenderManager manager, StateUnit unit) throws IllegalArgumentException {
		if (!this.isValidUnit(unit))
			throw new IllegalArgumentException("Invalid unit");
		this.restoreState(manager, manager.getRenderContext().getStateAtomPeer(this), unit);
	}
	
	private void restoreState(RenderManager manager, StateAtomPeer peer, StateUnit unit) {
		RenderContext c = manager.getRenderContext();
		if (c.getActiveStateAtom(this.dynamicType, unit.ordinal()) == this) {
			peer.setUnit(unit);
			peer.restoreState(this, this.boundResults[manager.getManagerID()]);
			c.setActiveStateAtom(null, this.dynamicType, unit.ordinal());
		}
	}

	public void update(RenderManager manager) {
		if (manager.getRenderContext().isCurrent()) {
			int id = manager.getManagerID();
			RenderContext context = manager.getRenderContext();
			StateAtomPeer peer = context.getStateAtomPeer(this);
			
			if (this.boundResults == null)
				this.boundResults = new StateRecord[id + 1];
			else if (id >= this.boundResults.length) {
				StateRecord[] temp = new StateRecord[id + 1];
				System.arraycopy(this.boundResults, 0, temp, 0, this.boundResults.length);
				this.boundResults = temp;
			}
			try {
				peer.validateStateAtom(this);
			} catch (StateUpdateException sue) {
				System.err.println("WARNING: StateAtom (" + this.getAtomType().getSimpleName() + ") failed to update");
				throw sue;
			}
			if (this.boundResults[id] == null || this.boundResults[id].validContextVersion < context.getContextVersion()) {
				StateRecord res = peer.initializeStateAtom(this);
				res.validContextVersion = context.getContextVersion();
				this.boundResults[id] = res;
			} else {
				peer.updateStateAtom(this, this.boundResults[id]);
			}
			manager.getStateAtomResourceHandler().unregisterUpdate(this);
		} else {
			manager.getStateAtomResourceHandler().registerUpdate(this);
		}
	}
	
	public void destroy(RenderManager manager) {
		if (manager.getRenderContext().isCurrent()) {
			int id = manager.getManagerID();
			RenderContext context = manager.getRenderContext();
			StateAtomPeer peer = context.getStateAtomPeer(this);
			
			StateRecord res = this.getStateRecord(manager);
			if (res != null && res.validContextVersion == context.getContextVersion())
				peer.cleanupStateAtom(res);
			this.boundResults[id] = null;
			manager.getStateAtomResourceHandler().unregisterDestroy(this);
		} else {
			manager.getStateAtomResourceHandler().registerDestroy(this);
		}
	}
	
	public void update() {
		ArrayList<RenderManager> mans = RenderManager.getRenderManagers();
		for (int i = 0; i < mans.size(); i++)
			this.update(mans.get(i));
	}
	
	public void destroy() {
		ArrayList<RenderManager> mans = RenderManager.getRenderManagers();
		for (int i = 0; i < mans.size(); i++)
			this.destroy(mans.get(i));
	}
	
	public abstract Class<? extends StateAtom> getAtomType();
	
	public abstract boolean isValidUnit(StateUnit unit);
	
	public void readChunk(InputChunk in) {
		
	}
	
	public void writeChunk(OutputChunk out) {
		
	}
}
