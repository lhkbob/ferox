package com.ferox.scene;

import java.util.List;

import com.ferox.math.bounds.BoundVolume;
import com.ferox.math.bounds.PlaneState;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.View;

/**
 * <p>
 * Group represents a collection of nodes within a scene. The scene tree build
 * of nodes will usually then have Groups being used as the branches
 * </p>
 * <p>
 * There are two notions of possession for a Group: 1. owning a Node -> a Node
 * is an immediate/direct child of the Group. 2. containing a Node -> this Group
 * or one of its children owns the node.
 * </p>
 * <p>
 * Although many of the methods defined here resemble that of a Collection
 * interface, they are strictly based on instance equality (==), not equals().
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Group extends Node {
	private static final int ALLOCATION_INCREMENT = 8;

	private Node[] children;
	private int size;

	/** Creates a Group with an initial capacity of 8. */
	public Group() {
		this(ALLOCATION_INCREMENT);
	}

	/**
	 * Creates a Group with the given capacity. The capacity is the number of
	 * children that it can own before reallocating internal storage.
	 * 
	 * @param capacity The size of the internal arrays
	 * @throws IllegalArgumentException if capacity < 1
	 */
	public Group(int capacity) {
		super();
		ensureCapacity(capacity);
	}

	/**
	 * Clones this group's first child's bounds and then encloses each remaining
	 * cached world bound of its children. To get correct results, its children
	 * must be updated first.
	 */
	@Override
	public void updateBounds() {
		BoundVolume world = null;
		for (int i = 0; i < size; i++)
			if (children[i].worldBounds != null)
				if (world == null)
					world = children[i].worldBounds.clone(worldBounds);
				else
					world.enclose(children[i].worldBounds);
		worldBounds = world;
	}

	@Override
	protected void updateTransformAndBounds(boolean initiator) {
		updateTransform(!initiator);
		for (int i = 0; i < size; i++)
			children[i].updateTransformAndBounds(false);
		updateBounds();
	}

	@Override
	protected void prepareLightsAndFog(List<LightNode<?>> lights, List<FogNode> fogs) {
		super.prepareLightsAndFog(lights, fogs);
		for (int i = 0; i < size; i++)
			children[i].prepareLightsAndFog(lights, fogs);
	}

	@Override
	protected void updateLight(LightNode<?> light) {
		// just visit children, if this light intersects our bounds
		if (light.worldBounds == null || worldBounds == null 
			|| worldBounds.intersects(light.worldBounds))
			for (int i = 0; i < size; i++)
				children[i].updateLight(light);
	}

	@Override
	protected void updateFog(FogNode fog) {
		// just visit children, if this fog intersects our bounds
		if (fog.worldBounds == null || worldBounds == null 
			|| worldBounds.intersects(fog.worldBounds))
			for (int i = 0; i < size; i++)
				children[i].updateFog(fog);
	}

	@Override
	public VisitResult visit(RenderQueue renderQueue, View view, PlaneState planeState, VisitResult parentResult) {
		VisitResult sp = super.visit(renderQueue, view, planeState, parentResult);
		if (sp == VisitResult.FAIL)
			return VisitResult.FAIL;

		int planeBits = (planeState != null ? planeState.get() : 0);

		for (int i = 0; i < size; i++) {
			children[i].visit(renderQueue, view, planeState, sp);
			if (planeState != null)
				planeState.set(planeBits);
		}

		return sp;
	}

	private void allocateChildren(int size) {
		Node[] temp = new Node[size];
		if (children != null)
			System.arraycopy(children, 0, temp, 0, Math.min(children.length, size));
		children = temp;
	}

	/**
	 * Make sure that this Group can hold at least size number of children,
	 * before having to re-initialize its internal data structures. This does
	 * nothing if size is less than the Group's internal capacity.
	 * 
	 * @param size The new size of the Group
	 * @throws IllegalArgumentException if size < 1
	 */
	public void ensureCapacity(int size) {
		if (size < 1)
			throw new IllegalArgumentException("Must ensure capacity with a positive size: " + size);
		if (children == null)
			allocateChildren(size);
		else if (size > children.length)
			allocateChildren((size - children.length > ALLOCATION_INCREMENT ? size 
																			: size + ALLOCATION_INCREMENT));
	}

	/**
	 * Get the number of immediate children for this Group.
	 * 
	 * @return The number of children directly owned by this Group
	 */
	public int getNumChildren() {
		return size;
	}

	/**
	 * Adds a Node to this Group. This method does not update elem, so it will
	 * have stale caches for its world transform and world bounds until update()
	 * is called for this Group. It's a no-op if elem is already a child or if
	 * elem is null.
	 * 
	 * @param elem The Node to add as a child to this Group
	 * @throws IllegalArgumentException if elem has a non-null parent that's not
	 *             this Group
	 */
	public void add(Node elem) {
		if (elem == null || elem.parent == this)
			return;

		if (elem.parent == null) {
			elem.parent = this;
			if (size == children.length)
				ensureCapacity(size + ALLOCATION_INCREMENT);
			children[size++] = elem;
		} else
			throw new IllegalArgumentException("Can't add a Node with a non-null parent to another Group");
	}

	/**
	 * Get the child Node at the given index.
	 * 
	 * @param index The index to access, must be in [0, getNumChildren() - 1]
	 * @return The child at the given index
	 * @throws IndexOutOfBoundsException if index is < 0 or >= getNumChildren()
	 */
	public Node getChild(int index) {
		if (index >= 0 && index < size)
			return children[index];
		else
			throw new IndexOutOfBoundsException("Illegal index for child access: " + index + 
												" expected [0-" + size + "]");
	}

	/**
	 * <p>
	 * Find the index of the given Node instance, such that
	 * getNodeAtIndex(getIndexOfNode(node)) == node is true. Returns -1 if this
	 * Group doesn't own the Node or if the Node is null.
	 * </p>
	 * <p>
	 * The returned index may be invalidated if the Group's children are
	 * modified by a call to add() or remove().
	 * </p>
	 * 
	 * @param node The Node to search for in this Group's direct descendents
	 * @return The index such that getChild(index) == node, or -1 if the Node
	 *         isn't found
	 */
	public int indexOf(Node node) {
		if (node == null || !owns(node))
			return -1;
		for (int i = 0; i < size; i++)
			if (children[i] == node)
				return i;
		return -1;
	}

	/**
	 * Whether or not Node is somewhere underneath this Group in a scene
	 * hierarchy. Returns false if elem is null.
	 * 
	 * @param elem The Node to search for
	 * @return True if elem is a descendent of this Group, otherwise false
	 */
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

	/**
	 * Whether or not Node is an immediate child of this Group. Returns false if
	 * elem is null. A Node is owned if its parent is the same instance as this
	 * Group.
	 * 
	 * @param elem The Node to check ownership of
	 * @return True if elem is owned by this Group
	 */
	public boolean owns(Node elem) {
		if (elem == null)
			return false;

		return elem.parent == this;
	}

	/**
	 * Removes the given Node from this Group. If elem isn't owned by this
	 * Group, then false is returned, otherwise elem is successfully removed and
	 * true is returned. It's a no-op is elem is null.
	 * 
	 * @param elem The Node to remove
	 * @return True if elem was successfully removed
	 */
	public boolean remove(Node elem) {
		if (owns(elem)) {
			int index = indexOf(elem);
			this.remove(index);
			return true;
		}

		return false;
	}

	/**
	 * Removes the node at the given index, adjusting the other node's indices
	 * afterwards.
	 * 
	 * @param index The index of the Node that is to be removed
	 * @return The Node originally stored at index
	 * @throws IndexOutOfBoundsException if index < 0 or if index >=
	 *             getNumChildNodes().
	 */
	public Node remove(int index) {
		Node elem = getChild(index); // fails if index is bad

		System.arraycopy(children, index + 1, children, index, size - index - 1);
		size--;
		elem.parent = null;

		return elem;
	}
}
