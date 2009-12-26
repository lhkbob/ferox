package com.ferox.resource;

/**
 * DirtyState represents an immutable collection of flags signaling a
 * Framework that some or all of a Resource's state is dirty and should be
 * updated.
 * 
 * @author Michael Ludwig
 * @param <T> The actual implementation of DirtyState
 */
public interface DirtyState<T extends DirtyState<T>> {
	/**
	 * <p>
	 * Create a new DirtyState instance of this type that represents the union
	 * of this DirtyState and the specified DirtyState. If state is null, the
	 * returned instance should be an effective clone of this DirtyState.
	 * </p>
	 * <p>
	 * When the ordering of operations is important for merging two DirtyStats,
	 * <tt>state</tt> should be considered the older of the two states.
	 * </p>
	 * 
	 * @param state The other DirtyState in the union
	 * @return A new DirtyState that's a union of this and state
	 */
	public T merge(T state);
}