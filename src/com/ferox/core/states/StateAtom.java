package com.ferox.core.states;

import com.ferox.core.renderer.RenderContext;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.tasks.SilentTask;
import com.ferox.core.util.FeroxException;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public abstract class StateAtom implements Chunkable {	
	
	public static abstract class StateRecord {
		private int validContextVersion;
		private RenderContext context;
		private boolean needsRefresh;
		private boolean cleaned;
		private StateAtomPeer peer;
	}
	
	public static class AtomUpdater extends SilentTask {
		private StateAtom atom;
		private StateRecord rec;
		
		public AtomUpdater(StateAtom atom, StateRecord r) {
			this.atom = atom;
			this.rec = r;
		}
		
		public void performTask() {
			if (this.rec.needsRefresh && this.rec.validContextVersion == this.rec.context.getContextVersion()
				&& !this.rec.cleaned) {
				// we can still perform an update
				this.rec.peer.updateStateAtom(this.atom, this.rec);
				this.rec.needsRefresh = false;
			}
		}
	}
	
	public static class AtomCleaner extends SilentTask {
		private StateRecord rec;
		
		public AtomCleaner(StateRecord r) {
			this.rec = r;
		}
		
		public void performTask() {
			if (this.rec.validContextVersion == this.rec.context.getContextVersion()) {
				this.rec.peer.cleanupStateAtom(this.rec);
				this.rec.cleaned = true;
			}
		}
	}
	
	private StateRecord[] boundResults;
	private int dynamicType;
	
	public StateAtom() {
		this.dynamicType = RenderContext.registerStateAtomType(this.getAtomType());
	}
	
	public int getDynamicType() {
		return this.dynamicType;
	}
	
	public boolean needsRefresh(RenderManager manager) {
		if (this.boundResults != null) {
			if (manager.getManagerID() < this.boundResults.length) {
				if (this.boundResults[manager.getManagerID()] != null) {
					return this.boundResults[manager.getManagerID()].needsRefresh;
				} else
					return false;
			} else
				return false;
		} else
			return false;
	}
	
	public StateRecord getStateRecord(RenderManager manager) {
		if (this.boundResults == null || manager.getManagerID() >= this.boundResults.length)
			return null;
		else
			return this.boundResults[manager.getManagerID()];
	}
	
	public void applyState(RenderManager manager, StateUnit unit) {
		if (!this.isValidUnit(unit))
			throw new IllegalArgumentException("Invalid unit");
		this.applyState(manager, manager.getRenderContext().getStateAtomPeer(this), unit);
	}
	
	private void applyState(RenderManager manager, StateAtomPeer peer, StateUnit unit) {
		//System.out.println("apply state" + this.getClass().getSimpleName());
		RenderContext c = manager.getRenderContext();
		StateAtom prev = c.getActiveStateAtom(this.dynamicType, unit.ordinal());
		if (prev != this) {
			StateRecord rec = this.getStateRecord(manager);
			if (rec == null || rec.validContextVersion < c.getContextVersion() || rec.cleaned) {
				this.initializeNow(manager);
				rec = this.getStateRecord(manager);
			} else if (rec.needsRefresh)
				this.updateNow(manager);
			
			peer.setUnit(unit);
			if (prev == null) 
				peer.applyState(null, null, this, rec);
			else 
				peer.applyState(prev, prev.boundResults[manager.getManagerID()], this, rec);
			c.setActiveStateAtom(this, this.dynamicType, unit.ordinal());
		}
	}
	
	public void restoreState(RenderManager manager, StateUnit unit) {
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
	
	@Override
	protected void finalize() {
		if (this.boundResults != null) {
			for (int i = 0; i < this.boundResults.length; i++) {
				this.boundResults[i].context.getRenderManager().attachTask(new AtomCleaner(this.boundResults[i]), RenderManager.AP_END_FRAME);
			}
			this.boundResults = null;
		}
	}

	public void cleanupStateAtom() {
		this.finalize();
	}
	
	public void cleanupStateAtom(RenderManager manager) {
		StateRecord s = this.getStateRecord(manager);
		if (s != null) {
			manager.attachTask(new AtomCleaner(s), RenderManager.AP_END_FRAME);
			this.boundResults[manager.getManagerID()] = null;
		}
	}
	
	public void updateStateAtom(RenderManager manager) {
		StateRecord s = this.getStateRecord(manager);
		if (s != null) {
			s.needsRefresh = true;
			manager.attachTask(new AtomUpdater(this, s), RenderManager.AP_START_FRAME);
		}
	}
	
	public void updateStateAtom() {
		if (this.boundResults != null)
			for (int i = 0; i < this.boundResults.length; i++)
				if (this.boundResults[i] != null && !this.boundResults[i].needsRefresh) {
					this.boundResults[i].needsRefresh = true;
					this.boundResults[i].context.getRenderManager().attachTask(new AtomUpdater(this, this.boundResults[i]), RenderManager.AP_START_FRAME);
				}
	}
	
	public void updateNow(RenderManager manager) {
		if (!manager.getRenderContext().isCurrent())
			throw new FeroxException("Context must be current for this method");
		StateRecord s = this.getStateRecord(manager);
		if (s != null) {
			s.needsRefresh = true;
			new AtomUpdater(this, s).performTask();
		}
	}
	
	public void cleanupNow(RenderManager manager) {
		if (!manager.getRenderContext().isCurrent())
			throw new FeroxException("Context must be current for this method");
		StateRecord s = this.getStateRecord(manager);
		if (s != null) {
			new AtomCleaner(s).performTask();
			this.boundResults[manager.getManagerID()] = null;
		}
	}
	
	public void initializeNow(RenderManager manager) {
		if (!manager.getRenderContext().isCurrent())
			throw new FeroxException("Context must be current for this method");
		
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
		if (this.boundResults[id] != null)
			this.cleanupNow(manager);
		StateRecord res = peer.initializeStateAtom(this);
			
		res.context = context;
		res.validContextVersion = context.getContextVersion();
		res.peer = peer;
		this.boundResults[id] = res;
	}
	
	public abstract Class<? extends StateAtom> getAtomType();
	
	public abstract boolean isValidUnit(StateUnit unit);
	
	public void readChunk(InputChunk in) {
		
	}
	
	public void writeChunk(OutputChunk out) {
		
	}
}
