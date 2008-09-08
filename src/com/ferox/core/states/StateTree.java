package com.ferox.core.states;

import com.ferox.core.renderer.Updatable;
import com.ferox.core.util.FeroxException;



/**
 * A StateTree is a hierarchy used to organize the states applied to each RenderAtom.
 * @author Michael Ludwig
 *
 */
public class StateTree implements Updatable {
	private StateBranch rootNode;
	private boolean invalidated;
	private StateBin queue;
	
	/**
	 * Creates an empty StateTree.
	 */
	public StateTree() {
		this(null);
	}
	
	/**
	 * Creates a StateTree with the given root StateBranch.
	 */
	public StateTree(StateBranch root) {
		this.setRootNode(root);
		this.queue = new StateBin(8);
	}
	
	/**
	 * Get the root node for this StateTree.
	 */
	public StateBranch getRootNode() {
		return this.rootNode;
	}

	/**
	 * Sets the root node for this StateTree.  If the given root node is already in a tree, it
	 * is removed first.
	 */
	public void setRootNode(StateBranch rootNode) {
		if (this.rootNode != null)
			this.rootNode.setStateTree(null);
		this.rootNode = rootNode;
		if (this.rootNode != null) {
			this.rootNode.setStateTree(this);
			this.invalidateStateTree();
		}
	}
	
	public StateBin getStateBin() {
		return this.queue;
	}
	
	/**
	 * Mark this tree is invalid so that all render atoms linked by leafs in this tree will have their
	 * states recomputed and cached.
	 */
	public void invalidateStateTree() {
		this.invalidated = true;
	}
	
	/**
	 * Prepares the all of the RenderAtom's state usages and sorts them so that actual state sorting
	 * per frame is quicker.  Does nothing if the tree has not been modified.  Should not be called
	 * directly, used by the RenderPass to update the state sorting information.
	 */
	public void update() {
		if (this.invalidated) {
			this.queue.clear();
			if (this.rootNode.getParent() != null)
				throw new FeroxException("Root node of a tree can't have a parent");
			if (this.rootNode != null)
				this.rootNode.submit();
			this.queue.optimize();
			this.invalidated = false;
		}
	}
}
