 package com.ferox.core.renderer;


/**
 * A simple interface for accumulating items, used by RenderAtomBin and StateBin.
 * 
 * @author Michael Ludwig
 *
 */
public interface Bin<T> {
	public int capacity();
	public int itemCount();
	public void clear();
	public void ensureCapacity(int size);
	public void add(T item);
	public void optimize();
}
