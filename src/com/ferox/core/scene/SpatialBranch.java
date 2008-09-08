package com.ferox.core.scene;

import com.ferox.core.renderer.RenderAtomBin;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.scene.bounds.BoundingVolume;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

/**
 * SpatialBranch represents a collection of children in the spatial hierarchy of the scene.  
 * @author Michael Ludwig
 *
 */
public class SpatialBranch extends SpatialNode {
	private static final int ALLOCATION_INCREMENT = 8;
	
	protected SpatialNode[] children;
	protected int size;
	
	/**
	 * Creates a SpatialBranch with no parent.
	 */
	public SpatialBranch() {
		super();
		this.ensureCapacity(ALLOCATION_INCREMENT);
	}
	
	/**
	 * Creates a SpatialBranch with the given parent.
	 */
	public SpatialBranch(SpatialBranch parent, int capacity) {
		super(parent);
		this.ensureCapacity(capacity);
	}
	
	@Override
	public void updateBounds() {		
		BoundingVolume world = null;
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
	
	public void lockBounds(boolean lock) {
		if (lock != this.isLocked(SpatialNode.LOCK_BOUNDS_BIT)) {
			for (int i = 0; i < this.size; i++) {
				this.children[i].lockBounds(lock);
			}
			super.lockBounds(lock);
		}
	}
	
	public void lockTransform(boolean lock) {
		if (lock != this.isLocked(SpatialNode.LOCK_TRANSFORM_BIT)) {
			super.lockTransform(lock);
			for (int i = 0; i < this.size; i++) {
				this.children[i].lockTransform(lock);
			}
		}
	}
	
	public void update(boolean initiator) {
		if (!this.isLocked(SpatialNode.LOCK_TRANSFORM_BIT))
			this.updateTransform(!initiator);
		for (int i = 0; i < this.size; i++)
			this.children[i].update(false);
		if (!this.isLocked(SpatialNode.LOCK_BOUNDS_BIT))
			this.updateBounds();
	}
	
	public boolean submit(View view, RenderManager manager, RenderAtomBin queue, boolean initiator) {
		int ps = (view != null ? view.getPlaneState() : 0);
		if (super.submit(view, manager, queue, initiator)) {
			for (int i = 0; i < this.size; i++)
				this.children[i].submit(view, manager, queue, false);
			view.setPlaneState(ps);
			return true;
		}
		view.setPlaneState(ps);
		return false;
	}
	
	private void allocateChildren(int size) {
		SpatialNode[] temp = new SpatialNode[size];
		if (this.children != null)
			System.arraycopy(this.children, 0, temp, 0, Math.min(this.children.length, size));
		this.children = temp;
	}
	
	/**
	 * Make sure that the SpatialBranch can hold at least size number of children, before having to update
	 * its internal structures.
	 */
	public void ensureCapacity(int size) throws IllegalArgumentException {
		if (size <= 0)
			throw new IllegalArgumentException("Must ensure capacity with a positive size: " + size);
		if (this.children == null)
			this.allocateChildren(size);
		else if (size > this.children.length) {
			this.allocateChildren((size - this.children.length > ALLOCATION_INCREMENT ? size : size + ALLOCATION_INCREMENT));
		} 
	}
	
	/**
	 * Get the number of children directly under this SpatialBranch
	 */
	public int getNumChildren() {
		return this.size;
	}
	
	/**
	 * Adds an element to this branch.  Throws an exception if elem is null or if
	 * it has another parent already.  Subclasses should not need to override this, instead
	 * use use callbacks with a tree. 
	 */
	public void add(SpatialNode elem) throws IllegalArgumentException {
		if (elem != null && elem.parent == null) {
			elem.parent = this;
			if (this.size == this.children.length)
				this.ensureCapacity(this.size + ALLOCATION_INCREMENT);
			this.children[this.size++] = elem;
		} else
			throw new IllegalArgumentException("Can't add a null SpatialNode or a SpatialNode that already has a parent");
	}
	
	/**
	 * Get the child at the given index.
	 */
	public SpatialNode getChild(int index) throws ArrayIndexOutOfBoundsException {
		if (index >= 0 && index < this.size)
			return this.children[index];
		else
			throw new ArrayIndexOutOfBoundsException("Illegal index for child access");
	}
	
	/**
	 * Checks to see if elem is contained within this SpatialBranch or in it's children.
	 */
	public boolean contains(SpatialNode elem) {
		if (elem == null)
			return false;
		SpatialBranch parent = elem.parent;
		while (parent != null) {
			if (parent == this)
				return true;
			parent = parent.parent;
		}
			
		return false;
	}
	
	/**
	 * Removes the given SpatialNode from this SpatialBranch.  If elem is not a direct child of this
	 * SpatialBranch but it is still contained under it, elem is removed from elem's immediate parent.
	 * Subclasses should not need to override this method, instead implement removeElement().
	 */
	public void remove(SpatialNode elem) {
		if (this.contains(elem)) {
			SpatialBranch parent = elem.parent;
			int index = -1;
			for (index = 0; index < this.size; index++) {
				if (parent.children[index] == elem)
					break;
			}
			System.arraycopy(parent.children, index + 1, parent.children, index, parent.size - index - 1);
			this.size--;
			elem.parent = null;
		}
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.setInt("numChildren", this.size);
		for (int i = 0; i < this.size; i++) 
			out.setObject("child_" + i, this.children[i]);
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.size = in.getInt("numChildren");
		this.ensureCapacity(this.size);
		
		for (int i = 0; i < this.size; i++)
			this.add((SpatialNode)in.getObject("child_" + i));
	}
}
