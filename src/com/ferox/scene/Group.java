package com.ferox.scene;

import com.ferox.math.BoundVolume;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.View;

/**
 * Group represents a collection of nodes within a scene.  It will always be the top-level component 
 * of a scene.  Group is backed by an array, so it provides methods to ensure its capacity, possibly 
 * improving performance during scene creation.
 * 
 * There are two notions of possession for a Group: 
 * 1. owning a Node -> a Node is an immediate/direct child of the Group.
 * 2. containing a Node -> this Group or one of its children owns the node.
 * 
 * Although many of the methods defined here resemble that of a Collection interface, they are strictly based
 * on instance equality (==), never equals().
 * @author Michael Ludwig
 *
 */
public class Group extends Node {
	private static final int ALLOCATION_INCREMENT = 8;
	
	private Node[] children;
	private int size;
	
	/** Creates a Group with an initial capacity of 8. */
	public Group() {
		this(ALLOCATION_INCREMENT);
	}
	
	/** Creates a Group with the given capacity (>= 1, as in ensureCapacity()). */
	public Group(int capacity) {
		super();
		this.ensureCapacity(capacity);
	}
	
	/** Clones this group's first child's bounds and then encloses each remaining cached world
	 * bound of its children.  To get correct results, its children must be updated first. */
	@Override
	public void updateBounds() {		
		BoundVolume world = null;
		for (int i = 0; i < this.size; i++) {
			if (this.children[i].worldBounds != null) {
				if (world == null) {
					world = this.children[i].worldBounds.clone(this.worldBounds);
				} else
					world.enclose(this.children[i].worldBounds);
			}
		}
		this.worldBounds = world;
	}
	
	/** Locks the bounds of its children as well. */
	@Override
	public void lockBounds(boolean lock) {
		if (lock != this.isBoundsLocked()) {
			// must lock children first, so they have correct bounds
			for (int i = 0; i < this.size; i++) {
				this.children[i].lockBounds(lock);
			}
			super.lockBounds(lock);
		}
	}
	
	/** Locks the transforms of its children, too. */
	@Override
	public void lockTransform(boolean lock) {
		if (lock != this.isTransformLocked()) {
			// must lock this transform first for children's to be accurate.
			super.lockTransform(lock);
			for (int i = 0; i < this.size; i++) {
				this.children[i].lockTransform(lock);
			}
		}
	}
	
	/** Override update(...) to be more efficient with updating bounds and transforms.  
	 * Updates this group's transform, then updates each child, then updates this group's bounds.
	 * Still obeys transform and bounds locks. */
	@Override
	public void update(boolean initiator) {
		if (!this.isTransformLocked())
			this.updateTransform(!initiator);
		for (int i = 0; i < this.size; i++)
			this.children[i].update(false);
		if (!this.isBoundsLocked())
			this.updateBounds();
	}

	/** Override visit(...) to visit children if super.visit() didn't fail. */
	@Override
	public VisitResult visit(RenderQueue renderQueue, View view, VisitResult parentResult) {
		VisitResult sp = super.visit(renderQueue, view, parentResult);
		if (sp == VisitResult.FAIL)
			return VisitResult.FAIL;
		
		int planeState = view.getPlaneState();
		
		for (int i = 0; i < this.size; i++) {
			this.children[i].visit(renderQueue, view, sp);
			view.setPlaneState(planeState);
		}
		
		return sp;
	}
	
	private void allocateChildren(int size) {
		Node[] temp = new Node[size];
		if (this.children != null)
			System.arraycopy(this.children, 0, temp, 0, Math.min(this.children.length, size));
		this.children = temp;
	}
	
	/** Make sure that this Group can hold at least size number of children, before having to re-initialize
	 * its internal data structures. size must be at least 1. */
	public void ensureCapacity(int size) throws SceneException {
		if (size <= 0)
			throw new SceneException("Must ensure capacity with a positive size: " + size);
		if (this.children == null)
			this.allocateChildren(size);
		else if (size > this.children.length) {
			this.allocateChildren((size - this.children.length > ALLOCATION_INCREMENT ? size : size + ALLOCATION_INCREMENT));
		} 
	}
	
	/** Get the number of immediate children for this Group. */
	public int getNumChildNodes() {
		return this.size;
	}
	
	/** Adds a Node to this Group.  This method does not update elem, so it will have stale caches
	 * for its world transform and world bounds until another update() is called.  This method fails if elem has a non-null
	 * parent that's not this Group.  It's a no-op if elem is already a child or if elem is null. */
	public void add(Node elem) throws SceneException {
		if (elem == null || elem.parent == this)
			return;
		
		if (elem.parent == null) {
			elem.parent = this;
			if (this.size == this.children.length)
				this.ensureCapacity(this.size + ALLOCATION_INCREMENT);
			this.children[this.size++] = elem;
		} else
			throw new SceneException("Can't add a Node with a non-null parent to another Group");
	}
	
	/** Get the child Node at the given index.  Fails if index is less than 0, or greater than numChildren() - 1. */
	public Node getNodeAtIndex(int index) throws SceneException {
		if (index >= 0 && index < this.size)
			return this.children[index];
		else
			throw new SceneException("Illegal index for child access: " + index + " expected [0-" + this.size + "]");
	}
	
	/** Determine the index of the given Node instance, such that getNodeAtIndex(getIndexOfNode(node)) == node is true.
	 * Returns -1 if this Group doesn't own the Node or if the Node is null. */
	public int getIndexOfNode(Node node) {
		if (node == null || !this.owns(node))
			return -1;
		for (int i = 0; i < this.size; i++)
			if (this.children[i] == node)
				return i;
		return -1;
	}
	
	/** Whether or not Node is somewhere underneath this Group in a scene hierarchy. Returns false if elem is null. */
	public boolean contains(Node elem) {
		if (elem == null)
			return false;
		Group parent = elem.parent;
		while (parent != null) {
			if (parent == this)
				return true;
			parent = parent.parent;
		}
			
		return false;
	}
	
	/** Whether or not Node is an immediate child of this Group.  Returns false if elem is null. */
	public boolean owns(Node elem) {
		if (elem == null)
			return false;
		
		return elem.parent == this;
	}
	
	/** Removes the given Node from this Group.  If elem isn't owned by this Group, then false is returned, otherwise
	 * elem is successfully removed and true is returned.  It's a no-op is elem is null.*/
	public boolean remove(Node elem) {
		if (this.owns(elem)) {
			int index = this.getIndexOfNode(elem);
			this.remove(index);
			return true;
		}
		
		return false;
	}
	
	/** Removes the node at the given index, adjusting the other node's indices afterwards.  Fails if index < 0
	 * or if index >= getNumChildNodes(). Returns the Node that was removed. */
	public Node remove(int index) {
		Node elem = this.getNodeAtIndex(index); // fails if index is bad
		
		System.arraycopy(this.children, index + 1, this.children, index, this.size - index - 1);
		this.size--;
		elem.parent = null;
		
		return elem;
	}
}
