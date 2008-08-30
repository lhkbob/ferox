package com.ferox.core.states;


/**
 * A StateLeaf represents a node in the StateTree that has no children.  It does however link to 
 * a RenderAtom that has all of the states linked from this leaf to the root node applied to it when it 
 * is rendered.  A StateLeaf can only have one linked RenderAtom (unlike the possible unlimited amounts
 * in SpatialLeaf) which means that if you're using a switch-like spatial leaf, you'll have to have 
 * multiple leafs in the state tree.
 * @author Michael Ludwig
 *
 */
public class StateLeaf extends StateNode {
	int sortIndex;
	
	/**
	 * Create a StateLeaf with no current parent.
	 */
	public StateLeaf() {
		super();
	}
	
	/**
	 * Create a StateLeaf with the given parent.
	 */
	public StateLeaf(StateBranch parent) {
		super(parent);
	}
	
	public StateManager[] getMergedStates() {
		boolean inv = this.isInvalidated();
		StateManager[] merged = super.getMergedStates();
		if (inv) {
			for (int i = 0; i < merged.length; i++)
				merged[i].update();
		}
		return merged;
	}
	
	public void submit() {
		super.submit();
		this.getStateTree().getStateBin().add(this);
	}
	
	public int getSortIndex() {
		return this.sortIndex;
	}
}
