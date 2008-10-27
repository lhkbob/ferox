package com.ferox.core.states;

import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

/**
 * SpatialBranch represents a collection of children in the spatial hierarchy of the scene.  
 * @author Michael Ludwig
 *
 */
public class StateBranch extends StateNode {
	private static final int ALLOCATION_INCREMENT = 8;
	
	protected StateNode[] children;
	protected int size;
	
	/**
	 * Creates a StateBranch with no parent.
	 */
	public StateBranch() {
		super();
		this.ensureCapacity(ALLOCATION_INCREMENT);
	}
	
	/**
	 * Creates a StateBranch with the given parent.
	 */
	public StateBranch(StateBranch parent, int capacity) {
		super(parent);
		this.ensureCapacity(capacity);
	}
	
	private void allocateChildren(int size) {
		StateNode[] temp = new StateNode[size];
		if (this.children != null)
			System.arraycopy(this.children, 0, temp, 0, Math.min(this.children.length, size));
		this.children = temp;
	}
	
	/**
	 * Make sure that the StateBranch can hold at least size number of children, before having to update
	 * its internal structures.
	 */
	public void ensureCapacity(int size) {
		if (this.children == null)
			this.allocateChildren(size);
		else if (size > this.children.length) {
			this.allocateChildren((size - this.children.length > ALLOCATION_INCREMENT ? size : size + ALLOCATION_INCREMENT));
		} 
	}
	
	public void invalidate() {
		super.invalidate();
		for (int i = 0; i < this.size; i++)
			this.children[i].invalidate();
	}
	
	public void submit() {
		super.submit();
		for (int i = 0; i < this.size; i++)
			this.children[i].submit();
	}
	
	protected void setStateTree(StateTree tree) {
		super.setStateTree(tree);
		
		for (int i = 0; i < this.size; i++)
			this.children[i].setStateTree(tree);
	}
	
	/**
	 * Get the number of children directly under this StateBranch
	 */
	public int getNumChildren() {
		return this.size;
	}
	
	/**
	 * Adds an element to this branch.  Throws an exception if elem is null or if
	 * it has another parent already.  Subclasses should not need to override this, instead
	 * use use callbacks with a tree. 
	 */
	public void add(StateNode elem) throws IllegalArgumentException {
		if (elem != null && elem.getParent() == null) {
			elem.setParentReal(this);
			if (this.size == this.children.length)
				this.ensureCapacity(this.size + ALLOCATION_INCREMENT);
			this.children[this.size++] = elem;
			elem.setStateTree(this.getStateTree());
		} else
			throw new IllegalArgumentException("Can't add a null StateNode or a StateNode that already has a parent");
	}
	
	/**
	 * Get the child at the given index.
	 */
	public StateNode getChild(int index) throws ArrayIndexOutOfBoundsException {
		if (index >= 0 && index < this.size)
			return this.children[index];
		else
			throw new ArrayIndexOutOfBoundsException("Illegal index for child access");
	}
	
	/**
	 * Checks to see if elem is contained within this StateBranch or in it's children.
	 */
	public boolean contains(StateNode elem) {
		StateBranch parent = elem.getParent();
		while (parent != null) {
			if (parent == this)
				return true;
			parent = parent.getParent();
		}
			
		return false;
	}
	
	/**
	 * Removes the given StateNode from this StateBranch.  If elem is not a direct child of this
	 * StateBranch but it is still contained under it, elem is removed from elem's immediate parent.
	 * Subclasses should not need to override this method, instead implement removeElement().
	 */
	public void remove(StateNode elem) {
		if (elem != null && this.contains(elem)) {
			StateBranch parent = elem.getParent();
			int index = -1;
			for (index = 0; index < this.size; index++) {
				if (parent.children[index] == elem)
					break;
			}
			System.arraycopy(parent.children, index + 1, parent.children, index, parent.size - index - 1);
			this.size--;
			elem.setParentReal(null);
			elem.setStateTree(null);
		}
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		Chunkable[] children = new Chunkable[this.size];
		System.arraycopy(this.children, 0, children, 0, this.size);
		out.set("children", children);
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		Chunkable[] children = in.getChunkArray("children");
		this.ensureCapacity(children.length);
		
		for (int i = 0; i < children.length; i++)
			this.add((StateNode)children[i]);
	}
}
