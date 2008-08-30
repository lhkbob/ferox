package com.ferox.core.scene;

import com.ferox.core.renderer.RenderAtomBin;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.renderer.Updatable;


/**
 * A SpatialTree is a spatial hierarchy used to efficiently view cull RenderAtoms from the scene each frame.
 * The various types of trees can be made by extending this and automating the generation of the intermediate
 * SpatialBranches and Leaves.  By using different types of Culler's, the most important BVH's can be 
 * organized without needing to change the Tree's actual visibility culling algorithm.
 * @author Michael Ludwig
 *
 */
public class SpatialTree implements Updatable {
	private SpatialBranch rootNode;
	private RenderAtomBin queue;
	
	/**
	 * Creates an empty SpatialTree.
	 */
	public SpatialTree() {
		this(null);
	}
	
	/**
	 * Creates a SpatialTree with the given root SpatialBranch.
	 */
	public SpatialTree(SpatialBranch root) {
		this.setRootNode(root);
		this.queue = new RenderAtomBin(10);
	}
	
	/**
	 * Get the root node for this SpatialTree.
	 */
	public SpatialBranch getRootNode() {
		return this.rootNode;
	}

	/**
	 * Sets the root node for this SpatialTree.  If the given root node is already in a tree, it
	 * is removed first.
	 */
	public void setRootNode(SpatialBranch rootNode) {
		this.rootNode = rootNode;
	}
	
	public RenderAtomBin getRenderAtomBin() {
		return this.queue;
	}
	
	/**
	 * Prepares the spatial tree's RenderQueue.  After this is called, it is valid to grab the 
	 * RenderQueue and render each RenderAtom present in it.  Used internally, don't call by directly
	 * (unless via super.prepareSpatialTree()).
	 */
	public void update() {
		if (this.rootNode == null)
			return;
		this.rootNode.detach();
		this.rootNode.update(true);
	}
	
	public void submit(View view, RenderManager manager) {
		this.queue.clear();
		this.rootNode.submit(view, manager, this.queue, true);
		this.queue.optimize();
	}
}
