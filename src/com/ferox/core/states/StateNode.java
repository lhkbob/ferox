package com.ferox.core.states;

import com.ferox.core.renderer.RenderContext;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

/**
 * StateNode represents an abstract element in a StateTree.  It can have an arbitrary number of StateAtom's
 * linked with it.  StateAtoms can be present in multiple state trees and even in the same tree with one
 * major caveat: linking a state that would result in a render atom having duplicates of the same state 
 * atom object will throw an exception.
 * @author Michael Ludwig
 *
 */
public abstract class StateNode implements Chunkable {
	private StateManager[] states;
	private StateManager[] merged;
	
	private int numStates;
	private StateBranch parent;
	
	private StateTree tree;
	private boolean invalidated;
	
	/**
	 * Creates a new StateNode with no parent, same as StateNode(null).
	 */
	public StateNode() {
		this(null);
	}
	
	/**
	 * Creates a new StateNode with the given parent (if parent is not null).
	 */
	public StateNode(StateBranch parent) {
		if (parent != null)
			parent.add(this);
		this.numStates = 0;
		this.states = new StateManager[0];
		this.merged = new StateManager[0];
	}
	
	/**
	 * Attaches the given StateManager to this node, if a manager with the same int type is already
	 * attached, that manager is replaced (ie only one manager of each type can be attached to a state
	 * node).  A manager can be attached to multiple nodes.
	 */
	public void addStateManager(StateManager manager) {
		if (manager != null) {
			int type = manager.getDynamicType();
			if (type >= this.states.length) {
				StateManager[] temp = new StateManager[type + 1];
				System.arraycopy(this.states, 0, temp, 0, this.states.length);
				this.states = temp;
			}
			if (this.states[type] != manager) {
				if (this.states[type] != null)
					this.states[type].removeStateNode(this);
				else
					this.numStates++;
				manager.addStateNode(this);
				this.states[type] = manager;
				this.invalidate();
			}
		}
	}
	
	/**
	 * Detaches the given StateManager from this node.
	 */
	public void removeStateManager(StateManager manager) {
		if (manager != null) {
			int type = manager.getDynamicType();
			if (this.states == null || type >= this.states.length || this.states[type] != manager)
				return;
			manager.removeStateNode(this);
			this.states[type] = null;
			this.invalidate();
			this.numStates--;
		}
	}
	
	/**
	 * Get the number of state managers linked to this state node.  Any children of this node will also
	 * have the state managers applied in a manager specific way
	 */
	public int getNumStateManagers() {
		return this.numStates;
	}
	
	/**
	 * Get this node's state manager for the given type of manager, null means that it doesn't have
	 * a manager for that type attached to it.
	 */
	public StateManager getStateManager(Class<? extends StateAtom> type) {
		int t = RenderContext.registerStateAtomType(type);
		if (this.states == null || t > this.states.length)
			return null;
		return this.states[t];
	}
	
	/**
	 * Detaches the StateNode from its parent if it has one.  If the StateNode has children,
	 * these will also no longer be reachable through the previous parent.  
	 * The children will remain attached to this StateBranch.
	 */
	public void detach() {
		if (this.parent != null)
			this.parent.remove(this);
	}
	
	/**
	 * Get the StateBranch parent for this SpatialNode.
	 */
	public StateBranch getParent() {
		return this.parent;
	}
	
	public StateTree getStateTree() {
		return this.tree;
	}
	
	/**
	 * The tree containing this node as invalid, implying that next frame the render atoms will
	 * have their state atoms re-calculated.
	 */
	public void invalidate() {
		this.invalidated = true;
		if (this.tree != null)
			this.tree.invalidateStateTree();
	}
	
	public boolean isInvalidated() {
		return this.invalidated;
	}
	
	/**
	 * Sets the parent of this StateNode.
	 */
	public void setParent(StateBranch parent) {
		this.detach();
		if (parent != null)
			parent.add(this);
	}
	
	public StateManager[] getMergedStates() {
		this.propagateMergeUp();
		return this.merged;
	}
	
	public void submit() {
		this.propagateMergeUp();
	}
	
	private void propagateMergeUp() {
		StateNode parent = this.parent;
		if (this.invalidated) {
			if (parent != null)
				parent.propagateMergeUp();
			this.mergeFromParent();
			this.invalidated = false;
		}
	}
	
	private void mergeFromParent() {
		StateNode parent = this.parent;
		this.createMergedArray();
		
		if (parent != null) {
			for (int i = 0; i < parent.merged.length; i++) 
				this.merged[i] = parent.merged[i];
		}
		
		StateManager a, b;
		for (int i = 0; i < this.states.length; i++) {
			if (this.states[i] != null) {
				a = this.merged[i];
				b = this.states[i];
				if (a != null) 
					b = b.merge(a);
				this.merged[i] = b;
			}
		}
	}
	
	private void createMergedArray() {
		int max = this.states.length;
		if (this.parent != null)
			max = Math.max(max, ((StateNode)this.parent).merged.length);
		if (this.merged == null || max > this.merged.length) {
			StateManager[] t = new StateManager[max];
			if (this.merged != null)
				System.arraycopy(this.merged, 0, t, 0, this.merged.length);
			this.merged = t;
		}
		for (int i = 0; i < this.merged.length; i++) 
			this.merged[i] = null;
	}
	
	/**
	 * Used by StateBranch in add() and remove(), not to be called directly.
	 */
	protected void setParentReal(StateBranch parent) {
		if (this.parent != null)
			this.parent.invalidate();
		this.parent = parent;
		this.invalidate();
	}
	
	protected void setStateTree(StateTree tree) {
		this.tree = tree;
	}
	
	public void writeChunk(OutputChunk out) {
		if (this.states == null) {
			out.set("states", (Chunkable[])null);
			return;
		}
		int c = 0;
		for (int i = 0; i < this.states.length; i++) {
			if (this.states[i] != null)
				c++;
		}
		Chunkable[] states = new Chunkable[c];
		c = 0;
		for (int i = 0; i < this.states.length; i++) {
			if (this.states[i] != null)
				states[c++] = this.states[i];
		}
		out.set("states", states);
	}
	
	public void readChunk(InputChunk in) {
		Chunkable[] states = in.getChunkArray("states");
		if (states != null) {
			for (int i = 0; i < states.length; i++)
				this.addStateManager((StateManager)states[i]);
		}
	}
}
